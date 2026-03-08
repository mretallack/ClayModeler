package com.claymodeler.model

import kotlin.math.sqrt

class ClayModel {
    val vertices = mutableListOf<Vector3>()
    val faces = mutableListOf<Face>()
    val normals = mutableListOf<Vector3>()
    
    var lightPosition = Vector3(2f, 3f, 2f)
    var lightIntensity = 1f
    
    fun resetLighting() {
        lightPosition = Vector3(2f, 3f, 2f)
        lightIntensity = 1f
    }
    
    fun initialize(subdivisions: Int = 3) {
        vertices.clear()
        faces.clear()
        normals.clear()
        
        createIcosphere(subdivisions)
        calculateNormals()
    }
    
    private fun createIcosphere(subdivisions: Int) {
        // Golden ratio
        val t = (1.0f + sqrt(5.0f)) / 2.0f
        
        // Create 12 vertices of icosahedron
        addVertex(Vector3(-1f, t, 0f).normalize())
        addVertex(Vector3(1f, t, 0f).normalize())
        addVertex(Vector3(-1f, -t, 0f).normalize())
        addVertex(Vector3(1f, -t, 0f).normalize())
        
        addVertex(Vector3(0f, -1f, t).normalize())
        addVertex(Vector3(0f, 1f, t).normalize())
        addVertex(Vector3(0f, -1f, -t).normalize())
        addVertex(Vector3(0f, 1f, -t).normalize())
        
        addVertex(Vector3(t, 0f, -1f).normalize())
        addVertex(Vector3(t, 0f, 1f).normalize())
        addVertex(Vector3(-t, 0f, -1f).normalize())
        addVertex(Vector3(-t, 0f, 1f).normalize())
        
        // Create 20 faces of icosahedron
        val icosahedronFaces = listOf(
            Face(0, 11, 5), Face(0, 5, 1), Face(0, 1, 7), Face(0, 7, 10), Face(0, 10, 11),
            Face(1, 5, 9), Face(5, 11, 4), Face(11, 10, 2), Face(10, 7, 6), Face(7, 1, 8),
            Face(3, 9, 4), Face(3, 4, 2), Face(3, 2, 6), Face(3, 6, 8), Face(3, 8, 9),
            Face(4, 9, 5), Face(2, 4, 11), Face(6, 2, 10), Face(8, 6, 7), Face(9, 8, 1)
        )
        
        faces.addAll(icosahedronFaces)
        
        // Subdivide faces
        repeat(subdivisions) {
            subdivideFaces()
        }
    }
    
    private fun addVertex(v: Vector3): Int {
        vertices.add(v)
        return vertices.size - 1
    }
    
    private fun subdivideFaces() {
        val newFaces = mutableListOf<Face>()
        val midpointCache = mutableMapOf<Pair<Int, Int>, Int>()
        
        for (face in faces) {
            val a = face.v1
            val b = face.v2
            val c = face.v3
            
            val ab = getMidpoint(a, b, midpointCache)
            val bc = getMidpoint(b, c, midpointCache)
            val ca = getMidpoint(c, a, midpointCache)
            
            newFaces.add(Face(a, ab, ca))
            newFaces.add(Face(b, bc, ab))
            newFaces.add(Face(c, ca, bc))
            newFaces.add(Face(ab, bc, ca))
        }
        
        faces.clear()
        faces.addAll(newFaces)
    }
    
    private fun getMidpoint(v1: Int, v2: Int, cache: MutableMap<Pair<Int, Int>, Int>): Int {
        val key = if (v1 < v2) Pair(v1, v2) else Pair(v2, v1)
        
        return cache.getOrPut(key) {
            val p1 = vertices[v1]
            val p2 = vertices[v2]
            val mid = ((p1 + p2) / 2f).normalize()
            addVertex(mid)
        }
    }
    
    fun calculateNormals() {
        normals.clear()
        
        // Initialize normals to zero
        repeat(vertices.size) {
            normals.add(Vector3(0f, 0f, 0f))
        }
        
        // Accumulate face normals
        for (face in faces) {
            val v1 = vertices[face.v1]
            val v2 = vertices[face.v2]
            val v3 = vertices[face.v3]
            
            val edge1 = v2 - v1
            val edge2 = v3 - v1
            val faceNormal = edge1.cross(edge2)
            
            normals[face.v1] = normals[face.v1] + faceNormal
            normals[face.v2] = normals[face.v2] + faceNormal
            normals[face.v3] = normals[face.v3] + faceNormal
        }
        
        // Normalize all normals
        for (i in normals.indices) {
            normals[i] = normals[i].normalize()
        }
    }
    
    fun recalculateNormalsForVertices(affectedVertices: Set<Int>) {
        // Reset normals for affected vertices only
        for (i in affectedVertices) {
            normals[i] = Vector3(0f, 0f, 0f)
        }
        
        // Accumulate face normals for faces that have at least one affected vertex
        for (face in faces) {
            if (face.v1 in affectedVertices || face.v2 in affectedVertices || face.v3 in affectedVertices) {
                val v1 = vertices[face.v1]
                val v2 = vertices[face.v2]
                val v3 = vertices[face.v3]
                
                val edge1 = v2 - v1
                val edge2 = v3 - v1
                val faceNormal = edge1.cross(edge2)
                
                // Only update normals for vertices that are in the affected set
                if (face.v1 in affectedVertices) normals[face.v1] = normals[face.v1] + faceNormal
                if (face.v2 in affectedVertices) normals[face.v2] = normals[face.v2] + faceNormal
                if (face.v3 in affectedVertices) normals[face.v3] = normals[face.v3] + faceNormal
            }
        }
        
        // Normalize affected normals
        for (i in affectedVertices) {
            normals[i] = normals[i].normalize()
        }
    }
    
    fun clone(): ClayModel {
        val copy = ClayModel()
        copy.vertices.addAll(vertices)
        copy.faces.addAll(faces)
        copy.normals.addAll(normals)
        return copy
    }
}
