package org.kaloscope.tv.core.designsystem

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

class LocalActionIconResourceTest {
    @Test
    fun searchIconPreservesWebUiGeometry() {
        val document = parse("ic_action_search")
        val root = document.documentElement
        val path = document.singlePath()

        assertResourceFrame("ic_action_search", root, document)
        assertEquals(searchPath, path.androidAttribute("pathData"))
        assertEquals("#FFFFFF", path.androidAttribute("fillColor"))
        assertEquals("", path.androidAttribute("strokeColor"))
    }

    @Test
    fun filterIconPreservesWebUiGeometryAndStroke() {
        val document = parse("ic_action_filter")
        val root = document.documentElement
        val path = document.singlePath()

        assertResourceFrame("ic_action_filter", root, document)
        assertEquals(filterPath, path.androidAttribute("pathData"))
        assertEquals("#00000000", path.androidAttribute("fillColor"))
        assertEquals("#FFFFFF", path.androidAttribute("strokeColor"))
        assertEquals("2", path.androidAttribute("strokeWidth"))
        assertEquals("round", path.androidAttribute("strokeLineCap"))
        assertEquals("round", path.androidAttribute("strokeLineJoin"))
    }

    @Test
    fun infoIconPreservesWebUiGeometry() {
        val document = parse("ic_info")
        val root = document.documentElement
        val path = document.singlePath()

        assertResourceFrame("ic_info", root, document)
        assertEquals(infoPath, path.androidAttribute("pathData"))
        assertEquals("#FFFFFF", path.androidAttribute("fillColor"))
        assertEquals("", path.androidAttribute("strokeColor"))
    }

    @Test
    fun deleteIconPreservesWebUiGeometry() {
        val document = parse("ic_delete")
        val root = document.documentElement
        val paths = document.paths()

        assertResourceFrame(
            resourceName = "ic_delete",
            root = root,
            document = document,
            expectedPathCount = deletePaths.size,
        )
        assertEquals(deletePaths, paths.map { it.androidAttribute("pathData") })
        assertEquals(
            listOf("#FFFFFF", "#FFFFFF", "#FFFFFF"),
            paths.map { it.androidAttribute("fillColor") },
        )
        assertEquals(
            listOf("", "", ""),
            paths.map { it.androidAttribute("strokeColor") },
        )
        assertEquals(
            listOf("evenOdd", "", ""),
            paths.map { it.androidAttribute("fillType") },
        )
    }

    private fun assertResourceFrame(
        resourceName: String,
        root: Element,
        document: Document,
        expectedPathCount: Int = 1,
    ) {
        assertEquals("$resourceName width", "24dp", root.androidAttribute("width"))
        assertEquals("$resourceName height", "24dp", root.androidAttribute("height"))
        assertEquals("$resourceName viewportWidth", "24", root.androidAttribute("viewportWidth"))
        assertEquals("$resourceName viewportHeight", "24", root.androidAttribute("viewportHeight"))
        assertEquals(
            "$resourceName path count",
            expectedPathCount,
            document.getElementsByTagName("path").length,
        )
        assertEquals("$resourceName group count", 0, document.getElementsByTagName("group").length)
    }

    private fun parse(resourceName: String): Document {
        val resourceFile = File(drawableDirectory, "$resourceName.xml")
        assertTrue("Missing ${resourceFile.path}", resourceFile.isFile)
        return DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(resourceFile)
    }

    private fun Document.singlePath(): Element =
        getElementsByTagName("path").item(0) as Element

    private fun Document.paths(): List<Element> {
        val nodes = getElementsByTagName("path")
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun Element.androidAttribute(name: String): String =
        getAttributeNS(androidNamespace, name)

    private val drawableDirectory: File = listOf(
        File("src/main/res/drawable"),
        File("app/src/main/res/drawable"),
    ).firstOrNull(File::isDirectory)
        ?: error("Cannot locate app/src/main/res/drawable")

    private companion object {
        const val androidNamespace = "http://schemas.android.com/apk/res/android"

        const val searchPath =
            "M16.102 17.162a8 8 0 1 1 1.06-1.06l4.618 4.618a.75.75 0 1 1-1.06 1.06z" +
                "M17.5 11a6.5 6.5 0 1 0-13 0a6.5 6.5 0 0 0 13 0"

        const val filterPath =
            "M12 6a2 2 0 1 0 4 0a2 2 0 1 0-4 0M4 6h8m4 0h4" +
                "M6 12a2 2 0 1 0 4 0a2 2 0 1 0-4 0m-2 0h2m4 0h10" +
                "m-5 6a2 2 0 1 0 4 0a2 2 0 1 0-4 0M4 18h11m4 0h1"

        const val infoPath =
            "M12.002 1.999c5.523 0 10.001 4.478 10.001 10.002" +
                "c0 5.523-4.478 10.001-10.001 10.001C6.478 22.002 2 17.524 2 12.001" +
                "C2 6.477 6.478 1.999 12.002 1.999m0 1.5a8.502 8.502 0 1 0 0 17.003" +
                "a8.502 8.502 0 0 0 0-17.003M12 10.5a.75.75 0 0 1 .75.75v5" +
                "a.75.75 0 0 1-1.5 0v-5a.75.75 0 0 1 .75-.75M12 9a1 1 0 1 0 0-2" +
                "a1 1 0 0 0 0 2"

        val deletePaths = listOf(
            "M10 5h4a2 2 0 1 0-4 0zM8.5 5a3.5 3.5 0 1 1 7 0h5.75" +
                "a.75.75 0 0 1 0 1.5h-1.32l-1.17 12.111A3.75 3.75 0 0 1 15.026 22" +
                "H8.974a3.75 3.75 0 0 1-3.733-3.389L4.07 6.5H2.75a.75.75 0 0 1 0-1.5z" +
                "M6.734 18.467a2.25 2.25 0 0 0 2.24 2.033h6.052" +
                "a2.25 2.25 0 0 0 2.24-2.033L18.424 6.5H5.576z",
            "M10.5 9.75a.75.75 0 0 0-1.5 0v7.5a.75.75 0 0 0 1.5 0z",
            "M14.25 9a.75.75 0 0 1 .75.75v7.5a.75.75 0 0 1-1.5 0v-7.5" +
                "a.75.75 0 0 1 .75-.75z",
        )
    }
}
