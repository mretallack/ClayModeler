package com.claymodeler.export

enum class LoopPosition {
    TOP, LEFT, RIGHT, FRONT, BACK
}

enum class LoopSize(val innerDiameter: Float, val wallThickness: Float) {
    SMALL(5f, 2f),
    MEDIUM(8f, 2.5f),
    LARGE(12f, 3f)
}

data class KeyringConfig(
    val size: LoopSize = LoopSize.MEDIUM,
    val position: LoopPosition = LoopPosition.TOP,
    val thickness: Float = 2.5f  // mm - ring tube thickness
)
