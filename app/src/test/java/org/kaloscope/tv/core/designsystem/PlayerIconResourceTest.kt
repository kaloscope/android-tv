package org.kaloscope.tv.core.designsystem

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

class PlayerIconResourceTest {
    @Test
    fun convertedTransportIconsPreserveSourceSvgGeometry() {
        convertedTransportPaths.forEach { (resourceName, expectedPath) ->
            val document = parse(resourceName)
            val root = document.documentElement
            val path = document.singlePath()

            assertResourceFrame(resourceName, root)
            assertEquals("$resourceName path", expectedPath, path.androidAttribute("pathData"))
            assertEquals("$resourceName color", "#FFFFFF", path.androidAttribute("fillColor"))
        }
    }

    @Test
    fun chapterNavigationIconsPreserveSourceSvgCutouts() {
        listOf("ic_action_previous", "ic_action_next").forEach { resourceName ->
            val path = parse(resourceName).singlePath()

            assertEquals(
                "$resourceName fill type",
                "evenOdd",
                path.androidAttribute("fillType"),
            )
        }
    }

    @Test
    fun playbackSpeedIconUsesConsistentRoundedStrokeStyle() {
        val resourceName = "ic_action_playback_speed"
        val document = parse(resourceName)
        val root = document.documentElement
        val paths = document.getElementsByTagName("path")

        assertResourceFrame(resourceName, root)
        assertTrue("$resourceName should contain multiple strokes", paths.length >= 2)
        repeat(paths.length) { index ->
            val path = paths.item(index) as Element
            assertTrue(
                "$resourceName path $index should contain geometry",
                path.androidAttribute("pathData").isNotBlank(),
            )
            assertEquals(
                "$resourceName path $index fill",
                "#00000000",
                path.androidAttribute("fillColor"),
            )
            assertEquals(
                "$resourceName path $index stroke",
                "#FFFFFF",
                path.androidAttribute("strokeColor"),
            )
            assertEquals(
                "$resourceName path $index stroke width",
                "1.8",
                path.androidAttribute("strokeWidth"),
            )
            assertEquals(
                "$resourceName path $index line cap",
                "round",
                path.androidAttribute("strokeLineCap"),
            )
            assertEquals(
                "$resourceName path $index line join",
                "round",
                path.androidAttribute("strokeLineJoin"),
            )
        }
    }

    @Test
    fun playPauseIconsPreserveCanonicalGeometry() {
        primaryTransportPaths.forEach { (resourceName, expectedPath) ->
            val document = parse(resourceName)
            val root = document.documentElement
            val path = document.singlePath()

            assertResourceFrame(resourceName, root)
            assertEquals("$resourceName path", expectedPath, path.androidAttribute("pathData"))
            assertEquals("$resourceName color", "#FFFFFF", path.androidAttribute("fillColor"))
        }
    }

    private fun assertResourceFrame(resourceName: String, root: Element) {
        assertEquals("$resourceName width", "24dp", root.androidAttribute("width"))
        assertEquals("$resourceName height", "24dp", root.androidAttribute("height"))
        assertEquals("$resourceName viewportWidth", "24", root.androidAttribute("viewportWidth"))
        assertEquals("$resourceName viewportHeight", "24", root.androidAttribute("viewportHeight"))
    }

    private fun parse(resourceName: String): Document {
        val resourceFile = File(drawableDirectory, "$resourceName.xml")
        assertTrue("Missing ${resourceFile.path}", resourceFile.isFile)
        return DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(resourceFile)
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

        val convertedTransportPaths = mapOf(
            "ic_action_previous" to
                "M21 4.753c0-1.408-1.578-2.24-2.74-1.444L7.763 10.503a1.75 1.75 0 0 0-.01 2.88l10.499 7.302" +
                "c1.16.807 2.749-.024 2.749-1.437zM19.109 4.547a.25.25 0 0 1 .39.206v14.495a.25.25 0 0 1-.392.205" +
                "L8.61 12.152a.25.25 0 0 1 .001-.412zM3 3.75a.75.75 0 0 1 1.5 0v16.5a.75.75 0 0 1-1.5 0z",
            "ic_action_counterclockwise" to
                "M12 4.5a7.5 7.5 0 1 1-7.419 6.392c.067-.454-.265-.892-.724-.892a.75.75 0 0 0-.752.623A9 9 0 1 0 6 5.292" +
                "V4.25a.75.75 0 0 0-1.5 0v3c0 .414.336.75.75.75h3a.75.75 0 0 0 0-1.5H6.9a7.47 7.47 0 0 1 5.1-2",
            "ic_action_clockwise" to
                "M12 4.5a7.5 7.5 0 1 0 7.419 6.392c-.067-.454.265-.892.724-.892c.37 0 .696.256.752.623Q21 11.297 21 12" +
                "a9 9 0 1 1-3-6.708V4.25a.75.75 0 0 1 1.5 0v3a.75.75 0 0 1-.75.75h-3a.75.75 0 0 1 0-1.5h1.35a7.47 7.47 0 0 0-5.1-2",
            "ic_action_next" to
                "M3 4.753c0-1.408 1.578-2.24 2.74-1.444l10.498 7.194a1.75 1.75 0 0 1 .01 2.88L5.749 20.685" +
                "C4.59 21.492 3 20.66 3 19.248zM4.891 4.547a.25.25 0 0 0-.39.206v14.495c0 .202.226.32.392.205l10.498-7.301" +
                "a.25.25 0 0 0-.001-.412zM21 3.75a.75.75 0 0 0-1.5 0v16.5a.75.75 0 0 0 1.5 0z",
        )

        val primaryTransportPaths = mapOf(
            "ic_action_play" to
                "M5 5.274c0-1.707 1.826-2.792 3.325-1.977l12.362 6.727c1.566.852 1.566 3.1 0 3.952L8.325 20.702" +
                "C6.826 21.518 5 20.432 5 18.726z",
            "ic_action_pause" to
                "M5.746 3a1.75 1.75 0 0 0-1.75 1.75v14.5c0 .966.784 1.75 1.75 1.75h3.5a1.75 1.75 0 0 0 1.75-1.75" +
                "V4.75A1.75 1.75 0 0 0 9.246 3zm9 0a1.75 1.75 0 0 0-1.75 1.75v14.5c0 .966.784 1.75 1.75 1.75h3.5" +
                "a1.75 1.75 0 0 0 1.75-1.75V4.75A1.75 1.75 0 0 0 18.246 3z",
        )
    }
}
