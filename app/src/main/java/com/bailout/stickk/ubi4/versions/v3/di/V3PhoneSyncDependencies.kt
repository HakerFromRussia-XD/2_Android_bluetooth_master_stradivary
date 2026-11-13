package com.bailout.stickk.ubi4.versions.v3.di

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.bailout.stickk.ubi4.ble.BLEController
import com.bailout.stickk.ubi4.versions.v3.data.phonesync.V3PhoneSyncRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.data.phonesync.V3PhoneSyncStateStore
import com.bailout.stickk.ubi4.versions.v3.domain.phonesync.AuthorizePhoneUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.phonesync.ObservePhoneSyncUseCaseV3
import java.util.WeakHashMap

object V3PhoneSyncDependencies {
    private val attempts = WeakHashMap<ViewModelStore, V3PhoneSyncStateStore>()

    @Synchronized private fun state(owner: ViewModelStoreOwner): V3PhoneSyncStateStore =
        attempts.getOrPut(owner.viewModelStore) { V3PhoneSyncStateStore() }

    fun bind(owner: ViewModelStoreOwner, controller: BLEController) {
        val repository = V3PhoneSyncRepositoryImpl(state(owner), controller::sendV3PhoneSyncPacket)
        controller.bindV3PhoneSync(repository, AuthorizePhoneUseCaseV3(repository))
    }

    fun observe(owner: ViewModelStoreOwner) = ObservePhoneSyncUseCaseV3(state(owner))
}
