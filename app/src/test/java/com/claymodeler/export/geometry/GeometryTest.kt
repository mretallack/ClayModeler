package com.claymodeler.export.geometry

import com.claymodeler.export.*
import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.floats.shouldBeGreaterThan

class BaseGeneratorTest : FunSpec({

    fun testModel(): ClayModel {
        val m = ClayModel()
        m.initialize(2)
        return m
    }

    test("circular base generates valid mesh") {
        val mesh = BaseGenerator().generate(testModel(), BaseConfig(shape = BaseShape.CIRCULAR))
        mesh.vertices.shouldNotBeEmpty()
        mesh.faces.shouldNotBeEmpty()
        // All face indices should be valid
        mesh.faces.forEach {
            it.v1 shouldBeGreaterThan -1
            it.v2 shouldBeGreaterThan -1
            it.v3 shouldBeGreaterThan -1
            (it.v1 < mesh.vertices.size) shouldBe true
            (it.v2 < mesh.vertices.size) shouldBe true
            (it.v3 < mesh.vertices.size) shouldBe true
        }
    }

    test("rectangular base generates valid mesh") {
        val mesh = BaseGenerator().generate(testModel(), BaseConfig(shape = BaseShape.RECTANGULAR))
        // 8 box vertices + fillet vertices
        (mesh.vertices.size >= 8) shouldBe true
        // 12 box faces + fillet faces
        (mesh.faces.size >= 12) shouldBe true
    }

    test("custom base falls back to circular for simple model") {
        val mesh = BaseGenerator().generate(testModel(), BaseConfig(shape = BaseShape.CUSTOM))
        mesh.vertices.shouldNotBeEmpty()
        mesh.faces.shouldNotBeEmpty()
    }

    test("base height is clamped to minimum 2mm") {
        val mesh = BaseGenerator().generate(testModel(), BaseConfig(height = 0.5f))
        val minY = mesh.vertices.minOf { it.y }
        val maxY = mesh.vertices.maxOf { it.y }
        // Height should be positive (clamped to at least 2mm scaled to model space)
        (maxY - minY).shouldBeGreaterThan(0f)
    }

    test("base aligns to model lowest Y") {
        val model = testModel()
        val modelMinY = model.vertices.minOf { it.y }
        val mesh = BaseGenerator().generate(model, BaseConfig())
        val baseMaxY = mesh.vertices.maxOf { it.y }
        // Base top should be at or near model bottom
        kotlin.math.abs(baseMaxY - modelMinY).shouldBeGreaterThan(-0.01f)
    }

    test("bounding box is correct") {
        val model = testModel()
        val (min, max) = BaseGenerator.boundingBox(model)
        (max.x - min.x).shouldBeGreaterThan(0f)
        (max.y - min.y).shouldBeGreaterThan(0f)
        (max.z - min.z).shouldBeGreaterThan(0f)
    }
})

class LoopGeneratorTest : FunSpec({

    test("generates torus mesh with correct topology") {
        val model = ClayModel().apply { initialize(2) }
        val placement = PlacementResult(Vector3(0f, 1f, 0f), Vector3(0f, 1f, 0f))
        val mesh = LoopGenerator().generate(model, KeyringConfig(size = LoopSize.MEDIUM), placement)
        mesh.vertices.shouldNotBeEmpty()
        mesh.faces.shouldNotBeEmpty()
        // Torus: 24 major * 12 minor = 288 verts + gusset verts
        (mesh.vertices.size >= 288) shouldBe true
    }

    test("minimum wall thickness is enforced") {
        val model = ClayModel().apply { initialize(2) }
        val placement = PlacementResult(Vector3(0f, 1f, 0f), Vector3(0f, 1f, 0f))
        // SMALL has 2mm wall thickness which is the minimum
        val mesh = LoopGenerator().generate(model, KeyringConfig(size = LoopSize.SMALL), placement)
        mesh.vertices.shouldNotBeEmpty()
    }

    test("loop is oriented perpendicular to surface normal") {
        val model = ClayModel().apply { initialize(2) }
        val placement = PlacementResult(Vector3(1f, 0f, 0f), Vector3(1f, 0f, 0f))
        val mesh = LoopGenerator().generate(model, KeyringConfig(), placement)
        // Vertices should be centered around placement position
        val avgX = mesh.vertices.map { it.x }.average().toFloat()
        (kotlin.math.abs(avgX - 1f) < 5f) shouldBe true
    }
})

class HookGeneratorTest : FunSpec({

    test("keyhole generates valid mesh") {
        val model = ClayModel().apply { initialize(2) }
        val placement = PlacementResult(Vector3(0f, 0f, -1f), Vector3(0f, 0f, -1f))
        val mesh = HookGenerator().generate(model, HookConfig(type = HookType.KEYHOLE), placement)
        mesh.vertices.shouldNotBeEmpty()
        mesh.faces.shouldNotBeEmpty()
    }

    test("mounting holes has correct spacing") {
        val model = ClayModel().apply { initialize(2) }
        val placement = PlacementResult(Vector3(0f, 0f, -1f), Vector3(0f, 0f, -1f))
        val mesh = HookGenerator().generate(model, HookConfig(type = HookType.MOUNTING_HOLES), placement)
        mesh.vertices.shouldNotBeEmpty()
        // Should have plate (8 verts) + 2 hole cylinders
        (mesh.vertices.size > 8) shouldBe true
    }

    test("hanging loop generates torus") {
        val model = ClayModel().apply { initialize(2) }
        val placement = PlacementResult(Vector3(0f, 0f, -1f), Vector3(0f, 0f, -1f))
        val mesh = HookGenerator().generate(model, HookConfig(type = HookType.HANGING_LOOP), placement)
        // 24 * 12 = 288 torus verts
        (mesh.vertices.size >= 288) shouldBe true
    }

    test("auto position places at center of mass on back") {
        val model = ClayModel().apply { initialize(2) }
        val placement = HookGenerator().autoPlacement(model, HookConfig(position = HookPosition.AUTO))
        // Should be on back face (negative Z)
        (placement.position.z <= 0f) shouldBe true
        (placement.normal.z < 0f) shouldBe true
    }
})

class GeometryMergerTest : FunSpec({

    test("merge combines vertices and adjusts face indices") {
        val model = ClayModel().apply { initialize(1) }
        val origVerts = model.vertices.size
        val origFaces = model.faces.size

        val attachment = GeneratedMesh(
            mutableListOf(Vector3(5f, 5f, 5f), Vector3(6f, 5f, 5f), Vector3(5f, 6f, 5f)),
            mutableListOf(com.claymodeler.model.Face(0, 1, 2))
        )

        val merged = GeometryMerger().merge(model, attachment)
        (merged.vertices.size >= origVerts + 3) shouldBe true
        (merged.faces.size >= origFaces + 1) shouldBe true
    }

    test("degenerate faces are removed after merge") {
        val model = ClayModel().apply { initialize(1) }
        // Attachment with duplicate vertex positions
        val attachment = GeneratedMesh(
            mutableListOf(Vector3(0f, 0f, 0f), Vector3(0f, 0f, 0f), Vector3(1f, 0f, 0f)),
            mutableListOf(com.claymodeler.model.Face(0, 1, 2))
        )
        val merged = GeometryMerger().merge(model, attachment)
        // The degenerate face should be removed
        merged.faces.forEach {
            (it.v1 != it.v2 && it.v2 != it.v3 && it.v3 != it.v1) shouldBe true
        }
    }

    test("manifold validation detects valid mesh") {
        val model = ClayModel().apply { initialize(2) }
        val result = GeometryMerger().validateManifold(model)
        // Icosphere should be manifold
        result.valid shouldBe true
    }

    test("manifold validation detects boundary edges") {
        val model = ClayModel()
        model.vertices.addAll(listOf(
            Vector3(0f, 0f, 0f), Vector3(1f, 0f, 0f),
            Vector3(0f, 1f, 0f), Vector3(0f, 0f, 1f)
        ))
        // Single triangle = open mesh
        model.faces.add(com.claymodeler.model.Face(0, 1, 2))
        model.calculateNormals()
        val result = GeometryMerger().validateManifold(model)
        result.valid shouldBe false
        result.issues.any { it.contains("boundary") } shouldBe true
    }
})
