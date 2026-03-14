package com.claymodeler.export.placement

import com.claymodeler.export.AttachmentType
import com.claymodeler.export.PlacementResult
import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3

data class ValidationResult(val valid: Boolean, val reason: String?, val suggestedPosition: PlacementResult?)

class PlacementValidator {

    fun validate(
        model: ClayModel, placement: PlacementResult, attachmentType: AttachmentType
    ): ValidationResult {
        // Check placement is on model surface (not interior)
        if (!isOnSurface(model, placement)) {
            val suggested = findNearestSurface(model, placement)
            return ValidationResult(false, "Placement is inside the model", suggested)
        }

        // Check minimum surface area at attachment point
        if (!hasSufficientArea(model, placement)) {
            return ValidationResult(false, "Insufficient surface area at attachment point", null)
        }

        return when (attachmentType) {
            AttachmentType.KEYRING_LOOP -> validateKeyring(model, placement)
            AttachmentType.WALL_HOOK -> validateHook(model, placement)
            else -> ValidationResult(true, null, null)
        }
    }

    private fun validateKeyring(model: ClayModel, placement: PlacementResult): ValidationResult {
        // Check loop opening clearance — ensure no geometry blocks the loop
        val testPoint = placement.position + placement.normal * 15f
        if (isInsideModel(model, testPoint)) {
            val suggested = placement.copy(
                position = placement.position + placement.normal * 5f
            )
            return ValidationResult(false, "Loop opening blocked by nearby geometry", suggested)
        }
        return ValidationResult(true, null, null)
    }

    private fun validateHook(model: ClayModel, placement: PlacementResult): ValidationResult {
        // Check sufficient flat area for hook mounting
        val flatRadius = 10f
        val neighborFaces = model.faces.indices.filter { i ->
            val f = model.faces[i]
            val center = (model.vertices[f.v1] + model.vertices[f.v2] + model.vertices[f.v3]) / 3f
            val d = center - placement.position
            (d.x * d.x + d.y * d.y + d.z * d.z) < flatRadius * flatRadius
        }
        if (neighborFaces.size < 4) {
            return ValidationResult(false, "Insufficient flat area for wall hook", null)
        }
        // Check normals are roughly aligned (flat surface)
        val avgNormal = neighborFaces.fold(Vector3(0f, 0f, 0f)) { acc, i ->
            val f = model.faces[i]
            val n = ((model.vertices[f.v2] - model.vertices[f.v1])
                .cross(model.vertices[f.v3] - model.vertices[f.v1])).normalize()
            acc + n
        }.normalize()
        if (avgNormal.dot(placement.normal) < 0.8f) {
            return ValidationResult(false, "Surface is not flat enough for wall hook", null)
        }
        return ValidationResult(true, null, null)
    }

    private fun isOnSurface(model: ClayModel, placement: PlacementResult): Boolean =
        placement.faceIndex in model.faces.indices

    private fun hasSufficientArea(model: ClayModel, placement: PlacementResult): Boolean {
        if (placement.faceIndex < 0) return false
        val f = model.faces[placement.faceIndex]
        val e1 = model.vertices[f.v2] - model.vertices[f.v1]
        val e2 = model.vertices[f.v3] - model.vertices[f.v1]
        return e1.cross(e2).length() > 0.0001f
    }

    private fun isInsideModel(model: ClayModel, point: Vector3): Boolean {
        // Simple ray-cast parity test along +X
        var crossings = 0
        val dir = Vector3(1f, 0f, 0f)
        for (face in model.faces) {
            if (rayHitsFace(point, dir, model.vertices[face.v1], model.vertices[face.v2], model.vertices[face.v3]))
                crossings++
        }
        return crossings % 2 == 1
    }

    private fun findNearestSurface(model: ClayModel, placement: PlacementResult): PlacementResult? {
        var bestDist = Float.MAX_VALUE
        var best: PlacementResult? = null
        for (i in model.faces.indices) {
            val f = model.faces[i]
            val center = (model.vertices[f.v1] + model.vertices[f.v2] + model.vertices[f.v3]) / 3f
            val d = center - placement.position
            val dist = d.x * d.x + d.y * d.y + d.z * d.z
            if (dist < bestDist) {
                bestDist = dist
                val normal = ((model.vertices[f.v2] - model.vertices[f.v1])
                    .cross(model.vertices[f.v3] - model.vertices[f.v1])).normalize()
                best = PlacementResult(center, normal, i, placement.rotation, placement.scale)
            }
        }
        return best
    }

    private fun rayHitsFace(origin: Vector3, dir: Vector3, v0: Vector3, v1: Vector3, v2: Vector3): Boolean {
        val e1 = v1 - v0; val e2 = v2 - v0
        val h = dir.cross(e2); val a = e1.dot(h)
        if (a > -1e-7f && a < 1e-7f) return false
        val f = 1f / a; val s = origin - v0
        val u = f * s.dot(h); if (u < 0f || u > 1f) return false
        val q = s.cross(e1); val v = f * dir.dot(q)
        if (v < 0f || u + v > 1f) return false
        return f * e2.dot(q) > 1e-7f
    }
}
