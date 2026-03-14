package com.claymodeler.ui.wizard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.claymodeler.export.AttachmentType
import com.claymodeler.export.BaseConfig
import com.claymodeler.export.ExportConfiguration
import com.claymodeler.export.HookConfig
import com.claymodeler.export.KeyringConfig
import com.claymodeler.export.PlacementResult
import com.claymodeler.export.placement.PlacementController
import com.claymodeler.model.ClayModel

class ExportWizardViewModel : ViewModel() {

    private val _currentStep = MutableLiveData(0)
    val currentStep: LiveData<Int> = _currentStep

    private val _model = MutableLiveData<ClayModel>()
    val model: LiveData<ClayModel> = _model

    var attachmentType = AttachmentType.NONE
    var scaleFactor = 1f
    var baseConfig = BaseConfig()
    var keyringConfig = KeyringConfig()
    var hookConfig = HookConfig()
    var placement: PlacementResult? = null
    var fileName = ""
    var saveAsPreset = false
    var presetName = ""

    val placementController = PlacementController()

    val totalSteps = 5

    /** The original unrotated model */
    private var originalModel: ClayModel? = null

    fun setModel(model: ClayModel) {
        originalModel = model.clone()
        _model.value = model
    }

    /** Rotate all vertices 90° around the given axis and recalculate normals */
    fun rotateModel90(axis: Char) {
        val src = _model.value ?: return
        val model = src.clone()
        for (i in model.vertices.indices) {
            val v = model.vertices[i]
            model.vertices[i] = when (axis) {
                'x' -> com.claymodeler.model.Vector3(v.x, -v.z, v.y)
                'y' -> com.claymodeler.model.Vector3(v.z, v.y, -v.x)
                'z' -> com.claymodeler.model.Vector3(-v.y, v.x, v.z)
                else -> v
            }
        }
        model.calculateNormals()
        _model.value = model
    }

    /** Rotate model by arbitrary angles (radians) around X and Y axes */
    fun rotateModelSmooth(angleX: Float, angleY: Float) {
        val src = _model.value ?: return
        val model = src.clone()
        val cosX = kotlin.math.cos(angleX); val sinX = kotlin.math.sin(angleX)
        val cosY = kotlin.math.cos(angleY); val sinY = kotlin.math.sin(angleY)
        for (i in model.vertices.indices) {
            var v = model.vertices[i]
            v = com.claymodeler.model.Vector3(v.x, v.y * cosX - v.z * sinX, v.y * sinX + v.z * cosX)
            v = com.claymodeler.model.Vector3(v.x * cosY + v.z * sinY, v.y, -v.x * sinY + v.z * cosY)
            model.vertices[i] = v
        }
        model.calculateNormals()
        _model.value = model
    }

    /** Bake a 4x4 model matrix rotation into the actual vertex positions */
    fun bakeModelMatrix(matrix: FloatArray) {
        val src = _model.value ?: return
        val model = src.clone()
        for (i in model.vertices.indices) {
            val v = model.vertices[i]
            val inp = floatArrayOf(v.x, v.y, v.z, 1f)
            val out = FloatArray(4)
            android.opengl.Matrix.multiplyMV(out, 0, matrix, 0, inp, 0)
            model.vertices[i] = com.claymodeler.model.Vector3(out[0], out[1], out[2])
        }
        model.calculateNormals()
        _model.value = model
    }

    fun resetModelOrientation() {
        val orig = originalModel ?: return
        val model = orig.clone()
        _model.value = model
    }

    fun goNext() {
        val step = (_currentStep.value ?: 0)
        if (step < totalSteps - 1) _currentStep.value = step + 1
    }

    fun goBack() {
        val step = (_currentStep.value ?: 0)
        if (step > 0) _currentStep.value = step - 1
    }

    fun goToStep(step: Int) {
        if (step in 0 until totalSteps) _currentStep.value = step
    }

    fun buildConfiguration() = ExportConfiguration(
        attachmentType = attachmentType,
        scaleFactor = scaleFactor,
        sizeInMm = 100f * scaleFactor,
        baseConfig = baseConfig,
        keyringConfig = keyringConfig,
        hookConfig = hookConfig,
        placement = placement,
        presetName = if (saveAsPreset) presetName else null
    )
}
