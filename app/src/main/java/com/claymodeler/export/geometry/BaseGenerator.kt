package com.claymodeler.export.geometry

import com.claymodeler.model.ClayModel
import com.claymodeler.model.Face
import com.claymodeler.model.Vector3
import com.claymodeler.export.BaseConfig
import com.claymodeler.export.BaseShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class GeneratedMesh(
    val vertices: MutableList<Vector3>,
    val faces: MutableList<Face>
)

class BaseGenerator {

    fun generate(model: ClayModel, config: BaseConfig, sizeInMm: Float = 100f): GeneratedMesh {
        val bb = boundingBox(model)
        val minY = bb.first.y
        val s = mmToModel(bb, sizeInMm)
        val height = max(config.height * s, 2f * s)
        val overlap = config.overlap * s
        val scaledConfig = config.copy(margin = config.margin * s, width = config.width * s, depth = config.depth * s, overlap = overlap)

        val mesh = when (config.shape) {
            BaseShape.CIRCULAR -> generateCircular(model, bb, scaledConfig, minY, height)
            BaseShape.RECTANGULAR -> generateRectangular(model, bb, scaledConfig, minY, height)
            BaseShape.CUSTOM -> generateCustom(model, bb, scaledConfig, minY, height)
        }

        return mesh
    }

    private fun generateCircular(
        model: ClayModel, bb: Pair<Vector3, Vector3>, config: BaseConfig, minY: Float, height: Float
    ): GeneratedMesh {
        val cx = (bb.first.x + bb.second.x) / 2f
        val cz = (bb.first.z + bb.second.z) / 2f

        // Bottom radius: full bounding box + margin (wide, stable)
        val rx = (bb.second.x - bb.first.x) / 2f + config.margin
        val rz = (bb.second.z - bb.first.z) / 2f + config.margin
        val bottomRadius = max(rx, rz)

        // Top of base penetrates into model by overlap amount
        val topY = minY + config.overlap

        // Top radius: match model's actual footprint at the overlap height
        val modelHeight = bb.second.y - bb.first.y
        val sliceThreshold = modelHeight * 0.03f + config.overlap
        var maxDistSq = 0f
        for (v in model.vertices) {
            if (v.y <= minY + sliceThreshold) {
                val dx = v.x - cx; val dz = v.z - cz
                val distSq = dx * dx + dz * dz
                if (distSq > maxDistSq) maxDistSq = distSq
            }
        }
        val topRadius = if (maxDistSq > 0f) kotlin.math.sqrt(maxDistSq)
                         else bottomRadius * 0.3f

        val segments = 32
        val verts = mutableListOf<Vector3>()
        val faces = mutableListOf<Face>()

        // bottom center = 0, top center = 1
        verts.add(Vector3(cx, minY - height, cz))
        verts.add(Vector3(cx, topY, cz))
        // bottom ring: 2 .. 2+segments-1
        for (i in 0 until segments) {
            val a = (2.0 * PI * i / segments).toFloat()
            verts.add(Vector3(cx + bottomRadius * cos(a), minY - height, cz + bottomRadius * sin(a)))
        }
        // top ring: narrower, at overlap height
        for (i in 0 until segments) {
            val a = (2.0 * PI * i / segments).toFloat()
            verts.add(Vector3(cx + topRadius * cos(a), topY, cz + topRadius * sin(a)))
        }

        val bStart = 2
        val tStart = 2 + segments
        for (i in 0 until segments) {
            val next = (i + 1) % segments
            faces.add(Face(0, bStart + next, bStart + i))
            faces.add(Face(1, tStart + i, tStart + next))
            faces.add(Face(bStart + i, bStart + next, tStart + i))
            faces.add(Face(tStart + i, bStart + next, tStart + next))
        }

        return GeneratedMesh(verts, faces)
    }

    private fun generateRectangular(
        model: ClayModel, bb: Pair<Vector3, Vector3>, config: BaseConfig, minY: Float, height: Float
    ): GeneratedMesh {
        val cx = (bb.first.x + bb.second.x) / 2f
        val cz = (bb.first.z + bb.second.z) / 2f

        // Bottom: full size + margin
        val bw = if (config.width > 0f) config.width else (bb.second.x - bb.first.x + config.margin * 2)
        val bd = if (config.depth > 0f) config.depth else (bb.second.z - bb.first.z + config.margin * 2)

        // Top: match model footprint at overlap height
        val modelHeight = bb.second.y - bb.first.y
        val sliceThreshold = modelHeight * 0.03f + config.overlap
        var topMinX = Float.MAX_VALUE; var topMaxX = -Float.MAX_VALUE
        var topMinZ = Float.MAX_VALUE; var topMaxZ = -Float.MAX_VALUE
        for (v in model.vertices) {
            if (v.y <= minY + sliceThreshold) {
                if (v.x < topMinX) topMinX = v.x; if (v.x > topMaxX) topMaxX = v.x
                if (v.z < topMinZ) topMinZ = v.z; if (v.z > topMaxZ) topMaxZ = v.z
            }
        }
        val tw = if (topMaxX > topMinX) (topMaxX - topMinX) else bw * 0.3f
        val td = if (topMaxZ > topMinZ) (topMaxZ - topMinZ) else bd * 0.3f

        val bhw = bw / 2f; val bhd = bd / 2f
        val thw = tw / 2f; val thd = td / 2f
        val yBot = minY - height; val yTop = minY + config.overlap

        val verts = mutableListOf(
            // bottom 0-3
            Vector3(cx - bhw, yBot, cz - bhd), Vector3(cx + bhw, yBot, cz - bhd),
            Vector3(cx + bhw, yBot, cz + bhd), Vector3(cx - bhw, yBot, cz + bhd),
            // top 4-7 (narrower)
            Vector3(cx - thw, yTop, cz - thd), Vector3(cx + thw, yTop, cz - thd),
            Vector3(cx + thw, yTop, cz + thd), Vector3(cx - thw, yTop, cz + thd)
        )

        val faces = mutableListOf(
            // bottom
            Face(0, 2, 1), Face(0, 3, 2),
            // top
            Face(4, 5, 6), Face(4, 6, 7),
            // front
            Face(0, 1, 5), Face(0, 5, 4),
            // back
            Face(2, 3, 7), Face(2, 7, 6),
            // left
            Face(3, 0, 4), Face(3, 4, 7),
            // right
            Face(1, 2, 6), Face(1, 6, 5)
        )

        return GeneratedMesh(verts, faces)
    }

    private fun generateCustom(
        model: ClayModel, bb: Pair<Vector3, Vector3>, config: BaseConfig, minY: Float, height: Float
    ): GeneratedMesh {
        // Trace outline at bottom of model, offset by margin, extrude down
        // Simplified: find vertices near minY, project to XZ, build convex hull, extrude
        val threshold = (bb.second.y - bb.first.y) * 0.1f
        val bottomVerts = model.vertices.filter { it.y <= minY + threshold }

        if (bottomVerts.size < 3) {
            return generateCircular(model, bb, config, minY, height)
        }

        val hull = convexHull2D(bottomVerts.map { Pair(it.x, it.z) })
        if (hull.size < 3) {
            return generateCircular(model, bb, config, minY, height)
        }

        // Offset hull outward by margin
        val cx = hull.map { it.first }.average().toFloat()
        val cz = hull.map { it.second }.average().toFloat()
        val expanded = hull.map { (x, z) ->
            val dx = x - cx
            val dz = z - cz
            val len = kotlin.math.sqrt(dx * dx + dz * dz)
            if (len > 0f) Pair(x + dx / len * config.margin, z + dz / len * config.margin)
            else Pair(x, z)
        }

        val n = expanded.size
        val verts = mutableListOf<Vector3>()
        val faces = mutableListOf<Face>()

        // Bottom center = 0, top center = 1
        verts.add(Vector3(cx, minY - height, cz))
        verts.add(Vector3(cx, minY, cz))

        // Bottom ring: 2..2+n-1, top ring: 2+n..2+2n-1
        for ((x, z) in expanded) {
            verts.add(Vector3(x, minY - height, z))
        }
        for ((x, z) in expanded) {
            verts.add(Vector3(x, minY, z))
        }

        val bStart = 2
        val tStart = 2 + n
        for (i in 0 until n) {
            val next = (i + 1) % n
            faces.add(Face(0, bStart + next, bStart + i))
            faces.add(Face(1, tStart + i, tStart + next))
            faces.add(Face(bStart + i, bStart + next, tStart + i))
            faces.add(Face(tStart + i, bStart + next, tStart + next))
        }

        return GeneratedMesh(verts, faces)
    }

    /**
     * Adds a smooth fillet (quarter-circle profile) around the top edge of the base
     * to create a smooth transition to the model.
     */
    private fun addFillet(mesh: GeneratedMesh, bb: Pair<Vector3, Vector3>, config: BaseConfig, minY: Float) {
        val cx = (bb.first.x + bb.second.x) / 2f
        val cz = (bb.first.z + bb.second.z) / 2f
        val filletR = config.margin.coerceIn(0.5f, 3f)
        val filletSegs = 4
        val circumSegs = 24

        // Find top-ring vertices: those at y == minY and on the outer perimeter
        // Generate a fillet ring inward and upward using quarter-circle profile
        for (i in 0 until circumSegs) {
            val angle = (2.0 * PI * i / circumSegs).toFloat()
            val dx = cos(angle)
            val dz = sin(angle)

            // Outer edge of base top
            val rx = (bb.second.x - bb.first.x) / 2f + config.margin
            val rz = (bb.second.z - bb.first.z) / 2f + config.margin
            val outerR = max(rx, rz)
            val ox = cx + outerR * dx
            val oz = cz + outerR * dz

            val baseIdx = mesh.vertices.size
            // Add fillet profile vertices (quarter circle from horizontal to vertical)
            for (j in 0..filletSegs) {
                val t = (PI / 2.0 * j / filletSegs).toFloat()
                val inward = filletR * (1f - sin(t))
                val upward = filletR * (1f - cos(t))
                mesh.vertices.add(Vector3(
                    ox - inward * dx,
                    minY + upward,
                    oz - inward * dz
                ))
            }

            // Connect to next circumference segment
            if (i > 0) {
                val prevBase = baseIdx - (filletSegs + 1)
                for (j in 0 until filletSegs) {
                    val a = prevBase + j
                    val b = prevBase + j + 1
                    val c = baseIdx + j + 1
                    val d = baseIdx + j
                    mesh.faces.add(Face(a, b, c))
                    mesh.faces.add(Face(a, c, d))
                }
            }
        }
        // Close the loop: connect last segment to first
        val totalFilletVerts = circumSegs * (filletSegs + 1)
        val firstBase = mesh.vertices.size - totalFilletVerts
        val lastBase = mesh.vertices.size - (filletSegs + 1)
        for (j in 0 until filletSegs) {
            val a = lastBase + j
            val b = lastBase + j + 1
            val c = firstBase + j + 1
            val d = firstBase + j
            mesh.faces.add(Face(a, b, c))
            mesh.faces.add(Face(a, c, d))
        }
    }

    companion object {
        fun boundingBox(model: ClayModel): Pair<Vector3, Vector3> {
            var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
            for (v in model.vertices) {
                minX = min(minX, v.x); minY = min(minY, v.y); minZ = min(minZ, v.z)
                maxX = max(maxX, v.x); maxY = max(maxY, v.y); maxZ = max(maxZ, v.z)
            }
            return Pair(Vector3(minX, minY, minZ), Vector3(maxX, maxY, maxZ))
        }

        /**
         * Returns a scale factor to convert mm dimensions to model-space units.
         * E.g. if model is 2 units wide and sizeInMm is 100, then 1mm = 0.02 model units.
         */
        fun mmToModel(bb: Pair<Vector3, Vector3>, sizeInMm: Float): Float {
            val modelSize = maxOf(bb.second.x - bb.first.x, bb.second.y - bb.first.y, bb.second.z - bb.first.z)
            return if (sizeInMm > 0f && modelSize > 0f) modelSize / sizeInMm else 0.01f
        }

        fun convexHull2D(points: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
            val sorted = points.sortedWith(compareBy({ it.first }, { it.second }))
            if (sorted.size < 3) return sorted

            fun cross(o: Pair<Float, Float>, a: Pair<Float, Float>, b: Pair<Float, Float>): Float =
                (a.first - o.first) * (b.second - o.second) - (a.second - o.second) * (b.first - o.first)

            val lower = mutableListOf<Pair<Float, Float>>()
            for (p in sorted) {
                while (lower.size >= 2 && cross(lower[lower.size - 2], lower.last(), p) <= 0) lower.removeAt(lower.size - 1)
                lower.add(p)
            }
            val upper = mutableListOf<Pair<Float, Float>>()
            for (p in sorted.reversed()) {
                while (upper.size >= 2 && cross(upper[upper.size - 2], upper.last(), p) <= 0) upper.removeAt(upper.size - 1)
                upper.add(p)
            }
            lower.removeAt(lower.size - 1)
            upper.removeAt(upper.size - 1)
            return lower + upper
        }
    }
}
