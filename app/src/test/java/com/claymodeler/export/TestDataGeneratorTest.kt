package com.claymodeler.export

import com.claymodeler.export.geometry.BaseGenerator
import com.claymodeler.export.geometry.GeometryMerger
import com.claymodeler.model.ClayModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * Generates reference test data files for regression testing.
 * Run this test to regenerate expected outputs.
 */
class TestDataGeneratorTest : FunSpec({

    val outputDir = File("build/test-data")

    beforeSpec {
        outputDir.mkdirs()
    }

    test("generate reference vertex counts for all test models") {
        val models = mapOf(
            "sphere" to TestModels.sphere(),
            "cube" to TestModels.cube(),
            "flat" to TestModels.flat(),
            "tiny" to TestModels.tiny(),
            "huge" to TestModels.huge(),
            "thin" to TestModels.thin(),
            "owl" to TestModels.owl()
        )

        val reference = StringBuilder()
        for ((name, model) in models) {
            reference.appendLine("$name: vertices=${model.vertices.size} faces=${model.faces.size}")
        }
        File(outputDir, "model_reference.txt").writeText(reference.toString())

        // Verify all models are valid
        models.values.forEach { model ->
            (model.vertices.size > 0) shouldBe true
            (model.faces.size > 0) shouldBe true
            model.normals.size shouldBe model.vertices.size
        }
    }

    test("generate reference merged model stats") {
        val model = TestModels.sphere(3)
        val config = BaseConfig(shape = BaseShape.CIRCULAR, height = 3f, margin = 2f)
        val base = BaseGenerator().generate(model, config)
        val merged = GeometryMerger().merge(model, base)

        val reference = buildString {
            appendLine("original: vertices=${model.vertices.size} faces=${model.faces.size}")
            appendLine("base: vertices=${base.vertices.size} faces=${base.faces.size}")
            appendLine("merged: vertices=${merged.vertices.size} faces=${merged.faces.size}")
            val manifold = GeometryMerger().validateManifold(merged)
            appendLine("manifold_valid=${manifold.valid}")
            if (!manifold.valid) appendLine("issues=${manifold.issues}")
        }
        File(outputDir, "merge_reference.txt").writeText(reference)
    }

    test("generate reference STL size estimates") {
        val models = mapOf(
            "sphere" to TestModels.sphere(3),
            "owl" to TestModels.owl()
        )

        val reference = StringBuilder()
        for ((name, model) in models) {
            val stlBytes = 84 + model.faces.size * 50
            reference.appendLine("$name: faces=${model.faces.size} stl_bytes=$stlBytes")
        }
        File(outputDir, "stl_size_reference.txt").writeText(reference.toString())
    }
})
