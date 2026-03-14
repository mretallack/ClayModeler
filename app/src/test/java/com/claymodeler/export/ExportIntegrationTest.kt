package com.claymodeler.export

import com.claymodeler.export.geometry.*
import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.collections.shouldNotBeEmpty

class ExportIntegrationTest : FunSpec({

    fun testModel(subdivisions: Int = 2): ClayModel {
        val m = ClayModel()
        m.initialize(subdivisions)
        return m
    }

    test("end-to-end: model with base produces valid merged mesh") {
        val model = testModel()
        val config = BaseConfig(shape = BaseShape.CIRCULAR, height = 5f, margin = 2f)
        val base = BaseGenerator().generate(model, config)
        val merged = GeometryMerger().merge(model, base)
        merged.vertices.size shouldBeGreaterThan model.vertices.size
        merged.faces.size shouldBeGreaterThan model.faces.size
        merged.normals.shouldNotBeEmpty()
    }

    test("end-to-end: model with keyring loop produces valid merged mesh") {
        val model = testModel()
        val placement = PlacementResult(Vector3(0f, 1f, 0f), Vector3(0f, 1f, 0f), 0)
        val loop = LoopGenerator().generate(model, KeyringConfig(size = LoopSize.MEDIUM), placement)
        val merged = GeometryMerger().merge(model, loop)
        merged.vertices.size shouldBeGreaterThan model.vertices.size
    }

    test("end-to-end: model with wall hook produces valid merged mesh") {
        val model = testModel()
        val hookGen = HookGenerator()
        val placement = hookGen.autoPlacement(model, HookConfig(type = HookType.KEYHOLE))
        val hook = hookGen.generate(model, HookConfig(type = HookType.KEYHOLE), placement)
        val merged = GeometryMerger().merge(model, hook)
        merged.vertices.size shouldBeGreaterThan model.vertices.size
    }

    test("each attachment type produces manifold-checkable mesh") {
        val model = testModel()

        // Base
        val base = BaseGenerator().generate(model, BaseConfig())
        val mergedBase = GeometryMerger().merge(model, base)
        GeometryMerger().validateManifold(mergedBase) // should not throw

        // Keyring
        val placement = PlacementResult(Vector3(0f, 1f, 0f), Vector3(0f, 1f, 0f), 0)
        val loop = LoopGenerator().generate(model, KeyringConfig(), placement)
        val mergedLoop = GeometryMerger().merge(model, loop)
        GeometryMerger().validateManifold(mergedLoop)

        // Hook
        val hookPlacement = HookGenerator().autoPlacement(model, HookConfig())
        val hook = HookGenerator().generate(model, HookConfig(), hookPlacement)
        val mergedHook = GeometryMerger().merge(model, hook)
        GeometryMerger().validateManifold(mergedHook)
    }

    test("export configuration builds correctly from wizard state") {
        val config = ExportConfiguration(
            attachmentType = AttachmentType.BASE,
            scaleFactor = 1.5f,
            sizeInMm = 150f,
            baseConfig = BaseConfig(shape = BaseShape.RECTANGULAR, height = 5f),
            presetName = "test-preset"
        )
        config.attachmentType shouldBe AttachmentType.BASE
        config.sizeInMm shouldBe 150f
        config.baseConfig.shape shouldBe BaseShape.RECTANGULAR
    }

    test("file sizes are reasonable for typical models") {
        val model = testModel(3) // Higher subdivision
        val base = BaseGenerator().generate(model, BaseConfig())
        val merged = GeometryMerger().merge(model, base)
        // Binary STL: 80 header + 4 count + 50 bytes per face
        val estimatedBytes = 84 + merged.faces.size * 50
        // Should be under 10MB for a typical model
        (estimatedBytes < 10_000_000) shouldBe true
    }

    test("size warnings for tiny model") {
        val model = testModel(1) // Small subdivision
        val bb = BaseGenerator.boundingBox(model)
        val size = bb.second - bb.first
        // At scale 0.1, model would be ~20mm
        val sizeInMm = size.x * 10f
        (sizeInMm < 50f) shouldBe true // Tiny model should trigger warning
    }
})
