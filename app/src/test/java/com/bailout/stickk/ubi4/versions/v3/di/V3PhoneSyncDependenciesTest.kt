package com.bailout.stickk.ubi4.versions.v3.di

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.bailout.stickk.ubi4.ble.BLEController
import com.bailout.stickk.ubi4.versions.v3.data.phonesync.V3PhoneSyncRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.phonesync.V3PhoneSyncState
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class V3PhoneSyncDependenciesTest {
    private class Owner(override val viewModelStore: ViewModelStore = ViewModelStore()) : ViewModelStoreOwner

    @Test fun `a failed attempt does not block a new selection from scanning`() {
        val first = Owner()
        val controller = mockk<BLEController>(relaxed = true)
        val repository = slot<V3PhoneSyncRepositoryImpl>()
        every { controller.bindV3PhoneSync(capture(repository), any()) } just Runs
        V3PhoneSyncDependencies.bind(first, controller)
        repository.captured.beginSession()
        repository.captured.cancel()
        assertEquals(V3PhoneSyncState.Failed, V3PhoneSyncDependencies.observe(first)().value)
        assertEquals(V3PhoneSyncState.Idle, V3PhoneSyncDependencies.observe(Owner())().value)
        // Activity recreation retains the current attempt rather than clearing its result.
        assertEquals(V3PhoneSyncState.Failed, V3PhoneSyncDependencies.observe(Owner(first.viewModelStore))().value)
    }
}
