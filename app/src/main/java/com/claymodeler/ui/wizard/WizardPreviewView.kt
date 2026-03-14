package com.claymodeler.ui.wizard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import android.widget.ImageButton
import com.claymodeler.export.PlacementResult
import com.claymodeler.export.placement.SurfacePicker
import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3
import com.claymodeler.renderer.ModelRenderer
import kotlin.math.atan2

/**
 * Reusable 3D preview with dual-mode touch:
 * - Camera mode: orbit/zoom/pan (default)
 * - Placement mode: tap/drag to place attachment on model surface
 */
class WizardPreviewView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    val renderer = ModelRenderer(context)
    private val glView: GLSurfaceView
    private val scaleDetector: ScaleGestureDetector
    private val surfacePicker = SurfacePicker()
    private var lastX = 0f
    private var lastY = 0f
    private var pointerCount = 0

    /** The original model (without attachment) for raycasting against */
    private var rawModel: ClayModel? = null

    var placementMode = false
        private set

    /** Called when user places/drags attachment. Null = placement cleared. */
    var onPlacementChanged: ((PlacementResult?) -> Unit)? = null

    /** Called when user drags in edit mode (dx, dy in screen pixels). Used for model rotation. */
    var onEditDrag: ((Float, Float) -> Unit)? = null

    /** Called when user lifts finger after edit drag. Bake the rotation. */
    var onEditDragEnd: (() -> Unit)? = null

    private var currentPlacement: PlacementResult? = null
    private var lastRebuildTime = 0L

    // Two-finger rotation tracking
    private var initialAngle = 0f
    private var isRotating = false

    private val borderPaint = Paint().apply {
        color = Color.parseColor("#FF6F00") // accent
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val toggleButton: ImageButton

    init {
        glView = GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            // Don't let GLSurfaceView consume touch events — parent handles them
            isClickable = false
            isFocusable = false
        }
        addView(glView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Mode toggle button
        toggleButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_edit)
            setBackgroundColor(Color.parseColor("#CC424242"))
            setPadding(24, 24, 24, 24)
            contentDescription = "Toggle placement mode. Currently: camera mode"
            setOnClickListener { toggleMode() }
            minimumWidth = 48
            minimumHeight = 48
        }
        val btnParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.END
            setMargins(0, 16, 16, 0)
        }
        addView(toggleButton, btnParams)

        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (placementMode && onEditDrag != null) {
                    // Edit mode: ignore pinch (or could scale model)
                    return true
                } else if (placementMode && currentPlacement != null) {
                    val newScale = (currentPlacement!!.scale * detector.scaleFactor).coerceIn(0.5f, 3f)
                    currentPlacement = currentPlacement!!.copy(scale = newScale)
                    onPlacementChanged?.invoke(currentPlacement)
                } else {
                    renderer.zoomCamera(detector.scaleFactor)
                }
                return true
            }
        })

        setWillNotDraw(false)
        isFocusable = true
        isFocusableInTouchMode = true
        contentDescription = "3D model preview. Tap edit button to enter placement mode."
    }

    fun setRawModel(model: ClayModel) {
        rawModel = model
    }

    fun setModel(model: ClayModel) {
        glView.queueEvent { renderer.setModel(model) }
    }

    fun updateModel() {
        glView.queueEvent { renderer.updateModel() }
    }

    fun onResume() = glView.onResume()
    fun onPause() = glView.onPause()

    private fun toggleMode() {
        placementMode = !placementMode
        toggleButton.setImageResource(
            if (placementMode) android.R.drawable.ic_menu_view
            else android.R.drawable.ic_menu_edit
        )
        toggleButton.contentDescription = "Toggle placement mode. Currently: ${if (placementMode) "placement" else "camera"} mode"
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (placementMode) {
            canvas.drawRect(2f, 2f, width - 2f, height - 2f, borderPaint)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null) {
            // Let the toggle button handle its own clicks
            val bx = toggleButton.left; val by = toggleButton.top
            if (ev.x >= bx && ev.x <= bx + toggleButton.width &&
                ev.y >= by && ev.y <= by + toggleButton.height) return false
        }
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        pointerCount = event.pointerCount

        if (placementMode) {
            handlePlacementTouch(event)
        } else {
            handleCameraTouch(event)
        }
        return true
    }

    private fun handleCameraTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { lastX = event.x; lastY = event.y }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX; val dy = event.y - lastY
                if (pointerCount == 1) renderer.rotateCamera(dx, dy)
                else if (pointerCount == 2 && !scaleDetector.isInProgress) renderer.panCamera(dx, dy)
                lastX = event.x; lastY = event.y
            }
        }
    }

    private fun handlePlacementTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x; lastY = event.y
                isRotating = false
                if (onEditDrag == null) doRaycastPlacement(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (onEditDrag != null && pointerCount == 1) {
                    val dx = event.x - lastX; val dy = event.y - lastY
                    // Rotate model matrix instantly (no vertex recalc)
                    glView.queueEvent { renderer.rotateModelMatrix(dx, dy) }
                    lastX = event.x; lastY = event.y
                } else if (pointerCount == 1 && onEditDrag == null) {
                    // Drag to slide along surface
                    throttledRaycast(event.x, event.y)
                } else if (pointerCount == 2 && currentPlacement != null && !scaleDetector.isInProgress) {
                    // Two-finger rotation
                    val angle = twoFingerAngle(event)
                    if (!isRotating) {
                        initialAngle = angle
                        isRotating = true
                    } else {
                        val delta = angle - initialAngle
                        currentPlacement = currentPlacement!!.copy(rotation = currentPlacement!!.rotation + delta)
                        initialAngle = angle
                        onPlacementChanged?.invoke(currentPlacement)
                        // Haptic tick every 15 degrees
                        if (kotlin.math.abs(delta) > Math.toRadians(15.0).toFloat()) {
                            performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                }
                lastX = event.x; lastY = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                isRotating = false
                if (onEditDrag != null && event.actionMasked == MotionEvent.ACTION_UP) {
                    onEditDragEnd?.invoke()
                }
            }
        }
    }

    private fun doRaycastPlacement(x: Float, y: Float) {
        val model = rawModel ?: return
        val viewMatrix = renderer.camera.getViewMatrix()
        val projMatrix = renderer.projectionMatrix
        val result = surfacePicker.pick(x, y, width, height, viewMatrix, projMatrix, model)
        if (result != null) {
            // Preserve existing rotation/scale if just moving
            currentPlacement = result.copy(
                rotation = currentPlacement?.rotation ?: 0f,
                scale = currentPlacement?.scale ?: 1f
            )
            onPlacementChanged?.invoke(currentPlacement)
        }
    }

    private fun throttledRaycast(x: Float, y: Float) {
        val now = System.currentTimeMillis()
        if (now - lastRebuildTime < 66) return // ~15fps max
        lastRebuildTime = now
        doRaycastPlacement(x, y)
    }

    private fun twoFingerAngle(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val dx = event.getX(1) - event.getX(0)
        val dy = event.getY(1) - event.getY(0)
        return atan2(dy, dx)
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        val step = 5f
        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> { renderer.rotateCamera(-step, 0f); true }
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> { renderer.rotateCamera(step, 0f); true }
            android.view.KeyEvent.KEYCODE_DPAD_UP -> { renderer.rotateCamera(0f, -step); true }
            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> { renderer.rotateCamera(0f, step); true }
            android.view.KeyEvent.KEYCODE_PLUS, android.view.KeyEvent.KEYCODE_EQUALS -> { renderer.zoomCamera(1.1f); true }
            android.view.KeyEvent.KEYCODE_MINUS -> { renderer.zoomCamera(0.9f); true }
            android.view.KeyEvent.KEYCODE_ENTER, android.view.KeyEvent.KEYCODE_DPAD_CENTER -> { renderer.resetCamera(); true }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
