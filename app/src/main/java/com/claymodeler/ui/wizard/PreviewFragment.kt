package com.claymodeler.ui.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.claymodeler.R
import com.claymodeler.export.AttachmentType
import com.claymodeler.export.geometry.*

class PreviewFragment : Fragment() {

    private lateinit var vm: ExportWizardViewModel
    private var previewView: WizardPreviewView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_preview, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm = ViewModelProvider(requireActivity())[ExportWizardViewModel::class.java]

        val container = view.findViewById<FrameLayout>(R.id.preview_3d_container)
        val txtInfo = view.findViewById<TextView>(R.id.txt_preview_info)
        val btnBack = view.findViewById<Button>(R.id.btn_back_to_edit)

        btnBack.setOnClickListener { vm.goToStep(2) }

        val model = vm.model.value
        if (model == null) {
            txtInfo.text = "No model loaded"
            return
        }

        // Build merged model for preview
        val merged = buildMergedModel()

        // Add 3D preview with merged model
        previewView = WizardPreviewView(requireContext()).also {
            container.addView(it)
            it.setModel(merged)
        }

        val bb = BaseGenerator.boundingBox(merged)
        val size = bb.second - bb.first
        val scale = vm.scaleFactor * 100f

        txtInfo.text = buildString {
            append("Vertices: ${merged.vertices.size}  Faces: ${merged.faces.size}\n")
            append("Dimensions: %.0f × %.0f × %.0f mm\n".format(size.x * scale, size.y * scale, size.z * scale))
            if (vm.attachmentType != AttachmentType.NONE) {
                append("Attachment: ${vm.attachmentType.name.replace('_', ' ').lowercase()}")
            }
        }

        // Validate manifold
        val manifold = GeometryMerger().validateManifold(merged)
        if (!manifold.valid) {
            txtInfo.append("\n⚠ Mesh issues: ${manifold.issues.joinToString(", ")}")
        }
    }

    override fun onResume() {
        super.onResume()
        previewView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        previewView?.onPause()
    }

    private fun buildMergedModel(): com.claymodeler.model.ClayModel {
        val model = vm.model.value!!
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
