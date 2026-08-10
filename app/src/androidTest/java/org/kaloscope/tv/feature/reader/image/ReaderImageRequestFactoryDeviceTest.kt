package org.kaloscope.tv.feature.reader.image

import androidx.test.platform.app.InstrumentationRegistry
import coil3.network.httpHeaders
import org.junit.Assert.assertEquals
import org.junit.Test
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser

class ReaderImageRequestFactoryDeviceTest {

    @Test
    fun coilRequestUsesTheResolvedUrlAndAuthorization() {
        val request = ReaderImageRequestFactory.create(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            session = Session(
                server = SavedServer("server-one", "Test", "https://media.example"),
                token = "token-one",
                user = SessionUser(1, "tv_user", "user"),
            ),
            rawUrl = "https://images.example/page-2.jpg",
        )

        checkNotNull(request)
        assertEquals(
            "https://media.example/_api/image/proxy?store=false&url=" +
                "https%3A%2F%2Fimages.example%2Fpage-2.jpg",
            request.data,
        )
        assertEquals("Token token-one", request.httpHeaders["Authorization"])
    }
}
