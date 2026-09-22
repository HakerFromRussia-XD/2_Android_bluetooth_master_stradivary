package com.bailout.stickk.ubi4.versions.v3.presentation.gestureeditor

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.*
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class V3GestureEditorViewModelTest {
    @BeforeEach fun setMain() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @AfterEach fun resetMain() { Dispatchers.resetMain() }
    private val editorRepository = mockk<V3GestureEditorRepository> {
        every { getHandSide() } returns 1
        every { observeSettings() } returns emptyFlow()
        every { writeSettings(any(), any(), any()) } just Runs
    }
    private val repository = mockk<V3AppSettingsRepository>()
    private val writes = mutableListOf<List<String>>()
    private var stored = V3GestureEditorNames(2, listOf("First", "Second", "Third"))
    private val viewModel: V3GestureEditorViewModel
    init {
        every { repository.getGestureEditorNames() } answers { stored }
        every { repository.saveGestureEditorNames(any()) } answers { writes.add(firstArg<List<String>>().toList()); Unit }
        viewModel = V3GestureEditorViewModel(GetGestureEditorNamesUseCaseV3(repository), SaveGestureEditorNamesUseCaseV3(repository),
            ObserveGestureSettingsUseCaseV3(editorRepository), RequestGestureSettingsUseCaseV3(editorRepository),
            EditGestureSettingsUseCaseV3(), WriteGestureSettingsUseCaseV3(editorRepository), GetGestureEditorHandSideUseCaseV3(editorRepository))
    }
    private fun send(action: V3GestureEditorAction) = viewModel.onAction(action)

    @Test fun `hand side is ready on creation and is reloaded only for a recreated screen`() {
        every { editorRepository.getHandSide() } returns 0
        send(V3GestureEditorAction.ViewCreated())
        assertEquals(0, viewModel.uiState.value.handSide)
        every { editorRepository.getHandSide() } returns 1
        send(V3GestureEditorAction.RendererReady)
        send(V3GestureEditorAction.ViewStopped)
        assertEquals(0, viewModel.uiState.value.handSide)
        send(V3GestureEditorAction.ViewDestroyed)
        send(V3GestureEditorAction.ViewCreated())
        assertEquals(1, viewModel.uiState.value.handSide)
        verify(exactly = 2) { editorRepository.getHandSide() }
        verify(exactly = 0) { editorRepository.writeSettings(any(), any(), any()) }
        assertTrue(writes.isEmpty())
    }

    @Test fun `load and entering input do not write and confirmation preserves other names`() {
        send(V3GestureEditorAction.ViewCreated())
        assertEquals("Second", viewModel.uiState.value.name)
        send(V3GestureEditorAction.NameChanged("Ignored"))
        send(V3GestureEditorAction.EditNameClicked)
        assertEquals("Second", viewModel.uiState.value.nameInput)
        send(V3GestureEditorAction.NameChanged("  Changed  "))
        assertTrue(writes.isEmpty())
        assertEquals("Second", viewModel.uiState.value.name)
        send(V3GestureEditorAction.EditNameClicked)
        assertEquals(listOf(listOf("First", "  Changed  ", "Third")), writes)
        assertEquals("  Changed  ", viewModel.uiState.value.name)
        assertFalse(viewModel.uiState.value.isEditingName)
        send(V3GestureEditorAction.EditNameClicked)
        assertEquals("  Changed  ", viewModel.uiState.value.nameInput)
        assertEquals(1, writes.size)
    }

    @Test fun `save button accepts empty input and preserves snapshot of all names`() {
        send(V3GestureEditorAction.ViewCreated())
        send(V3GestureEditorAction.EditNameClicked)
        send(V3GestureEditorAction.NameChanged(""))
        stored = V3GestureEditorNames(1, listOf("External", "Second", "Third"))
        send(V3GestureEditorAction.SaveClicked)
        assertEquals(listOf(listOf("First", "", "Third")), writes)
        assertTrue(viewModel.uiState.value.isEditingName)
        assertEquals("Second", viewModel.uiState.value.name)
    }

    @Test fun `destroy discards unsaved input and recreated activity reloads preferences`() {
        send(V3GestureEditorAction.ViewCreated())
        send(V3GestureEditorAction.EditNameClicked)
        send(V3GestureEditorAction.NameChanged("Unsaved"))
        send(V3GestureEditorAction.ViewDestroyed)
        send(V3GestureEditorAction.SaveClicked)
        stored = V3GestureEditorNames(1, listOf("Reloaded"))
        send(V3GestureEditorAction.ViewCreated())
        assertEquals(V3GestureEditorUiState(name = "Reloaded"), viewModel.uiState.value)
        assertTrue(writes.isEmpty())
    }

    @Test fun `cleared viewmodel cannot reload or save`() {
        send(V3GestureEditorAction.ViewCreated())
        val store = ViewModelStore()
        store.put("editor", viewModel)
        store.clear()
        send(V3GestureEditorAction.ViewCreated())
        send(V3GestureEditorAction.EditNameClicked)
        send(V3GestureEditorAction.SaveClicked)
        verify(exactly = 1) { repository.getGestureEditorNames() }
        assertTrue(writes.isEmpty())
    }

    @Test fun `invalid saved number is not silently clamped to another gesture`() {
        stored = V3GestureEditorNames(0, listOf("First"))
        assertThrows(IndexOutOfBoundsException::class.java) { send(V3GestureEditorAction.ViewCreated()) }
        assertTrue(writes.isEmpty())
    }
}
