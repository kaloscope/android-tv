package org.kaloscope.tv.data.reader

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.*
import org.kaloscope.tv.core.network.ApiClientFactory
import org.kaloscope.tv.core.reader.ReaderRequest

class LocalReaderRepositoryTest {
    private val server = MockWebServer()
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val repository = LocalReaderRepository(ApiClientFactory(json), json)
    private val version = "a".repeat(64)
    private val chapter = "b".repeat(32)

    @Before fun start() = server.start()
    @After fun stop() = server.shutdown()

    @Test fun `TXT uses content endpoint without image pagination and retains paragraphs`() = runTest {
        enqueueHistory()
        enqueueFixture("txt")
        val content = success(repository.open(session(), request())) as ReaderTextContent
        assertEquals(3, content.paragraphs.size)
        assertEquals("第二段，含有\n换行。", content.paragraphs[1])
        assertEquals(2, content.chapters[1].part)
        assertEquals("测试作品", content.title)
        val history = server.takeRequest()
        assertEquals("text", history.requestUrl?.queryParameter("rel_type"))
        assertEquals("101", history.requestUrl?.queryParameter("rel_id"))
        val body = server.takeRequest()
        assertEquals("/_api/media/101/content", body.path)
        assertEquals("Token fixture-token", body.getHeader("Authorization"))
    }

    @Test fun `EPUB preserves text styles lists images and missing image placeholders`() = runTest {
        enqueueHistory()
        enqueueFixture("epub")
        val content = success(repository.open(session(), request())) as ReaderTextContent
        assertEquals(6, content.blocks.size)
        assertEquals(1, (content.blocks[0] as ReaderTextBlock.Text).headingLevel)
        assertTrue((content.blocks[1] as ReaderTextBlock.Text).runs[0].strong)
        assertTrue((content.blocks[1] as ReaderTextBlock.Text).runs[1].emphasis)
        assertTrue((content.blocks[2] as ReaderTextBlock.Text).quote)
        assertEquals(3, (content.blocks[3] as ReaderTextBlock.ListBlock).start)
        assertNotNull((content.blocks[4] as ReaderTextBlock.Image).url)
        assertNull((content.blocks[5] as ReaderTextBlock.Image).url)
    }

    @Test fun `comic appends bounded versioned pages and does not use indexers`() = runTest {
        enqueueHistory()
        enqueueFixture("comic")
        val content = success(repository.open(session(), request(MediaLibraryType.Comic))) as ReaderImageContent
        server.takeRequest()
        server.takeRequest()
        server.enqueue(response(comicPage(20, 20)))
        val page = success(repository.imagePage(session(), content, previous = false))
        assertEquals(20, page.images.size)
        assertEquals(40, page.nextOffset)
        val url = server.takeRequest().requestUrl!!
        assertEquals("/_api/media/201/content", url.encodedPath)
        assertEquals("item:202", url.queryParameter("chapter_id"))
        assertEquals(version, url.queryParameter("version"))
        assertEquals("20", url.queryParameter("offset"))
        assertEquals("20", url.queryParameter("limit"))
    }

    @Test fun `comic resumes by page ID and can fetch preceding pages`() = runTest {
        val pageId = 31.toString(16).padStart(32, '0')
        enqueueHistory("""{"rel_type":"image","rel_id":201,"percentage":67,"locator":{"version":"$version","chapter_item_id":202,"page_id":"$pageId","offset":0.4}}""")
        server.enqueue(response(comicPage(30, 15)))
        val content = success(repository.open(session(), request(MediaLibraryType.Comic))) as ReaderImageContent
        assertEquals(30, content.imageOffset)
        assertEquals(0.4, content.initialPosition.offset, 0.0001)
        server.takeRequest()
        val url = server.takeRequest().requestUrl!!
        assertEquals(pageId, url.queryParameter("page_id"))
        assertNull(url.queryParameter("offset"))
        server.enqueue(response(comicPage(10, 20)))
        val page = success(repository.imagePage(session(), content, previous = true))
        assertEquals(10, page.imageOffset)
        assertEquals("10", server.takeRequest().requestUrl?.queryParameter("offset"))
    }

    @Test fun `novel resumes the matching paragraph and records a text locator`() = runTest {
        enqueueHistory("""{"rel_type":"text","rel_id":101,"percentage":20,"locator":{"version":"$version","chapter_id":"$chapter","paragraph":1,"offset":0.25}}""")
        enqueueFixture("txt")
        val content = success(repository.open(session(), request())) as ReaderTextContent
        assertEquals(ReaderPosition(1, 0.25), content.initialPosition)
        server.takeRequest()
        val url = server.takeRequest().requestUrl!!
        assertEquals(chapter, url.queryParameter("chapter_id"))
        assertNull(url.queryParameter("page_id"))
        server.enqueue(MockResponse().setResponseCode(204))
        assertTrue(repository.recordProgress(session(), content, ReaderPosition(2, 0.5)) is AppResult.Success)
        val record = server.takeRequest()
        assertEquals("/_api/user/history/record", record.path)
        val body = json.parseToJsonElement(record.body.readUtf8()).jsonObject
        assertEquals("\"text\"", body["rel_type"].toString())
        assertFalse(body.containsKey("position"))
        assertEquals("2", body["locator"]?.jsonObject?.get("paragraph").toString())
        assertFalse(body["locator"]!!.jsonObject.containsKey("block_id"))
    }

    @Test fun `EPUB history uses block IDs and comics retain parent ownership`() = runTest {
        enqueueHistory()
        enqueueFixture("epub")
        val epub = success(repository.open(session(), request()))
        val textRecord = epub.readingRecord(ReaderPosition(1, 0.5))!!
        assertEquals("2".repeat(32), textRecord.locator.blockId)
        assertNull(textRecord.locator.paragraph)
        enqueueHistory()
        enqueueFixture("comic")
        val comic = success(repository.open(session(), request(MediaLibraryType.Comic)))
        val imageRecord = comic.readingRecord(ReaderPosition(3, 0.5))!!
        assertEquals(201L, imageRecord.relationId)
        assertEquals(202L, imageRecord.locator.chapterItemId)
        assertEquals(4.toString(16).padStart(32, '0'), imageRecord.locator.pageId)
        assertEquals("image", imageRecord.relationType)
        assertNull(imageRecord.locator.chapterId)
    }

    @Test fun `content change discards stale novel selection and retries only once`() = runTest {
        enqueueHistory("""{"rel_type":"text","rel_id":101,"locator":{"version":"$version","chapter_id":"$chapter","paragraph":1}}""")
        server.enqueue(error(409, "content_changed"))
        enqueueFixture("txt")
        assertTrue(repository.open(session(), request()) is AppResult.Success)
        server.takeRequest()
        server.takeRequest()
        assertEquals("/_api/media/101/content", server.takeRequest().path)
        enqueueHistory()
        server.enqueue(error(409, "content_changed"))
        server.enqueue(error(409, "content_changed"))
        assertEquals("content_changed", ((repository.open(session(), request()) as AppResult.Failure).error as AppError.Api).code)
        assertEquals(6, server.requestCount)
    }

    @Test fun `comic chapter switch omits old chapter version`() = runTest {
        enqueueHistory()
        enqueueFixture("comic")
        val content = success(repository.open(session(), request(MediaLibraryType.Comic)))
        server.takeRequest()
        server.takeRequest()
        server.enqueue(error(422, "content_not_ready"))
        val result = repository.resolveChapter(session(), content, 1)
        assertTrue(result is AppResult.Failure)
        val url = server.takeRequest().requestUrl!!
        assertEquals("item:203", url.queryParameter("chapter_id"))
        assertNull(url.queryParameter("version"))
    }

    @Test fun `novel chapter change refreshes a stale index only once`() = runTest {
        enqueueHistory()
        enqueueFixture("txt")
        val content = success(repository.open(session(), request()))
        server.takeRequest()
        server.takeRequest()
        server.enqueue(error(409, "content_changed"))
        enqueueFixture("txt")
        assertTrue(repository.resolveChapter(session(), content, 1) is AppResult.Success)
        assertEquals(version, server.takeRequest().requestUrl?.queryParameter("version"))
        assertEquals("/_api/media/101/content", server.takeRequest().path)
        assertEquals(4, server.requestCount)
    }

    @Test fun `unauthorized and forbidden remain distinct and stop history resolution`() = runTest {
        for ((status, expected) in listOf(401 to AppError.Unauthorized, 403 to AppError.Forbidden)) {
            server.enqueue(error(status, "denied"))
            assertEquals(expected, (repository.open(session(), request()) as AppResult.Failure).error)
        }
        assertEquals(2, server.requestCount)
    }

    @Test fun `cross origin local assets and mismatched owners are rejected`() = runTest {
        for (body in listOf(
            fixture("comic").replace("/_api/media/202/assets/", "https://foreign.example/assets/"),
            fixture("comic").replace("/_api/media/202/assets/", "/_api/media/202/assets/../other/"),
            fixture("comic").replace("\"item_id\": 201", "\"item_id\": 999"),
            fixture("comic").replace("\"next_offset\": 20", "\"next_offset\": 0"),
            comicPage(0, 0),
        )) {
            enqueueHistory()
            server.enqueue(response(body))
            assertTrue((repository.open(session(), request(MediaLibraryType.Comic)) as AppResult.Failure).error is AppError.InvalidData)
        }
    }

    private fun request(type: MediaLibraryType = MediaLibraryType.Novel) = ReaderRequest.Local(
        "local-reader", "server", "测试作品", if (type == MediaLibraryType.Comic) 201 else 101,
        type, imageSettings = ImageReaderSettings(), textSettings = TextReaderSettings(),
        chapterOrder = ReaderChapterOrder.Ascending,
    )
    private fun session() = Session(SavedServer("server", "Test", server.url("/").toString().trimEnd('/')), "fixture-token", SessionUser(1, "reader", "user"))
    private fun enqueueHistory(item: String = "") = server.enqueue(response("""{"status":200,"data":{"items":[$item]}}"""))
    private fun enqueueFixture(name: String) = server.enqueue(response(fixture(name)))
    private fun fixture(name: String) = checkNotNull(javaClass.getResource("/fixtures/api/local-reading-$name.json")).readText()
    private fun response(body: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(body)
    private fun error(status: Int, code: String) = response("""{"status":$status,"message":"$code"}""").setResponseCode(status)
    private fun <T> success(result: AppResult<T>): T = (result as AppResult.Success).value
    private fun comicPage(offset: Int, size: Int): String {
        val images = (offset + 1..offset + size).joinToString(",") {
            "\"/_api/media/202/assets/${it.toString(16).padStart(32, '0')}?version=$version\""
        }
        val next = if (offset + size < 45) (offset + size).toString() else "null"
        return """{"status":200,"data":{"item_id":201,"source_item_id":202,"title":"第一卷","version":"$version","chapter_id":"item:202","chapters":[{"id":"item:202","title":"第一卷"}],"media_type":"image","format":"cbz","content_type":"images","offset":$offset,"image_count":45,"next_offset":$next,"images":[$images]}}"""
    }
}
