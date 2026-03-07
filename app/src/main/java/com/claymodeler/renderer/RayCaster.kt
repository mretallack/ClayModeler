package com.claymodeler.renderer

import android.opengl.Matrix
import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3
import kotlin.math.min
import kotlin.math.max

data class RayHit(
    val hitPoint: Vector3,
    val normal: Vector3,
    val distance: Float
)

class RayCaster {
    
    fun screenToWorldRay(
        screenX: Float,
        screenY: Float,
        screenWidth: Int,
        screenHeight: Int,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray
    ): Pair<Vector3, Vector3> {
        // Convert screen coordinates to NDC (-1 to 1)
        val ndcX = (2f * screenX) / screenWidth - 1f
        val ndcY = 1f - (2f * screenY) / screenHeight
        
        // Create inverse matrices
        val vpMatrix = FloatArray(16)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        
        val invVPMatrix = FloatArray(16)
        Matrix.invertM(invVPMatrix, 0, vpMatrix, 0)
        
        // Near and far points in NDC
        val nearPoint = floatArrayOf(ndcX, ndcY, -1f, 1f)
        val farPoint = floatArrayOf(ndcX, ndcY, 1f, 1f)
        
        // Transform to world space
        val nearWorld = FloatArray(4)
        val farWorld = FloatArray(4)
        
        Matrix.multiplyMV(nearWorld, 0, invVPMatrix, 0, nearPoint, 0)
        Matrix.multiplyMV(farWorld, 0, invVPMatrix, 0, farPoint, 0)
        
        // Perspective divide
        val rayOrigin = Vector3(
            nearWorld[0] / nearWorld[3],
            nearWorld[1] / nearWorld[3],
            nearWorld[2] / nearWorld[3]
        )
        
        val rayEnd = Vector3(
            farWorld[0] / farWorld[3],
            farWorld[1] / farWorld[3],
            farWorld[2] / farWorld[3]
        )
        
        val rayDirection = (rayEnd - rayOrigin).normalize()
        
        return Pair(rayOrigin, rayDirection)
    }
    
    fun raycast(
        rayOrigin: Vector3,
        rayDirection: Vector3,
        model: ClayModel,
        octree: Octree? = null
    ): RayHit? {
        var closestHit: RayHit? = null
        var closestDistance = Float.MAX_VALUE
        
        // Get candidate faces from octree or test all
        val candidateFaces = octree?.query(rayOrigin, rayDirection) ?: model.faces.indices.toList()
        
        // Test candidate triangles
        for (faceIndex in candidateFaces) {
            val face = model.faces[faceIndex]
            val v0 = model.vertices[face.v1]
            val v1 = model.vertices[face.v2]
            val v2 = model.vertices[face.v3]
            
            val hit = rayTriangleIntersect(rayOrigin, rayDirection, v0, v1, v2)
            
            if (hit != null && hit.distance < closestDistance) {
                closestDistance = hit.distance
                
                // Get interpolated normal
                val n0 = model.normals[face.v1]
                val n1 = model.normals[face.v2]
                val n2 = model.normals[face.v3]
                val normal = ((n0 + n1 + n2) * (1f / 3f)).normalize()
                
                closestHit = RayHit(hit.hitPoint, normal, hit.distance)
            }
        }
        
        return closestHit
    }
    
    private fun rayTriangleIntersect(
        rayOrigin: Vector3,
        rayDirection: Vector3,
        v0: Vector3,
        v1: Vector3,
        v2: Vector3
    ): RayHit? {
        val epsilon = 0.0000001f
        
        val edge1 = v1 - v0
        val edge2 = v2 - v0
        val h = rayDirection.cross(edge2)
        val a = edge1.dot(h)
        
        if (a > -epsilon && a < epsilon) {
            return null // Ray parallel to triangle
        }
        
        val f = 1f / a
        val s = rayOrigin - v0
        val u = f * s.dot(h)
        
        if (u < 0f || u > 1f) {
            return null
        }
        
        val q = s.cross(edge1)
        val v = f * rayDirection.dot(q)
        
        if (v < 0f || u + v > 1f) {
            return null
        }
        
        val t = f * edge2.dot(q)
        
        if (t > epsilon) {
            val hitPoint = rayOrigin + (rayDirection * t)
            val normal = edge1.cross(edge2).normalize()
            return RayHit(hitPoint, normal, t)
        }
        
        return null
    }
}
