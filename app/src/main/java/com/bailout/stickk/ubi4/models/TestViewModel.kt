package com.bailout.stickk.ubi4.models

import androidx.lifecycle.ViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class MyItem(var id: Int, var title: String) // Пример структуры данных

class MyViewModel @Inject constructor(): ViewModel() {
    private val _item = MutableStateFlow<MyItem?>(null)
    val items: StateFlow<MyItem?> get() = _item

    fun setItems(newItem: MyItem) {
        _item.value = newItem
    }

    fun updateItem(newItem: MyItem) {
        _item.update { currentItem ->
            currentItem?.copy(id = newItem.id, title = newItem.title)
        }
    }
}