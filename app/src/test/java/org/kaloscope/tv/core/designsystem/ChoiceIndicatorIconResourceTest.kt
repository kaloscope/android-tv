package org.kaloscope.tv.core.designsystem

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class ChoiceIndicatorIconResourceTest {
    @Test
    fun choiceIndicatorUsesDownwardChevronGeometry() {
        val resourceFile = File(drawableDirectory, "ic_choice_expand.xml")
        assertTrue("Missing ${resourceFile.path}", resourceFile.isFile)
        val document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(resourceFile)
        val root = document.documentElement
        val paths = document.getElementsByTagName("path")
        val path = paths.item(0) as Element

        assertEquals("24dp", root.androidAttribute("width"))
        assertEquals("24dp", root.androidAttribute("height"))
        assertEquals("24", root.androidAttribute("viewportWidth"))
        assertEquals("24", root.androidAttribute("viewportHeight"))
        assertEquals(1, paths.length)
        assertEquals("M7 9.5L12 14.5L17 9.5", path.androidAttribute("pathData"))
        assertEquals("#00000000", path.androidAttribute("fillColor"))
        assertEquals("#FFFFFF", path.androidAttribute("strokeColor"))
        assertEquals("1.8", path.androidAttribute("strokeWidth"))
        assertEquals("round", path.androidAttribute("strokeLineCap"))
        assertEquals("round", path.androidAttribute("strokeLineJoin"))
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
    }
}
