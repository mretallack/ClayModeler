package com.claymodeler.export.geometry

import com.claymodeler.export.KeyringConfig
import com.claymodeler.export.PlacementResult
import com.claymodeler.model.ClayModel
import com.claymodeler.model.Face
import com.claymodeler.model.Vector3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class LoopGenerator {

    fun generate(model: ClayModel, config: KeyringConfig, placement: PlacementResult, sizeInMm: Float = 100f): GeneratedMesh {
        val s = BaseGenerator.mmToModel(BaseGenerator.boundingBox(model), sizeInMm)
        val innerR = config.size.innerDiameter / 2f * s
        val tubeR = max(config.thickness, 2f) / 2f * s
        val majorSegments = 24
        val minorSegments = 12

        // Generate torus centered at origin in XY plane
        val verts = mutableListOf<Vector3>()
        val faces = mutableListOf<Face>()

        for (i in 0 until majorSegments) {
            val theta = (2.0 * PI * i / majorSegments).toFloat()
            val ct = cos(theta); val st = sin(theta)
            for (j in 0 until minorSegments) {
                val phi = (2.0 * PI * j / minorSegments).toFloat()
                val cp = cos(phi); val sp = sin(phi)
                val x = (innerR + tubeR + tubeR * cp) * ct
                val y = (innerR + tubeR + tubeR * cp) * st
                val z = tubeR * sp
                verts.add(Vector3(x, y, z))
            }
        }

        for (i in 0 until majorSegments) {
            val next = (i + 1) % majorSegments
            for (j in 0 until minorSegments) {
                val jn = (j + 1) % minorSegments
                val a = i * minorSegments + j
                val b = i * minorSegments + jn
                val c = next * minorSegments + jn
                val d = next * minorSegments + j
                faces.add(Face(a, b, c))
                faces.add(Face(a, c, d))
            }
        }

        // Orient: align torus so it's perpendicular to surface normal at placement point
        val normal = placement.normal.normalize()
        val up = if (kotlin.math.abs(normal.y) < 0.99f) Vector3(0f, 1f, 0f) else Vector3(1f, 0f, 0f)
        val right = up.cross(normal).normalize()
        val correctedUp = normal.cross(right).normalize()

        val cosR = cos(placement.rotation)
        val sinR = sin(placement.rotation)
        val ps = placement.scale

        for (i in verts.indices) {
            val v = verts[i]
            // Tip the torus 90° so ring stands vertical from surface (swap y and z)
            val tx = v.x
            val ty = v.z
            val tz = v.y
            // Then apply user rotation around normal axis
            val rx = tx * cosR - ty * sinR
            val ry = tx * sinR + ty * cosR
            val rz = tz
            // Transform to world space
            verts[i] = placement.position +
                right * (rx * ps) +
                correctedUp * (ry * ps) +
                normal * (rz * ps)
        }

        return GeneratedMesh(verts, faces)
    }
}
