package com.claymodeler.export

enum class AttachmentType {
    NONE, BASE, KEYRING_LOOP, WALL_HOOK
}

data class ExportConfiguration(
    val attachmentType: AttachmentType = AttachmentType.NONE,
    val scaleFactor: Float = 1f,
    val sizeInMm: Float = 100f,
    val baseConfig: BaseConfig = BaseConfig(),
    val keyringConfig: KeyringConfig = KeyringConfig(),
    val hookConfig: HookConfig = HookConfig(),
    val placement: PlacementResult? = null,
    val presetName: String? = null
)
