package com.claymodeler.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.claymodeler.R

class LoadDialog(
    context: Context,
    private val files: List<String>,
    private val onLoad: (String) -> Unit
) : Dialog(context) {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_load)
        
        val container = findViewById<LinearLayout>(R.id.files_container)
        val btnCancel = findViewById<Button>(R.id.btn_cancel)
        
        if (files.isEmpty()) {
            val emptyText = TextView(context).apply {
                text = "No saved models"
                textSize = 16f
                setPadding(16, 16, 16, 16)
            }
            container.addView(emptyText)
        } else {
            files.forEach { filename ->
                val button = Button(context).apply {
                    text = filename
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, 8)
                    }
                    setOnClickListener {
                        onLoad(filename)
                        dismiss()
                    }
                }
                container.addView(button)
            }
        }
        
        btnCancel.setOnClickListener {
            dismiss()
        }
    }
}
