package com.claymodeler.export

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import java.io.File

/**
 * Tests for preset storage logic without Android Context dependency.
 * Tests file I/O and enum serialization directly.
 */
class PresetManagerSerializationTest : FunSpec({

    test("AttachmentType enum values round-trip through name") {
        for (type in AttachmentType.entries) {
            AttachmentType.valueOf(type.name) shouldBe type
        }
    }

    test("BaseShape enum values round-trip through name") {
        for (shape in BaseShape.entries) {
            BaseShape.valueOf(shape.name) shouldBe shape
        }
    }

    test("LoopSize enum values round-trip through name") {
        for (size in LoopSize.entries) {
            LoopSize.valueOf(size.name) shouldBe size
        }
    }

    test("LoopPosition enum values round-trip through name") {
        for (pos in LoopPosition.entries) {
            LoopPosition.valueOf(pos.name) shouldBe pos
        }
    }

    test("HookType enum values round-trip through name") {
        for (type in HookType.entries) {
            HookType.valueOf(type.name) shouldBe type
        }
    }

    test("HookPosition enum values round-trip through name") {
        for (pos in HookPosition.entries) {
            HookPosition.valueOf(pos.name) shouldBe pos
        }
    }

    test("ExportConfiguration defaults are sensible") {
        val config = ExportConfiguration()
        config.attachmentType shouldBe AttachmentType.NONE
        config.scaleFactor shouldBe 1f
        config.sizeInMm shouldBe 100f
        config.baseConfig.height shouldBe 3f
        config.keyringConfig.size shouldBe LoopSize.MEDIUM
        config.hookConfig.type shouldBe HookType.KEYHOLE
    }

    test("file-based save/load/delete using temp dir") {
        val dir = kotlin.io.path.createTempDirectory("presets").toFile()
        try {
            // Save
            val content = """{"attachmentType":"BASE","scaleFactor":1.5}"""
            File(dir, "test.json").writeText(content)
            File(dir, "test.json").exists() shouldBe true

            // Load
            val loaded = File(dir, "test.json").readText()
            loaded.contains("BASE") shouldBe true

            // Delete
            File(dir, "test.json").delete()
            File(dir, "test.json").exists() shouldBe false

            // List
            File(dir, "a.json").writeText("{}")
            File(dir, "b.json").writeText("{}")
            File(dir, "c.txt").writeText("not a preset")
            val list = dir.listFiles()?.filter { it.extension == "json" }?.map { it.nameWithoutExtension } ?: emptyList()
            list shouldContain "a"
            list shouldContain "b"
            (list.contains("c")) shouldBe false
        } finally {
            dir.deleteRecursively()
        }
    }

    test("corrupted file content can be detected") {
        val dir = kotlin.io.path.createTempDirectory("presets").toFile()
        try {
            File(dir, "bad.json").writeText("not valid json {{{")
            val content = File(dir, "bad.json").readText()
            val isValid = try {
                // Simulate what PresetManager does: try to parse, catch, delete
                if (!content.startsWith("{") || !content.endsWith("}")) throw Exception("Invalid")
                true
            } catch (_: Exception) {
                File(dir, "bad.json").delete()
                false
            }
            isValid shouldBe false
            File(dir, "bad.json").exists() shouldBe false
        } finally {
            dir.deleteRecursively()
        }
    }
})
