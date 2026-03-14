package com.claymodeler.export

import android.content.Context
import org.json.JSONObject
import java.io.File

class PresetManager(context: Context) {

    private val dir = File(context.filesDir, "presets").apply { mkdirs() }

    fun save(name: String, config: ExportConfiguration) {
        File(dir, "$name.json").writeText(toJson(config).toString())
    }

    fun load(name: String): ExportConfiguration? {
        val file = File(dir, "$name.json")
        return try {
            if (file.exists()) fromJson(JSONObject(file.readText())) else null
        } catch (_: Exception) {
            file.delete()
            null
        }
    }

    fun delete(name: String) {
        File(dir, "$name.json").delete()
    }

    fun listPresets(): List<String> =
        (dir.listFiles()?.filter { it.extension == "json" }?.map { it.nameWithoutExtension } ?: emptyList())

    private fun toJson(c: ExportConfiguration) = JSONObject().apply {
        put("attachmentType", c.attachmentType.name)
        put("scaleFactor", c.scaleFactor.toDouble())
        put("sizeInMm", c.sizeInMm.toDouble())
        put("baseShape", c.baseConfig.shape.name)
        put("baseWidth", c.baseConfig.width.toDouble())
        put("baseDepth", c.baseConfig.depth.toDouble())
        put("baseHeight", c.baseConfig.height.toDouble())
        put("baseMargin", c.baseConfig.margin.toDouble())
        put("keyringSize", c.keyringConfig.size.name)
        put("keyringPosition", c.keyringConfig.position.name)
        put("hookType", c.hookConfig.type.name)
        put("hookPosition", c.hookConfig.position.name)
    }

    private fun fromJson(j: JSONObject) = ExportConfiguration(
        attachmentType = AttachmentType.valueOf(j.getString("attachmentType")),
        scaleFactor = j.getDouble("scaleFactor").toFloat(),
        sizeInMm = j.getDouble("sizeInMm").toFloat(),
        baseConfig = BaseConfig(
            shape = BaseShape.valueOf(j.getString("baseShape")),
            width = j.getDouble("baseWidth").toFloat(),
            depth = j.getDouble("baseDepth").toFloat(),
            height = j.getDouble("baseHeight").toFloat(),
            margin = j.getDouble("baseMargin").toFloat()
        ),
        keyringConfig = KeyringConfig(
            size = LoopSize.valueOf(j.getString("keyringSize")),
            position = LoopPosition.valueOf(j.getString("keyringPosition"))
        ),
        hookConfig = HookConfig(
            type = HookType.valueOf(j.getString("hookType")),
            position = HookPosition.valueOf(j.getString("hookPosition"))
        )
    )
}
