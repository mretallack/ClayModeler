package com.claymodeler.export

enum class BaseShape {
    CIRCULAR, RECTANGULAR, CUSTOM
}

data class BaseConfig(
    val shape: BaseShape = BaseShape.CIRCULAR,
    val width: Float = 0f,
    val depth: Float = 0f,
    val height: Float = 3f,
    val margin: Float = 2f,
    val overlap: Float = 3f  // mm - how far base top penetrates into model
)
