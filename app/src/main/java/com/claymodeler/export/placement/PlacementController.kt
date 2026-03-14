package com.claymodeler.export.placement

import com.claymodeler.export.PlacementResult
import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3
import com.claymodeler.renderer.Octree

class PlacementController(
    private val surfacePicker: SurfacePicker = SurfacePicker(),
    private val undoManager: PlacementUndoManager = PlacementUndoManager()
) {
    var currentPlacement: PlacementResult? = null
        private set
    var snapEnabled = false
    var snapGridSize = 5f

    private var viewMatrix = FloatArray(16)
    private var projMatrix = FloatArray(16)
    private var screenW = 0
    private var screenH = 0

    fun setMatrices(view: FloatArray, proj: FloatArray, w: Int, h: Int) {
        viewMatrix = view; projMatrix = proj; screenW = w; screenH = h
    }

    /** Returns true if the tap point is near the current attachment placement. */
    fun isOnAttachment(x: Float, y: Float, model: ClayModel, octree: Octree? = null): Boolean {
        val cur = currentPlacement ?: return false
        val hit = surfacePicker.pick(x, y, screenW, screenH, viewMatrix, projMatrix, model, octree)
            ?: return false
        return distSq(hit.position, cur.position) < 0.1f
    }

    // Single tap → place
    fun onTap(x: Float, y: Float, model: ClayModel, octree: Octree? = null): PlacementResult? {
        val result = surfacePicker.pick(x, y, screenW, screenH, viewMatrix, projMatrix, model, octree)
            ?: return null
        val snapped = maybeSnap(result)
        val old = currentPlacement
        currentPlacement = snapped
        undoManager.record(PlacementAction.PLACE, old, snapped)
        return snapped
    }

    // Drag → slide along surface
    fun onDrag(x: Float, y: Float, model: ClayModel, octree: Octree? = null): PlacementResult? {
        val cur = currentPlacement ?: return null
        val hit = surfacePicker.pick(x, y, screenW, screenH, viewMatrix, projMatrix, model, octree)
            ?: return cur
        val moved = hit.copy(rotation = cur.rotation, scale = cur.scale)
        val snapped = maybeSnap(moved)
        val old = currentPlacement
        currentPlacement = snapped
        undoManager.record(PlacementAction.MOVE, old, snapped)
        return snapped
    }

    // Two-finger rotate
    fun onRotate(angleDelta: Float) {
        val cur = currentPlacement ?: return
        val old = cur
        currentPlacement = cur.copy(rotation = cur.rotation + angleDelta)
        undoManager.record(PlacementAction.ROTATE, old, currentPlacement)
    }

    // Pinch → scale
    fun onScale(scaleFactor: Float) {
        val cur = currentPlacement ?: return
        val newScale = (cur.scale * scaleFactor).coerceIn(0.5f, 3f)
        val old = cur
        currentPlacement = cur.copy(scale = newScale)
        undoManager.record(PlacementAction.RESIZE, old, currentPlacement)
    }

    // Long press → snap to nearest preset position
    fun onLongPress(presetPositions: List<PlacementResult>) {
        val cur = currentPlacement ?: return
        val nearest = presetPositions.minByOrNull { distSq(it.position, cur.position) } ?: return
        val old = cur
        currentPlacement = nearest.copy(rotation = cur.rotation, scale = cur.scale)
        undoManager.record(PlacementAction.MOVE, old, currentPlacement)
    }

    // Double tap → remove
    fun onDoubleTap() {
        val old = currentPlacement ?: return
        currentPlacement = null
        undoManager.record(PlacementAction.REMOVE, old, null)
    }

    // Three-finger tap → undo
    fun onThreeFingerTap() { undo() }

    fun undo() { currentPlacement = undoManager.undo() }
    fun redo() { currentPlacement = undoManager.redo() }
    fun canUndo() = undoManager.canUndo()
    fun canRedo() = undoManager.canRedo()

    private fun maybeSnap(p: PlacementResult): PlacementResult {
        if (!snapEnabled) return p
        val gs = snapGridSize
        return p.copy(position = Vector3(
            kotlin.math.round(p.position.x / gs) * gs,
            kotlin.math.round(p.position.y / gs) * gs,
            kotlin.math.round(p.position.z / gs) * gs
        ))
    }

    private fun distSq(a: Vector3, b: Vector3): Float {
        val d = a - b; return d.x * d.x + d.y * d.y + d.z * d.z
    }
}
