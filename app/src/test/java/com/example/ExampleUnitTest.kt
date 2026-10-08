package com.example

import com.example.editor.CadAutocomplete
import com.example.editor.CadSyntaxHighlighter
import com.example.engine.CadGeometryBuilder
import com.example.engine.CadScriptEvaluator
import com.example.export.CadExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testCadScriptEvaluatorBasicBox() {
        val script = """
            import cadquery as cq
            largo = 100.0
            ancho = 60.0
            alto = 30.0
            pieza = cq.Workplane("XY").box(largo, ancho, alto)
        """.trimIndent()

        val result = CadScriptEvaluator.evaluate(script)
        assertTrue(result.isSuccess)
        assertEquals(1, result.objects.size)

        val obj = result.objects.first()
        assertTrue(obj.triangles.isNotEmpty())
        assertTrue(obj.volume > 0)
        assertEquals(100f, obj.boundingBox.sizeX, 1.0f)
        assertEquals(60f, obj.boundingBox.sizeY, 1.0f)
        assertEquals(30f, obj.boundingBox.sizeZ, 1.0f)
    }

    @Test
    fun testCadScriptEvaluatorFilletAndHole() {
        val script = """
            import cadquery as cq
            pieza = cq.Workplane("XY").box(120, 70, 40).edges("|Z").fillet(6.0).faces(">Z").hole(20.0)
            show_object(pieza, name="Bloque Redondeado")
        """.trimIndent()

        val result = CadScriptEvaluator.evaluate(script)
        assertTrue(result.isSuccess)
        assertEquals(1, result.objects.size)
        assertEquals("Bloque Redondeado", result.objects.first().name)
        assertTrue(result.objects.first().featureEdges.any { it.isFeatureEdge })
    }

    @Test
    fun testParameterExtractionAndReplacement() {
        val script = """
            largo = 120.0
            ancho = 70.0
            alto = 40.0
        """.trimIndent()

        val params = CadScriptEvaluator.extractParameters(script)
        assertEquals(3, params.size)
        assertEquals("largo", params[0].name)
        assertEquals(120.0f, params[0].value, 0.01f)

        val updated = CadScriptEvaluator.updateParameterInCode(script, "largo", 150.0f)
        assertTrue(updated.contains("largo = 150.0"))
    }

    @Test
    fun testAutocompleteBoxMethod() {
        val code = "cq.Workplane(\"XY\").bo"
        val (ghost, suggestions) = CadAutocomplete.getSuggestions(code, code.length)
        assertEquals("x", ghost)
        assertTrue(suggestions.any { it.completionText == "box" })
    }

    @Test
    fun testSyntaxHighlightingSelectors() {
        val code = "pieza.edges(\"|Z and >Y\").fillet(5)"
        val annotated = CadSyntaxHighlighter.highlight(code)
        assertNotNull(annotated)
        assertTrue(annotated.text == code)
    }

    @Test
    fun testExportersGenerateValidOutput() {
        val box = CadGeometryBuilder.createBox(name = "TestBox", length = 50f, width = 40f, height = 30f)
        val objects = listOf(box)

        val stlAscii = CadExporter.exportAsciiStl(objects)
        assertTrue(stlAscii.startsWith("solid"))
        assertTrue(stlAscii.contains("facet normal"))
        assertTrue(stlAscii.contains("endsolid"))

        val stlBinary = CadExporter.exportBinaryStl(objects)
        assertTrue(stlBinary.size > 84)

        val obj = CadExporter.exportObj(objects)
        assertTrue(obj.contains("v "))
        assertTrue(obj.contains("vn "))
        assertTrue(obj.contains("f "))

        val step = CadExporter.exportStep(objects)
        assertTrue(step.startsWith("ISO-10303-21;"))
        assertTrue(step.contains("END-ISO-10303-21;"))
    }
}
