package com.example.myapplication.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.User
import com.example.myapplication.data.model.repository.AuthRepository
import com.example.myapplication.data.model.websocket.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)
    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    private val _registerResponse = MutableStateFlow("")
    val registerResponse: StateFlow<String> = _registerResponse.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()



    fun login(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.loginUser(
                username,
                password,
                onSuccess = { token ->
                    _isLoading.value = false
                    _isLoggedIn.value = true
                    _authToken.value = token


                    val privateKey = SecureStorage.getPrivateKey(getApplication(), username)
                    val passphrase = SecureStorage.getPGPPassphrase(getApplication(), username)

                    if (privateKey != null && passphrase != null) {
                        Log.d("PGP", "Private Key and passphrase loaded for $username")
                    } else {
                        _errorMessage.value = "Private key or passphrase missing for user."
                    }
                },
                onError = { error ->
                    _isLoading.value = false
                    _errorMessage.value = error
                }
            )
        }
    }

    fun register(username: String, password: String, publicKey: String) {
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.registerUser(
                username,
                password,
                publicKey,
                onSuccess = { response ->
                    _isLoading.value = false
                    _registerResponse.value = response
                },
                onError = { error ->
                    _isLoading.value = false
                    _errorMessage.value = error
                }
            )
        }
    }
    fun logout() {
        _authToken.value = null
    }
    fun getUserPublicKey(
        username: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            authRepository.getUserPublicKey(
                username = username,
                onSuccess = onSuccess,
                onError = onError
            )
        }
    }
    suspend fun getUserPublicKey(username: String): String {
        return suspendCoroutine { continuation ->
            authRepository.getUserPublicKey(
                username = username,
                onSuccess = { key -> continuation.resume(key) },
                onError = { error -> continuation.resumeWithException(Exception(error)) }
            )
        }
    }
    fun searchUsers(query: String) {
        if (query.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }

        authRepository.searchUsers(
            query = query,
            onSuccess = { users ->
                _searchResults.value = users
            },
            onError = { error ->
                Log.e("AuthViewModel", error)
            }
        )

        fun searchUsers(query: String) {
            if (query.isEmpty()) {
                _searchResults.value = emptyList()
                return
            }

            authRepository.searchUsers(
                query = query,
                onSuccess = { users ->
                    _searchResults.value = users
                },
                onError = { error ->
                    Log.e("AuthViewModel", error)
                }
            )
        }
    }
}
