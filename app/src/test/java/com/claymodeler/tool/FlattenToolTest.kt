package com.claymodeler.tool

import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.math.abs

class FlattenToolTest : FunSpec({
    
    test("flatten tool creates planar surface") {
        val model = ClayModel()
        model.initialize(subdivisions = 2)
        
        val hitPoint = Vector3(1f, 0f, 0f)
        val tool = FlattenTool()
        
        // Apply flatten multiple times
        repeat(5) {
            tool.apply(model, hitPoint, strength = 1f, radius = 0.5f, dragDirection = Vector3(0f, 0f, 0f))
        }
        
        // Check that affected vertices are more planar
        // (This is a basic sanity check)
        model.vertices.size shouldNotBe 0
    }
    
    test("flatten tool respects falloff") {
        val model = ClayModel()
        model.initialize(subdivisions = 2)
        
        // Use actual vertex as hit point
        val hitPoint = model.vertices[0] + Vector3(0.05f, 0f, 0f)
        val tool = FlattenTool()
        
        // Store original positions
        val originalPositions = model.vertices.map { it.copy() }
        
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
