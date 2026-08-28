package org.kaloscope.tv.feature.player

import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Test

class DanmakuPlaybackBindingTest {
    @Test
    fun `transient listener detach preserves synchronization until disposal`() {
        val runtime = RecordingDanmakuRuntime()
        val player = RecordingPlayer()
        val binding = DanmakuPlaybackBinding(
            player = player.instance,
            synchronizer = DanmakuPlaybackSynchronizer(runtime),
        )

        binding.attach()
        runtime.commands.clear()

        binding.detach()
        player.isPlaying = true
        player.currentPosition = 42_500
        player.playbackParameters = PlaybackParameters(1.25f)
        binding.attach()

        assertEquals(1, player.listenerCount)
        assertEquals(
            listOf("speed:1.25", "seek:42500", "start"),
            runtime.commands,
        )

        binding.dispose()
        runtime.commands.clear()
        binding.attach()

        assertEquals(0, player.listenerCount)
        assertEquals(emptyList<String>(), runtime.commands)
    }
}

private class RecordingPlayer {
    private val listeners = linkedSetOf<Player.Listener>()

    var isPlaying: Boolean = false
    var currentPosition: Long = 0
    var playbackParameters: PlaybackParameters = PlaybackParameters.DEFAULT

    val listenerCount: Int
        get() = listeners.size

    val instance: Player = Proxy.newProxyInstance(
        Player::class.java.classLoader,
        arrayOf(Player::class.java),
    ) { _, method, arguments ->
        when (method.name) {
            "addListener" -> {
                listeners += arguments.orEmpty().single() as Player.Listener
                null
            }

            "removeListener" -> {
                listeners -= arguments.orEmpty().single() as Player.Listener
                null
            }

            "isPlaying" -> isPlaying
            "getCurrentPosition" -> currentPosition
            "getPlaybackParameters" -> playbackParameters
            else -> defaultValue(method.returnType)
        }
    } as Player

    private fun defaultValue(returnType: Class<*>): Any? =
        when (returnType) {
            Boolean::class.javaPrimitiveType -> false
            Byte::class.javaPrimitiveType -> 0.toByte()
            Char::class.javaPrimitiveType -> 0.toChar()
            Short::class.javaPrimitiveType -> 0.toShort()
            Int::class.javaPrimitiveType -> 0
            Long::class.javaPrimitiveType -> 0L
            Float::class.javaPrimitiveType -> 0f
            Double::class.javaPrimitiveType -> 0.0
            else -> null
        }
}
