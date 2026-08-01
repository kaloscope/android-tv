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
    fun transportIconsPreserveCanonicalWebUiGeometry() {
        transportPaths.forEach { (resourceName, expectedPath) ->
            val document = parse(resourceName)
            val root = document.documentElement
            val path = document.singlePath()

            assertResourceFrame(resourceName, root)
            assertEquals("$resourceName path", expectedPath, path.androidAttribute("pathData"))
            assertEquals("$resourceName color", "#FFFFFF", path.androidAttribute("fillColor"))
        }
    }

    @Test
    fun forwardSeekMirrorsTheCanonicalWebUiResetIcon() {
        val document = parse("ic_action_seek_forward")
        val group = document.getElementsByTagName("group").item(0) as Element

        assertEquals("12", group.androidAttribute("pivotX"))
        assertEquals("-1", group.androidAttribute("scaleX"))
    }

    @Test
    fun playbackSpeedUsesFluentGaugeGeometry() {
        val document = parse("ic_action_playback_speed")
        val root = document.documentElement
        val path = document.singlePath()

        assertResourceFrame("ic_action_playback_speed", root)
        assertEquals(fluentGaugePath, path.androidAttribute("pathData"))
        assertEquals("#FFFFFF", path.androidAttribute("fillColor"))
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

        val transportPaths = mapOf(
            "ic_action_previous" to
                "M6 3a1 1 0 0 0-.993.883L5 4v16a1 1 0 0 0 1.993.117L7 20V4a1 1 0 0 0-1-1" +
                "m12.707.293a1 1 0 0 0-1.32-.083l-.094.083l-8 8a1 1 0 0 0-.083 1.32l.083.094l8 8" +
                "a1 1 0 0 0 1.497-1.32l-.083-.094L11.414 12l7.293-7.293a1 1 0 0 0 0-1.414",
            "ic_action_seek_backward" to
                "M6.78 2.72a.75.75 0 0 1 0 1.06L4.56 6h8.69a7.75 7.75 0 1 1-7.75 7.75a.75.75 0 0 1 1.5 0" +
                "a6.25 6.25 0 1 0 6.25-6.25H4.56l2.22 2.22a.75.75 0 1 1-1.06 1.06l-3.5-3.5a.75.75 0 0 1 0-1.06" +
                "l3.5-3.5a.75.75 0 0 1 1.06 0",
            "ic_action_play" to
                "M5 5.274c0-1.707 1.826-2.792 3.325-1.977l12.362 6.727c1.566.852 1.566 3.1 0 3.952L8.325 20.702" +
                "C6.826 21.518 5 20.432 5 18.726z",
            "ic_action_pause" to
                "M5.746 3a1.75 1.75 0 0 0-1.75 1.75v14.5c0 .966.784 1.75 1.75 1.75h3.5a1.75 1.75 0 0 0 1.75-1.75" +
                "V4.75A1.75 1.75 0 0 0 9.246 3zm9 0a1.75 1.75 0 0 0-1.75 1.75v14.5c0 .966.784 1.75 1.75 1.75h3.5" +
                "a1.75 1.75 0 0 0 1.75-1.75V4.75A1.75 1.75 0 0 0 18.246 3z",
            "ic_action_seek_forward" to
                "M6.78 2.72a.75.75 0 0 1 0 1.06L4.56 6h8.69a7.75 7.75 0 1 1-7.75 7.75a.75.75 0 0 1 1.5 0" +
                "a6.25 6.25 0 1 0 6.25-6.25H4.56l2.22 2.22a.75.75 0 1 1-1.06 1.06l-3.5-3.5a.75.75 0 0 1 0-1.06" +
                "l3.5-3.5a.75.75 0 0 1 1.06 0",
            "ic_action_next" to
                "M18 3a1 1 0 0 1 .993.883L19 4v16a1 1 0 0 1-1.993.117L17 20V4a1 1 0 0 1 1-1" +
                "m-12.707.293a1 1 0 0 1 1.32-.083l.094.083l8 8a1 1 0 0 1 .083 1.32l-.083.094l-8 8" +
                "a1 1 0 0 1-1.497-1.32l.083-.094L12.586 12L5.293 4.707a1 1 0 0 1 0-1.414",
        )

        const val fluentGaugePath =
            "M7.93413 16.0659C8.22703 16.3588 8.22703 16.8336 7.93413 17.1265C7.64124 17.4194 7.16637 " +
                "17.4194 6.87347 17.1265C4.04217 14.2952 4.04217 9.70478 6.87347 6.87348C8.71833 5.02862 11.3099 " +
                "4.38674 13.6723 4.94459C14.0755 5.03978 14.3251 5.44375 14.2299 5.84687C14.1347 6.25 13.7308 " +
                "6.49963 13.3276 6.40444C11.45 5.96106 9.39622 6.47205 7.93413 7.93414C5.68862 10.1797 5.68862 " +
                "13.8203 7.93413 16.0659ZM17.8879 9.1415C18.2789 9.00477 18.7067 9.21089 18.8435 9.60189C19.7333 " +
                "12.1463 19.1624 15.0907 17.1265 17.1265C16.8336 17.4194 16.3588 17.4194 16.0659 17.1265C15.773 " +
                "16.8336 15.773 16.3588 16.0659 16.0659C17.6791 14.4526 18.1344 12.1183 17.4276 10.097C17.2908 " +
                "9.70604 17.4969 9.27824 17.8879 9.1415ZM15.8791 6.66732C16.1062 6.47297 16.439 6.46653 16.6734 " +
                "6.65195C16.9078 6.83738 16.9781 7.16278 16.8412 7.42842L16.7119 7.67862C16.6295 7.83801 16.5113 " +
                "8.06624 16.3681 8.34179C16.0818 8.89278 15.6954 9.63339 15.2955 10.3912C14.8959 11.1485 14.4815 " +
                "11.9253 14.1395 12.5479C13.9686 12.8589 13.8142 13.1344 13.6879 13.3509C13.5703 13.5524 13.4548 " +
                "13.7421 13.3688 13.8508C12.7263 14.6629 11.5471 14.8004 10.735 14.1579C9.92288 13.5154 9.78538 " +
                "12.3362 10.4279 11.5241C10.5139 11.4154 10.672 11.2593 10.8409 11.0986C11.0226 10.9258 11.2552 " +
                "10.7121 11.5185 10.4744C12.0457 9.9983 12.7063 9.41631 13.3514 8.85315C13.9969 8.28961 14.6288 " +
                "7.74321 15.0991 7.33783C15.3343 7.1351 15.5292 6.96755 15.6654 6.85065L15.8791 6.66732Z" +
                "M22 12C22 17.5228 17.5228 22 12 22C6.47715 22 2 17.5228 2 12C2 6.47715 6.47715 2 12 2" +
                "C17.5228 2 22 6.47715 22 12ZM3.5 12C3.5 16.6944 7.30558 20.5 12 20.5C16.6944 20.5 20.5 " +
                "16.6944 20.5 12C20.5 7.30558 16.6944 3.5 12 3.5C7.30558 3.5 3.5 7.30558 3.5 12Z"
    }
}
