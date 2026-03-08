package com.claymodeler.tool

import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.math.abs

class SmoothToolTest : FunSpec({
    
    test("smooth tool averages neighboring vertices") {
        val model = ClayModel()
        model.initialize(subdivisions = 1)
        
        val hitPoint = model.vertices[0]
        val originalVertex = model.vertices[0].copy()
        
        val tool = SmoothTool()
        tool.apply(model, hitPoint, strength = 1f, radius = 0.5f, dragDirection = Vector3(0f, 0f, 0f))
        
        // Vertex should have moved toward average of neighbors
        model.vertices[0] shouldNotBe originalVertex
    }
    
    test("smooth tool respects strength parameter") {
        val model1 = ClayModel()
        model1.initialize(subdivisions = 1)
        val model2 = ClayModel()
        model2.initialize(subdivisions = 1)
        
        val hitPoint = model1.vertices[0]
        val tool = SmoothTool()
        
        // Apply with different strengths
        tool.apply(model1, hitPoint, strength = 0.5f, radius = 0.5f, dragDirection = Vector3(0f, 0f, 0f))
        tool.apply(model2, hitPoint, strength = 1.0f, radius = 0.5f, dragDirection = Vector3(0f, 0f, 0f))
        
        // Higher strength should move vertex more
        val movement1 = (model1.vertices[0] - hitPoint).length()
        val movement2 = (model2.vertices[0] - hitPoint).length()
        
        (movement2 > movement1) shouldBe true
    }
    
    test("smooth tool respects radius") {
        val model = ClayModel()
        model.initialize(subdivisions = 1)
        
        val hitPoint = Vector3(0f, 0f, 0f)
        val farVertex = model.vertices.maxByOrNull { (it - hitPoint).length() }!!
        val farVertexIndex = model.vertices.indexOf(farVertex)
        val originalFarVertex = farVertex.copy()
        
        val tool = SmoothTool()
        tool.apply(model, hitPoint, strength = 1f, radius = 0.1f, dragDirection = Vector3(0f, 0f, 0f))
        
        // Far vertex should not be affected
        model.vertices[farVertexIndex] shouldBe originalFarVertex
    }
    
    test("smooth tool preserves overall volume") {
        val model = ClayModel()
        model.initialize(subdivisions = 2)
        
        // Calculate approximate volume before
        val centerBefore = Vector3(0f, 0f, 0f)
        var avgRadiusBefore = 0f
        for (vertex in model.vertices) {
            avgRadiusBefore += (vertex - centerBefore).length()
        }
        avgRadiusBefore /= model.vertices.size
        
        val hitPoint = model.vertices[0]
        val tool = SmoothTool()
        
        // Apply smooth multiple times
        repeat(5) {
            tool.apply(model, hitPoint, strength = 0.5f, radius = 1.0f, dragDirection = Vector3(0f, 0f, 0f))
        }
        
        // Calculate approximate volume after
        val centerAfter = Vector3(0f, 0f, 0f)
        var avgRadiusAfter = 0f
        for (vertex in model.vertices) {
            avgRadiusAfter += (vertex - centerAfter).length()
        }
        avgRadiusAfter /= model.vertices.size
        
        // Volume change should be minimal (within 20%)
        val volumeChange = abs(avgRadiusAfter - avgRadiusBefore) / avgRadiusBefore
        (volumeChange < 0.2f) shouldBe true
    }
})
