package com.bailout.stickk.ubi4.versions.v3.presentation.accountstatistics

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class V3AccountStatisticsFragmentFactoryTest {
    @Test fun `saved old statistics class resolves to current class while other fragments keep their factory`() {
        val loader = javaClass.classLoader
        val delegate = mockk<FragmentFactory>()
        val fragment = mockk<Fragment>()
        every { delegate.instantiate(loader, any()) } returns fragment
        val factory = delegate.withAccountStatisticsCompatibility()
        val statistics = AccountFragmentStatisticsV3::class.java.name
        val other = "com.bailout.stickk.ubi4.ui.fragments.SensorsFragment"

        assertSame(fragment, factory.instantiate(loader,
            "com.bailout.stickk.ubi4.ui.fragments.account.statisticsFragmentV3.AccountFragmentStatisticsV3"))
        assertSame(fragment, factory.instantiate(loader, statistics))
        assertSame(fragment, factory.instantiate(loader, other))
        verifySequence {
            delegate.instantiate(loader, statistics)
            delegate.instantiate(loader, statistics)
            delegate.instantiate(loader, other)
        }
    }
}
