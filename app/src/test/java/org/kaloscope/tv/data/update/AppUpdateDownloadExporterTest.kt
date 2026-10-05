package org.kaloscope.tv.data.update

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppUpdateDownloadExporterTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun `legacy download export copies the APK and retains the verified source`() = runBlocking {
        val apk = temporary.newFile("verified.apk").apply { writeText("synthetic-apk") }
        val downloads = File(temporary.root, "Download")

        val name = copyUpdateApkToDirectory(apk, downloads, "kaloscope-tv-1.0.0.apk")

        assertEquals("kaloscope-tv-1.0.0.apk", name)
        assertEquals("synthetic-apk", File(downloads, name).readText())
        assertEquals("synthetic-apk", apk.readText())
        assertEquals(1, downloads.listFiles().orEmpty().size)
    }

    @Test
    fun `export never overwrites an existing file in the public directory`() = runBlocking {
        val apk = temporary.newFile("verified.apk").apply { writeText("new-apk") }
        val downloads = temporary.newFolder("Download")
        val existing = File(downloads, "kaloscope-tv-1.0.0.apk").apply { writeText("user-file") }

        val name = copyUpdateApkToDirectory(apk, downloads, existing.name)

        assertEquals("kaloscope-tv-1.0.0 (1).apk", name)
        assertEquals("new-apk", File(downloads, name).readText())
        assertEquals("user-file", existing.readText())
    }

    @Test
    fun `failed copy removes the temporary download and preserves other files`() = runBlocking {
        val apk = temporary.newFile("empty.apk")
        val downloads = temporary.newFolder("Download")
        val existing = File(downloads, "notes.txt").apply { writeText("user-file") }

        try {
            copyUpdateApkToDirectory(apk, downloads, "kaloscope-tv-1.0.0.apk")
            fail("Empty APK must fail export")
        } catch (_: IOException) {
            assertEquals(listOf(existing), downloads.listFiles().orEmpty().toList())
        }
    }

    @Test
    fun `copy preserves all bytes across buffer boundaries`() = runBlocking {
        val bytes = ByteArray(150 * 1024) { (it % 251).toByte() }
        val apk = temporary.newFile("verified.apk").apply { writeBytes(bytes) }
        val output = ByteArrayOutputStream()

        copyUpdateApk(apk, output)

        assertArrayEquals(bytes, output.toByteArray())
    }

    @Test(timeout = 5_000)
    fun `cancelled copy stops before writing the next buffer`() = runBlocking {
        val apk = temporary.newFile("verified.apk").apply { writeBytes(ByteArray(150 * 1024)) }
        val copyJob = Job(currentCoroutineContext().job)
        var written = 0
        var cancelled = false
        try {
            withContext(copyJob) {
                copyUpdateApk(apk, object : OutputStream() {
                    override fun write(value: Int) = error("Expected buffered writes")

                    override fun write(buffer: ByteArray, offset: Int, length: Int) {
                        written += length
                        copyJob.cancel()
                    }
                })
            }
        } catch (_: CancellationException) {
            cancelled = true
        }

        assertTrue(cancelled)
        assertEquals(64 * 1024, written)
        assertEquals(150 * 1024L, apk.length())
    }
}
