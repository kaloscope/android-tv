package org.kaloscope.tv.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.kaloscope.tv.app.bootstrap.BootstrapCoordinator
import org.kaloscope.tv.app.bootstrap.BootstrapRepository
import org.kaloscope.tv.app.bootstrap.BootstrapState
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.data.auth.SessionRepository
import org.kaloscope.tv.data.server.ServerRepository
import org.kaloscope.tv.feature.login.LoginCoordinator
import org.kaloscope.tv.feature.login.LoginState
import org.kaloscope.tv.feature.server.SavedServerDeletionCoordinator
import org.kaloscope.tv.feature.server.SavedServerDeletionState
import org.kaloscope.tv.feature.server.ServerSetupCoordinator
import org.kaloscope.tv.feature.server.ServerSetupState

@HiltViewModel
class KaloscopeViewModel @Inject constructor(
    private val bootstrapRepository: BootstrapRepository,
    private val serverRepository: ServerRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val bootstrapCoordinator = BootstrapCoordinator(bootstrapRepository)
    private val serverCoordinator = ServerSetupCoordinator(
        repository = serverRepository,
        createServerId = { UUID.randomUUID().toString() },
    )
    private val serverDeletionCoordinator = SavedServerDeletionCoordinator(
        serverRepository = serverRepository,
        sessionRepository = sessionRepository,
    )
    private val mutableBootstrapState =
        MutableStateFlow<BootstrapState>(BootstrapState.Loading)
    private val mutableLoginState = MutableStateFlow(LoginState())

    private var loginCoordinator: LoginCoordinator? = null
    private var loginStateJob: Job? = null

    val bootstrapState: StateFlow<BootstrapState> = mutableBootstrapState.asStateFlow()
    val serverSetupState: StateFlow<ServerSetupState> = serverCoordinator.state
    val serverDeletionState: StateFlow<SavedServerDeletionState> =
        serverDeletionCoordinator.state
    val loginState: StateFlow<LoginState> = mutableLoginState.asStateFlow()

    init {
        retryBootstrap()
    }

    fun updateServerName(value: String) = serverCoordinator.updateName(value)

    fun updateServerUrl(value: String) = serverCoordinator.updateUrl(value)

    fun testServerConnection() {
        viewModelScope.launch {
            serverCoordinator.testConnection()
        }
    }

    fun saveServer() {
        viewModelScope.launch {
            serverCoordinator.save()?.let(::showLogin)
        }
    }

    fun showServerSelection() {
        viewModelScope.launch {
            // Drop any password-bearing login state before showing another root screen.
            stopLoginCollection()
            serverDeletionCoordinator.clearError()
            mutableBootstrapState.value = BootstrapState.NeedsServer(
                bootstrapRepository.getServers(),
            )
        }
    }

    fun deleteServer(server: SavedServer) {
        viewModelScope.launch {
            serverDeletionCoordinator.delete(server.id)?.let { remainingServers ->
                mutableBootstrapState.value = BootstrapState.NeedsServer(remainingServers)
            }
        }
    }

    fun clearServerDeletionError() = serverDeletionCoordinator.clearError()

    fun selectServer(server: SavedServer) {
        viewModelScope.launch {
            serverRepository.setActiveServer(server.id)
            // Tokens are isolated by server ID and never reused across origins.
            val token = sessionRepository.getToken(server.id)
            if (token.isNullOrBlank()) {
                showLogin(server)
            } else {
                retryBootstrap()
            }
        }
    }

    fun updateUsername(value: String) {
        loginCoordinator?.updateUsername(value)
    }

    fun updatePassword(value: String) {
        loginCoordinator?.updatePassword(value)
    }

    fun submitLogin() {
        val coordinator = loginCoordinator ?: return
        viewModelScope.launch {
            coordinator.submit()?.let { session ->
                // Replacing the root state removes the entire login subtree from composition.
                mutableBootstrapState.value = BootstrapState.Ready(session)
                stopLoginCollection()
            }
        }
    }

    fun retryBootstrap() {
        viewModelScope.launch {
            mutableBootstrapState.value = BootstrapState.Loading
            val resolved = bootstrapCoordinator.resolve()
            if (resolved is BootstrapState.NeedsLogin) {
                showLogin(resolved.server)
            } else {
                mutableBootstrapState.value = resolved
            }
        }
    }

    fun useDifferentAccount(server: SavedServer) {
        viewModelScope.launch {
            sessionRepository.clearToken(server.id)
            showLogin(server)
        }
    }

    fun logout() {
        val ready = mutableBootstrapState.value as? BootstrapState.Ready ?: return
        useDifferentAccount(ready.session.server)
    }

    fun handleUnauthorized(session: Session) {
        val ready = mutableBootstrapState.value as? BootstrapState.Ready ?: return
        if (ready.session.server.id != session.server.id) {
            return
        }
        useDifferentAccount(session.server)
    }

    private fun showLogin(server: SavedServer) {
        // A coordinator is bound to one server, so switching servers must replace it.
        stopLoginCollection()
        val coordinator = LoginCoordinator(server, sessionRepository)
        loginCoordinator = coordinator
        mutableLoginState.value = coordinator.state.value
        loginStateJob = viewModelScope.launch {
            coordinator.state.collect { mutableLoginState.value = it }
        }
        mutableBootstrapState.value = BootstrapState.NeedsLogin(server)
    }

    private fun stopLoginCollection() {
        loginStateJob?.cancel()
        loginStateJob = null
        loginCoordinator = null
        // Resetting the state also removes any password still held in memory.
        mutableLoginState.value = LoginState()
    }
}
