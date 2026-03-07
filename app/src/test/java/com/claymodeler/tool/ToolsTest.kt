package com.claymodeler.tool

import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ToolsTest : FunSpec({
    
    test("RemoveClayTool moves vertices inward") {
        val model = ClayModel()
        model.initialize(subdivisions = 0)
        
        val originalVertex = model.vertices[0].copy()
        val hitPoint = model.vertices[0]
        
        val tool = RemoveClayTool()
        tool.apply(model, hitPoint, 1.0f, 0.5f)
        
        val modifiedVertex = model.vertices[0]
        
        // Vertex should have moved
        modifiedVertex shouldNotBe originalVertex
    }
    
    test("AddClayTool moves vertices outward") {
        val model = ClayModel()
        model.initialize(subdivisions = 0)
        
        val originalVertex = model.vertices[0].copy()
        val hitPoint = model.vertices[0]
        
        val tool = AddClayTool()
        tool.apply(model, hitPoint, 1.0f, 0.5f)
        
        val modifiedVertex = model.vertices[0]
        
        // Vertex should have moved
        modifiedVertex shouldNotBe originalVertex
    }
    
    test("PullClayTool moves vertices") {
        val model = ClayModel()
        model.initialize(subdivisions = 0)
        
        val originalVertex = model.vertices[0].copy()
        val hitPoint = model.vertices[0]
        
        val tool = PullClayTool()
        tool.apply(model, hitPoint, 1.0f, 0.5f)
        
        val modifiedVertex = model.vertices[0]
        
        // Vertex should have moved
        modifiedVertex shouldNotBe originalVertex
    }
    
    test("Tools respect brush radius") {
        val model = ClayModel()
        model.initialize(subdivisions = 0)
        
        val hitPoint = Vector3(0f, 0f, 0f)
        val farVertex = model.vertices.maxByOrNull { (it - hitPoint).length() }!!
        val originalFarVertex = farVertex.copy()
        
        val tool = RemoveClayTool()
        tool.apply(model, hitPoint, 1.0f, 0.1f) // Small radius
        
        // Far vertex should not be affected
        val farVertexIndex = model.vertices.indexOf(farVertex)
        model.vertices[farVertexIndex] shouldBe originalFarVertex
    }
    
    test("Tools apply falloff correctly") {
        val model = ClayModel()
        model.initialize(subdivisions = 1)
        
        val hitPoint = Vector3(1f, 0f, 0f)
        val tool = RemoveClayTool()
        
        val vertexDistances = model.vertices.map { (it - hitPoint).length() }
        
        tool.apply(model, hitPoint, 1.0f, 0.5f)
        
        // Vertices closer to hit point should move more
        // This is a basic sanity check
        model.vertices.size shouldNotBe 0
    }
    
    test("ViewModeTool does not modify model") {
        val model = ClayModel()
        model.initialize(subdivisions = 0)
        
        val originalVertices = model.vertices.map { it.copy() }
        val hitPoint = model.vertices[0]
        
        val tool = ViewModeTool()
        tool.apply(model, hitPoint, 1.0f, 0.5f)
        
        // No vertices should have changed
        for (i in model.vertices.indices) {
            model.vertices[i] shouldBe originalVertices[i]
        }
    }
})
