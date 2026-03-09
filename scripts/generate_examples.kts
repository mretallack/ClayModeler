#!/usr/bin/env kotlin

@file:DependsOn("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import kotlin.math.*

// Data classes
data class Vector3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vector3(x / scalar, y / scalar, z / scalar)
    fun length() = sqrt(x * x + y * y + z * z)
    fun normalize() = if (length() > 0) this / length() else this
    fun dot(other: Vector3) = x * other.x + y * other.y + z * other.z
    fun cross(other: Vector3) = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )
}

data class Face(val v1: Int, val v2: Int, val v3: Int)

class ClayModel {
    val vertices = mutableListOf<Vector3>()
    val faces = mutableListOf<Face>()
    val normals = mutableListOf<Vector3>()
    var lightPosition = Vector3(2f, 3f, 2f)
    var lightIntensity = 1f
    
    fun initialize(subdivisions: Int = 2) {
        vertices.clear()
        faces.clear()
        normals.clear()
        
        // Create icosahedron
        val t = (1f + sqrt(5f)) / 2f
        val vertices = listOf(
            Vector3(-1f, t, 0f), Vector3(1f, t, 0f), Vector3(-1f, -t, 0f), Vector3(1f, -t, 0f),
            Vector3(0f, -1f, t), Vector3(0f, 1f, t), Vector3(0f, -1f, -t), Vector3(0f, 1f, -t),
            Vector3(t, 0f, -1f), Vector3(t, 0f, 1f), Vector3(-t, 0f, -1f), Vector3(-t, 0f, 1f)
        ).map { it.normalize() }
        
        this.vertices.addAll(vertices)
        
        // Create faces
        val faceIndices = listOf(
            0 to 11 to 5, 0 to 5 to 1, 0 to 1 to 7, 0 to 7 to 10, 0 to 10 to 11,
            1 to 5 to 9, 5 to 11 to 4, 11 to 10 to 2, 10 to 7 to 6, 7 to 1 to 8,
            3 to 9 to 4, 3 to 4 to 2, 3 to 2 to 6, 3 to 6 to 8, 3 to 8 to 9,
            4 to 9 to 5, 2 to 4 to 11, 6 to 2 to 10, 8 to 6 to 7, 9 to 8 to 1
        )
        
        for ((a, b, c) in faceIndices) {
            faces.add(Face(a, b, c))
        }
        
        // Subdivide
        repeat(subdivisions) { subdivide() }
        
        recalculateNormals()
    }
    
    private fun subdivide() {
        val newFaces = mutableListOf<Face>()
        val midpointCache = mutableMapOf<Pair<Int, Int>, Int>()
        
        fun getMidpoint(v1: Int, v2: Int): Int {
            val key = if (v1 < v2) v1 to v2 else v2 to v1
            return midpointCache.getOrPut(key) {
                val mid = (vertices[v1] + vertices[v2]) / 2f
                vertices.add(mid.normalize())
                vertices.size - 1
            }
        }
        
        for (face in faces) {
            val a = getMidpoint(face.v1, face.v2)
            val b = getMidpoint(face.v2, face.v3)
            val c = getMidpoint(face.v3, face.v1)
            
            newFaces.add(Face(face.v1, a, c))
            newFaces.add(Face(face.v2, b, a))
            newFaces.add(Face(face.v3, c, b))
            newFaces.add(Face(a, b, c))
        }
        
        faces.clear()
        faces.addAll(newFaces)
    }
    
    fun recalculateNormals() {
        normals.clear()
        normals.addAll(List(vertices.size) { Vector3(0f, 0f, 0f) })
        
        for (face in faces) {
            val v1 = vertices[face.v1]
            val v2 = vertices[face.v2]
            val v3 = vertices[face.v3]
            val normal = (v2 - v1).cross(v3 - v1).normalize()
            
            normals[face.v1] = normals[face.v1] + normal
            normals[face.v2] = normals[face.v2] + normal
            normals[face.v3] = normals[face.v3] + normal
        }
        
        for (i in normals.indices) {
            normals[i] = normals[i].normalize()
        }
    }
    
    fun applyRemoveTool(hitPoint: Vector3, strength: Float, radius: Float) {
        for (i in vertices.indices) {
            val dist = (vertices[i] - hitPoint).length()
            if (dist < radius) {
                val falloff = 1f - (dist / radius)
                val displacement = normals[i] * (-strength * falloff * 0.1f)
                vertices[i] = vertices[i] + displacement
            }
        }
        recalculateNormals()
    }
    
    fun applyAddTool(hitPoint: Vector3, strength: Float, radius: Float, direction: Vector3? = null) {
        for (i in vertices.indices) {
            val dist = (vertices[i] - hitPoint).length()
            if (dist < radius) {
                val falloff = 1f - (dist / radius)
                val dir = direction ?: normals[i]
                val displacement = dir.normalize() * (strength * falloff * 0.1f)
                vertices[i] = vertices[i] + displacement
            }
        }
        recalculateNormals()
    }
    
    fun applyFlattenTool(hitPoint: Vector3, strength: Float, radius: Float, planeNormal: Vector3) {
        for (i in vertices.indices) {
            val dist = (vertices[i] - hitPoint).length()
            if (dist < radius) {
                val falloff = 1f - (dist / radius)
                val toPlane = (vertices[i] - hitPoint).dot(planeNormal)
                val projected = vertices[i] - planeNormal * toPlane
                vertices[i] = vertices[i] + (projected - vertices[i]) * (strength * falloff)
            }
        }
        recalculateNormals()
    }
    
    fun applyPinchTool(hitPoint: Vector3, strength: Float, radius: Float) {
        for (i in vertices.indices) {
            val dist = (vertices[i] - hitPoint).length()
            if (dist < radius) {
                val falloff = (1f - (dist / radius).pow(2)).pow(2)
                val direction = (hitPoint - vertices[i]).normalize()
                val displacement = direction * (strength * falloff * 0.15f)
                vertices[i] = vertices[i] + displacement
            }
        }
        recalculateNormals()
    }
}

fun saveModel(model: ClayModel, filename: String, outputDir: String) {
    val file = File(outputDir, "$filename.clay")
    
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
    
    val vertexDataSize = model.vertices.size * 12
    val faceDataSize = model.faces.size * 12
    val normalDataSize = model.normals.size * 12
    val totalDataSize = metadataBytes.size + vertexDataSize + faceDataSize + normalDataSize
    
    RandomAccessFile(file, "rw").use { raf ->
        raf.writeInt(0x434C4159) // CLAY magic
        raf.writeInt(1) // version
        raf.writeInt(metadataBytes.size)
        raf.writeInt(0) // placeholder checksum
        
        raf.write(metadataBytes)
        
        val vertexBuffer = ByteBuffer.allocate(vertexDataSize).order(ByteOrder.LITTLE_ENDIAN)
        for (v in model.vertices) {
            vertexBuffer.putFloat(v.x)
            vertexBuffer.putFloat(v.y)
            vertexBuffer.putFloat(v.z)
        }
        raf.write(vertexBuffer.array())
        
        val faceBuffer = ByteBuffer.allocate(faceDataSize).order(ByteOrder.LITTLE_ENDIAN)
        for (f in model.faces) {
            faceBuffer.putInt(f.v1)
            faceBuffer.putInt(f.v2)
            faceBuffer.putInt(f.v3)
        }
        raf.write(faceBuffer.array())
        
        val normalBuffer = ByteBuffer.allocate(normalDataSize).order(ByteOrder.LITTLE_ENDIAN)
        for (n in model.normals) {
            normalBuffer.putFloat(n.x)
            normalBuffer.putFloat(n.y)
            normalBuffer.putFloat(n.z)
        }
        raf.write(normalBuffer.array())
        
        raf.seek(16)
        val dataBytes = ByteArray(totalDataSize)
        raf.readFully(dataBytes)
        
        val crc = CRC32()
        crc.update(dataBytes)
        
        raf.seek(12)
        raf.writeInt(crc.value.toInt())
    }
    
    println("Generated: $filename.clay (${model.vertices.size} vertices, ${model.faces.size} faces)")
}

// Generate examples
val outputDir = "app/src/main/assets/examples"

// 1. Sphere (basic)
println("Generating sphere...")
val sphere = ClayModel()
sphere.initialize(2)
saveModel(sphere, "sphere", outputDir)

// 2. Cube (flatten 6 sides)
println("Generating cube...")
val cube = ClayModel()
cube.initialize(3)
val cubePoints = listOf(
    Vector3(0f, 0f, 1f), Vector3(0f, 0f, -1f),
    Vector3(1f, 0f, 0f), Vector3(-1f, 0f, 0f),
    Vector3(0f, 1f, 0f), Vector3(0f, -1f, 0f)
)
for (point in cubePoints) {
    repeat(3) {
        cube.applyFlattenTool(point, 0.8f, 0.6f, point)
    }
}
saveModel(cube, "cube", outputDir)

// 3. Vase (pull top, pinch middle, remove bottom)
println("Generating vase...")
val vase = ClayModel()
vase.initialize(3)
repeat(4) {
    vase.applyAddTool(Vector3(0f, 1f, 0f), 0.7f, 0.5f, Vector3(0f, 1f, 0f))
}
repeat(3) {
    vase.applyPinchTool(Vector3(0f, 0f, 0f), 0.6f, 0.8f)
}
repeat(2) {
    vase.applyRemoveTool(Vector3(0f, -0.8f, 0f), 0.5f, 0.4f)
}
saveModel(vase, "vase", outputDir)

// 4. Character head (add features)
println("Generating character...")
val character = ClayModel()
character.initialize(3)
// Nose
repeat(3) {
    character.applyAddTool(Vector3(0f, 0.2f, 0.9f), 0.6f, 0.3f, Vector3(0f, 0f, 1f))
}
// Eyes (pinch)
character.applyPinchTool(Vector3(-0.3f, 0.4f, 0.8f), 0.7f, 0.25f)
character.applyPinchTool(Vector3(0.3f, 0.4f, 0.8f), 0.7f, 0.25f)
// Ears
repeat(2) {
    character.applyAddTool(Vector3(-0.8f, 0f, 0f), 0.5f, 0.3f, Vector3(-1f, 0f, 0f))
    character.applyAddTool(Vector3(0.8f, 0f, 0f), 0.5f, 0.3f, Vector3(1f, 0f, 0f))
}
saveModel(character, "character", outputDir)

// 5. Abstract (random interesting shape)
println("Generating abstract...")
val abstract = ClayModel()
abstract.initialize(3)
val abstractOps = listOf(
    { abstract.applyAddTool(Vector3(0.7f, 0.5f, 0.3f), 0.8f, 0.5f, Vector3(1f, 0.5f, 0.3f).normalize()) },
    { abstract.applyPinchTool(Vector3(-0.6f, -0.4f, 0.5f), 0.7f, 0.4f) },
    { abstract.applyAddTool(Vector3(0.3f, -0.7f, -0.4f), 0.6f, 0.4f, Vector3(0.3f, -1f, -0.4f).normalize()) },
    { abstract.applyRemoveTool(Vector3(-0.5f, 0.6f, -0.3f), 0.5f, 0.35f) },
    { abstract.applyAddTool(Vector3(0f, 0.8f, 0.5f), 0.7f, 0.45f, Vector3(0f, 1f, 0.5f).normalize()) }
)
abstractOps.forEach { it() }
saveModel(abstract, "abstract", outputDir)

println("\nAll examples generated!")
