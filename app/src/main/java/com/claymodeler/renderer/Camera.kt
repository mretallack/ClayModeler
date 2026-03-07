package com.claymodeler.renderer

import android.opengl.Matrix
import kotlin.math.cos
import kotlin.math.sin

class Camera {
    private var distance = 4f
    private var angleX = 0f  // Rotation around X axis (up/down)
    private var angleY = 0f  // Rotation around Y axis (left/right)
    private var panX = 0f
    private var panY = 0f
    
    private val viewMatrix = FloatArray(16)
    
    fun getViewMatrix(): FloatArray {
        // Calculate camera position using spherical coordinates
        val eyeX = distance * cos(Math.toRadians(angleX.toDouble())).toFloat() * sin(Math.toRadians(angleY.toDouble())).toFloat() + panX
        val eyeY = distance * sin(Math.toRadians(angleX.toDouble())).toFloat() + panY
        val eyeZ = distance * cos(Math.toRadians(angleX.toDouble())).toFloat() * cos(Math.toRadians(angleY.toDouble())).toFloat()
        
        Matrix.setLookAtM(viewMatrix, 0,
            eyeX, eyeY, eyeZ,  // eye
            panX, panY, 0f,     // center (with pan offset)
            0f, 1f, 0f          // up
        )
        
        return viewMatrix
    }
    
    fun rotate(deltaX: Float, deltaY: Float) {
        angleY += deltaX * 0.3f
        angleX += deltaY * 0.3f
        
        // Clamp vertical rotation
        angleX = angleX.coerceIn(-89f, 89f)
    }
    
    fun zoom(scaleFactor: Float) {
        val oldDistance = distance
        distance /= scaleFactor
        distance = distance.coerceIn(3f, 20f)
        android.util.Log.d("Camera", "zoom: scaleFactor=$scaleFactor, oldDistance=$oldDistance, newDistance=$distance")
    }
    
    fun pan(deltaX: Float, deltaY: Float) {
        val scale = distance * 0.001f
        panX += deltaX * scale
        panY -= deltaY * scale
    }
    
    fun reset() {
        distance = 4f
        angleX = 0f
        angleY = 0f
        panX = 0f
        panY = 0f
    }
    
    fun getDistance(): Float = distance
}
