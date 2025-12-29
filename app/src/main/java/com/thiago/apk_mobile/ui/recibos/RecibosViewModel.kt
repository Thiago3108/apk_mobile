package com.thiago.apk_mobile.ui.recibos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thiago.apk_mobile.data.model.Recibo
import com.thiago.apk_mobile.data.repository.ReciboRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecibosViewModel @Inject constructor(
    private val repository: ReciboRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _startDate = MutableStateFlow(0L)
    val startDate: StateFlow<Long> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow(Long.MAX_VALUE)
    val endDate: StateFlow<Long> = _endDate.asStateFlow()

    val recibos: StateFlow<List<Recibo>> = 
        combine(_searchQuery, _startDate, _endDate) { query, start, end ->
            Triple(query, start, end)
        }.flatMapLatest { (query, start, end) ->
            repository.getRecibosFiltrados(query, start, end)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedRecibo = MutableStateFlow<Recibo?>(null)
    val selectedRecibo: StateFlow<Recibo?> = _selectedRecibo.asStateFlow()
    
    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onDateRangeChange(start: Long, end: Long) {
        _startDate.value = start
        _endDate.value = end
    }

    fun getReciboById(id: Long) {
        viewModelScope.launch {
            repository.getRecibo(id).collect {
                _selectedRecibo.value = it
            }
        }
    }

    fun insertarRecibo(recibo: Recibo) {
        viewModelScope.launch {
            repository.insertar(recibo)
        }
    }

    fun actualizarRecibo(recibo: Recibo) {
        viewModelScope.launch {
            repository.actualizar(recibo)
        }
    }
}
