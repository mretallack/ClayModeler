package com.claymodeler.renderer

import com.claymodeler.model.ClayModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class ModelRendererTest : FunSpec({
    
    test("default model has vertices") {
        val model = ClayModel()
        model.initialize(subdivisions = 3)
        
        model.vertices.size shouldBeGreaterThan 0
        model.faces.size shouldBeGreaterThan 0
        model.normals.size shouldBe model.vertices.size
    }
    
    test("model with subdivision 3 has expected complexity") {
        val model = ClayModel()
        model.initialize(subdivisions = 3)
        
        // 3 subdivisions should create ~1280 faces
        model.faces.size shouldBeGreaterThan 1000
        model.vertices.size shouldBeGreaterThan 600
    }
})
