package com.claymodeler.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.claymodeler.model.ClayModel
import com.claymodeler.tool.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ModelingViewModel : ViewModel() {
    private val _model = MutableLiveData<ClayModel>()
    val model: LiveData<ClayModel> = _model
    
    private val _activeTool = MutableLiveData<Tool>()
    val activeTool: LiveData<Tool> = _activeTool
    
    private val _canUndo = MutableLiveData<Boolean>(false)
    val canUndo: LiveData<Boolean> = _canUndo
    
    private val _canRedo = MutableLiveData<Boolean>(false)
    val canRedo: LiveData<Boolean> = _canRedo
    
    val toolEngine = ToolEngine()
    
    val removeClayTool = RemoveClayTool()
    val addClayTool = AddClayTool()
    val pullClayTool = PullClayTool()
    val smoothTool = SmoothTool()
    val flattenTool = FlattenTool()
    val pinchTool = PinchTool()
    val inflateTool = InflateTool()
    val viewModeTool = ViewModeTool()
    val lightModeTool = LightModeTool()
    
    private val undoStack = ArrayDeque<ClayModel>(20)
    private val redoStack = ArrayDeque<ClayModel>(20)
    private val maxHistorySize = 20
    
    private var autoSaveJob: Job? = null
    private var lastModifiedTime = System.currentTimeMillis()
    var onAutoSave: ((ClayModel) -> Unit)? = null
    
    init {
        // Initialize with default sphere
        val initialModel = ClayModel()
        initialModel.initialize()
        _model.value = initialModel
        
        // Set default tool
        setTool(removeClayTool)
    }
    
    fun setTool(tool: Tool) {
        toolEngine.setActiveTool(tool)
        _activeTool.value = tool
    }
    
    fun setBrushSize(size: Float) {
        toolEngine.brushSize = size
    }
    
    fun setStrength(strength: Float) {
        toolEngine.strength = strength
    }
    
    fun setSymmetryEnabled(enabled: Boolean) {
        toolEngine.symmetryEnabled = enabled
    }
    
    fun toggleSymmetry() {
        toolEngine.symmetryEnabled = !toolEngine.symmetryEnabled
    }
    
    fun isSymmetryEnabled(): Boolean = toolEngine.symmetryEnabled
    
    fun saveState() {
        val currentModel = _model.value ?: return
        
        // Clone current model and add to undo stack
        val clonedModel = cloneModel(currentModel)
        undoStack.addLast(clonedModel)
        
        // Limit stack size
        if (undoStack.size > maxHistorySize) {
            undoStack.removeFirst()
        }
        
        // Clear redo stack when new action is performed
        redoStack.clear()
        
        updateUndoRedoState()
        
        // Mark as modified
        lastModifiedTime = System.currentTimeMillis()
    }
    
    fun startAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            while (true) {
                delay(60000) // 1 minute
                val model = _model.value
                if (model != null) {
                    onAutoSave?.invoke(model)
                }
            }
        }
    }
    
    fun stopAutoSave() {
        autoSaveJob?.cancel()
    }
    
    fun undo() {
        if (undoStack.isEmpty()) return
        
        val currentModel = _model.value ?: return
        
        // Save current state to redo stack
        redoStack.addLast(cloneModel(currentModel))
        if (redoStack.size > maxHistorySize) {
            redoStack.removeFirst()
        }
        
        // Restore previous state
        val previousModel = undoStack.removeLast()
        _model.value = previousModel
        
        updateUndoRedoState()
    }
    
    fun redo() {
        if (redoStack.isEmpty()) return
        
        val currentModel = _model.value ?: return
        
        // Save current state to undo stack
        undoStack.addLast(cloneModel(currentModel))
        if (undoStack.size > maxHistorySize) {
            undoStack.removeFirst()
        }
        
        // Restore next state
        val nextModel = redoStack.removeLast()
        _model.value = nextModel
        
        updateUndoRedoState()
    }
    
    private fun updateUndoRedoState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }
    
    private fun cloneModel(model: ClayModel): ClayModel {
        val cloned = ClayModel()
        cloned.vertices.addAll(model.vertices.map { it.copy() })
        cloned.faces.addAll(model.faces.map { it.copy() })
        cloned.normals.addAll(model.normals.map { it.copy() })
        return cloned
    }
    
    fun createNewModel() {
        val newModel = ClayModel()
        newModel.initialize(3)
        _model.value = newModel
        undoStack.clear()
        redoStack.clear()
        updateUndoRedoState()
    }
}
