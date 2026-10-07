package org.kaloscope.tv.test.golden

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import java.io.ByteArrayOutputStream
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser

internal const val CinematicGoldenArtworkPath = "/cinematic.png"

internal fun withCinematicGoldenArtwork(capture: (Session) -> Unit) {
    val image = cinematicArtworkPng()
    MockWebServer().use { server ->
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (request.path == CinematicGoldenArtworkPath) {
                    MockResponse()
                        .setHeader("Content-Type", "image/png")
                        .setBody(Buffer().write(image))
                } else {
                    MockResponse().setResponseCode(404)
                }
        }
        server.start()
        capture(
            Session(
                server = SavedServer("cinematic-golden", "Golden", server.url("/").toString()),
                token = "fixture",
                user = SessionUser(1, "golden", "user"),
            ),
        )
    }
}

private fun cinematicArtworkPng(): ByteArray {
    // Synthetic high-contrast artwork keeps screenshot checks independent of private media.
    val bitmap = Bitmap.createBitmap(960, 540, Bitmap.Config.ARGB_8888)
    try {
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f, 0f, 960f, 540f,
            intArrayOf(0xFF263C5C.toInt(), 0xFFF1C794.toInt(), 0xFF516C86.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawPaint(paint)
        paint.shader = null
        paint.color = 0xFFFFEBD0.toInt()
        canvas.drawCircle(740f, 185f, 115f, paint)
        paint.color = Color.rgb(47, 65, 78)
        val ridge = Path().apply {
            moveTo(0f, 390f)
            lineTo(240f, 280f)
            lineTo(410f, 380f)
            lineTo(650f, 260f)
            lineTo(960f, 350f)
            lineTo(960f, 540f)
            lineTo(0f, 540f)
            close()
        }
        canvas.drawPath(ridge, paint)
        return ByteArrayOutputStream().use { bytes ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes))
            bytes.toByteArray()
        }
    } finally {
        bitmap.recycle()
    }
}
