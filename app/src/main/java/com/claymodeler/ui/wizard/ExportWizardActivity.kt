package com.claymodeler.ui.wizard

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.claymodeler.R
import com.claymodeler.model.ClayModel

class ExportWizardActivity : AppCompatActivity() {

    private lateinit var vm: ExportWizardViewModel
    private lateinit var stepText: TextView
    private lateinit var dotsContainer: LinearLayout
    private lateinit var btnBack: Button
    private lateinit var btnNext: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_wizard)

        vm = ViewModelProvider(this)[ExportWizardViewModel::class.java]

        stepText = findViewById(R.id.step_text)
        dotsContainer = findViewById(R.id.dots_container)
        btnBack = findViewById(R.id.btn_back)
        btnNext = findViewById(R.id.btn_next)

        // Load model from companion holder
        modelHolder?.let { vm.setModel(it) }

        findViewById<Button>(R.id.btn_cancel).setOnClickListener { finish() }
        btnBack.setOnClickListener { vm.goBack() }
        btnNext.setOnClickListener {
            if ((vm.currentStep.value ?: 0) == vm.totalSteps - 1) {
                // On last step, next = finish handled by ExportFragment
            } else {
                vm.goNext()
            }
        }

        vm.currentStep.observe(this) { step ->
            updateStep(step)
        }

        if (savedInstanceState == null) {
            updateStep(0)
        }
    }

    private fun updateStep(step: Int) {
        stepText.text = "Step ${step + 1} of ${vm.totalSteps}"
        btnBack.isEnabled = step > 0
        btnNext.text = if (step == vm.totalSteps - 1) "Export" else "Next"

        updateDots(step)

        val fragment: Fragment = when (step) {
            0 -> ModelReviewFragment()
            1 -> AttachmentSelectionFragment()
            2 -> ConfigurationFragment()
            3 -> PreviewFragment()
            4 -> ExportFragment()
            else -> ModelReviewFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.wizard_content, fragment)
            .commit()
    }

    private fun updateDots(current: Int) {
        dotsContainer.removeAllViews()
        for (i in 0 until vm.totalSteps) {
            val dot = TextView(this).apply {
                text = "●"
                textSize = 12f
                setTextColor(if (i == current) getColor(R.color.accent) else getColor(android.R.color.white))
                setPadding(8, 0, 8, 0)
                contentDescription = "Step ${i + 1}" + if (i == current) ", current" else ""
            }
            dotsContainer.addView(dot)
        }
    }

    companion object {
        var modelHolder: ClayModel? = null
    }
}
