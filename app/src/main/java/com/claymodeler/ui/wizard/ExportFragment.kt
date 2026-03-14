package com.claymodeler.ui.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.claymodeler.R
import com.claymodeler.export.AttachmentType
import com.claymodeler.export.PresetManager
import com.claymodeler.export.STLExporter
import com.claymodeler.export.geometry.*

class ExportFragment : Fragment() {

    private lateinit var vm: ExportWizardViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_export, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm = ViewModelProvider(requireActivity())[ExportWizardViewModel::class.java]

        val txtSummary = view.findViewById<TextView>(R.id.txt_summary)
        val editFilename = view.findViewById<EditText>(R.id.edit_filename)
        val chkPreset = view.findViewById<CheckBox>(R.id.chk_save_preset)
        val editPreset = view.findViewById<EditText>(R.id.edit_preset_name)
        val progress = view.findViewById<ProgressBar>(R.id.progress_export)
        val txtResult = view.findViewById<TextView>(R.id.txt_result)

        val model = vm.model.value
        if (model == null) {
            txtSummary.text = "No model loaded"
            return
        }

        val estFaces = model.faces.size + if (vm.attachmentType != AttachmentType.NONE) 1000 else 0
        val estSize = estFaces * 50 / 1024 // rough KB estimate for binary STL

        txtSummary.text = buildString {
            append("Attachment: ${vm.attachmentType.name.replace('_', ' ')}\n")
            append("Scale: ${String.format("%.1f", vm.scaleFactor)}x\n")
            append("Vertices: ~${model.vertices.size}\n")
            append("Faces: ~$estFaces\n")
            append("Estimated file size: ~${estSize} KB")
        }

        editFilename.setText(vm.fileName.ifEmpty { "clay_model" })

        chkPreset.setOnCheckedChangeListener { _, checked ->
            editPreset.visibility = if (checked) View.VISIBLE else View.GONE
        }

        // Wire the Export button (which is the "Next" button on step 5)
        val btnExport = requireActivity().findViewById<Button>(R.id.btn_next)
        btnExport.text = "Export"
        btnExport.setOnClickListener {
            val filename = editFilename.text.toString().trim()
            if (filename.isEmpty()) {
                editFilename.error = "Enter a filename"
                return@setOnClickListener
            }

            vm.fileName = filename
            vm.saveAsPreset = chkPreset.isChecked
            vm.presetName = editPreset.text.toString().trim()

            progress.visibility = View.VISIBLE
            progress.isIndeterminate = true
            btnExport.isEnabled = false

            try {
                val finalModel = buildFinalModel()
                val exporter = STLExporter(requireContext())
                val path = exporter.exportBinary(finalModel, filename, vm.scaleFactor * 100f)

                // Save preset if requested
                if (vm.saveAsPreset && vm.presetName.isNotEmpty()) {
                    PresetManager(requireContext()).save(vm.presetName, vm.buildConfiguration())
                }

                progress.visibility = View.GONE
                txtResult.text = "✓ Exported to $path"
                txtResult.setTextColor(resources.getColor(R.color.primary, null))
                txtResult.visibility = View.VISIBLE
                btnExport.text = "Done"
                btnExport.isEnabled = true
                btnExport.setOnClickListener { requireActivity().finish() }
            } catch (e: Exception) {
                progress.visibility = View.GONE
                txtResult.text = "✗ Export failed: ${e.message}"
                txtResult.setTextColor(resources.getColor(R.color.accent, null))
                txtResult.visibility = View.VISIBLE
                btnExport.isEnabled = true
            }
        }
    }

    private fun buildFinalModel(): com.claymodeler.model.ClayModel {
        val model = vm.model.value!!

        if (vm.attachmentType == AttachmentType.NONE) return model

        val placement = vm.placement
        val attachment = when (vm.attachmentType) {
            AttachmentType.BASE -> BaseGenerator().generate(model, vm.baseConfig, vm.scaleFactor * 100f)
            AttachmentType.KEYRING_LOOP -> {
                if (placement == null) return model
                LoopGenerator().generate(model, vm.keyringConfig, placement, vm.scaleFactor * 100f)
            }
            AttachmentType.WALL_HOOK -> {
                if (placement == null) return model
                HookGenerator().generate(model, vm.hookConfig, placement, vm.scaleFactor * 100f)
            }
            AttachmentType.NONE -> return model
        }

        return GeometryMerger().merge(model, attachment)
    }
}
