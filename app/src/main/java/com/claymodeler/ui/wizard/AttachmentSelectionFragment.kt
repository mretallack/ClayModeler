package com.claymodeler.ui.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.claymodeler.R
import com.claymodeler.export.AttachmentType
import com.claymodeler.export.PresetManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class AttachmentSelectionFragment : Fragment() {

    private lateinit var vm: ExportWizardViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_attachment_selection, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm = ViewModelProvider(requireActivity())[ExportWizardViewModel::class.java]

        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_attachment)
        val chipGroup = view.findViewById<ChipGroup>(R.id.preset_chips)

        // Load presets
        val presetManager = PresetManager(requireContext())
        for (name in presetManager.listPresets()) {
            val chip = Chip(requireContext()).apply {
                text = name
                isCheckable = true
                setOnClickListener {
                    val config = presetManager.load(name) ?: return@setOnClickListener
                    vm.attachmentType = config.attachmentType
                    vm.baseConfig = config.baseConfig
                    vm.keyringConfig = config.keyringConfig
                    vm.hookConfig = config.hookConfig
                    syncRadio(radioGroup)
                }
            }
            chipGroup.addView(chip)
        }

        // Set current selection
        syncRadio(radioGroup)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            vm.attachmentType = when (checkedId) {
                R.id.radio_base -> AttachmentType.BASE
                R.id.radio_keyring -> AttachmentType.KEYRING_LOOP
                R.id.radio_hook -> AttachmentType.WALL_HOOK
                else -> AttachmentType.NONE
            }
            // Validate combinations
            val warning = view.findViewById<TextView>(R.id.txt_combination_warning)
            warning.visibility = View.GONE
        }
    }

    private fun syncRadio(radioGroup: RadioGroup) {
        radioGroup.check(when (vm.attachmentType) {
            AttachmentType.NONE -> R.id.radio_none
            AttachmentType.BASE -> R.id.radio_base
            AttachmentType.KEYRING_LOOP -> R.id.radio_keyring
            AttachmentType.WALL_HOOK -> R.id.radio_hook
        })
    }
}
