package com.claymodeler.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.claymodeler.R

class SaveDialog(
    context: Context,
    private val onSave: (String) -> Unit
) : Dialog(context) {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_save)
        
        val editText = findViewById<EditText>(R.id.edit_filename)
        val btnSave = findViewById<Button>(R.id.btn_save)
        val btnCancel = findViewById<Button>(R.id.btn_cancel)
        
        btnSave.setOnClickListener {
            val filename = editText.text.toString().trim()
            
            if (filename.isEmpty()) {
                Toast.makeText(context, "Please enter a filename", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (!isValidFilename(filename)) {
                Toast.makeText(context, "Invalid filename. Use only letters, numbers, and underscores", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            onSave(filename)
            dismiss()
        }
        
        btnCancel.setOnClickListener {
            dismiss()
        }
    }
    
    private fun isValidFilename(filename: String): Boolean {
        return filename.matches(Regex("^[a-zA-Z0-9_-]+$"))
    }
}
