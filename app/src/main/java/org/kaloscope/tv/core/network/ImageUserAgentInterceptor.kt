package org.kaloscope.tv.core.network

import okhttp3.Interceptor
import okhttp3.Response

/** Keeps image requests compatible with sites that expect a browser User-Agent. */
internal object ImageUserAgentInterceptor : Interceptor {
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 9) AppleWebKit/537.36 " +
            "Chrome/140.0.0.0 Safari/537.36"

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        // The server image proxy forwards this header to the remote image host.
        val request = if (original.header("User-Agent") == null) {
            original.newBuilder().header("User-Agent", USER_AGENT).build()
        } else {
            original
        }
        return chain.proceed(request)
    }
}
