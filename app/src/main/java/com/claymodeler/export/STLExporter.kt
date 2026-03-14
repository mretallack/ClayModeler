package com.claymodeler.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class STLExporter(private val context: Context) {
    
    fun exportBinary(
        model: ClayModel,
        filename: String,
        sizeInMm: Float = 100f,
        validate: Boolean = true
    ): String {
        if (validate) {
            validateMesh(model)
        }
        return writeStl(model, filename, sizeInMm)
    }

    fun exportWithConfiguration(
        model: ClayModel,
        filename: String,
        config: com.claymodeler.export.ExportConfiguration
    ): String {
        val finalModel = if (config.attachmentType != com.claymodeler.export.AttachmentType.NONE) {
            val attachment = when (config.attachmentType) {
                com.claymodeler.export.AttachmentType.BASE ->
                    com.claymodeler.export.geometry.BaseGenerator().generate(model, config.baseConfig, config.sizeInMm)
                com.claymodeler.export.AttachmentType.KEYRING_LOOP ->
                    if (config.placement != null) com.claymodeler.export.geometry.LoopGenerator().generate(model, config.keyringConfig, config.placement!!, config.sizeInMm) else null
                com.claymodeler.export.AttachmentType.WALL_HOOK ->
                    if (config.placement != null) com.claymodeler.export.geometry.HookGenerator().generate(model, config.hookConfig, config.placement!!, config.sizeInMm) else null
                com.claymodeler.export.AttachmentType.NONE -> null
            }
            if (attachment != null) {
                val merged = com.claymodeler.export.geometry.GeometryMerger().merge(model, attachment)
                val manifold = com.claymodeler.export.geometry.GeometryMerger().validateManifold(merged)
                if (!manifold.valid) {
                    android.util.Log.w("STLExporter", "Manifold issues: ${manifold.issues}")
                }
                merged
            } else model
        } else model

        validateMesh(finalModel)
        return writeStl(finalModel, filename, config.sizeInMm)
    }

    private fun writeStl(model: ClayModel, filename: String, sizeInMm: Float): String {
        
        // Calculate scale factor (model is normalized to ~1 unit)
        val scale = sizeInMm
        
        // Create output stream using MediaStore
        val outputStream = createOutputStream(filename)
        
        try {
            // Write header (80 bytes)
            val header = ByteArray(80)
            "Binary STL exported from ClayModeller".toByteArray().copyInto(header)
            outputStream.write(header)
            
            // Write triangle count
            val triangleCount = model.faces.size
            val countBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
            countBuffer.putInt(triangleCount)
            outputStream.write(countBuffer.array())
            
            // Write triangles
            for (face in model.faces) {
                val v0 = model.vertices[face.v1]
                val v1 = model.vertices[face.v2]
                val v2 = model.vertices[face.v3]
                
                // Calculate face normal
                val edge1 = v1 - v0
                val edge2 = v2 - v0
                val normal = edge1.cross(edge2).normalize()
                
                // Convert to Z-up coordinate system and scale
                val buffer = ByteBuffer.allocate(50).order(ByteOrder.LITTLE_ENDIAN)
                
                // Normal (Z-up: swap Y and Z)
                buffer.putFloat(normal.x)
                buffer.putFloat(normal.z)
                buffer.putFloat(normal.y)
                
                // Vertices (Z-up and scaled)
                writeVertex(buffer, v0, scale)
                writeVertex(buffer, v1, scale)
                writeVertex(buffer, v2, scale)
                
                // Attribute byte count (unused)
                buffer.putShort(0)
                
                outputStream.write(buffer.array())
            }
            
            outputStream.flush()
            return "Downloads/$filename.stl"
        } finally {
            outputStream.close()
        }
    }
    
    private fun writeVertex(buffer: ByteBuffer, vertex: Vector3, scale: Float) {
        // Convert Y-up to Z-up and scale
        buffer.putFloat(vertex.x * scale)
        buffer.putFloat(vertex.z * scale)
        buffer.putFloat(vertex.y * scale)
    }
    
    private fun createOutputStream(filename: String): OutputStream {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - use MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$filename.stl")
                put(MediaStore.MediaColumns.MIME_TYPE, "application/sla")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw IllegalStateException("Failed to create file")
            
            context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Failed to open output stream")
        } else {
            // Android 9 and below
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, "$filename.stl")
            file.outputStream()
        }
    }
    
    private fun validateMesh(model: ClayModel) {
        var degenerateCount = 0
        
        for (face in model.faces) {
            val v0 = model.vertices[face.v1]
            val v1 = model.vertices[face.v2]
            val v2 = model.vertices[face.v3]
            
            // Check for degenerate triangles
            val edge1 = v1 - v0
            val edge2 = v2 - v0
            val cross = edge1.cross(edge2)
            
            if (cross.length() < 0.0001f) {
                degenerateCount++
            }
        }
        
        if (degenerateCount > 0) {
            // Log warning but don't fail
            android.util.Log.w("STLExporter", "Found $degenerateCount degenerate triangles")
        }
    }
}
