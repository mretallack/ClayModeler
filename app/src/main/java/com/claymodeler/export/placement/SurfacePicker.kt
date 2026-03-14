package com.claymodeler.export.placement

import com.claymodeler.export.PlacementResult
import com.claymodeler.model.ClayModel
import com.claymodeler.renderer.Octree
import com.claymodeler.renderer.RayCaster

class SurfacePicker(private val rayCaster: RayCaster = RayCaster()) {

    fun pick(
        screenX: Float, screenY: Float,
        screenWidth: Int, screenHeight: Int,
        viewMatrix: FloatArray, projectionMatrix: FloatArray,
        model: ClayModel, octree: Octree? = null
    ): PlacementResult? {
        val (origin, direction) = rayCaster.screenToWorldRay(
            screenX, screenY, screenWidth, screenHeight, viewMatrix, projectionMatrix
        )
        val hit = rayCaster.raycast(origin, direction, model, octree) ?: return null

        // Find face index
        val faceIndex = findFaceIndex(origin, direction, model)

        return PlacementResult(
            position = hit.hitPoint,
            normal = hit.normal,
            faceIndex = faceIndex
        )
    }

    private fun findFaceIndex(origin: com.claymodeler.model.Vector3, direction: com.claymodeler.model.Vector3, model: ClayModel): Int {
        var closest = Float.MAX_VALUE
        var idx = -1
        for (i in model.faces.indices) {
            val f = model.faces[i]
            val v0 = model.vertices[f.v1]
            val v1 = model.vertices[f.v2]
            val v2 = model.vertices[f.v3]
            val e1 = v1 - v0; val e2 = v2 - v0
            val h = direction.cross(e2)
            val a = e1.dot(h)
            if (a > -1e-7f && a < 1e-7f) continue
            val ff = 1f / a
            val s = origin - v0
            val u = ff * s.dot(h)
            if (u < 0f || u > 1f) continue
            val q = s.cross(e1)
            val v = ff * direction.dot(q)
            if (v < 0f || u + v > 1f) continue
            val t = ff * e2.dot(q)
            if (t > 1e-7f && t < closest) { closest = t; idx = i }
        }
        return idx
    }
}
