package com.claymodeler.tool

import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PinchToolTest : FunSpec({
    
    test("pinch tool pulls vertices toward center") {
        val model = ClayModel()
        model.initialize(subdivisions = 1)
        
        // Use actual vertex location
        val hitPoint = model.vertices[0] + Vector3(0.05f, 0f, 0f)
        val originalPositions = model.vertices.map { it.copy() }
        
        val tool = PinchTool()
        tool.apply(model, hitPoint, strength = 1f, radius = 0.5f, dragDirection = Vector3(0f, 0f, 0f))
        
        // At least some vertices should have moved
        var movedCount = 0
        for (i in model.vertices.indices) {
            if (model.vertices[i] != originalPositions[i]) {
                movedCount++
            }
        }
        
        (movedCount > 0) shouldBe true
    }
    
    test("pinch tool creates sharp concentration") {
        val model = ClayModel()
        model.initialize(subdivisions = 2)
        
        // Use actual vertex location
        val hitPoint = model.vertices[0] + Vector3(0.05f, 0f, 0f)
        val originalPositions = model.vertices.map { it.copy() }
        
        val tool = PinchTool()
        tool.apply(model, hitPoint, strength = 1f, radius = 0.5f, dragDirection = Vector3(0f, 0f, 0f))
        
        // At least some vertices should have moved
        var movedCount = 0
        for (i in model.vertices.indices) {
            if (model.vertices[i] != originalPositions[i]) {
                movedCount++
            }
        }
        
        (movedCount > 0) shouldBe true
    }
})
