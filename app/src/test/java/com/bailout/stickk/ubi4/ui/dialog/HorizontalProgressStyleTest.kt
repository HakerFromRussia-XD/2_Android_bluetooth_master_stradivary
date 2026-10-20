package com.bailout.stickk.ubi4.ui.dialog

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.w3c.dom.Element

class HorizontalProgressStyleTest {
    private val resources = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
        .map { File(it, "app/src/main/res") }.first { it.isDirectory }
    private val safeStyle = "@style/MotoricaHorizontalProgressBar"

    private fun elements(file: File, tag: String): List<Element> {
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            .getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun items(style: Element): Map<String, String> {
        val nodes = style.getElementsByTagName("item")
        return (0 until nodes.length).map { nodes.item(it) as Element }
            .associate { it.getAttribute("name") to it.textContent.trim() }
    }

    @Test fun `horizontal style has no framework indeterminate drawable`() {
        val style = elements(File(resources, "values/progress_styles.xml"), "style").single()
        assertEquals("MotoricaHorizontalProgressBar", style.getAttribute("name"))
        assertEquals("@null", items(style)["android:indeterminateDrawable"])
        assertEquals("false", items(style)["android:indeterminate"])
        assertEquals("@drawable/ubi4_progress_bar", items(style)["android:progressDrawable"])
    }

    @Test fun `every application theme uses the safe horizontal style`() {
        val themes = resources.walkTopDown().filter { it.isFile && it.extension == "xml" }
            .filter { it.parentFile.name.startsWith("values") }
            .flatMap { elements(it, "style").asSequence() }
            .filter { it.getAttribute("name") == "AppTheme" }.toList()
        assertTrue(themes.isNotEmpty())
        themes.forEach { assertEquals(safeStyle, items(it)["android:progressBarStyleHorizontal"]) }
    }

    @Test fun `no layout can opt back into the framework horizontal indicator`() {
        val layouts = resources.walkTopDown().filter { it.isFile && it.extension == "xml" }
            .filter { it.parentFile.name.startsWith("layout") }.toList()
        val dialogProgressLayouts = setOf("ubi4_dialog_progressbar_firmware.xml", "ubi4_dialog_progressbar.xml",
            "ubi4_dialog_sync_progress.xml", "dialog_progress_communication_test.xml", "layout_updating_le.xml")
        layouts.forEach { file ->
            elements(file, "ProgressBar").forEach { bar ->
                val style = bar.getAttribute("style")
                assertFalse(style.contains("android:"), "${file.name}: framework progress style $style")
                if (file.name in dialogProgressLayouts) {
                    assertEquals(safeStyle, style, file.name)
                    assertFalse(bar.hasAttribute("android:indeterminateDrawable"), file.name)
                }
            }
        }
        assertTrue(dialogProgressLayouts.all { name -> layouts.any { it.name == name } })
    }
}
