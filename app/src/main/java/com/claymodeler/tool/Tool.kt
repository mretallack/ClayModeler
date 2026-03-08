package com.claymodeler.tool

import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3

interface Tool {
    fun apply(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float, dragDirection: Vector3 = Vector3(0f, 0f, 0f))
    fun getName(): String
    fun isEditTool(): Boolean = true
}
