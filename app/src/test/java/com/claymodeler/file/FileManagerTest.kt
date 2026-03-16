package com.claymodeler.file

import android.content.Context
import com.claymodeler.model.ClayModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import java.io.File

class FileManagerTest : FunSpec({
    
    test("save and load round trip preserves model") {
        val context = mockk<Context>()
        val tempDir = createTempDir()
        every { context.filesDir } returns tempDir
        every { context.getExternalFilesDir(null) } returns null
        
        val fileManager = FileManager(context)
        
        val originalModel = ClayModel()
        originalModel.initialize(subdivisions = 1)
        
        val originalVertexCount = originalModel.vertices.size
        val originalFaceCount = originalModel.faces.size
        
        fileManager.save(originalModel, "test")
        val loadedModel = fileManager.load("test")
        
        loadedModel.vertices.size shouldBe originalVertexCount
        loadedModel.faces.size shouldBe originalFaceCount
        loadedModel.normals.size shouldBe originalVertexCount
        
        tempDir.deleteRecursively()
    }
    
    test("save creates file with .clay extension") {
        val context = mockk<Context>()
        val tempDir = createTempDir()
        every { context.filesDir } returns tempDir
        every { context.getExternalFilesDir(null) } returns null
        
        val fileManager = FileManager(context)
        val model = ClayModel()
        model.initialize(subdivisions = 0)
        
        val file = fileManager.save(model, "mymodel")
        
        file.name shouldBe "mymodel.clay"
        file.exists() shouldBe true
        
        tempDir.deleteRecursively()
    }
    
    test("load throws exception for non-existent file") {
        val context = mockk<Context>()
        val tempDir = createTempDir()
        every { context.filesDir } returns tempDir
        every { context.getExternalFilesDir(null) } returns null
        
        val fileManager = FileManager(context)
        
        var exceptionThrown = false
        try {
            fileManager.load("nonexistent")
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        
        exceptionThrown shouldBe true
        
        tempDir.deleteRecursively()
    }
    
    test("listFiles returns saved models") {
        val context = mockk<Context>()
        val tempDir = createTempDir()
        every { context.filesDir } returns tempDir
        every { context.getExternalFilesDir(null) } returns null
        
        val fileManager = FileManager(context)
        val model = ClayModel()
        model.initialize(subdivisions = 0)
        
        fileManager.save(model, "model1")
        fileManager.save(model, "model2")
        
        val files = fileManager.listFiles()
        
        files.size shouldBe 2
        files.contains("model1") shouldBe true
        files.contains("model2") shouldBe true
        
        tempDir.deleteRecursively()
    }
    
    test("delete removes file") {
        val context = mockk<Context>()
        val tempDir = createTempDir()
        every { context.filesDir } returns tempDir
        every { context.getExternalFilesDir(null) } returns null
        
        val fileManager = FileManager(context)
        val model = ClayModel()
        model.initialize(subdivisions = 0)
        
        fileManager.save(model, "todelete")
        val deleted = fileManager.delete("todelete")
        
        deleted shouldBe true
        fileManager.listFiles().contains("todelete") shouldBe false
        
        tempDir.deleteRecursively()
    }
    
    test("atomic write uses temp file") {
        val context = mockk<Context>()
        val tempDir = createTempDir()
        every { context.filesDir } returns tempDir
        every { context.getExternalFilesDir(null) } returns null
        
        val fileManager = FileManager(context)
        val model = ClayModel()
        model.initialize(subdivisions = 0)
        
        fileManager.save(model, "atomic")
        
        // Temp file should not exist after save
        val tempFile = File(tempDir, "atomic.clay.tmp")
        tempFile.exists() shouldBe false
        
        tempDir.deleteRecursively()
    }
})
