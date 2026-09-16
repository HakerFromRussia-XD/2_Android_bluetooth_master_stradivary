package com.bailout.stickk.ubi4.versions.v3.di

import androidx.lifecycle.ViewModel
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.GetRotationGroupSelectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.EditRotationGroupSelectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.SaveRotationGroupSelectionUseCaseV3
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.GetGesturesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.LoadRotationGroupUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.RemoveGestureFromRotationGroupUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.ObserveGesturesChangesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.RequestActiveGestureUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.SelectGestureUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.V3GesturesRepository
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.MoveGestureInRotationGroupUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.gestures.V3GesturesViewModel

class V3GesturesViewModelFactory(private val repository: V3GesturesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3GesturesViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3GesturesViewModel(
            getGestures = GetGesturesUseCaseV3(repository),
            observeGesturesChanges = ObserveGesturesChangesUseCaseV3(repository),
            selectGesture = SelectGestureUseCaseV3(repository),
            requestActiveGesture = RequestActiveGestureUseCaseV3(repository),
            loadRotationGroup = LoadRotationGroupUseCaseV3(repository),
            removeGestureFromRotationGroup = RemoveGestureFromRotationGroupUseCaseV3(repository),
            getRotationGroupSelection = GetRotationGroupSelectionUseCaseV3(repository),
            editRotationGroupSelection = EditRotationGroupSelectionUseCaseV3(),
            saveRotationGroupSelection = SaveRotationGroupSelectionUseCaseV3(repository),
            moveGestureInRotationGroup = MoveGestureInRotationGroupUseCaseV3(repository),
        ) as T
    }
}
