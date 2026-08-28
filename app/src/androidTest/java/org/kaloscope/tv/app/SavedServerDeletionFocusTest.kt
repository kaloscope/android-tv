package org.kaloscope.tv.app

import android.graphics.Color as AndroidColor
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.core.designsystem.KaloscopeMotion
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.feature.server.SavedServerDeletionState
import org.kaloscope.tv.feature.server.ServerSetupState

class SavedServerDeletionFocusTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rightRevealsOnlyFocusedServerDeleteAction() {
        setScreen()

        composeRule.onNodeWithTag("saved-server-home")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.onNodeWithTag("delete-server-home").assertIsFocused()
        composeRule.onNodeWithTag("delete-server-demo").assertDoesNotExist()

        composeRule.onNodeWithTag("saved-server-home")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("delete-server-home").assertIsFocused()

        composeRule.onNodeWithTag("saved-server-demo")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.onNodeWithTag("delete-server-home").assertDoesNotExist()
        composeRule.onNodeWithTag("delete-server-demo").assertIsFocused()
    }

    @Test
    fun focusedDeleteActionUsesARecognizableTwoDimensionalIcon() {
        setScreen()
        composeRule.onNodeWithTag("saved-server-home")
            .performKeyInput { pressKey(Key.DirectionRight) }

        val bitmap = composeRule.onNodeWithTag("delete-server-home")
            .captureToImage()
            .asAndroidBitmap()
        val lightPixelRows = (0 until bitmap.height).count { y ->
            (0 until bitmap.width).any { x ->
                val pixel = bitmap.getPixel(x, y)
                android.graphics.Color.red(pixel) > 220 &&
                    android.graphics.Color.green(pixel) > 220 &&
                    android.graphics.Color.blue(pixel) > 220
            }
        }

        assertTrue(
            "Icon occupied only $lightPixelRows of ${bitmap.height} rows",
            lightPixelRows > bitmap.height / 4,
        )
    }

    @Test
    fun focusedDeleteActionUsesACircularSurface() {
        setScreen()
        composeRule.onNodeWithTag("saved-server-home")
            .performKeyInput { pressKey(Key.DirectionRight) }

        val bitmap = composeRule.onNodeWithTag("delete-server-home")
            .captureToImage()
            .asAndroidBitmap()
        val foregroundPixels = (0 until bitmap.height).sumOf { y ->
            (0 until bitmap.width).count { x ->
                android.graphics.Color.red(bitmap.getPixel(x, y)) > 70
            }
        }
        val occupiedFraction = foregroundPixels.toFloat() /
            (bitmap.width * bitmap.height)

        assertTrue(
            "Focused surface occupied ${occupiedFraction * 100}% of its bounds",
            occupiedFraction in 0.70f..0.86f,
        )
    }

    @Test
    fun focusedServerAndDeleteActionsHaveTheSameVisualHeight() {
        setScreen()
        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density
        composeRule.mainClock.advanceTimeBy(KaloscopeMotion.FocusMillis.toLong() + 20)

        val focusedServerBounds = composeRule.onNodeWithTag("saved-server-home")
            .assertIsFocused()
            .getUnclippedBoundsInRoot()
        val focusedServerHeight = composeRule.onRoot()
            .captureToImage()
            .asAndroidBitmap()
            .visibleSurfaceHeight(
                bounds = focusedServerBounds,
                density = density,
                surfaceColor = AndroidColor.rgb(0xE8, 0xED, 0xF4),
            )
        composeRule.onNodeWithTag("saved-server-home")
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.mainClock.advanceTimeBy(KaloscopeMotion.FocusMillis.toLong() + 20)
        val focusedDeleteBounds = composeRule.onNodeWithTag("delete-server-home")
            .assertIsFocused()
            .getUnclippedBoundsInRoot()
        val focusedDeleteHeight = composeRule.onRoot()
            .captureToImage()
            .asAndroidBitmap()
            .visibleSurfaceHeight(
                bounds = focusedDeleteBounds,
                density = density,
                surfaceColor = AndroidColor.rgb(0x8F, 0x24, 0x37),
            )

        assertEquals(
            "Focused server surface was $focusedServerHeight px high; " +
                "focused delete surface was $focusedDeleteHeight px high",
            focusedServerHeight,
            focusedDeleteHeight,
        )
    }

    @Test
    fun downFromDeleteActionMovesToNextServerAndHidesAction() {
        setScreen()
        composeRule.onNodeWithTag("saved-server-home")
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.onNodeWithTag("delete-server-home")
            .performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.onNodeWithTag("delete-server-home").assertDoesNotExist()
        composeRule.onNodeWithTag("saved-server-demo").assertIsFocused()
    }

    @Test
    fun upFromDeleteActionMovesToPreviousServerAndHidesAction() {
        setScreen()
        composeRule.onNodeWithTag("saved-server-demo")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.onNodeWithTag("delete-server-demo")
            .performKeyInput { pressKey(Key.DirectionUp) }

        composeRule.onNodeWithTag("delete-server-demo").assertDoesNotExist()
        composeRule.onNodeWithTag("saved-server-home").assertIsFocused()
    }

    @Test
    fun upFromFirstDeleteActionKeepsActionVisibleAndFocused() {
        setScreen()
        composeRule.onNodeWithTag("saved-server-home")
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.onNodeWithTag("delete-server-home")
            .performKeyInput { pressKey(Key.DirectionUp) }

        composeRule.onNodeWithTag("delete-server-home").assertIsFocused()
    }

    @Test
    fun downFromLastDeleteActionMovesToServerNameAndHidesAction() {
        setScreen()
        composeRule.onNodeWithTag("saved-server-demo")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.onNodeWithTag("delete-server-demo")
            .performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.onNodeWithTag("delete-server-demo").assertDoesNotExist()
        composeRule.onNodeWithTag("server-name-selector").assertIsFocused()
    }

    @Test
    fun longServerIdentityKeepsTheSameSingleLineRowHeight() {
        val servers = mutableStateOf(
            listOf(
                SavedServer("short", "家庭服务器", "https://home.example"),
                SavedServer(
                    "long",
                    "这是一个明显超过可用宽度的超长服务器名称",
                    "https://a-very-long-server-origin-that-must-not-wrap.example.invalid",
                ),
            ),
        )
        setScreen(servers = servers)

        val shortBounds = composeRule.onNodeWithTag("saved-server-short")
            .getUnclippedBoundsInRoot()
        val longBounds = composeRule.onNodeWithTag("saved-server-long")
            .getUnclippedBoundsInRoot()
        val shortHeight = (shortBounds.bottom - shortBounds.top).value
        val longHeight = (longBounds.bottom - longBounds.top).value

        assertEquals(shortHeight, longHeight, 1f)
    }

    @Test
    fun leftHidesDeleteActionAndRestoresServerFocus() {
        setScreen()

        composeRule.onNodeWithTag("saved-server-home")
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("delete-server-home")
            .performKeyInput { pressKey(Key.DirectionLeft) }

        composeRule.onNodeWithTag("delete-server-home").assertDoesNotExist()
        composeRule.onNodeWithTag("saved-server-home").assertIsFocused()
    }

    @Test
    fun backHidesDeleteActionBeforeLeavingScreen() {
        setScreen()

        composeRule.onNodeWithTag("saved-server-home")
            .performKeyInput { pressKey(Key.DirectionRight) }
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("delete-server-home").assertDoesNotExist()
        composeRule.onNodeWithTag("saved-server-home").assertIsFocused()
    }

    @Test
    fun cancelingDialogReturnsFocusToVisibleDeleteAction() {
        setScreen()

        composeRule.onNodeWithTag("saved-server-home")
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("delete-server-home")
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("confirm-dialog-cancel")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("kaloscope-confirm-dialog").assertDoesNotExist()
        composeRule.onNodeWithTag("delete-server-home").assertIsFocused()
    }

    @Test
    fun dialogBackReturnsFocusToVisibleDeleteAction() {
        setScreen()
        openDeleteDialog("home")
        composeRule.onNodeWithTag("confirm-dialog-cancel").assertIsFocused()

        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("kaloscope-confirm-dialog").assertDoesNotExist()
        composeRule.onNodeWithTag("delete-server-home").assertIsFocused()
    }

    @Test
    fun deletingMiddleServerFocusesNextRow() {
        val servers = mutableStateOf(
            listOf(
                SavedServer("home", "家庭服务器", "https://home.example"),
                SavedServer("office", "办公室", "https://office.example"),
                SavedServer("demo", "演示", "https://demo.example"),
            ),
        )
        val deletionState = mutableStateOf<SavedServerDeletionState>(
            SavedServerDeletionState.Idle,
        )
        setScreen(
            servers = servers,
            deletionState = deletionState,
            onDeleteServer = { server ->
                deletionState.value = SavedServerDeletionState.Deleting(server.id)
                servers.value = servers.value.filterNot { it.id == server.id }
                deletionState.value = SavedServerDeletionState.Idle
            },
        )

        openDeleteDialog("office")
        composeRule.onNodeWithTag("confirm-dialog-cancel")
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("confirm-dialog-confirm")
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("saved-server-demo").assertIsFocused()
    }

    @Test
    fun deletingLastServerFocusesPreviousRow() {
        val servers = mutableStateOf(savedServers())
        setScreen(
            servers = servers,
            onDeleteServer = { server ->
                servers.value = servers.value.filterNot { it.id == server.id }
            },
        )

        openDeleteDialog("demo")
        composeRule.onNodeWithTag("confirm-dialog-cancel")
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("confirm-dialog-confirm")
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("saved-server-home").assertIsFocused()
    }

    @Test
    fun deletingOnlyServerFocusesServerName() {
        val servers = mutableStateOf(
            listOf(SavedServer("home", "家庭服务器", "https://home.example")),
        )
        setScreen(
            servers = servers,
            onDeleteServer = { servers.value = emptyList() },
        )

        openDeleteDialog("home")
        composeRule.onNodeWithTag("confirm-dialog-cancel")
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("confirm-dialog-confirm")
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("server-name-selector").assertIsFocused()
    }

    @Test
    fun deletionFailureKeepsDialogAndShowsRetryableError() {
        val deletionState = mutableStateOf<SavedServerDeletionState>(
            SavedServerDeletionState.Idle,
        )
        setScreen(
            deletionState = deletionState,
            onDeleteServer = { server ->
                deletionState.value = SavedServerDeletionState.Failed(server.id)
            },
        )

        openDeleteDialog("home")
        composeRule.onNodeWithTag("confirm-dialog-cancel")
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("confirm-dialog-confirm")
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("kaloscope-confirm-dialog").assertExists()
        composeRule.onNodeWithText("无法删除服务器，请重试。").assertExists()
        composeRule.onNodeWithTag("confirm-dialog-confirm").assertIsFocused()
    }

    @Test
    fun deletingServerKeepsTheConfirmLabelStable() {
        val deletionState = mutableStateOf<SavedServerDeletionState>(
            SavedServerDeletionState.Idle,
        )
        setScreen(
            deletionState = deletionState,
            onDeleteServer = { server ->
                deletionState.value = SavedServerDeletionState.Deleting(server.id)
            },
        )

        openDeleteDialog("home")
        composeRule.onNodeWithTag("confirm-dialog-cancel")
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("confirm-dialog-confirm")
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithText("删除").assertExists()
        composeRule.onNodeWithText("正在删除…").assertDoesNotExist()
    }

    private fun openDeleteDialog(serverId: String) {
        composeRule.onNodeWithTag("saved-server-$serverId")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("delete-server-$serverId")
            .performKeyInput { pressKey(Key.Enter) }
    }

    private fun setScreen(
        servers: androidx.compose.runtime.MutableState<List<SavedServer>> =
            mutableStateOf(savedServers()),
        deletionState: androidx.compose.runtime.MutableState<SavedServerDeletionState> =
            mutableStateOf(SavedServerDeletionState.Idle),
        onDeleteServer: (SavedServer) -> Unit = {},
    ) {
        composeRule.setContent {
            KaloscopeTheme {
                ServerSetupScreen(
                    savedServers = servers.value,
                    state = ServerSetupState(),
                    deletionState = deletionState.value,
                    onNameChange = {},
                    onUrlChange = {},
                    onTest = {},
                    onSave = {},
                    onSelectServer = {},
                    onDeleteServer = onDeleteServer,
                    onClearDeletionError = {
                        deletionState.value = SavedServerDeletionState.Idle
                    },
                )
            }
        }
    }

    private fun savedServers() = listOf(
        SavedServer("home", "家庭服务器", "https://home.example"),
        SavedServer("demo", "演示", "https://demo.example"),
    )
}

private fun android.graphics.Bitmap.visibleSurfaceHeight(
    bounds: androidx.compose.ui.unit.DpRect,
    density: Float,
    surfaceColor: Int,
): Int {
    val margin = (8 * density).roundToInt()
    val left = (bounds.left.value * density).roundToInt()
        .minus(margin)
        .coerceAtLeast(0)
    val right = (bounds.right.value * density).roundToInt()
        .plus(margin)
        .coerceAtMost(width - 1)
    val top = (bounds.top.value * density).roundToInt()
        .minus(margin)
        .coerceAtLeast(0)
    val bottom = (bounds.bottom.value * density).roundToInt()
        .plus(margin)
        .coerceAtMost(height - 1)
    return (top..bottom).count { y ->
        (left..right).any { x -> getPixel(x, y) == surfaceColor }
    }
}
