package com.claymodeler.export.geometry

import com.claymodeler.model.ClayModel
import com.claymodeler.model.Face
import com.claymodeler.model.Vector3

data class ManifoldResult(val valid: Boolean, val issues: List<String>)

class GeometryMerger {

    fun merge(model: ClayModel, attachment: GeneratedMesh): ClayModel {
        val result = model.clone()
        val offset = result.vertices.size

        // Add attachment vertices
        result.vertices.addAll(attachment.vertices)

        // Add attachment faces with adjusted indices
        for (face in attachment.faces) {
            result.faces.add(Face(face.v1 + offset, face.v2 + offset, face.v3 + offset))
        }

        // Remove duplicate vertices within tolerance
        removeDuplicates(result, 0.001f)

        result.calculateNormals()
        return result
    }

    fun validateManifold(model: ClayModel): ManifoldResult {
        val issues = mutableListOf<String>()
        val edgeCount = mutableMapOf<Pair<Int, Int>, Int>()

        for (face in model.faces) {
            val edges = listOf(
                edgeKey(face.v1, face.v2),
                edgeKey(face.v2, face.v3),
                edgeKey(face.v3, face.v1)
            )
            for (e in edges) {
                edgeCount[e] = (edgeCount[e] ?: 0) + 1
            }
        }

        val boundary = edgeCount.count { it.value == 1 }
        val nonManifold = edgeCount.count { it.value > 2 }

        if (boundary > 0) issues.add("$boundary boundary edges (holes in mesh)")
        if (nonManifold > 0) issues.add("$nonManifold non-manifold edges")

        // Check for inverted normals (consistent winding)
        val directedEdgeCount = mutableMapOf<Pair<Int, Int>, Int>()
        for (face in model.faces) {
            for (e in listOf(Pair(face.v1, face.v2), Pair(face.v2, face.v3), Pair(face.v3, face.v1))) {
                directedEdgeCount[e] = (directedEdgeCount[e] ?: 0) + 1
            }
        }
        val invertedCount = directedEdgeCount.count { it.value > 1 }
        if (invertedCount > 0) issues.add("$invertedCount edges with inconsistent winding (inverted normals)")

        return ManifoldResult(issues.isEmpty(), issues)
    }

    private fun removeDuplicates(model: ClayModel, tolerance: Float) {
        val tolSq = tolerance * tolerance
        val remap = IntArray(model.vertices.size) { it }
        val kept = mutableListOf<Int>()

        for (i in model.vertices.indices) {
            var merged = false
            for (k in kept) {
                val d = model.vertices[i] - model.vertices[k]
                if (d.x * d.x + d.y * d.y + d.z * d.z < tolSq) {
                    remap[i] = k
                    merged = true
                    break
                }
            }
            if (!merged) {
                remap[i] = i
                kept.add(i)
            }
        }

        // Remap faces
        for (i in model.faces.indices) {
            val f = model.faces[i]
            model.faces[i] = Face(remap[f.v1], remap[f.v2], remap[f.v3])
        }

        // Remove degenerate faces (where two or more indices are the same after remap)
        model.faces.removeAll { it.v1 == it.v2 || it.v2 == it.v3 || it.v3 == it.v1 }
    }

    private fun edgeKey(a: Int, b: Int): Pair<Int, Int> =
        if (a < b) Pair(a, b) else Pair(b, a)
}
