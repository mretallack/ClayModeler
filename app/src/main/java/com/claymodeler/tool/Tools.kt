package com.claymodeler.tool

import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3

class RemoveClayTool : Tool {
    override fun apply(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float, dragDirection: Vector3) {
        val affectedVertices = mutableSetOf<Int>()
        
        for (i in model.vertices.indices) {
            val vertex = model.vertices[i]
            val distance = (vertex - hitPoint).length()
            
            if (distance < radius && distance > 0.001f) {
                // Linear falloff
                val normalizedDist = distance / radius
                val falloff = 1f - normalizedDist
                
                val direction = (hitPoint - vertex).normalize()
                val offset = direction * (strength * falloff * 0.02f)
                model.vertices[i] = vertex + offset
                affectedVertices.add(i)
            }
        }
        
        model.recalculateNormalsForVertices(affectedVertices)
    }
    
    override fun getName() = "Remove"
}

class AddClayTool : Tool {
    override fun apply(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float, dragDirection: Vector3) {
        val originalNormals = model.normals.toList()
        val affectedVertices = mutableSetOf<Int>()
        
        // Check if drag direction is significant
        val dragLength = dragDirection.length()
        val useDrag = dragLength > 0.01f
        
        for (i in model.vertices.indices) {
            val vertex = model.vertices[i]
            val distance = (vertex - hitPoint).length()
            
            if (distance < radius) {
                val normalizedDist = distance / radius
                val falloff = 1f - normalizedDist
                
                // Use drag direction if available, otherwise use surface normal
                val direction = if (useDrag) {
                    dragDirection.normalize()
                } else {
                    originalNormals[i]
                }
                
                val offset = direction * (strength * falloff * 0.1f)
                model.vertices[i] = vertex + offset
                affectedVertices.add(i)
            }
        }
        
        model.recalculateNormalsForVertices(affectedVertices)
    }
    
    override fun getName() = "Add"
}

class PullClayTool : Tool {
    override fun apply(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float, dragDirection: Vector3) {
        val dragLength = dragDirection.length()
        if (dragLength < 0.001f) return // No drag, no pull
        
        val direction = dragDirection.normalize()
        val affectedVertices = mutableSetOf<Int>()
        
        for (i in model.vertices.indices) {
            val vertex = model.vertices[i]
            val distance = (vertex - hitPoint).length()
            
            if (distance < radius) {
                val normalizedDist = distance / radius
                val falloff = 1f - normalizedDist
                
                val offset = direction * (strength * falloff * 0.1f)
                model.vertices[i] = vertex + offset
                affectedVertices.add(i)
            }
        }
        
        model.recalculateNormalsForVertices(affectedVertices)
    }
    
    override fun getName() = "Pull"
}

class ViewModeTool : Tool {
    override fun apply(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float, dragDirection: Vector3) {
        // View mode doesn't modify the model
    }
    
    override fun getName() = "View"
    
    override fun isEditTool() = false
}
