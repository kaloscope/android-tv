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
    fun prototypeTransportIconsUseConsistentRoundedStrokeStyle() {
        prototypeTransportIcons.forEach { resourceName ->
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

        val prototypeTransportIcons = listOf(
            "ic_action_previous",
            "ic_action_seek_backward",
            "ic_action_seek_forward",
            "ic_action_next",
            "ic_action_playback_speed",
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
