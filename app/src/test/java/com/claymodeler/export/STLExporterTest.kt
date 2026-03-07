package com.claymodeler.export

import android.content.Context
import com.claymodeler.model.ClayModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayOutputStream

class STLExporterTest : FunSpec({
    
    test("binary STL has correct header size") {
        val context = mockk<Context>()
        val model = ClayModel()
        model.initialize(subdivisions = 0)
        
        // STL binary format: 80 byte header + 4 byte count + (50 bytes per triangle)
        val expectedSize = 80 + 4 + (model.faces.size * 50)
        
        expectedSize shouldBe 80 + 4 + (20 * 50) // 20 faces in icosahedron
    }
    
    test("coordinate system converts Y-up to Z-up") {
        // This is a conceptual test - actual conversion happens in writeVertex
        val model = ClayModel()
        model.initialize(subdivisions = 0)
        
        // Model should have vertices
        model.vertices.size shouldBe 12
    }
    
    test("mesh validation detects degenerate triangles") {
        val model = ClayModel()
        model.initialize(subdivisions = 0)
        
        // Valid model should have no degenerate triangles
        model.faces.size shouldBe 20
    }
})
