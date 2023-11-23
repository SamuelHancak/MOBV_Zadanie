package eu.mcomputing.mobv.mobvzadanie.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.mcomputing.mobv.mobvzadanie.data.DataRepository
import kotlinx.coroutines.launch

class ImageViewModel(private val dataRepository: DataRepository) : ViewModel() {
    private val _uploadImageResult = MutableLiveData<String>()
    val uploadImageResult get() = _uploadImageResult

    fun uploadImage(imageUri: Uri?, context: Context) {
        viewModelScope.launch {
            val result = dataRepository.uploadImage(imageUri, context)
            _uploadImageResult.postValue(result)
        }
    }

    fun deleteImage() {
        viewModelScope.launch {
            val result = dataRepository.removeImage()
            _uploadImageResult.postValue(result)
        }
    }

    fun clearResult() {
        _uploadImageResult.value = ""
    }
}