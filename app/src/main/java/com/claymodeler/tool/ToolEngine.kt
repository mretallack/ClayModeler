package com.claymodeler.tool

class ToolEngine {
    private var activeTool: Tool? = null
    var brushSize: Float = 0.5f
        set(value) {
            field = value.coerceIn(0.1f, 2.0f)
        }
    var strength: Float = 0.5f
        set(value) {
            field = value.coerceIn(0.1f, 1.0f)
        }
    var symmetryEnabled: Boolean = false
    
    fun setActiveTool(tool: Tool) {
        activeTool = tool
    }
    
    fun getActiveTool(): Tool? = activeTool
    
    fun isEditMode(): Boolean = activeTool?.isEditTool() ?: false
}
