package com.claymodeler.export

import com.claymodeler.model.Vector3

data class PlacementResult(
    val position: Vector3,
    val normal: Vector3,
    val faceIndex: Int = -1,
    val rotation: Float = 0f,
    val scale: Float = 1f
)
