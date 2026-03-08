package com.claymodeler.file

import android.content.Context
import com.claymodeler.model.ClayModel
import com.claymodeler.model.Face
import com.claymodeler.model.Vector3
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

class FileManager(private val context: Context) {
    
    companion object {
        private const val MAGIC_NUMBER = 0x434C4159 // "CLAY"
        private const val VERSION = 1
        private const val HEADER_SIZE = 16 // magic(4) + version(4) + metadataSize(4) + checksum(4)
    }
    
    fun save(model: ClayModel, filename: String): File {
        val file = File(context.filesDir, "$filename.clay")
        val tempFile = File(context.filesDir, "$filename.clay.tmp")
        
        try {
            // Create metadata (simple key=value format)
            val metadata = buildString {
                appendLine("name=$filename")
                appendLine("created=${System.currentTimeMillis()}")
                appendLine("vertexCount=${model.vertices.size}")
                appendLine("faceCount=${model.faces.size}")
                appendLine("light_x=${model.lightPosition.x}")
                appendLine("light_y=${model.lightPosition.y}")
                appendLine("light_z=${model.lightPosition.z}")
                appendLine("light_intensity=${model.lightIntensity}")
            }
            val metadataBytes = metadata.toByteArray(Charsets.UTF_8)
            
            // Calculate data size
            val vertexDataSize = model.vertices.size * 12 // 3 floats per vertex
            val faceDataSize = model.faces.size * 12 // 3 ints per face
            val normalDataSize = model.normals.size * 12 // 3 floats per normal
            val totalDataSize = metadataBytes.size + vertexDataSize + faceDataSize + normalDataSize
            
            // Write to temp file
            RandomAccessFile(tempFile, "rw").use { raf ->
                // Write header
                raf.writeInt(MAGIC_NUMBER)
                raf.writeInt(VERSION)
                raf.writeInt(metadataBytes.size)
                raf.writeInt(0) // Placeholder for checksum
                
                // Write metadata
                raf.write(metadataBytes)
                
                // Write vertex data
                val vertexBuffer = ByteBuffer.allocate(vertexDataSize).order(ByteOrder.LITTLE_ENDIAN)
                for (vertex in model.vertices) {
                    vertexBuffer.putFloat(vertex.x)
                    vertexBuffer.putFloat(vertex.y)
                    vertexBuffer.putFloat(vertex.z)
                }
                raf.write(vertexBuffer.array())
                
                // Write face data
                val faceBuffer = ByteBuffer.allocate(faceDataSize).order(ByteOrder.LITTLE_ENDIAN)
                for (face in model.faces) {
                    faceBuffer.putInt(face.v1)
                    faceBuffer.putInt(face.v2)
                    faceBuffer.putInt(face.v3)
                }
                raf.write(faceBuffer.array())
                
                // Write normal data
                val normalBuffer = ByteBuffer.allocate(normalDataSize).order(ByteOrder.LITTLE_ENDIAN)
                for (normal in model.normals) {
                    normalBuffer.putFloat(normal.x)
                    normalBuffer.putFloat(normal.y)
                    normalBuffer.putFloat(normal.z)
                }
                raf.write(normalBuffer.array())
                
                // Calculate checksum
                raf.seek(HEADER_SIZE.toLong())
                val dataBytes = ByteArray(totalDataSize)
                raf.readFully(dataBytes)
                
                val crc = CRC32()
                crc.update(dataBytes)
                val checksum = crc.value.toInt()
                
                // Write checksum
                raf.seek(12)
                raf.writeInt(checksum)
            }
            
            // Atomic rename
            if (file.exists()) {
                file.delete()
            }
            tempFile.renameTo(file)
            
            return file
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }
    
    fun load(filename: String): ClayModel {
        val file = File(context.filesDir, "$filename.clay")
        
        if (!file.exists()) {
            throw IllegalArgumentException("File not found: $filename")
        }
        
        RandomAccessFile(file, "r").use { raf ->
            // Read header
            val magic = raf.readInt()
            if (magic != MAGIC_NUMBER) {
                throw IllegalArgumentException("Invalid file format")
            }
            
            val version = raf.readInt()
            if (version != VERSION) {
                throw IllegalArgumentException("Unsupported version: $version")
            }
            
            val metadataSize = raf.readInt()
            val storedChecksum = raf.readInt()
            
            // Read all data for checksum verification
            val totalDataSize = file.length() - HEADER_SIZE
            val dataBytes = ByteArray(totalDataSize.toInt())
            raf.readFully(dataBytes)
            
            // Verify checksum
            val crc = CRC32()
            crc.update(dataBytes)
            val calculatedChecksum = crc.value.toInt()
            
            if (calculatedChecksum != storedChecksum) {
                throw IllegalArgumentException("Checksum mismatch - file corrupted")
            }
            
            // Parse metadata
            val metadataBytes = dataBytes.copyOfRange(0, metadataSize)
            val metadataStr = String(metadataBytes, Charsets.UTF_8)
            val metadataMap = metadataStr.lines()
                .filter { it.contains("=") }
                .associate {
                    val (key, value) = it.split("=", limit = 2)
                    key to value
                }
            
            val vertexCount = metadataMap["vertexCount"]?.toInt() ?: 0
            val faceCount = metadataMap["faceCount"]?.toInt() ?: 0
            
            // Parse vertex data
            var offset = metadataSize
            val vertexBuffer = ByteBuffer.wrap(dataBytes, offset, vertexCount * 12).order(ByteOrder.LITTLE_ENDIAN)
            val vertices = mutableListOf<Vector3>()
            repeat(vertexCount) {
                vertices.add(Vector3(
                    vertexBuffer.float,
                    vertexBuffer.float,
                    vertexBuffer.float
                ))
            }
            offset += vertexCount * 12
            
            // Parse face data
            val faceBuffer = ByteBuffer.wrap(dataBytes, offset, faceCount * 12).order(ByteOrder.LITTLE_ENDIAN)
            val faces = mutableListOf<Face>()
            repeat(faceCount) {
                faces.add(Face(
                    faceBuffer.int,
                    faceBuffer.int,
                    faceBuffer.int
                ))
            }
            offset += faceCount * 12
            
            // Parse normal data
            val normalBuffer = ByteBuffer.wrap(dataBytes, offset, vertexCount * 12).order(ByteOrder.LITTLE_ENDIAN)
            val normals = mutableListOf<Vector3>()
            repeat(vertexCount) {
                normals.add(Vector3(
                    normalBuffer.float,
                    normalBuffer.float,
                    normalBuffer.float
                ))
            }
            
            // Reconstruct model
            val model = ClayModel()
            model.vertices.addAll(vertices)
            model.faces.addAll(faces)
            model.normals.addAll(normals)
            
            // Load lighting
            model.lightPosition = Vector3(
                metadataMap["light_x"]?.toFloatOrNull() ?: 2f,
                metadataMap["light_y"]?.toFloatOrNull() ?: 3f,
                metadataMap["light_z"]?.toFloatOrNull() ?: 2f
            )
            model.lightIntensity = metadataMap["light_intensity"]?.toFloatOrNull() ?: 1f
            
            return model
        }
    }
    
    fun listFiles(): List<String> {
        return context.filesDir.listFiles()
            ?.filter { it.extension == "clay" }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()
    }
    
    fun delete(filename: String): Boolean {
        val file = File(context.filesDir, "$filename.clay")
        return file.delete()
    }
}
