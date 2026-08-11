package org.kaloscope.tv.core.designsystem

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

class AdjustmentArrowIconResourceTest {
    @Test
    fun decreaseArrowUsesLeftChevronDrawableGeometry() {
        assertChevron(
            resourceName = "ic_adjustment_decrease",
            expectedPath = "M9.52 4.32L4.48 9L9.52 13.68",
        )
    }

    @Test
    fun increaseArrowUsesRightChevronDrawableGeometry() {
        assertChevron(
            resourceName = "ic_adjustment_increase",
            expectedPath = "M4.48 4.32L9.52 9L4.48 13.68",
        )
    }

    private fun assertChevron(
        resourceName: String,
        expectedPath: String,
    ) {
        val document = parse(resourceName)
        val root = document.documentElement
        val path = document.singlePath()

        assertEquals("14dp", root.androidAttribute("width"))
        assertEquals("18dp", root.androidAttribute("height"))
        assertEquals("14", root.androidAttribute("viewportWidth"))
        assertEquals("18", root.androidAttribute("viewportHeight"))
        assertEquals(1, document.getElementsByTagName("path").length)
        assertEquals(0, document.getElementsByTagName("group").length)
        assertEquals(expectedPath, path.androidAttribute("pathData"))
        assertEquals("#00000000", path.androidAttribute("fillColor"))
        assertEquals("#FFFFFF", path.androidAttribute("strokeColor"))
        assertEquals("1.5", path.androidAttribute("strokeWidth"))
        assertEquals("round", path.androidAttribute("strokeLineCap"))
        assertEquals("round", path.androidAttribute("strokeLineJoin"))
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
    }
}
