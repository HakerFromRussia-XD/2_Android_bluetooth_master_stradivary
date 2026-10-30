package com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3

import android.opengl.GLES20
import android.util.Log
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Exercises the real ShaderHelper with driver failures injected at the GLES boundary. */
class CollectionShaderProgramTest {
    private val shaderTypes = mutableMapOf<Int, Int>()
    private val deletedShaders = mutableListOf<Int>()
    private val createdPrograms = mutableListOf<Int>()
    private val deletedPrograms = mutableListOf<Int>()
    private var nextShader = 1
    private var nextProgram = 100
    private var failedCompileType: Int? = null
    private var failedCreateType: Int? = null
    private var failLink = false
    private var failCreateProgram = false

    @BeforeEach fun setUp() {
        mockkStatic(GLES20::class, Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { GLES20.glCreateShader(any()) } answers {
            val type = firstArg<Int>()
            if (type == failedCreateType) 0 else nextShader++.also { shaderTypes[it] = type }
        }
        every { GLES20.glShaderSource(any(), any()) } just Runs
        every { GLES20.glCompileShader(any()) } just Runs
        every { GLES20.glGetShaderiv(any(), GLES20.GL_COMPILE_STATUS, any<IntArray>(), any()) } answers {
            arg<IntArray>(2)[arg(3)] = if (shaderTypes[firstArg<Int>()] == failedCompileType) 0 else 1
        }
        every { GLES20.glGetShaderInfoLog(any()) } returns "Injected compilation failure"
        every { GLES20.glDeleteShader(any()) } answers { deletedShaders += firstArg<Int>() }
        every { GLES20.glCreateProgram() } answers {
            if (failCreateProgram) 0 else nextProgram++.also(createdPrograms::add)
        }
        every { GLES20.glAttachShader(any(), any()) } just Runs
        every { GLES20.glBindAttribLocation(any(), any(), any()) } just Runs
        every { GLES20.glLinkProgram(any()) } just Runs
        every { GLES20.glGetProgramiv(any(), GLES20.GL_LINK_STATUS, any<IntArray>(), any()) } answers {
            arg<IntArray>(2)[arg(3)] = if (failLink) 0 else 1
        }
        every { GLES20.glGetProgramInfoLog(any()) } returns "Injected link failure"
        every { GLES20.glDeleteProgram(any()) } answers { deletedPrograms += firstArg<Int>() }
    }

    @AfterEach fun tearDown() {
        unmockkStatic(GLES20::class, Log::class)
    }

    @Test fun `successful link releases shaders and transfers program to card`() {
        val program = createProgram()

        assertEquals(listOf(program), createdPrograms)
        assertTrue(deletedPrograms.isEmpty(), "A successful program belongs to the card until teardown")
        assertAllShadersDeletedOnce()
    }

    @Test fun `vertex compilation failure releases its shader without starting fragment or program`() {
        failedCompileType = GLES20.GL_VERTEX_SHADER

        assertThrows(RuntimeException::class.java) { createProgram() }

        assertEquals(listOf(GLES20.GL_VERTEX_SHADER), shaderTypes.values.toList())
        assertTrue(createdPrograms.isEmpty())
        assertAllShadersDeletedOnce()
    }

    @Test fun `fragment compilation failure also releases the previously compiled vertex shader`() {
        failedCompileType = GLES20.GL_FRAGMENT_SHADER

        assertThrows(RuntimeException::class.java) { createProgram() }

        assertEquals(2, shaderTypes.size)
        assertTrue(createdPrograms.isEmpty())
        assertAllShadersDeletedOnce()
    }

    @Test fun `link failure releases failed program and both compiled shaders`() {
        failLink = true

        assertThrows(RuntimeException::class.java) { createProgram() }

        assertEquals(1, createdPrograms.size)
        assertEquals(createdPrograms, deletedPrograms)
        assertAllShadersDeletedOnce()
    }

    @Test fun `vertex allocation failure does not delete a zero handle`() {
        failedCreateType = GLES20.GL_VERTEX_SHADER

        assertThrows(RuntimeException::class.java) { createProgram() }

        assertTrue(shaderTypes.isEmpty())
        assertTrue(createdPrograms.isEmpty())
        assertAllShadersDeletedOnce()
    }

    @Test fun `fragment allocation failure releases the already compiled vertex shader`() {
        failedCreateType = GLES20.GL_FRAGMENT_SHADER

        assertThrows(RuntimeException::class.java) { createProgram() }

        assertEquals(listOf(GLES20.GL_VERTEX_SHADER), shaderTypes.values.toList())
        assertTrue(createdPrograms.isEmpty())
        assertAllShadersDeletedOnce()
    }

    @Test fun `program allocation failure releases both shaders without deleting a zero program`() {
        failCreateProgram = true

        assertThrows(RuntimeException::class.java) { createProgram() }

        assertEquals(2, shaderTypes.size)
        assertTrue(createdPrograms.isEmpty())
        assertTrue(deletedPrograms.isEmpty())
        assertAllShadersDeletedOnce()
    }

    @Test fun `separate card creations retain separate programs`() {
        val first = createProgram()
        val second = createProgram()

        assertNotEquals(first, second)
        assertEquals(listOf(first, second), createdPrograms)
        assertTrue(deletedPrograms.isEmpty())
        assertAllShadersDeletedOnce()
    }

    @Test fun `missing vertex source fails before allocating GL resources`() {
        assertThrows(IllegalArgumentException::class.java) { createProgram(vertexSource = null) }

        assertTrue(shaderTypes.isEmpty())
        assertTrue(createdPrograms.isEmpty())
        assertAllShadersDeletedOnce()
    }

    @Test fun `missing fragment source fails before allocating GL resources`() {
        assertThrows(IllegalArgumentException::class.java) { createProgram(fragmentSource = null) }

        assertTrue(shaderTypes.isEmpty())
        assertTrue(createdPrograms.isEmpty())
        assertAllShadersDeletedOnce()
    }

    private fun createProgram(vertexSource: String? = "vertex source", fragmentSource: String? = "fragment source") =
        UBI4GripperSettingsWithEncodersRendererV3.createCollectionProgram(vertexSource, fragmentSource)

    private fun assertAllShadersDeletedOnce() {
        assertEquals(shaderTypes.keys.sorted(), deletedShaders.sorted(),
            "Every created shader must be deleted exactly once; zero handles must not be deleted")
    }
}
