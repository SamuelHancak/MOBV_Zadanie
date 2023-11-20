package eu.mcomputing.mobv.mobvzadanie.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import eu.mcomputing.mobv.mobvzadanie.data.DataRepository
import eu.mcomputing.mobv.mobvzadanie.data.db.entities.UserEntity
import eu.mcomputing.mobv.mobvzadanie.utils.Evento
import kotlinx.coroutines.launch

class FeedViewModel(private val repository: DataRepository) : ViewModel() {

    val feed_items: LiveData<List<UserEntity>?> =
        liveData {
            loading.postValue(true)
            repository.apiListGeofence()
            loading.postValue(false)
            emitSource(repository.getUsers())
        }

    val loading = MutableLiveData(false)

    private val _message = MutableLiveData<Evento<String>>()
    val message: LiveData<Evento<String>>
        get() = _message

    fun updateItems() {
        viewModelScope.launch {
            loading.postValue(true)
            _message.postValue(Evento(repository.apiListGeofence()))
            loading.postValue(false)
        }
    }
}