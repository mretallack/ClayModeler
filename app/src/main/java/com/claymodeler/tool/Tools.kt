package com.claymodeler.tool

import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3

class RemoveClayTool : Tool {
    override fun apply(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float) {
        val affectedVertices = mutableSetOf<Int>()
        
        for (i in model.vertices.indices) {
            val vertex = model.vertices[i]
            val distance = (vertex - hitPoint).length()
            
            if (distance < radius && distance > 0.001f) {
                val normalizedDist = distance / radius
                val falloff = (1f - normalizedDist) * (1f - normalizedDist)
                
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
    override fun apply(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float) {
        val originalNormals = model.normals.toList()
        val affectedVertices = mutableSetOf<Int>()
        
        for (i in model.vertices.indices) {
            val vertex = model.vertices[i]
            val distance = (vertex - hitPoint).length()
            
            if (distance < radius) {
                val normalizedDist = distance / radius
                val falloff = (1f - normalizedDist) * (1f - normalizedDist)
                
                val normal = originalNormals[i]
                val offset = normal * (strength * falloff * 0.05f)
                model.vertices[i] = vertex + offset
                affectedVertices.add(i)
            }
        }
        
        model.recalculateNormalsForVertices(affectedVertices)
    }
    
    override fun getName() = "Add"
}

class PullClayTool : Tool {
    private var lastHitPoint: Vector3? = null
    
    override fun apply(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float) {
        val dragDirection = if (lastHitPoint != null) {
            (hitPoint - lastHitPoint!!).normalize()
        } else {
            Vector3(0f, 0f, 0f)
        }
        lastHitPoint = hitPoint
        
        val affectedVertices = mutableSetOf<Int>()
        
        for (i in model.vertices.indices) {
            val vertex = model.vertices[i]
            val distance = (vertex - hitPoint).length()
            
            if (distance < radius) {
                val normalizedDist = distance / radius
                val falloff = (1f - normalizedDist) * (1f - normalizedDist)
                
                val offset = dragDirection * (strength * falloff * 0.1f)
                model.vertices[i] = vertex + offset
                affectedVertices.add(i)
            }
        }
        
        model.recalculateNormalsForVertices(affectedVertices)
    }
    
    override fun getName() = "Pull"
}

class ViewModeTool : Tool {
    override fun apply(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float) {
        // View mode doesn't modify the model
    }
    
    override fun getName() = "View"
    
    override fun isEditTool() = false
}
