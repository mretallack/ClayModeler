package com.claymodeler.tool

import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ToolLocalityTest : FunSpec({
    
    test("AddClayTool should not affect opposite side of sphere") {
        val model = ClayModel()
        model.initialize(2)
        
        val hitPoint = Vector3(1f, 0f, 0f)
        val oppositeVertices = mutableMapOf<Int, Vector3>()
        
        for (i in model.vertices.indices) {
            val vertex = model.vertices[i]
            if (vertex.x < -0.5f) {
                oppositeVertices[i] = Vector3(vertex.x, vertex.y, vertex.z)
            }
        }
        
        val tool = AddClayTool()
        tool.apply(model, hitPoint, strength = 1f, radius = 0.5f, dragDirection = Vector3(0f, 0f, 0f))
        
        var maxMovement = 0f
        for ((i, originalVertex) in oppositeVertices) {
            val currentVertex = model.vertices[i]
            val moved = (currentVertex - originalVertex).length()
            if (moved > maxMovement) maxMovement = moved
        }
        
        maxMovement shouldBe 0f
    }
    
    test("RemoveClayTool should not affect opposite side of sphere") {
        val model = ClayModel()
        model.initialize(2)
        
        val hitPoint = Vector3(1f, 0f, 0f)
        val oppositeVertices = mutableMapOf<Int, Vector3>()
        
        for (i in model.vertices.indices) {
            val vertex = model.vertices[i]
            if (vertex.x < -0.5f) {
                oppositeVertices[i] = Vector3(vertex.x, vertex.y, vertex.z)
            }
        }
        
        val tool = RemoveClayTool()
        tool.apply(model, hitPoint, strength = 1f, radius = 0.5f, dragDirection = Vector3(0f, 0f, 0f))
        
        var maxMovement = 0f
        for ((i, originalVertex) in oppositeVertices) {
            val currentVertex = model.vertices[i]
            val moved = (currentVertex - originalVertex).length()
            if (moved > maxMovement) maxMovement = moved
        }
        
        maxMovement shouldBe 0f
    }
})
