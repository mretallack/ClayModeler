package com.claymodeler.renderer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import androidx.core.content.ContextCompat
import com.claymodeler.R

class ToolCursorView(context: Context) : View(context) {
    private var cursorX = 0f
    private var cursorY = 0f
    private var cursorRadius = 50f
    private var isVisible = false
    
    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        color = 0x80FFFFFF.toInt() // 50% white
        isAntiAlias = true
    }
    
    private val strokePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    
    init {
        strokePaint.color = ContextCompat.getColor(context, R.color.primary)
    }
    
    fun updateCursor(x: Float, y: Float, radius: Float, visible: Boolean) {
        cursorX = x
        cursorY = y
        cursorRadius = radius
        isVisible = visible
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (isVisible) {
            canvas.drawCircle(cursorX, cursorY, cursorRadius, fillPaint)
            canvas.drawCircle(cursorX, cursorY, cursorRadius, strokePaint)
        }
    }
}
