package com.bailout.stickk.ubi4.versions.v3.presentation.settingsprofiles

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4ItemSettingsProfileSpinnerBinding
import com.bailout.stickk.databinding.Ubi4WidgetSpinnerBinding
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.widgets.SpinnerItemV3
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SETTINGS_PROFILE
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.V3SettingsProfile
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.SpinnerDelegateAdapterV3
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.*
import com.skydoves.powerspinner.PowerSpinnerInterface
import com.skydoves.powerspinner.OnSpinnerItemSelectedListener
import com.skydoves.powerspinner.PowerSpinnerView
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsProfileSpinnerStateBindingTest {
    private val context = mockk<Context>(relaxed = true)
    private val spinner = mockk<PowerSpinnerView>(relaxed = true)
    private val selections = mutableListOf<Int>()
    private val deletions = mutableListOf<Int>()
    private val sends = mutableListOf<Int>()
    private var creations = 0
    private val callbacks = mutableListOf<() -> Unit>()
    private lateinit var delegate: SpinnerDelegateAdapterV3
    private lateinit var popup: PowerSpinnerInterface<CharSequence>
    private lateinit var binding: Ubi4WidgetSpinnerBinding
    private val originalInteraction = UiState.v3WidgetsInteractionEnabled.value
    private val state = V3SettingsProfilesUiState(
        profiles = listOf(V3SettingsProfile(1, "Normal"), V3SettingsProfile(3, "Sport")),
        activeProfileId = 3, canCreate = true, isLoading = false, isEnabled = true,
    )
    private val item = V3SpecialSettingsWidgetMapper().toItems(listOf(
        V3SpecialSettingsWidget.SettingsProfile(V3SpecialSettingsWidgetInfo(P_KEY_SETTINGS_PROFILE, "Profiles", 5), emptyList(), 0),
    )).single() as SpinnerItemV3

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        mockkStatic(ContextCompat::class, ResourcesCompat::class, Log::class)
        every { ContextCompat.getColor(any(), any()) } returns 0
        every { ResourcesCompat.getFont(any<Context>(), any()) } returns mockk<Typeface>()
        every { Log.d(any(), any()) } returns 0
        every { spinner.context } returns context
        every { spinner.selectedIndex } returns -1
        every { context.getString(any(), *anyVararg()) } returns "Profile"
        every { spinner.setSpinnerAdapter(any<PowerSpinnerInterface<CharSequence>>()) } answers {
            val real = firstArg<PowerSpinnerInterface<CharSequence>>() as SettingsProfileSpinnerAdapterV3
            popup = spyk(real).also { every { it.notifyDataSetChanged() } just Runs }
        }
        every { spinner.getSpinnerAdapter<CharSequence>() } answers { popup }
        every { spinner.setItems(any<List<CharSequence>>()) } answers { popup.setItems(firstArg()) }
        every { spinner.setOnSpinnerItemSelectedListener(any<(Int, CharSequence?, Int, CharSequence) -> Unit>()) } answers {
            val block = firstArg<(Int, CharSequence?, Int, CharSequence) -> Unit>()
            popup.onSpinnerItemSelectedListener = OnSpinnerItemSelectedListener { oldIndex, oldItem, newIndex, newItem ->
                block(oldIndex, oldItem, newIndex, newItem)
            }
        }
        every { spinner.selectItemByIndex(any()) } answers { popup.notifyItemSelected(firstArg()) }
        val constructor = Ubi4WidgetSpinnerBinding::class.java.declaredConstructors.single().apply { isAccessible = true }
        binding = constructor.newInstance(*constructor.parameterTypes.map { type ->
            if (type == PowerSpinnerView::class.java) spinner else mockkClass(type.kotlin, relaxed = true)
        }.toTypedArray()) as Ubi4WidgetSpinnerBinding
        delegate = SpinnerDelegateAdapterV3(
            onDestroyParent = callbacks::add, settingsProfilesFromState = true,
            onSettingsProfileSelected = selections::add, onSettingsProfileCreateRequested = { creations++ },
            onSettingsProfileDeleteRequested = deletions::add,
            onSettingsProfileSendRequested = sends::add,
        )
    }

    @AfterEach
    fun tearDown() {
        delegate.onDestroy()
        UiState.v3WidgetsInteractionEnabled.value = originalInteraction
        Dispatchers.resetMain()
        unmockkStatic(ContextCompat::class, ResourcesCompat::class, Log::class)
    }

    private fun bind() {
        delegate.javaClass.getDeclaredMethod("onBind", Ubi4WidgetSpinnerBinding::class.java, SpinnerItemV3::class.java)
            .apply { isAccessible = true }.invoke(delegate, binding, item)
    }

    @Test
    fun `binding uses only screen availability and does not start old subscriptions or read preferences`() = runTest {
        UiState.v3WidgetsInteractionEnabled.value = false
        delegate.renderSettingsProfiles(state)
        bind(); runCurrent()
        verify { spinner.isEnabled = true }
        assertEquals(1, popup.index)
        assertTrue(selections.isEmpty())
        popup.notifyItemSelected(0)
        popup.notifyItemSelected(2)
        assertEquals(listOf(1), selections)
        assertEquals(1, creations)
        verify(exactly = 0) { context.getSharedPreferences(any(), any()) }
        // A global change cannot unlock a row whose screen state is disabled.
        UiState.v3WidgetsInteractionEnabled.value = true
        delegate.renderSettingsProfiles(state.copy(isEnabled = false)); runCurrent()
        verify { spinner.isEnabled = false }
        popup.notifyItemSelected(0)
        assertEquals(listOf(1), selections)
    }

    @Test
    fun `rebind recycle and destroy discard old profile callbacks without applying a profile`() = runTest {
        delegate.renderSettingsProfiles(state)
        bind()
        val oldListener = requireNotNull(popup.onSpinnerItemSelectedListener)
        bind()
        oldListener.onItemSelected(1, "Sport", 0, "Normal")
        assertTrue(selections.isEmpty())
        val current = popup
        delegate.javaClass.getDeclaredMethod("onRecycled", Ubi4WidgetSpinnerBinding::class.java)
            .apply { isAccessible = true }.invoke(delegate, binding)
        assertNull(current.onSpinnerItemSelectedListener)
        bind()
        val beforeDestroy = popup
        delegate.onDestroy()
        assertNull(beforeDestroy.onSpinnerItemSelectedListener)
        assertTrue(selections.isEmpty())
        assertEquals(0, creations)
    }

    @Test fun `baseline has a fixed label no rename action and remains selectable`() = runTest {
        every { context.getString(any<Int>()) } returns "Base settings"
        delegate.renderSettingsProfiles(state.copy(
            profiles = listOf(V3SettingsProfile(0, "Ignored name"), V3SettingsProfile(1, "Everyday")), activeProfileId = 0,
        ))
        bind()
        verify { spinner.setItems(listOf("Base settings", "Everyday", "+")) }
        @Suppress("UNCHECKED_CAST")
        val editable = SettingsProfileSpinnerAdapterV3::class.java.getDeclaredField("isEditable")
            .apply { isAccessible = true }.get(popup) as (Int) -> Boolean
        assertFalse(editable(0))
        assertTrue(editable(1))
        assertFalse(editable(2))
        popup.notifyItemSelected(0)
        assertEquals(listOf(0), selections)
    }

    @Test fun `delete callback targets personal profile without selecting it and rejects recycled callbacks`() = runTest {
        delegate.renderSettingsProfiles(state.copy(profiles = listOf(V3SettingsProfile(0, null), V3SettingsProfile(1, "Personal")), activeProfileId = 1))
        bind()
        @Suppress("UNCHECKED_CAST")
        val delete = SettingsProfileSpinnerAdapterV3::class.java.getDeclaredField("onDeleteClick")
            .apply { isAccessible = true }.get(popup) as (Int) -> Unit
        delete(0)
        assertTrue(deletions.isEmpty())
        delete(1)
        assertEquals(listOf(1), deletions)
        assertTrue(selections.isEmpty())
        delegate.onDestroy()
        delete(1)
        assertEquals(listOf(1), deletions)
    }

    @Test fun `role change rebinds profile action from disabled send back to rename`() = runTest {
        delegate.renderSettingsProfiles(state.copy(showSendAction = true))
        bind()
        val adapter = popup as SettingsProfileSpinnerAdapterV3
        val constructor = Ubi4ItemSettingsProfileSpinnerBinding::class.java.declaredConstructors.single()
            .apply { isAccessible = true }
        val row = constructor.newInstance(*constructor.parameterTypes.map { mockkClass(it.kotlin, relaxed = true) }
            .toTypedArray()) as Ubi4ItemSettingsProfileSpinnerBinding
        val action = row.ubi4SettingsProfileItemEdit
        every { action.context } returns context
        every { spinner.spinnerItemHeight } returns Int.MIN_VALUE
        val holder = adapter.ViewHolder(row)

        adapter.onBindViewHolder(holder, 0)
        verify { action.setImageResource(R.drawable.ubi4_ic_send) }
        verify { action.isEnabled = false }
        verify { row.ubi4SettingsProfileItemDelete.visibility = View.GONE }

        every { spinner.selectedIndex } returns adapter.index
        clearMocks(adapter, answers = false, recordedCalls = true)
        delegate.renderSettingsProfiles(state)
        verify(exactly = 1) { adapter.notifyDataSetChanged() }
        adapter.onBindViewHolder(holder, 0)
        verify { action.setImageResource(R.drawable.ubi4_ic_edit) }
        verify { action.isEnabled = true }
        verify { row.ubi4SettingsProfileItemDelete.visibility = View.VISIBLE }
    }

    @Test fun `send action routes only the profile with a draft`() = runTest {
        delegate.renderSettingsProfiles(state.copy(
            showSendAction = true,
            profiles = listOf(state.profiles[0], state.profiles[1].copy(hasEngineerDraft = true)),
        ))
        bind()
        @Suppress("UNCHECKED_CAST")
        val click = SettingsProfileSpinnerAdapterV3::class.java.getDeclaredField("onEditClick")
            .apply { isAccessible = true }.get(popup) as (Int, String) -> Unit
        click(0, "Normal")
        click(1, "Sport")
        assertEquals(listOf(3), sends)
        assertTrue(selections.isEmpty())
    }
}
