package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.LinkButton
import com.example.data.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    // App state
    enum class Screen {
        MainDashboard,
        AdminCenter,
        AuthPage
    }

    private val _currentScreen = MutableStateFlow(Screen.MainDashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    val buttons: StateFlow<List<LinkButton>> = repository.allLinkButtons
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val registeredUsersCount: StateFlow<Int> = repository.usersCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _loggedInUser = MutableStateFlow<User?>(null)
    val loggedInUser: StateFlow<User?> = _loggedInUser.asStateFlow()

    // Admin Auth
    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()

    // Popup Ad flow
    private val _activeAdButton = MutableStateFlow<LinkButton?>(null)
    val activeAdButton: StateFlow<LinkButton?> = _activeAdButton.asStateFlow()

    private val _countdown = MutableStateFlow(0)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    private val _isAdSkippable = MutableStateFlow(false)
    val isAdSkippable: StateFlow<Boolean> = _isAdSkippable.asStateFlow()

    // General messages / status alerts
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Pending URL redirect (notified to UI when user clicks Proceed)
    private val _pendingRedirectUrl = MutableStateFlow<String?>(null)
    val pendingRedirectUrl: StateFlow<String?> = _pendingRedirectUrl.asStateFlow()

    private var countdownJob: Job? = null

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun clearRedirectUrl() {
        _pendingRedirectUrl.value = null
    }

    // --- BUTTON MANAGEMENT (ADMIN ACTIONS) ---
    fun addButton(label: String, targetUrl: String, popupTitle: String, popupMessage: String, countdownSeconds: Int) {
        viewModelScope.launch {
            if (label.isBlank() || targetUrl.isBlank() || popupTitle.isBlank() || popupMessage.isBlank()) {
                _userMessage.value = "Hata: Tüm alanlar doldurulmalıdır!"
                return@launch
            }
            // Format URL safely
            val formattedUrl = if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                "https://$targetUrl"
            } else {
                targetUrl
            }

            val newBtn = LinkButton(
                label = label.trim(),
                targetUrl = formattedUrl.trim(),
                popupTitle = popupTitle.trim(),
                popupMessage = popupMessage.trim(),
                countdownSeconds = countdownSeconds.coerceIn(1, 60)
            )
            repository.insertLinkButton(newBtn)
            _userMessage.value = "Buton başarıyla eklendi!"
        }
    }

    fun updateButton(button: LinkButton) {
        viewModelScope.launch {
            repository.updateLinkButton(button)
            _userMessage.value = "Buton güncellemesi başarılı!"
        }
    }

    fun deleteButton(id: Int) {
        viewModelScope.launch {
            repository.deleteLinkButtonById(id)
            _userMessage.value = "Buton başarıyla silindi!"
        }
    }

    // --- POPUP AD LOGIC ---
    fun clickActionButton(button: LinkButton) {
        _activeAdButton.value = button
        _countdown.value = button.countdownSeconds
        _isAdSkippable.value = false
        _pendingRedirectUrl.value = null

        // Increment click score
        viewModelScope.launch {
            repository.insertLinkButton(button.copy(clickCount = button.clickCount + 1))
        }

        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_countdown.value > 0) {
                delay(1000)
                _countdown.value = _countdown.value - 1
            }
            _isAdSkippable.value = true
        }
    }

    fun closeAdPopup(proceedToTarget: Boolean) {
        countdownJob?.cancel()
        val currentTarget = _activeAdButton.value?.targetUrl
        _activeAdButton.value = null

        if (proceedToTarget && currentTarget != null) {
            _pendingRedirectUrl.value = currentTarget
        }
    }

    // --- AUTHENTICATION FLOW (USER) ---
    fun registerNewUser(username: String, email: String, pass: String) {
        viewModelScope.launch {
            if (username.isBlank() || email.isBlank() || pass.isBlank()) {
                _userMessage.value = "Lütfen tüm kayıt alanlarını doldurun!"
                return@launch
            }
            val userExists = repository.getUser(username.trim())
            if (userExists != null) {
                _userMessage.value = "Bu kullanıcı adı zaten alınmış!"
                return@launch
            }

            val passHash = hashPassword(pass)
            val newUser = User(
                username = username.trim(),
                email = email.trim(),
                passwordHash = passHash
            )
            val success = repository.registerUser(newUser)
            if (success) {
                _loggedInUser.value = newUser
                _userMessage.value = "Kayıt başarılı! Hoş geldin, ${newUser.username}!"
                _currentScreen.value = Screen.MainDashboard
            } else {
                _userMessage.value = "Kayıt olurken bir hata oluştu!"
            }
        }
    }

    fun loginUser(username: String, pass: String) {
        viewModelScope.launch {
            if (username.isBlank() || pass.isBlank()) {
                _userMessage.value = "Kullanıcı adı ve şifre boş olamaz!"
                return@launch
            }
            val user = repository.getUser(username.trim())
            if (user != null && user.passwordHash == hashPassword(pass)) {
                _loggedInUser.value = user
                _userMessage.value = "Giriş başarılı! Hoş geldin, ${user.username}!"
                _currentScreen.value = Screen.MainDashboard
            } else {
                _userMessage.value = "Hatalı kullanıcı adı veya şifre!"
            }
        }
    }

    fun logout() {
        _loggedInUser.value = null
        _userMessage.value = "Çıkış yapıldı."
    }

    // --- ADMIN ACCESS ---
    fun authenticateAdmin(pin: String): Boolean {
        return if (pin == "1234") {
            _isAdminAuthenticated.value = true
            _userMessage.value = "Yönetici girişi onaylandı!"
            true
        } else {
            _userMessage.value = "Geçersiz PIN kodu!"
            false
        }
    }

    fun deauthenticateAdmin() {
        _isAdminAuthenticated.value = false
        _userMessage.value = "Yönetici oturumu sonlandırıldı."
    }

    private fun hashPassword(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            password
        }
    }
}

class MainViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
