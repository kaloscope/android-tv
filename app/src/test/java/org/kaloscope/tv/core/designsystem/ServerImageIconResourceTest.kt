package org.kaloscope.tv.core.designsystem

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class ServerImageIconResourceTest {
    @Test
    fun failedImageIconPreservesWebUiGeometry() {
        val resourceFile = File(drawableDirectory, "ic_image_broken.xml")
        assertTrue("Missing ${resourceFile.path}", resourceFile.isFile)
        val document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(resourceFile)
        val root = document.documentElement
        val path = document.getElementsByTagName("path").item(0) as Element

        assertEquals("48dp", root.androidAttribute("width"))
        assertEquals("48dp", root.androidAttribute("height"))
        assertEquals("24", root.androidAttribute("viewportWidth"))
        assertEquals("24", root.androidAttribute("viewportHeight"))
        assertEquals(1, document.getElementsByTagName("path").length)
        assertEquals(webUiImagePath, path.androidAttribute("pathData"))
        assertEquals("#FFFFFF", path.androidAttribute("fillColor"))
        assertEquals("", path.androidAttribute("strokeColor"))
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

        const val webUiImagePath =
            "M17.75 3C19.5449 3 21 4.45507 21 6.25V17.75C21 19.5449 19.5449 21 17.75 21" +
                "H6.25C4.45507 21 3 19.5449 3 17.75V6.25C3 4.45507 4.45507 3 6.25 3H17.75Z" +
                "M18.3305 19.4014L12.5247 13.7148C12.2596 13.4553 11.8501 13.4316 11.5588 13.644" +
                "L11.4752 13.7148L5.66845 19.4011C5.8504 19.4651 6.04613 19.5 6.25 19.5H17.75" +
                "C17.9535 19.5 18.1489 19.4653 18.3305 19.4014L12.5247 13.7148L18.3305 19.4014Z" +
                "M17.75 4.5H6.25C5.2835 4.5 4.5 5.2835 4.5 6.25V17.75C4.5 17.9584 4.53643 18.1583 4.60326 18.3437" +
                "L10.4258 12.643C11.2589 11.8273 12.5675 11.7885 13.4458 12.5266L13.5742 12.6431L19.3964 18.3447" +
                "C19.4634 18.159 19.5 17.9588 19.5 17.75V6.25C19.5 5.2835 18.7165 4.5 17.75 4.5Z" +
                "M15.2521 6.5C16.4959 6.5 17.5042 7.50831 17.5042 8.75212C17.5042 9.99592 16.4959 11.0042 15.2521 11.0042" +
                "C14.0083 11.0042 13 9.99592 13 8.75212C13 7.50831 14.0083 6.5 15.2521 6.5Z" +
                "M15.2521 8C14.8367 8 14.5 8.33673 14.5 8.75212C14.5 9.1675 14.8367 9.50423 15.2521 9.50423" +
                "C15.6675 9.50423 16.0042 9.1675 16.0042 8.75212C16.0042 8.33673 15.6675 8 15.2521 8Z"
    }
}
