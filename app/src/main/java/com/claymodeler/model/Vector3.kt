package com.claymodeler.model

import kotlin.math.sqrt

data class Vector3(
    val x: Float,
    val y: Float,
    val z: Float
) {
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    
    operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)
    
    operator fun div(scalar: Float) = Vector3(x / scalar, y / scalar, z / scalar)
    
    fun dot(other: Vector3) = x * other.x + y * other.y + z * other.z
    
    fun cross(other: Vector3) = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )
    
    fun length() = sqrt(x * x + y * y + z * z)
    
    fun normalize(): Vector3 {
        val len = length()
        return if (len > 0f) this / len else this
    }
}
