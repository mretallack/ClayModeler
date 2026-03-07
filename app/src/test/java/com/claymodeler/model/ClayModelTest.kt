package com.claymodeler.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class ClayModelTest : FunSpec({
    
    test("initialize creates sphere with vertices") {
        val model = ClayModel()
        model.initialize(subdivisions = 0)
        
        model.vertices.size shouldBe 12 // Icosahedron has 12 vertices
    }
    
    test("initialize creates sphere with faces") {
        val model = ClayModel()
        model.initialize(subdivisions = 0)
        
        model.faces.size shouldBe 20 // Icosahedron has 20 faces
    }
    
    test("subdivision increases vertex count") {
        val model = ClayModel()
        model.initialize(subdivisions = 1)
        
        model.vertices.size shouldBeGreaterThan 12
    }
    
    test("subdivision increases face count") {
        val model = ClayModel()
        model.initialize(subdivisions = 1)
        
        model.faces.size shouldBeGreaterThan 20
    }
    
    test("default subdivision level creates reasonable mesh") {
        val model = ClayModel()
        model.initialize(subdivisions = 3)
        
        // 3 subdivisions should create ~1280 faces
        model.faces.size shouldBeGreaterThan 1000
        model.vertices.size shouldBeGreaterThan 600
    }
    
    test("normals are calculated for all vertices") {
        val model = ClayModel()
        model.initialize(subdivisions = 2)
        
        model.normals.size shouldBe model.vertices.size
    }
    
    test("all vertices are on unit sphere surface") {
        val model = ClayModel()
        model.initialize(subdivisions = 2)
        
        for (vertex in model.vertices) {
            val distance = vertex.length()
            // Should be approximately 1.0 (unit sphere)
            assert(distance > 0.99f && distance < 1.01f) {
                "Vertex not on sphere surface: distance = $distance"
            }
        }
    }
    
    test("clone creates independent copy") {
        val original = ClayModel()
        original.initialize(subdivisions = 1)
        
        val copy = original.clone()
        
        copy.vertices.size shouldBe original.vertices.size
        copy.faces.size shouldBe original.faces.size
        copy.normals.size shouldBe original.normals.size
        
        // Modify copy
        copy.vertices.clear()
        
        // Original should be unchanged
        original.vertices.size shouldBeGreaterThan 0
    }
    
    test("faces reference valid vertex indices") {
        val model = ClayModel()
        model.initialize(subdivisions = 2)
        
        val maxIndex = model.vertices.size - 1
        
        for (face in model.faces) {
            assert(face.v1 in 0..maxIndex) { "Invalid vertex index: ${face.v1}" }
            assert(face.v2 in 0..maxIndex) { "Invalid vertex index: ${face.v2}" }
            assert(face.v3 in 0..maxIndex) { "Invalid vertex index: ${face.v3}" }
        }
    }
})
