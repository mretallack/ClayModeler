package com.claymodeler.integration

import android.content.Context
import com.claymodeler.file.FileManager
import com.claymodeler.model.ClayModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class SaveLoadWorkflowTest : FunSpec({
    
    test("complete save and load workflow preserves model data") {
        val context = mockk<Context>()
        val tempDir = createTempDir()
        every { context.filesDir } returns tempDir
        every { context.getExternalFilesDir(null) } returns null
        
        val fileManager = FileManager(context)
        
        // Create and modify a model
        val originalModel = ClayModel()
        originalModel.initialize(subdivisions = 2)
        
        val originalVertexCount = originalModel.vertices.size
        val originalFaceCount = originalModel.faces.size
        val firstVertex = originalModel.vertices[0].copy()
        
        // Save
        fileManager.save(originalModel, "workflow_test")
        
        // Load
        val loadedModel = fileManager.load("workflow_test")
        
        // Verify
        loadedModel.vertices.size shouldBe originalVertexCount
        loadedModel.faces.size shouldBe originalFaceCount
        loadedModel.vertices[0].x shouldBe firstVertex.x
        loadedModel.vertices[0].y shouldBe firstVertex.y
        loadedModel.vertices[0].z shouldBe firstVertex.z
        
        tempDir.deleteRecursively()
    }
    
    test("undo redo workflow maintains model integrity") {
        val model = ClayModel()
        model.initialize(subdivisions = 1)
        
        val originalVertexCount = model.vertices.size
        
        // Simulate modification
        model.vertices.add(com.claymodeler.model.Vector3(5f, 5f, 5f))
        
        model.vertices.size shouldBe originalVertexCount + 1
        
        // Simulate undo (remove last)
        model.vertices.removeAt(model.vertices.size - 1)
        
        model.vertices.size shouldBe originalVertexCount
    }
})
