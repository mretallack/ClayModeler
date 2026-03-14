package com.claymodeler.examples

import android.content.Context
import com.claymodeler.model.ClayModel
import com.claymodeler.model.Face
import com.claymodeler.model.Vector3
import org.json.JSONObject
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class ExampleInfo(
    val filename: String,
    val name: String,
    val description: String,
    val difficulty: String
)

class ExampleManager(private val context: Context) {
    
    fun loadExampleList(): List<ExampleInfo> {
        return try {
            val json = context.assets.open("examples/examples.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(json)
            val examples = jsonObject.getJSONArray("examples")
            
            (0 until examples.length()).map { i ->
                val example = examples.getJSONObject(i)
                ExampleInfo(
                    filename = example.getString("filename"),
                    name = example.getString("name"),
                    description = example.getString("description"),
                    difficulty = example.getString("difficulty")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun loadExample(filename: String): ClayModel? {
        return try {
            context.assets.open("examples/$filename").use { inputStream ->
                val bytes = inputStream.readBytes()
                val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                
                // Read header - detect byte order from magic
                // Detect byte order from first byte ('Y'=LE, 'C'=BE)
                val b0 = bytes[0]
                val headerOrder = if (b0 == 'Y'.code.toByte()) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
                buffer.order(headerOrder)

                val magic = buffer.int
                val version = buffer.int
                android.util.Log.d("ExampleManager", "Loading $filename headerOrder=$headerOrder version=$version")
                if (version != 1) { android.util.Log.e("ExampleManager", "Bad version $version"); return null }

                val metadataSize = buffer.int
                buffer.int // checksum

                // Skip metadata
                buffer.position(buffer.position() + metadataSize)

                // Data is always little-endian regardless of header byte order
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                
                // Parse metadata to get counts
                val metadataBytes = bytes.copyOfRange(16, 16 + metadataSize)
                val metadataStr = String(metadataBytes, Charsets.UTF_8)
                val metadataMap = metadataStr.lines()
                    .filter { it.contains("=") }
                    .associate {
                        val (key, value) = it.split("=", limit = 2)
                        key to value
                    }
                
                val vertexCount = metadataMap["vertexCount"]?.toInt() ?: 0
                val faceCount = metadataMap["faceCount"]?.toInt() ?: 0
                
                // Read vertices
                val vertices = mutableListOf<Vector3>()
                repeat(vertexCount) {
                    vertices.add(Vector3(buffer.float, buffer.float, buffer.float))
                }
                
                // Read faces
                val faces = mutableListOf<Face>()
                repeat(faceCount) {
                    faces.add(Face(buffer.int, buffer.int, buffer.int))
                }
                
                // Read normals
                val normals = mutableListOf<Vector3>()
                repeat(vertexCount) {
                    normals.add(Vector3(buffer.float, buffer.float, buffer.float))
                }
                
                // Create model
                ClayModel().apply {
                    this.vertices.addAll(vertices)
                    this.faces.addAll(faces)
                    this.normals.addAll(normals)
                    
                    // Load lighting
                    lightPosition = Vector3(
                        metadataMap["light_x"]?.toFloatOrNull() ?: 2f,
                        metadataMap["light_y"]?.toFloatOrNull() ?: 3f,
                        metadataMap["light_z"]?.toFloatOrNull() ?: 2f
                    )
                    lightIntensity = metadataMap["light_intensity"]?.toFloatOrNull() ?: 1f
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ExampleManager", "Failed to load $filename", e)
            null
        }
    }
}
