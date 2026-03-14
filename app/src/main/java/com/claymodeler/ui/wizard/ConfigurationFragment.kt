package com.claymodeler.ui.wizard

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.claymodeler.R
import com.claymodeler.export.*
import com.claymodeler.export.geometry.BaseGenerator
import com.claymodeler.export.geometry.GeometryMerger
import com.claymodeler.export.geometry.HookGenerator
import com.claymodeler.export.geometry.LoopGenerator
import com.claymodeler.model.Vector3

class ConfigurationFragment : Fragment() {

    private lateinit var vm: ExportWizardViewModel
    private var previewView: WizardPreviewView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_configuration, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm = ViewModelProvider(requireActivity())[ExportWizardViewModel::class.java]

        val previewContainer = view.findViewById<FrameLayout>(R.id.config_preview_container)
        val noAttachment = view.findViewById<TextView>(R.id.txt_no_attachment)
        val sectionBase = view.findViewById<View>(R.id.section_base)
        val sectionKeyring = view.findViewById<View>(R.id.section_keyring)
        val sectionHook = view.findViewById<View>(R.id.section_hook)
        val toolbar = view.findViewById<View>(R.id.placement_toolbar)

        // Setup 3D preview
        val model = vm.model.value
        previewView = WizardPreviewView(requireContext()).also {
            previewContainer.addView(it)
            if (model != null) {
                it.setRawModel(model)
                it.setModel(model)
            }
            // Wire placement mode: when user drags on model, update placement and rebuild preview
            it.onPlacementChanged = { placement ->
                vm.placement = placement
                refreshPreview()
            }
        }

        // Show relevant section
        noAttachment.visibility = View.GONE
        sectionBase.visibility = View.GONE
        sectionKeyring.visibility = View.GONE
        sectionHook.visibility = View.GONE
        toolbar.visibility = View.GONE

        when (vm.attachmentType) {
            AttachmentType.NONE -> {
                noAttachment.visibility = View.VISIBLE
                previewContainer.visibility = View.GONE
            }
            AttachmentType.BASE -> { sectionBase.visibility = View.VISIBLE; setupBase(view) }
            AttachmentType.KEYRING_LOOP -> {
                sectionKeyring.visibility = View.VISIBLE; toolbar.visibility = View.VISIBLE; setupKeyring(view)
            }
            AttachmentType.WALL_HOOK -> {
                sectionHook.visibility = View.VISIBLE; toolbar.visibility = View.VISIBLE; setupHook(view)
            }
        }

        // Snap toggle
        view.findViewById<CheckBox>(R.id.chk_snap).setOnCheckedChangeListener { _, checked ->
            vm.placementController.snapEnabled = checked
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

    private fun refreshPreview() {
        val model = vm.model.value ?: return
        val placement = vm.placement

        val merged = when (vm.attachmentType) {
            AttachmentType.BASE -> {
                val base = BaseGenerator().generate(model, vm.baseConfig, vm.scaleFactor * 100f)
                GeometryMerger().merge(model, base)
            }
            AttachmentType.KEYRING_LOOP -> {
                if (placement == null) model
                else GeometryMerger().merge(model, LoopGenerator().generate(model, vm.keyringConfig, placement, vm.scaleFactor * 100f))
            }
            AttachmentType.WALL_HOOK -> {
                if (placement == null) model
                else GeometryMerger().merge(model, HookGenerator().generate(model, vm.hookConfig, placement, vm.scaleFactor * 100f))
            }
            AttachmentType.NONE -> model
        }
        previewView?.setModel(merged)
    }

    private fun setupBase(view: View) {
        val radioShape = view.findViewById<RadioGroup>(R.id.radio_base_shape)
        val sliderHeight = view.findViewById<SeekBar>(R.id.slider_base_height)
        val sliderMargin = view.findViewById<SeekBar>(R.id.slider_base_margin)
        val sliderOverlap = view.findViewById<SeekBar>(R.id.slider_base_overlap)

        radioShape.check(when (vm.baseConfig.shape) {
            BaseShape.CIRCULAR -> R.id.radio_circular
            BaseShape.RECTANGULAR -> R.id.radio_rectangular
            BaseShape.CUSTOM -> R.id.radio_custom
        })

        sliderHeight.progress = vm.baseConfig.height.toInt()
        sliderMargin.progress = vm.baseConfig.margin.toInt()
        sliderOverlap.progress = vm.baseConfig.overlap.toInt()

        // Auto-populate dimensions from model
        val model = vm.model.value
        if (model != null && vm.baseConfig.width == 0f) {
            val bb = BaseGenerator.boundingBox(model)
            val size = bb.second - bb.first
            vm.baseConfig = vm.baseConfig.copy(width = size.x + vm.baseConfig.margin * 2, depth = size.z + vm.baseConfig.margin * 2)
        }

        radioShape.setOnCheckedChangeListener { _, id ->
            vm.baseConfig = vm.baseConfig.copy(shape = when (id) {
                R.id.radio_rectangular -> BaseShape.RECTANGULAR
                R.id.radio_custom -> BaseShape.CUSTOM
                else -> BaseShape.CIRCULAR
            })
            refreshPreview()
        }

        sliderHeight.setOnSeekBarChangeListener(simpleSeekListener {
            vm.baseConfig = vm.baseConfig.copy(height = it.toFloat())
            refreshPreview()
        })
        sliderMargin.setOnSeekBarChangeListener(simpleSeekListener {
            vm.baseConfig = vm.baseConfig.copy(margin = it.toFloat())
            refreshPreview()
        })
        sliderOverlap.setOnSeekBarChangeListener(simpleSeekListener {
            vm.baseConfig = vm.baseConfig.copy(overlap = it.toFloat())
            refreshPreview()
        })

        refreshPreview()
    }

    private fun setupKeyring(view: View) {
        val radioSize = view.findViewById<RadioGroup>(R.id.radio_loop_size)
        val txtValidation = view.findViewById<TextView>(R.id.txt_keyring_validation)
        val sliderThickness = view.findViewById<SeekBar>(R.id.slider_ring_thickness)

        radioSize.check(when (vm.keyringConfig.size) {
            LoopSize.SMALL -> R.id.radio_small
            LoopSize.MEDIUM -> R.id.radio_medium
            LoopSize.LARGE -> R.id.radio_large
        })

        sliderThickness.progress = vm.keyringConfig.thickness.toInt()

        radioSize.setOnCheckedChangeListener { _, id ->
            vm.keyringConfig = vm.keyringConfig.copy(size = when (id) {
                R.id.radio_small -> LoopSize.SMALL
                R.id.radio_large -> LoopSize.LARGE
                else -> LoopSize.MEDIUM
            })
            refreshPreview()
        }

        sliderThickness.setOnSeekBarChangeListener(simpleSeekListener {
            vm.keyringConfig = vm.keyringConfig.copy(thickness = it.toFloat())
            refreshPreview()
        })

        fun setPos(pos: LoopPosition) {
            vm.keyringConfig = vm.keyringConfig.copy(position = pos)
            val model = vm.model.value ?: return
            val bb = BaseGenerator.boundingBox(model)
            val center = (bb.first + bb.second) / 2f
            val placement = when (pos) {
                LoopPosition.TOP -> PlacementResult(Vector3(center.x, bb.second.y, center.z), Vector3(0f, 1f, 0f))
                LoopPosition.LEFT -> PlacementResult(Vector3(bb.first.x, center.y, center.z), Vector3(-1f, 0f, 0f))
                LoopPosition.RIGHT -> PlacementResult(Vector3(bb.second.x, center.y, center.z), Vector3(1f, 0f, 0f))
                LoopPosition.FRONT -> PlacementResult(Vector3(center.x, center.y, bb.second.z), Vector3(0f, 0f, 1f))
                LoopPosition.BACK -> PlacementResult(Vector3(center.x, center.y, bb.first.z), Vector3(0f, 0f, -1f))
            }
            vm.placement = placement

            val validator = com.claymodeler.export.placement.PlacementValidator()
            val result = validator.validate(model, placement, AttachmentType.KEYRING_LOOP)
            if (!result.valid) {
                txtValidation.text = "⚠ ${result.reason}"
                txtValidation.setTextColor(resources.getColor(R.color.accent, null))
                txtValidation.visibility = View.VISIBLE
                view?.performHapticFeedback(HapticFeedbackConstants.REJECT)
            } else {
                txtValidation.text = "✓ Valid placement"
                txtValidation.setTextColor(resources.getColor(R.color.primary, null))
                txtValidation.visibility = View.VISIBLE
                view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            }
            refreshPreview()
        }

        view.findViewById<Button>(R.id.btn_pos_top).setOnClickListener { setPos(LoopPosition.TOP) }
        view.findViewById<Button>(R.id.btn_pos_left).setOnClickListener { setPos(LoopPosition.LEFT) }
        view.findViewById<Button>(R.id.btn_pos_right).setOnClickListener { setPos(LoopPosition.RIGHT) }
        view.findViewById<Button>(R.id.btn_pos_front).setOnClickListener { setPos(LoopPosition.FRONT) }
        view.findViewById<Button>(R.id.btn_pos_back).setOnClickListener { setPos(LoopPosition.BACK) }
    }

    private fun setupHook(view: View) {
        val radioType = view.findViewById<RadioGroup>(R.id.radio_hook_type)
        val txtValidation = view.findViewById<TextView>(R.id.txt_hook_validation)

        radioType.check(when (vm.hookConfig.type) {
            HookType.KEYHOLE -> R.id.radio_keyhole
            HookType.MOUNTING_HOLES -> R.id.radio_holes
            HookType.HANGING_LOOP -> R.id.radio_hanging
        })

        radioType.setOnCheckedChangeListener { _, id ->
            vm.hookConfig = vm.hookConfig.copy(type = when (id) {
                R.id.radio_holes -> HookType.MOUNTING_HOLES
                R.id.radio_hanging -> HookType.HANGING_LOOP
                else -> HookType.KEYHOLE
            })
            refreshPreview()
        }

        fun setHookPos(pos: HookPosition) {
            vm.hookConfig = vm.hookConfig.copy(position = pos)
            val model = vm.model.value ?: return
            val hookGen = HookGenerator()
            val placement = hookGen.autoPlacement(model, vm.hookConfig)
            vm.placement = placement

            val validator = com.claymodeler.export.placement.PlacementValidator()
            val result = validator.validate(model, placement, AttachmentType.WALL_HOOK)
            if (!result.valid) {
                txtValidation.text = "⚠ ${result.reason}"
                txtValidation.setTextColor(resources.getColor(R.color.accent, null))
                view?.performHapticFeedback(HapticFeedbackConstants.REJECT)
            } else {
                txtValidation.text = "✓ Valid placement"
                txtValidation.setTextColor(resources.getColor(R.color.primary, null))
                view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            }
            txtValidation.visibility = View.VISIBLE
            refreshPreview()
        }

        view.findViewById<Button>(R.id.btn_hook_auto).setOnClickListener { setHookPos(HookPosition.AUTO) }
        view.findViewById<Button>(R.id.btn_hook_top).setOnClickListener { setHookPos(HookPosition.TOP_CENTER) }
        view.findViewById<Button>(R.id.btn_hook_center).setOnClickListener { setHookPos(HookPosition.CENTER) }
        view.findViewById<Button>(R.id.btn_hook_bottom).setOnClickListener { setHookPos(HookPosition.BOTTOM_CENTER) }
    }

    private fun simpleSeekListener(onChange: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) { onChange(progress) }
        override fun onStartTrackingTouch(sb: SeekBar?) {}
        override fun onStopTrackingTouch(sb: SeekBar?) {}
    }
}
