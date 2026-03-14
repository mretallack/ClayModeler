package com.claymodeler.export

enum class HookType {
    KEYHOLE, MOUNTING_HOLES, HANGING_LOOP
}

enum class HookPosition {
    AUTO, TOP_CENTER, CENTER, BOTTOM_CENTER
}

data class HookConfig(
    val type: HookType = HookType.KEYHOLE,
    val position: HookPosition = HookPosition.AUTO
)
