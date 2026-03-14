package com.claymodeler.export.geometry

import com.claymodeler.export.HookConfig
import com.claymodeler.export.HookPosition
import com.claymodeler.export.HookType
import com.claymodeler.export.PlacementResult
import com.claymodeler.model.ClayModel
import com.claymodeler.model.Face
import com.claymodeler.model.Vector3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class HookGenerator {

    fun generate(model: ClayModel, config: HookConfig, placement: PlacementResult, sizeInMm: Float = 100f): GeneratedMesh {
        val s = BaseGenerator.mmToModel(BaseGenerator.boundingBox(model), sizeInMm)
        val raw = when (config.type) {
            HookType.KEYHOLE -> generateKeyhole(s)
            HookType.MOUNTING_HOLES -> generateMountingHoles(s)
            HookType.HANGING_LOOP -> generateHangingLoop(s)
        }
        transformToPlacement(raw, placement)
        return raw
    }

    fun autoPlacement(model: ClayModel, config: HookConfig): PlacementResult {
        if (config.position != HookPosition.AUTO) {
            val bb = BaseGenerator.boundingBox(model)
            val center = (bb.first + bb.second) / 2f
            val pos = when (config.position) {
                HookPosition.TOP_CENTER -> Vector3(center.x, bb.second.y, center.z)
                HookPosition.CENTER -> center
                HookPosition.BOTTOM_CENTER -> Vector3(center.x, bb.first.y, center.z)
                else -> center
            }
            return PlacementResult(pos, Vector3(0f, 0f, -1f))
        }
        // AUTO: center of mass, back face
        val cx = model.vertices.map { it.x }.average().toFloat()
        val cy = model.vertices.map { it.y }.average().toFloat()
        val minZ = model.vertices.minOf { it.z }
        return PlacementResult(Vector3(cx, cy, minZ), Vector3(0f, 0f, -1f))
    }

    private fun generateKeyhole(s: Float): GeneratedMesh {
        // Inverted keyhole: 8mm head circle + 4mm slot
        val verts = mutableListOf<Vector3>()
        val faces = mutableListOf<Face>()
        val segs = 16
        val headR = 4f * s
        val slotW = 2f * s
        val slotH = 8f * s
        val depth = 3f * s

        // Head circle front + back
        verts.add(Vector3(0f, 0f, 0f))       // 0: front center
        verts.add(Vector3(0f, 0f, -depth))    // 1: back center
        for (i in 0 until segs) {
            val a = (2.0 * PI * i / segs).toFloat()
            verts.add(Vector3(headR * cos(a), headR * sin(a), 0f))
            verts.add(Vector3(headR * cos(a), headR * sin(a), -depth))
        }
        for (i in 0 until segs) {
            val next = (i + 1) % segs
            val fi = 2 + i * 2; val bi = 3 + i * 2
            val fn = 2 + next * 2; val bn = 3 + next * 2
            faces.add(Face(0, fi, fn))
            faces.add(Face(1, bn, bi))
            faces.add(Face(fi, bi, bn))
            faces.add(Face(fi, bn, fn))
        }

        // Slot (box below head)
        val slotBase = verts.size
        verts.add(Vector3(-slotW, 0f, 0f))
        verts.add(Vector3(slotW, 0f, 0f))
        verts.add(Vector3(slotW, -slotH, 0f))
        verts.add(Vector3(-slotW, -slotH, 0f))
        verts.add(Vector3(-slotW, 0f, -depth))
        verts.add(Vector3(slotW, 0f, -depth))
        verts.add(Vector3(slotW, -slotH, -depth))
        verts.add(Vector3(-slotW, -slotH, -depth))
        val s = slotBase
        faces.add(Face(s, s+1, s+2)); faces.add(Face(s, s+2, s+3))
        faces.add(Face(s+4, s+6, s+5)); faces.add(Face(s+4, s+7, s+6))
        faces.add(Face(s, s+3, s+7)); faces.add(Face(s, s+7, s+4))
        faces.add(Face(s+1, s+5, s+6)); faces.add(Face(s+1, s+6, s+2))
        faces.add(Face(s+3, s+2, s+6)); faces.add(Face(s+3, s+6, s+7))

        return GeneratedMesh(verts, faces)
    }

    private fun generateMountingHoles(s: Float): GeneratedMesh {
        // Two countersunk holes, 4mm diameter, 25mm spacing
        val verts = mutableListOf<Vector3>()
        val faces = mutableListOf<Face>()
        val spacing = 12.5f * s
        val plateW = 30f * s; val plateH = 10f * s; val plateD = 2f * s

        // Mounting plate (simple box)
        verts.add(Vector3(-plateW/2, -plateH/2, 0f))
        verts.add(Vector3(plateW/2, -plateH/2, 0f))
        verts.add(Vector3(plateW/2, plateH/2, 0f))
        verts.add(Vector3(-plateW/2, plateH/2, 0f))
        verts.add(Vector3(-plateW/2, -plateH/2, -plateD))
        verts.add(Vector3(plateW/2, -plateH/2, -plateD))
        verts.add(Vector3(plateW/2, plateH/2, -plateD))
        verts.add(Vector3(-plateW/2, plateH/2, -plateD))

        faces.add(Face(0, 1, 2)); faces.add(Face(0, 2, 3))
        faces.add(Face(4, 6, 5)); faces.add(Face(4, 7, 6))
        faces.add(Face(0, 4, 5)); faces.add(Face(0, 5, 1))
        faces.add(Face(2, 6, 7)); faces.add(Face(2, 7, 3))
        faces.add(Face(0, 3, 7)); faces.add(Face(0, 7, 4))
        faces.add(Face(1, 5, 6)); faces.add(Face(1, 6, 2))

        // Add hole cylinders (countersunk) at ±spacing
        for (offsetX in listOf(-spacing, spacing)) {
            addHoleCylinder(verts, faces, offsetX, 0f, 2f * s, (plateD + 1f) * s, 12)
        }

        return GeneratedMesh(verts, faces)
    }

    private fun addHoleCylinder(
        verts: MutableList<Vector3>, faces: MutableList<Face>,
        cx: Float, cy: Float, radius: Float, depth: Float, segs: Int
    ) {
        val base = verts.size
        verts.add(Vector3(cx, cy, 0.5f))     // front center (countersunk)
        verts.add(Vector3(cx, cy, -depth))    // back center
        for (i in 0 until segs) {
            val a = (2.0 * PI * i / segs).toFloat()
            verts.add(Vector3(cx + radius * cos(a), cy + radius * sin(a), 0.5f))
            verts.add(Vector3(cx + radius * cos(a), cy + radius * sin(a), -depth))
        }
        for (i in 0 until segs) {
            val next = (i + 1) % segs
            val fi = base + 2 + i * 2; val bi = base + 3 + i * 2
            val fn = base + 2 + next * 2; val bn = base + 3 + next * 2
            faces.add(Face(base, fn, fi))
            faces.add(Face(base + 1, bi, bn))
            faces.add(Face(fi, fn, bn))
            faces.add(Face(fi, bn, bi))
        }
    }

    private fun generateHangingLoop(s: Float): GeneratedMesh {
        // Large torus: 15mm inner diameter
        val innerR = 7.5f * s
        val tubeR = 2.5f * s
        val majorSegs = 24
        val minorSegs = 12
        val verts = mutableListOf<Vector3>()
        val faces = mutableListOf<Face>()

        for (i in 0 until majorSegs) {
            val theta = (2.0 * PI * i / majorSegs).toFloat()
            for (j in 0 until minorSegs) {
                val phi = (2.0 * PI * j / minorSegs).toFloat()
                val x = (innerR + tubeR + tubeR * cos(phi)) * cos(theta)
                val y = (innerR + tubeR + tubeR * cos(phi)) * sin(theta)
                val z = tubeR * sin(phi)
                verts.add(Vector3(x, y, z))
            }
        }
        for (i in 0 until majorSegs) {
            val next = (i + 1) % majorSegs
            for (j in 0 until minorSegs) {
                val jn = (j + 1) % minorSegs
                val a = i * minorSegs + j
                val b = i * minorSegs + jn
                val c = next * minorSegs + jn
                val d = next * minorSegs + j
                faces.add(Face(a, b, c))
                faces.add(Face(a, c, d))
            }
        }

        return GeneratedMesh(verts, faces)
    }

    private fun transformToPlacement(mesh: GeneratedMesh, placement: PlacementResult) {
        val normal = placement.normal.normalize()
        val up = if (kotlin.math.abs(normal.y) < 0.99f) Vector3(0f, 1f, 0f) else Vector3(1f, 0f, 0f)
        val right = up.cross(normal).normalize()
        val correctedUp = normal.cross(right).normalize()
        val cosR = cos(placement.rotation)
        val sinR = sin(placement.rotation)
        val s = placement.scale

        for (i in mesh.vertices.indices) {
            val v = mesh.vertices[i]
            val rx = v.x * cosR - v.y * sinR
            val ry = v.x * sinR + v.y * cosR
            val rz = v.z
            mesh.vertices[i] = placement.position +
                right * (rx * s) + correctedUp * (ry * s) + normal * (rz * s)
        }
    }
}
