package com.claymodeler.tool

import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3

interface Tool {
    fun apply(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float, dragDirection: Vector3 = Vector3(0f, 0f, 0f))
    fun getName(): String
    fun isEditTool(): Boolean = true
    
    fun applyWithSymmetry(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float, dragDirection: Vector3 = Vector3(0f, 0f, 0f), symmetryEnabled: Boolean) {
        apply(model, hitPoint, strength, radius, dragDirection)
        
        if (symmetryEnabled) {
            val mirroredHitPoint = Vector3(-hitPoint.x, hitPoint.y, hitPoint.z)
            val mirroredDragDirection = Vector3(-dragDirection.x, dragDirection.y, dragDirection.z)
            apply(model, mirroredHitPoint, strength, radius, mirroredDragDirection)
        }
    }
}
