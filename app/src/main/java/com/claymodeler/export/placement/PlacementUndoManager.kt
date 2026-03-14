package com.claymodeler.export.placement

import com.claymodeler.export.PlacementResult

enum class PlacementAction { PLACE, MOVE, ROTATE, RESIZE, REMOVE }

data class UndoEntry(val action: PlacementAction, val before: PlacementResult?, val after: PlacementResult?)

class PlacementUndoManager(private val maxSize: Int = 20) {

    private val undoStack = mutableListOf<UndoEntry>()
    private val redoStack = mutableListOf<UndoEntry>()

    fun record(action: PlacementAction, before: PlacementResult?, after: PlacementResult?) {
        undoStack.add(UndoEntry(action, before, after))
        if (undoStack.size > maxSize) undoStack.removeAt(0)
        redoStack.clear()
    }

    fun undo(): PlacementResult? {
        if (undoStack.isEmpty()) return null
        val entry = undoStack.removeAt(undoStack.size - 1)
        redoStack.add(entry)
        return entry.before
    }

    fun redo(): PlacementResult? {
        if (redoStack.isEmpty()) return null
        val entry = redoStack.removeAt(redoStack.size - 1)
        undoStack.add(entry)
        return entry.after
    }

    fun canUndo() = undoStack.isNotEmpty()
    fun canRedo() = redoStack.isNotEmpty()
}
