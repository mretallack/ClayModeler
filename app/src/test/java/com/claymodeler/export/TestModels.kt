package com.claymodeler.export

import com.claymodeler.model.ClayModel
import com.claymodeler.model.Face
import com.claymodeler.model.Vector3
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Test data factory for various model shapes used across test suites.
 */
object TestModels {

    /** Standard icosphere */
    fun sphere(subdivisions: Int = 2): ClayModel = ClayModel().apply { initialize(subdivisions) }

    /** Cube (6 faces, 8 vertices) */
    fun cube(): ClayModel = ClayModel().apply {
        val s = 0.5f
        vertices.addAll(listOf(
            Vector3(-s, -s, -s), Vector3(s, -s, -s), Vector3(s, s, -s), Vector3(-s, s, -s),
            Vector3(-s, -s, s), Vector3(s, -s, s), Vector3(s, s, s), Vector3(-s, s, s)
        ))
        faces.addAll(listOf(
            Face(0, 2, 1), Face(0, 3, 2), Face(4, 5, 6), Face(4, 6, 7),
            Face(0, 1, 5), Face(0, 5, 4), Face(2, 3, 7), Face(2, 7, 6),
            Face(0, 4, 7), Face(0, 7, 3), Face(1, 2, 6), Face(1, 6, 5)
        ))
        calculateNormals()
    }

    /** Thin flat disc */
    fun flat(): ClayModel {
        val m = sphere(2)
        // Flatten Y axis
        for (i in m.vertices.indices) {
            val v = m.vertices[i]
            m.vertices[i] = Vector3(v.x, v.y * 0.1f, v.z)
        }
        m.calculateNormals()
        return m
    }

    /** Tiny model (scaled down) */
    fun tiny(): ClayModel {
        val m = sphere(1)
        for (i in m.vertices.indices) {
            m.vertices[i] = m.vertices[i] * 0.01f
        }
        m.calculateNormals()
        return m
    }

    /** Huge model (scaled up) */
    fun huge(): ClayModel {
        val m = sphere(1)
        for (i in m.vertices.indices) {
            m.vertices[i] = m.vertices[i] * 100f
        }
        m.calculateNormals()
        return m
    }

    /** Thin elongated model */
    fun thin(): ClayModel {
        val m = sphere(2)
        for (i in m.vertices.indices) {
            val v = m.vertices[i]
            m.vertices[i] = Vector3(v.x * 0.1f, v.y * 5f, v.z * 0.1f)
        }
        m.calculateNormals()
        return m
    }

    /** Owl-like model: sphere with ear bumps and flattened face */
    fun owl(): ClayModel {
        val m = sphere(3)
        for (i in m.vertices.indices) {
            val v = m.vertices[i]
            // Flatten front face
            var z = v.z
            if (z > 0.5f) z = 0.5f + (z - 0.5f) * 0.3f
            // Add ear bumps
            var y = v.y
            if (y > 0.6f && kotlin.math.abs(v.x) > 0.3f) {
                y += 0.3f * (y - 0.6f)
            }
            // Slight body taper at bottom
            val xScale = if (v.y < -0.3f) 0.8f else 1f
            m.vertices[i] = Vector3(v.x * xScale, y, z)
        }
        m.calculateNormals()
        return m
    }
}

class TestModelsTest : FunSpec({

    test("sphere model is valid") {
        val m = TestModels.sphere()
        m.vertices.shouldNotBeEmpty()
        m.faces.shouldNotBeEmpty()
        m.normals.size shouldBe m.vertices.size
    }

    test("cube model has correct topology") {
        val m = TestModels.cube()
        m.vertices.size shouldBe 8
        m.faces.size shouldBe 12
    }

    test("flat model is flattened") {
        val m = TestModels.flat()
        val yRange = m.vertices.maxOf { it.y } - m.vertices.minOf { it.y }
        val xRange = m.vertices.maxOf { it.x } - m.vertices.minOf { it.x }
        (xRange > yRange * 5) shouldBe true
    }

    test("tiny model is very small") {
        val m = TestModels.tiny()
        val maxDim = maxOf(
            m.vertices.maxOf { it.x } - m.vertices.minOf { it.x },
            m.vertices.maxOf { it.y } - m.vertices.minOf { it.y },
            m.vertices.maxOf { it.z } - m.vertices.minOf { it.z }
        )
        (maxDim < 0.1f) shouldBe true
    }

    test("huge model is very large") {
        val m = TestModels.huge()
        val maxDim = maxOf(
            m.vertices.maxOf { it.x } - m.vertices.minOf { it.x },
            m.vertices.maxOf { it.y } - m.vertices.minOf { it.y },
            m.vertices.maxOf { it.z } - m.vertices.minOf { it.z }
        )
        (maxDim > 100f) shouldBe true
    }

    test("thin model is elongated") {
        val m = TestModels.thin()
        val yRange = m.vertices.maxOf { it.y } - m.vertices.minOf { it.y }
        val xRange = m.vertices.maxOf { it.x } - m.vertices.minOf { it.x }
        (yRange > xRange * 10) shouldBe true
    }

    test("owl model has ear bumps") {
        val m = TestModels.owl()
        m.vertices.shouldNotBeEmpty()
        m.faces.shouldNotBeEmpty()
        // Owl should be taller than a plain sphere due to ears
        val yRange = m.vertices.maxOf { it.y } - m.vertices.minOf { it.y }
        (yRange > 2f) shouldBe true
    }

    test("all test models work with base generator") {
        val gen = com.claymodeler.export.geometry.BaseGenerator()
        val config = BaseConfig()
        listOf(TestModels.sphere(), TestModels.cube(), TestModels.flat(),
            TestModels.tiny(), TestModels.huge(), TestModels.thin(), TestModels.owl()).forEach { model ->
            val mesh = gen.generate(model, config)
            mesh.vertices.shouldNotBeEmpty()
            mesh.faces.shouldNotBeEmpty()
        }
    }
})
