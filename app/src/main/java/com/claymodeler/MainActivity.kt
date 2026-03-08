package com.claymodeler

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.claymodeler.renderer.ModelRenderer
import com.claymodeler.viewmodel.ModelingViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: ModelingViewModel
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: ModelRenderer
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector
    private lateinit var permissionManager: PermissionManager
    private lateinit var cursorView: com.claymodeler.renderer.ToolCursorView
    private lateinit var fileManager: com.claymodeler.file.FileManager
    private lateinit var stlExporter: com.claymodeler.export.STLExporter
    
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var pointerCount = 0
    private var hasAppliedTool = false
    private var previousHitPoint: com.claymodeler.model.Vector3? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[ModelingViewModel::class.java]
        
        // Initialize permission manager
        permissionManager = PermissionManager(this)
        
        // Initialize file manager
        fileManager = com.claymodeler.file.FileManager(this)
        
        // Initialize STL exporter
        stlExporter = com.claymodeler.export.STLExporter(this)
        
        // Request storage permission if needed
        if (!permissionManager.hasStoragePermission()) {
            permissionManager.requestStoragePermission()
        }
        
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "ClayModeler"
        supportActionBar?.setDisplayShowTitleEnabled(true)
        
        // Set up GLSurfaceView
        glSurfaceView = GLSurfaceView(this)
        glSurfaceView.setEGLContextClientVersion(3) // OpenGL ES 3.0
        
        renderer = ModelRenderer(this)
        glSurfaceView.setRenderer(renderer)
        
        // Add GLSurfaceView to container
        val container = findViewById<FrameLayout>(R.id.viewport_container)
        container.addView(glSurfaceView)
        
        // Add cursor overlay
        cursorView = com.claymodeler.renderer.ToolCursorView(this)
        container.addView(cursorView)
        
        // Set up gesture detectors
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
        gestureDetector = GestureDetector(this, GestureListener())
        
        // Set touch listener
        glSurfaceView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            handleTouchEvent(event)
            true
        }
        
        // Set up tool buttons
        val btnUndo = findViewById<android.widget.Button>(R.id.btn_undo)
        val btnRedo = findViewById<android.widget.Button>(R.id.btn_redo)
        val btnRemove = findViewById<android.widget.Button>(R.id.btn_remove)
        val btnAdd = findViewById<android.widget.Button>(R.id.btn_add)
        val btnPull = findViewById<android.widget.Button>(R.id.btn_pull)
        val btnView = findViewById<android.widget.Button>(R.id.btn_view)
        
        val toolButtons = listOf(btnRemove, btnAdd, btnPull, btnView)
        
        btnUndo.setOnClickListener {
            viewModel.undo()
        }
        btnRedo.setOnClickListener {
            viewModel.redo()
        }
        btnRemove.setOnClickListener {
            viewModel.setTool(viewModel.removeClayTool)
        }
        btnAdd.setOnClickListener {
            viewModel.setTool(viewModel.addClayTool)
        }
        btnPull.setOnClickListener {
            viewModel.setTool(viewModel.pullClayTool)
        }
        btnView.setOnClickListener {
            viewModel.setTool(viewModel.viewModeTool)
        }
        
        // Observe undo/redo state
        viewModel.canUndo.observe(this) { canUndo ->
            btnUndo.isEnabled = canUndo
        }
        viewModel.canRedo.observe(this) { canRedo ->
            btnRedo.isEnabled = canRedo
        }
        
        // Observe active tool changes to update UI
        viewModel.activeTool.observe(this) { tool ->
            toolButtons.forEach { it.isSelected = false }
            when (tool) {
                viewModel.removeClayTool -> btnRemove.isSelected = true
                viewModel.addClayTool -> btnAdd.isSelected = true
                viewModel.pullClayTool -> btnPull.isSelected = true
                viewModel.viewModeTool -> btnView.isSelected = true
            }
            // Hide cursor in view mode
            if (!tool.isEditTool()) {
                cursorView.updateCursor(0f, 0f, 0f, false)
            }
        }
        
        // Update cursor size when brush size changes
        val updateCursorSize = {
            val radiusPixels = viewModel.toolEngine.brushSize * 100f
            // Cursor will be shown on touch, just update size
            cursorView.updateCursor(cursorView.width / 2f, cursorView.height / 2f, radiusPixels, false)
        }
        
        // Set up sliders
        findViewById<android.widget.SeekBar>(R.id.slider_size).setOnSeekBarChangeListener(
            object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    viewModel.setBrushSize(progress / 100f * 0.9f + 0.1f) // 0.1 to 1.0 (reduced from 2.0)
                    updateCursorSize()
                }
                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            }
        )
        
        findViewById<android.widget.SeekBar>(R.id.slider_strength).setOnSeekBarChangeListener(
            object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    viewModel.setStrength(progress / 100f * 0.9f + 0.1f) // 0.1 to 1.0
                }
                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            }
        )
        
        // Observe tool changes
        viewModel.activeTool.observe(this) { tool ->
            // Update UI to highlight selected tool
            updateToolSelection(tool)
        }
        
        // Observe model changes
        viewModel.model.observe(this) { model ->
            glSurfaceView.queueEvent {
                renderer.setModel(model)
            }
        }
        
        // Set up auto-save
        viewModel.onAutoSave = { model ->
            try {
                fileManager.save(model, "autosave")
            } catch (e: Exception) {
                // Silent fail for auto-save
            }
        }
        viewModel.startAutoSave()
        
        // Check for autosave on start
        checkForAutosave()
    }
    
    private fun updateToolSelection(tool: com.claymodeler.tool.Tool) {
        // Reset all buttons
        findViewById<android.widget.Button>(R.id.btn_remove).isSelected = false
        findViewById<android.widget.Button>(R.id.btn_add).isSelected = false
        findViewById<android.widget.Button>(R.id.btn_pull).isSelected = false
        findViewById<android.widget.Button>(R.id.btn_view).isSelected = false
        
        // Highlight active tool
        when (tool) {
            viewModel.removeClayTool -> findViewById<android.widget.Button>(R.id.btn_remove).isSelected = true
            viewModel.addClayTool -> findViewById<android.widget.Button>(R.id.btn_add).isSelected = true
            viewModel.pullClayTool -> findViewById<android.widget.Button>(R.id.btn_pull).isSelected = true
            viewModel.viewModeTool -> findViewById<android.widget.Button>(R.id.btn_view).isSelected = true
        }
    }
    
    private fun handleTouchEvent(event: MotionEvent) {
        pointerCount = event.pointerCount
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                hasAppliedTool = false
                previousHitPoint = null
                
                // Show cursor for edit tools on single touch
                if (pointerCount == 1 && viewModel.toolEngine.isEditMode()) {
                    val radiusPixels = viewModel.toolEngine.brushSize * 100f
                    cursorView.updateCursor(event.x, event.y, radiusPixels, true)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y
                val dx = x - lastTouchX
                val dy = y - lastTouchY
                
                when (pointerCount) {
                    1 -> {
                        // Single finger - rotate or edit
                        if (viewModel.toolEngine.isEditMode()) {
                            // Update cursor position
                            val radiusPixels = viewModel.toolEngine.brushSize * 100f
                            cursorView.updateCursor(x, y, radiusPixels, true)
                            
                            // Save state before first tool application
                            if (!hasAppliedTool) {
                                viewModel.saveState()
                                hasAppliedTool = true
                            }
                            
                            // Apply tool
                            glSurfaceView.queueEvent {
                                val hit = renderer.performRaycast(
                                    x, y,
                                    glSurfaceView.width, glSurfaceView.height
                                )
                                
                                if (hit != null) {
                                    val model = viewModel.model.value
                                    val tool = viewModel.toolEngine.getActiveTool()
                                    
                                    if (model != null && tool != null) {
                                        // Calculate drag direction
                                        val dragDir = if (previousHitPoint != null) {
                                            hit.hitPoint - previousHitPoint!!
                                        } else {
                                            com.claymodeler.model.Vector3(0f, 0f, 0f)
                                        }
                                        
                                        tool.apply(
                                            model,
                                            hit.hitPoint,
                                            viewModel.toolEngine.strength,
                                            viewModel.toolEngine.brushSize,
                                            dragDir
                                        )
                                        renderer.updateModel()
                                        previousHitPoint = hit.hitPoint
                                    }
                                }
                            }
                        } else {
                            glSurfaceView.queueEvent {
                                renderer.rotateCamera(dx, dy)
                            }
                        }
                    }
                    2 -> {
                        // Two fingers - pinch zoom (handled by scaleGestureDetector) or pan
                        cursorView.updateCursor(0f, 0f, 0f, false)
                        // Only pan if not actively pinching
                        if (!scaleGestureDetector.isInProgress) {
                            glSurfaceView.queueEvent {
                                renderer.panCamera(dx * 0.01f, dy * 0.01f)
                            }
                        }
                    }
                }
                
                lastTouchX = x
                lastTouchY = y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                // Hide cursor when touch ends
                cursorView.updateCursor(0f, 0f, 0f, false)
            }
        }
    }
    
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            android.util.Log.d("MainActivity", "onScale called: scaleFactor=$scaleFactor")
            glSurfaceView.queueEvent {
                renderer.zoomCamera(scaleFactor)
            }
            return true
        }
        
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            android.util.Log.d("MainActivity", "onScaleBegin")
            return true
        }
    }
    
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            glSurfaceView.queueEvent {
                renderer.resetCamera()
            }
            return true
        }
    }
    
    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }
    
    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }
    
    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menu.add(0, 1, 0, "New")
        menu.add(0, 2, 0, "Save")
        menu.add(0, 3, 0, "Load")
        menu.add(0, 4, 0, "Export STL")
        return true
    }
    
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            1 -> {
                showNewDialog()
                true
            }
            2 -> {
                showSaveDialog()
                true
            }
            3 -> {
                showLoadDialog()
                true
            }
            4 -> {
                showExportDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showNewDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("New Model")
            .setMessage("Create a new model? Unsaved changes will be lost.")
            .setPositiveButton("Create") { _, _ ->
                viewModel.createNewModel()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showSaveDialog() {
        com.claymodeler.ui.SaveDialog(this) { filename ->
            try {
                val model = viewModel.model.value ?: return@SaveDialog
                fileManager.save(model, filename)
                android.widget.Toast.makeText(this, "Saved: $filename", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(this, "Save failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }.show()
    }
    
    private fun showLoadDialog() {
        val files = fileManager.listFiles()
        com.claymodeler.ui.LoadDialog(this, files) { filename ->
            try {
                val model = fileManager.load(filename)
                viewModel.model.value?.let { currentModel ->
                    currentModel.vertices.clear()
                    currentModel.faces.clear()
                    currentModel.normals.clear()
                    currentModel.vertices.addAll(model.vertices)
                    currentModel.faces.addAll(model.faces)
                    currentModel.normals.addAll(model.normals)
                    
                    glSurfaceView.queueEvent {
                        renderer.setModel(currentModel)
                    }
                }
                android.widget.Toast.makeText(this, "Loaded: $filename", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(this, "Load failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }.show()
    }
    
    private fun checkForAutosave() {
        val files = fileManager.listFiles()
        if (files.contains("autosave")) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Restore Autosave")
                .setMessage("An autosaved model was found. Do you want to restore it?")
                .setPositiveButton("Restore") { _, _ ->
                    try {
                        val model = fileManager.load("autosave")
                        viewModel.model.value?.let { currentModel ->
                            currentModel.vertices.clear()
                            currentModel.faces.clear()
                            currentModel.normals.clear()
                            currentModel.vertices.addAll(model.vertices)
                            currentModel.faces.addAll(model.faces)
                            currentModel.normals.addAll(model.normals)
                            
                            glSurfaceView.queueEvent {
                                renderer.setModel(currentModel)
                            }
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(this, "Restore failed", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Discard") { _, _ ->
                    fileManager.delete("autosave")
                }
                .show()
        }
    }
    
    private fun showExportDialog() {
        com.claymodeler.ui.ExportDialog(this) { filename, size, validate ->
            try {
                val model = viewModel.model.value ?: return@ExportDialog
                val path = stlExporter.exportBinary(model, filename, size, validate)
                android.widget.Toast.makeText(this, "Exported to $path", android.widget.Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(this, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }.show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopAutoSave()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.handlePermissionResult(
            requestCode,
            grantResults,
            onGranted = {
                // Permission granted - can now save/load files
            },
            onDenied = {
                // Permission denied - show message
                android.widget.Toast.makeText(
                    this,
                    "Storage permission required for saving models",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        )
    }
}
