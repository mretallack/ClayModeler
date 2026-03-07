package com.claymodeler.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.*
import com.claymodeler.R

class ExportDialog(
    context: Context,
    private val onExport: (String, Float, Boolean) -> Unit
) : Dialog(context) {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_export)
        
        val editFilename = findViewById<EditText>(R.id.edit_filename)
        val spinnerSize = findViewById<Spinner>(R.id.spinner_size)
        val checkValidate = findViewById<CheckBox>(R.id.check_validate)
        val btnExport = findViewById<Button>(R.id.btn_export)
        val btnCancel = findViewById<Button>(R.id.btn_cancel)
        
        // Set up size spinner
        val sizes = arrayOf("50mm", "100mm", "150mm", "200mm")
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, sizes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSize.adapter = adapter
        spinnerSize.setSelection(1) // Default 100mm
        
        btnExport.setOnClickListener {
            val filename = editFilename.text.toString().trim()
            
            if (filename.isEmpty()) {
                Toast.makeText(context, "Please enter a filename", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val sizeStr = spinnerSize.selectedItem.toString().replace("mm", "")
            val size = sizeStr.toFloatOrNull() ?: 100f
            val validate = checkValidate.isChecked
            
            onExport(filename, size, validate)
            dismiss()
        }
        
        btnCancel.setOnClickListener {
            dismiss()
        }
    }
}
