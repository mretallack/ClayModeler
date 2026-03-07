package com.claymodeler.renderer

import com.claymodeler.model.ClayModel
import com.claymodeler.model.Face
import com.claymodeler.model.Vector3
import kotlin.math.max
import kotlin.math.min

data class AABB(
    val min: Vector3,
    val max: Vector3
) {
    fun intersectsRay(rayOrigin: Vector3, rayDirection: Vector3): Boolean {
        val invDir = Vector3(1f / rayDirection.x, 1f / rayDirection.y, 1f / rayDirection.z)
        
        val t1 = (min.x - rayOrigin.x) * invDir.x
        val t2 = (max.x - rayOrigin.x) * invDir.x
        val t3 = (min.y - rayOrigin.y) * invDir.y
        val t4 = (max.y - rayOrigin.y) * invDir.y
        val t5 = (min.z - rayOrigin.z) * invDir.z
        val t6 = (max.z - rayOrigin.z) * invDir.z
        
        val tmin = max(max(min(t1, t2), min(t3, t4)), min(t5, t6))
        val tmax = min(min(max(t1, t2), max(t3, t4)), max(t5, t6))
        
        return tmax >= 0 && tmin <= tmax
    }
}

class OctreeNode(
    val bounds: AABB,
    val depth: Int,
    val maxDepth: Int = 4
) {
    val faces = mutableListOf<Int>()
    var children: Array<OctreeNode?>? = null
    
    fun insert(faceIndex: Int, model: ClayModel) {
        if (depth >= maxDepth || faces.size < 8) {
            faces.add(faceIndex)
            return
        }
        
        if (children == null) {
            subdivide()
        }
        
        val face = model.faces[faceIndex]
        val faceV0 = model.vertices[face.v1]
        val faceV1 = model.vertices[face.v2]
        val faceV2 = model.vertices[face.v3]
        
        children?.forEach { child ->
            if (child != null && triangleIntersectsAABB(faceV0, faceV1, faceV2, child.bounds)) {
                child.insert(faceIndex, model)
            }
        }
    }
    
    private fun subdivide() {
        val center = (bounds.min + bounds.max) * 0.5f
        val min = bounds.min
        val max = bounds.max
        
        children = arrayOf(
            OctreeNode(AABB(Vector3(min.x, min.y, min.z), Vector3(center.x, center.y, center.z)), depth + 1, maxDepth),
            OctreeNode(AABB(Vector3(center.x, min.y, min.z), Vector3(max.x, center.y, center.z)), depth + 1, maxDepth),
            OctreeNode(AABB(Vector3(min.x, center.y, min.z), Vector3(center.x, max.y, center.z)), depth + 1, maxDepth),
            OctreeNode(AABB(Vector3(center.x, center.y, min.z), Vector3(max.x, max.y, center.z)), depth + 1, maxDepth),
            OctreeNode(AABB(Vector3(min.x, min.y, center.z), Vector3(center.x, center.y, max.z)), depth + 1, maxDepth),
            OctreeNode(AABB(Vector3(center.x, min.y, center.z), Vector3(max.x, center.y, max.z)), depth + 1, maxDepth),
            OctreeNode(AABB(Vector3(min.x, center.y, center.z), Vector3(center.x, max.y, max.z)), depth + 1, maxDepth),
            OctreeNode(AABB(Vector3(center.x, center.y, center.z), Vector3(max.x, max.y, max.z)), depth + 1, maxDepth)
        )
    }
    
    fun query(rayOrigin: Vector3, rayDirection: Vector3, result: MutableList<Int>) {
        if (!bounds.intersectsRay(rayOrigin, rayDirection)) {
            return
        }
        
        result.addAll(faces)
        
        children?.forEach { child ->
            child?.query(rayOrigin, rayDirection, result)
        }
    }
    
    private fun triangleIntersectsAABB(v0: Vector3, v1: Vector3, v2: Vector3, aabb: AABB): Boolean {
        val triMin = Vector3(
            min(v0.x, min(v1.x, v2.x)),
            min(v0.y, min(v1.y, v2.y)),
            min(v0.z, min(v1.z, v2.z))
        )
        val triMax = Vector3(
            max(v0.x, max(v1.x, v2.x)),
            max(v0.y, max(v1.y, v2.y)),
            max(v0.z, max(v1.z, v2.z))
        )
        
        return !(triMin.x > aabb.max.x || triMax.x < aabb.min.x ||
                triMin.y > aabb.max.y || triMax.y < aabb.min.y ||
                triMin.z > aabb.max.z || triMax.z < aabb.min.z)
    }
}

class Octree(model: ClayModel) {
    private val root: OctreeNode
    
    init {
        val bounds = calculateBounds(model)
        root = OctreeNode(bounds, 0)
        
        for (i in model.faces.indices) {
            root.insert(i, model)
        }
    }
    
    fun query(rayOrigin: Vector3, rayDirection: Vector3): List<Int> {
        val result = mutableListOf<Int>()
        root.query(rayOrigin, rayDirection, result)
        return result
    }
    
    private fun calculateBounds(model: ClayModel): AABB {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE
        
        for (vertex in model.vertices) {
            minX = min(minX, vertex.x)
            minY = min(minY, vertex.y)
            minZ = min(minZ, vertex.z)
            maxX = max(maxX, vertex.x)
            maxY = max(maxY, vertex.y)
            maxZ = max(maxZ, vertex.z)
        }
        
        return AABB(Vector3(minX, minY, minZ), Vector3(maxX, maxY, maxZ))
    }
}
