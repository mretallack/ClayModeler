package com.claymodeler.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import com.claymodeler.R
import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3

class LightingDialog(
    context: Context,
    private val model: ClayModel
) : Dialog(context) {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_lighting)
        setTitle("Lighting Settings")
        
        val sliderX = findViewById<SeekBar>(R.id.slider_light_x)
        val sliderY = findViewById<SeekBar>(R.id.slider_light_y)
        val sliderZ = findViewById<SeekBar>(R.id.slider_light_z)
        val sliderIntensity = findViewById<SeekBar>(R.id.slider_light_intensity)
        val btnReset = findViewById<Button>(R.id.btn_reset_lighting)
        
        // Initialize with current values (-5 to 5 mapped to 0-100)
        sliderX.progress = ((model.lightPosition.x + 5f) / 10f * 100f).toInt()
        sliderY.progress = ((model.lightPosition.y + 5f) / 10f * 100f).toInt()
        sliderZ.progress = ((model.lightPosition.z + 5f) / 10f * 100f).toInt()
        sliderIntensity.progress = (model.lightIntensity / 2f * 100f).toInt()
        
        sliderX.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                model.lightPosition = Vector3(progress / 100f * 10f - 5f, model.lightPosition.y, model.lightPosition.z)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        sliderY.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                model.lightPosition = Vector3(model.lightPosition.x, progress / 100f * 10f - 5f, model.lightPosition.z)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        sliderZ.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                model.lightPosition = Vector3(model.lightPosition.x, model.lightPosition.y, progress / 100f * 10f - 5f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        sliderIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                model.lightIntensity = progress / 100f * 2f
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        btnReset.setOnClickListener {
            model.resetLighting()
            sliderX.progress = 70
            sliderY.progress = 80
            sliderZ.progress = 70
            sliderIntensity.progress = 50
        }
    }
}
