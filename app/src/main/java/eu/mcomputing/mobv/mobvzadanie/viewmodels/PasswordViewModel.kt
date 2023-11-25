package eu.mcomputing.mobv.mobvzadanie.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.mcomputing.mobv.mobvzadanie.data.DataRepository
import kotlinx.coroutines.launch

class PasswordViewModel(private val dataRepository: DataRepository) : ViewModel() {
    private val _changePasswordResult = MutableLiveData<String>()
    val changePasswordResult: LiveData<String> get() = _changePasswordResult

    val password = MutableLiveData<String>()
    val repeatPassword = MutableLiveData<String>()

    fun changePassword() {
        viewModelScope.launch {
            val result =
                dataRepository.apiChangePassword(password.value ?: "", repeatPassword.value ?: "")
            _changePasswordResult.postValue(result)
        }
    }

    fun clearResult() {
        _changePasswordResult.value = ""
    }
}