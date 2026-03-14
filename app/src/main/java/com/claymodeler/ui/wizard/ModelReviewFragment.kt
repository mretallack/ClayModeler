package com.claymodeler.ui.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.claymodeler.R
import com.claymodeler.export.geometry.BaseGenerator

class ModelReviewFragment : Fragment() {

    private lateinit var vm: ExportWizardViewModel
    private var previewView: WizardPreviewView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_model_review, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm = ViewModelProvider(requireActivity())[ExportWizardViewModel::class.java]

        val container = view.findViewById<FrameLayout>(R.id.preview_container)
        val txtDimensions = view.findViewById<TextView>(R.id.txt_dimensions)
        val txtMeshInfo = view.findViewById<TextView>(R.id.txt_mesh_info)
        val txtWarning = view.findViewById<TextView>(R.id.txt_size_warning)
        val txtScale = view.findViewById<TextView>(R.id.txt_scale_value)
        val slider = view.findViewById<SeekBar>(R.id.slider_scale)

        // Add 3D preview with ground grid
        previewView = WizardPreviewView(requireContext()).also {
            it.renderer.showGroundGrid = true
            container.addView(it)
        }

        slider.progress = (vm.scaleFactor * 100).toInt()

        fun update(scale: Float) {
            vm.scaleFactor = scale
            txtScale.text = "${String.format("%.1f", scale)}x"

            val model = vm.model.value ?: return
            previewView?.setModel(model)

            val bb = BaseGenerator.boundingBox(model)
            val size = bb.second - bb.first
            val sizeInMm = 100f * scale
            val w = size.x * sizeInMm; val h = size.y * sizeInMm; val d = size.z * sizeInMm
            txtDimensions.text = "Dimensions: %.0f × %.0f × %.0f mm".format(w, h, d)
            txtMeshInfo.text = "Vertices: ${model.vertices.size}  Faces: ${model.faces.size}"

            val maxDim = maxOf(w, h, d)
            when {
                maxDim < 20f -> {
                    txtWarning.text = "⚠ Very small — consider scaling up"
                    txtWarning.visibility = View.VISIBLE
                }
                maxDim > 200f -> {
                    txtWarning.text = "⚠ Large model — may take long to print"
                    txtWarning.visibility = View.VISIBLE
                }
                else -> txtWarning.visibility = View.GONE
            }
        }

        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                update(progress / 100f)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        view.findViewById<android.widget.Button>(R.id.btn_rot_reset).setOnClickListener {
            vm.resetModelOrientation(); update(vm.scaleFactor)
        }

        // In edit mode, dragging rotates via GL model matrix (instant).
        // On finger up, bake rotation into actual vertices.
        previewView?.onEditDrag = { _, _ -> } // just needs to be non-null to enable edit mode
        previewView?.onEditDragEnd = end@{
            val matrix = previewView?.renderer?.getModelMatrix() ?: return@end
            vm.bakeModelMatrix(matrix)
            previewView?.renderer?.resetModelMatrix()
            update(vm.scaleFactor)
        }

        vm.model.observe(viewLifecycleOwner) { update(vm.scaleFactor) }
    }

    override fun onResume() {
        super.onResume()
        previewView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        previewView?.onPause()
    }
}
