package org.kaloscope.tv.core.designsystem

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

class LocalNavigationIconResourceTest {
    @Test
    fun allApprovedIconsUseTheWebUiViewport() {
        resourceNames.forEach { resourceName ->
            val root = parse(resourceName).documentElement
            assertEquals("$resourceName width", "24dp", root.androidAttribute("width"))
            assertEquals("$resourceName height", "24dp", root.androidAttribute("height"))
            assertEquals("$resourceName viewportWidth", "24", root.androidAttribute("viewportWidth"))
            assertEquals("$resourceName viewportHeight", "24", root.androidAttribute("viewportHeight"))
        }
    }

    @Test
    fun danmakuIconPreservesEvenOddHoles() {
        val paths = parse("ic_settings_danmaku").paths()

        assertTrue(
            "The visible danmaku path must retain fill-rule=evenodd",
            paths.any { it.androidAttribute("fillType") == "evenOdd" },
        )
    }

    @Test
    fun strokeIconsPreserveRoundCapsAndJoins() {
        val dashboardPaths = parse("ic_nav_home").paths()
        assertTrue(
            "dashboardBar must retain its round bar caps",
            dashboardPaths.any { it.androidAttribute("strokeLineCap") == "round" },
        )

        val televisionPaths = parse("ic_library_tv_show").paths()
        assertTrue(
            "deviceTvOld must retain round caps",
            televisionPaths.any { it.androidAttribute("strokeLineCap") == "round" },
        )
        assertTrue(
            "deviceTvOld must retain round joins",
            televisionPaths.any { it.androidAttribute("strokeLineJoin") == "round" },
        )
    }

    @Test
    fun readingIconPreservesSourceSvgGeometry() {
        val path = parse("ic_settings_reading").singlePath()

        assertEquals("reading icon path", readingIconPath, path.androidAttribute("pathData"))
        assertEquals("reading icon fill", "#00000000", path.androidAttribute("fillColor"))
        assertEquals("reading icon stroke", "#FFFFFF", path.androidAttribute("strokeColor"))
        assertEquals("reading icon stroke width", "1.5", path.androidAttribute("strokeWidth"))
        assertEquals("reading icon line cap", "round", path.androidAttribute("strokeLineCap"))
        assertEquals("reading icon line join", "round", path.androidAttribute("strokeLineJoin"))
    }

    private fun parse(resourceName: String): Document {
        val resourceFile = File(drawableDirectory, "$resourceName.xml")
        assertTrue("Missing ${resourceFile.path}", resourceFile.isFile)
        return DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(resourceFile)
    }

    private fun Document.paths(): List<Element> {
        val nodes = getElementsByTagName("path")
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun Document.singlePath(): Element {
        val paths = getElementsByTagName("path")
        assertEquals("path count", 1, paths.length)
        return paths.item(0) as Element
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

        val resourceNames = listOf(
            "ic_nav_home",
            "ic_nav_home_filled",
            "ic_nav_search",
            "ic_nav_search_filled",
            "ic_nav_library",
            "ic_nav_library_filled",
            "ic_nav_settings",
            "ic_nav_settings_filled",
            "ic_settings_playback",
            "ic_settings_danmaku",
            "ic_settings_subtitle",
            "ic_settings_reading",
            "ic_settings_behavior",
            "ic_settings_server_account",
            "ic_library_movie",
            "ic_library_tv_show",
            "ic_library_unknown",
        )

        val readingIconPath =
            "M2.756 16.358a1.09 1.09 0 0 0 1.154 1.198a16.6 16.6 0 0 1 3.54.338c1.635.2 3.197.794 4.552 1.731" +
                "V6.448A10.16 10.16 0 0 0 7.45 4.694a16.6 16.6 0 0 0-3.605-.316a1.09 1.09 0 0 0-1.09 1.09z" +
                "m18.492 0a1.09 1.09 0 0 1-1.154 1.154a16.6 16.6 0 0 0-3.54.338a10.16 10.16 0 0 0-4.552 1.775V6.448" +
                "a10.16 10.16 0 0 1 4.552-1.754a16.6 16.6 0 0 1 3.605-.316a1.09 1.09 0 0 1 1.089 1.155z" +
                "M5.621 8.234h1.252m-1.252 6.011h1.834M5.78 11.24h3.35"
    }
}
