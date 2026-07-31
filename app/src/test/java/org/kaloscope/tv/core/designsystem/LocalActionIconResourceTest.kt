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

    private fun assertResourceFrame(
        resourceName: String,
        root: Element,
        document: Document,
    ) {
        assertEquals("$resourceName width", "24dp", root.androidAttribute("width"))
        assertEquals("$resourceName height", "24dp", root.androidAttribute("height"))
        assertEquals("$resourceName viewportWidth", "24", root.androidAttribute("viewportWidth"))
        assertEquals("$resourceName viewportHeight", "24", root.androidAttribute("viewportHeight"))
        assertEquals("$resourceName path count", 1, document.getElementsByTagName("path").length)
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
    }
}
