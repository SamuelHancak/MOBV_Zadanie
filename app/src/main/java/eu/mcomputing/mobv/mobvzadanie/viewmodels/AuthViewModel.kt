package eu.mcomputing.mobv.mobvzadanie.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.mcomputing.mobv.mobvzadanie.data.DataRepository
import eu.mcomputing.mobv.mobvzadanie.data.model.User
import kotlinx.coroutines.launch

class AuthViewModel(private val dataRepository: DataRepository) : ViewModel() {
    private val _registrationResult = MutableLiveData<String?>()
    val registrationResult: LiveData<String?> get() = _registrationResult

    private val _loginResult = MutableLiveData<String?>()
    val loginResult: LiveData<String?> get() = _loginResult

    private val _userResult = MutableLiveData<User?>()
    val userResult: LiveData<User?> get() = _userResult

    private val _forgotPasswordResult = MutableLiveData<Pair<String, String>>()
    val forgotPasswordResult: LiveData<Pair<String, String>> get() = _forgotPasswordResult

    val username = MutableLiveData<String>()
    val email = MutableLiveData<String>()
    val password = MutableLiveData<String>()
    val repeatPassword = MutableLiveData<String>()

    fun registerUser() {
        viewModelScope.launch {
            val result = dataRepository.apiRegisterUser(
                username.value ?: "",
                email.value ?: "",
                password.value ?: "",
                repeatPassword.value ?: ""
            )
            _registrationResult.postValue(result.first)
            _userResult.postValue(result.second)
        }
    }

    fun loginUser() {
        viewModelScope.launch {
            val result = dataRepository.apiLoginUser(username.value ?: "", password.value ?: "")
            _loginResult.postValue(result.first)
            _userResult.postValue(result.second)
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            _loginResult.postValue(null)
            _userResult.postValue(null)
        }
    }

    fun forgottenPassword() {
        viewModelScope.launch {
            val result = dataRepository.forgottenPassword(username.value ?: "")
            _forgotPasswordResult.postValue(result)
        }
    }

    fun clearInputs() {
        viewModelScope.launch {
            username.postValue("")
            email.postValue("")
            password.postValue("")
            repeatPassword.postValue("")
        }
    }
}