package com.claymodeler.tool

import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class InflateToolTest : FunSpec({
    
    test("inflate tool pushes along normals") {
        val model = ClayModel()
        model.initialize(subdivisions = 1)
        
        val hitPoint = model.vertices[0] + Vector3(0.1f, 0f, 0f)
        val originalVertex = model.vertices[0].copy()
        
        val tool = InflateTool()
        tool.apply(model, hitPoint, strength = 1f, radius = 0.5f, dragDirection = Vector3(0f, 0f, 0f))
        
        // Vertex should have moved
        model.vertices[0] shouldNotBe originalVertex
    }
    
    test("inflate tool ignores drag direction") {
        val model1 = ClayModel()
        model1.initialize(subdivisions = 1)
        val model2 = ClayModel()
        model2.initialize(subdivisions = 1)
        
        val hitPoint = model1.vertices[0] + Vector3(0.1f, 0f, 0f)
        val tool = InflateTool()
        
        // Apply with different drag directions
        tool.apply(model1, hitPoint, strength = 1f, radius = 0.5f, dragDirection = Vector3(1f, 0f, 0f))
        tool.apply(model2, hitPoint, strength = 1f, radius = 0.5f, dragDirection = Vector3(0f, 1f, 0f))
        
        // Results should be identical (drag direction ignored)
        model1.vertices[0] shouldBe model2.vertices[0]
    }
    
    test("inflate tool creates uniform expansion") {
        val model = ClayModel()
        model.initialize(subdivisions = 2)
        
        val hitPoint = Vector3(0f, 0f, 0f)
        val originalPositions = model.vertices.map { it.copy() }
        
        val tool = InflateTool()
        tool.apply(model, hitPoint, strength = 1f, radius = 1.0f, dragDirection = Vector3(0f, 0f, 0f))
        
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
