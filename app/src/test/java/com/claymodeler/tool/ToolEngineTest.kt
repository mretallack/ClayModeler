package com.claymodeler.tool

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ToolEngineTest : FunSpec({
    
    test("setActiveTool changes active tool") {
        val engine = ToolEngine()
        val tool = RemoveClayTool()
        
        engine.setActiveTool(tool)
        
        engine.getActiveTool() shouldBe tool
    }
    
    test("isEditMode returns true for edit tools") {
        val engine = ToolEngine()
        engine.setActiveTool(RemoveClayTool())
        
        engine.isEditMode() shouldBe true
    }
    
    test("isEditMode returns false for view mode") {
        val engine = ToolEngine()
        engine.setActiveTool(ViewModeTool())
        
        engine.isEditMode() shouldBe false
    }
    
    test("brushSize is clamped to valid range") {
        val engine = ToolEngine()
        
        engine.brushSize = -1f
        engine.brushSize shouldBe 0.1f
        
        engine.brushSize = 5f
        engine.brushSize shouldBe 2.0f
        
        engine.brushSize = 1.0f
        engine.brushSize shouldBe 1.0f
    }
    
    test("strength is clamped to valid range") {
        val engine = ToolEngine()
        
        engine.strength = -1f
        engine.strength shouldBe 0.1f
        
        engine.strength = 5f
        engine.strength shouldBe 1.0f
        
        engine.strength = 0.5f
        engine.strength shouldBe 0.5f
    }
    
    test("all tool types have correct names") {
        RemoveClayTool().getName() shouldBe "Remove"
        AddClayTool().getName() shouldBe "Add"
        PullClayTool().getName() shouldBe "Pull"
        ViewModeTool().getName() shouldBe "View"
    }
    
    test("ViewModeTool is not an edit tool") {
        ViewModeTool().isEditTool() shouldBe false
    }
    
    test("edit tools return true for isEditTool") {
        RemoveClayTool().isEditTool() shouldBe true
        AddClayTool().isEditTool() shouldBe true
        PullClayTool().isEditTool() shouldBe true
    }
})
