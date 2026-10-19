package com.ppo.timetracker.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ppo.domain.model.UserRole
import com.ppo.domain.usecase.GetUserInfoUseCase
import com.ppo.domain.usecase.LoginUserUseCase
import com.ppo.domain.usecase.RegisterUserUseCase
import com.ppo.timetracker.session.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val loginUseCase: LoginUserUseCase,
    private val registerUseCase: RegisterUserUseCase,
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = sessionManager.currentUserIdFlow
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), sessionManager.currentUserId != null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val userName: StateFlow<String?> = sessionManager.currentUserIdFlow
        .flatMapLatest { userId ->
            flow {
                if (userId != null) {
                    emit(getUserInfoUseCase(userId)?.name)
                } else {
                    emit(null)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun login(login: String, pass: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = loginUseCase(login, pass)
            result.onSuccess { user ->
                sessionManager.login(user.id)
                _state.value = AuthState.Success("Добро пожаловать, ${user.name}")
            }.onFailure {
                _state.value = AuthState.Error(it.message ?: "Ошибка входа")
            }
        }
    }

    fun register(login: String, name: String, pass: String, isAdmin: Boolean) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val role = if (isAdmin) UserRole.ADMIN else UserRole.USER
            val result = registerUseCase(login, name, pass, role)
            result.onSuccess {
                _state.value = AuthState.Success("Регистрация успешна. Теперь войдите.")
            }.onFailure {
                _state.value = AuthState.Error(it.message ?: "Ошибка регистрации")
            }
        }
    }

    fun logout() {
        sessionManager.logout()
        _state.value = AuthState.Idle
    }

    fun clearState() {
        _state.value = AuthState.Idle
    }
}
