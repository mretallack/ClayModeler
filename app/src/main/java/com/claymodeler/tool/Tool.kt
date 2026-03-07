package com.claymodeler.tool

import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3

interface Tool {
    fun apply(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float)
    fun getName(): String
    fun isEditTool(): Boolean = true
}
