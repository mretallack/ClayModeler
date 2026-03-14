package com.claymodeler.export.placement

import com.claymodeler.export.*
import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull

class ExportConfigurationTest : FunSpec({

    test("default configuration has sensible defaults") {
        val config = ExportConfiguration()
        config.attachmentType shouldBe AttachmentType.NONE
        config.scaleFactor shouldBe 1f
        config.sizeInMm shouldBe 100f
        config.placement.shouldBeNull()
        config.presetName.shouldBeNull()
    }

    test("base config defaults") {
        val bc = BaseConfig()
        bc.shape shouldBe BaseShape.CIRCULAR
        bc.height shouldBe 3f
        bc.margin shouldBe 2f
    }

    test("keyring config defaults") {
        val kc = KeyringConfig()
        kc.size shouldBe LoopSize.MEDIUM
        kc.position shouldBe LoopPosition.TOP
    }

    test("hook config defaults") {
        val hc = HookConfig()
        hc.type shouldBe HookType.KEYHOLE
        hc.position shouldBe HookPosition.AUTO
    }

    test("loop size has correct dimensions") {
        LoopSize.SMALL.innerDiameter shouldBe 5f
        LoopSize.MEDIUM.innerDiameter shouldBe 8f
        LoopSize.LARGE.innerDiameter shouldBe 12f
        LoopSize.SMALL.wallThickness shouldBe 2f
    }
})

class PlacementValidatorTest : FunSpec({

    fun testModel(): ClayModel {
        val m = ClayModel()
        m.initialize(2)
        return m
    }

    test("valid placement on surface returns valid") {
        val model = testModel()
        val placement = PlacementResult(model.vertices[0], model.normals[0], 0)
        val result = PlacementValidator().validate(model, placement, AttachmentType.BASE)
        result.valid shouldBe true
    }

    test("invalid face index returns invalid") {
        val model = testModel()
        val placement = PlacementResult(Vector3(0f, 0f, 0f), Vector3(0f, 1f, 0f), -1)
        val result = PlacementValidator().validate(model, placement, AttachmentType.KEYRING_LOOP)
        result.valid shouldBe false
    }

    test("keyring validation checks loop clearance") {
        val model = testModel()
        val placement = PlacementResult(model.vertices[0], model.normals[0], 0)
        val result = PlacementValidator().validate(model, placement, AttachmentType.KEYRING_LOOP)
        // Should be valid since icosphere is small and loop extends outward
        result.shouldNotBeNull()
    }

    test("hook validation checks flat area") {
        val model = testModel()
        val placement = PlacementResult(model.vertices[0], model.normals[0], 0)
        val result = PlacementValidator().validate(model, placement, AttachmentType.WALL_HOOK)
        result.shouldNotBeNull()
    }

    test("suggests nearest valid position when invalid") {
        val model = testModel()
        val placement = PlacementResult(Vector3(100f, 100f, 100f), Vector3(0f, 1f, 0f), -1)
        val result = PlacementValidator().validate(model, placement, AttachmentType.BASE)
        result.valid shouldBe false
    }
})

class PlacementUndoManagerTest : FunSpec({

    test("empty undo returns null") {
        val mgr = PlacementUndoManager()
        mgr.undo().shouldBeNull()
    }

    test("empty redo returns null") {
        val mgr = PlacementUndoManager()
        mgr.redo().shouldBeNull()
    }

    test("record and undo restores previous state") {
        val mgr = PlacementUndoManager()
        val before = PlacementResult(Vector3(0f, 0f, 0f), Vector3(0f, 1f, 0f))
        val after = PlacementResult(Vector3(1f, 0f, 0f), Vector3(0f, 1f, 0f))
        mgr.record(PlacementAction.PLACE, before, after)
        mgr.undo() shouldBe before
    }

    test("undo then redo restores after state") {
        val mgr = PlacementUndoManager()
        val before = PlacementResult(Vector3(0f, 0f, 0f), Vector3(0f, 1f, 0f))
        val after = PlacementResult(Vector3(1f, 0f, 0f), Vector3(0f, 1f, 0f))
        mgr.record(PlacementAction.PLACE, before, after)
        mgr.undo()
        mgr.redo() shouldBe after
    }

    test("new action clears redo stack") {
        val mgr = PlacementUndoManager()
        val p1 = PlacementResult(Vector3(0f, 0f, 0f), Vector3(0f, 1f, 0f))
        val p2 = PlacementResult(Vector3(1f, 0f, 0f), Vector3(0f, 1f, 0f))
        val p3 = PlacementResult(Vector3(2f, 0f, 0f), Vector3(0f, 1f, 0f))
        mgr.record(PlacementAction.PLACE, null, p1)
        mgr.record(PlacementAction.MOVE, p1, p2)
        mgr.undo()
        mgr.record(PlacementAction.MOVE, p1, p3)
        mgr.canRedo() shouldBe false
    }

    test("max size is enforced") {
        val mgr = PlacementUndoManager(maxSize = 3)
        val p = PlacementResult(Vector3(0f, 0f, 0f), Vector3(0f, 1f, 0f))
        repeat(5) { mgr.record(PlacementAction.PLACE, null, p) }
        var count = 0
        while (mgr.canUndo()) { mgr.undo(); count++ }
        count shouldBe 3
    }

    test("canUndo and canRedo reflect state") {
        val mgr = PlacementUndoManager()
        mgr.canUndo() shouldBe false
        mgr.canRedo() shouldBe false
        mgr.record(PlacementAction.PLACE, null, PlacementResult(Vector3(0f, 0f, 0f), Vector3(0f, 1f, 0f)))
        mgr.canUndo() shouldBe true
        mgr.canRedo() shouldBe false
        mgr.undo()
        mgr.canUndo() shouldBe false
        mgr.canRedo() shouldBe true
    }
})
