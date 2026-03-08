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

class SmoothTool : Tool {
    override fun apply(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float, dragDirection: Vector3) {
        val affectedVertices = mutableSetOf<Int>()
        val newPositions = mutableMapOf<Int, Vector3>()
        
        // Find affected vertices and calculate their new positions
        for (i in model.vertices.indices) {
            val vertex = model.vertices[i]
            val distance = (vertex - hitPoint).length()
            
            if (distance < radius) {
                // Calculate average position of neighboring vertices
                val neighbors = mutableListOf<Vector3>()
                
                // Find neighbors by checking faces
                for (face in model.faces) {
                    if (face.v1 == i || face.v2 == i || face.v3 == i) {
                        if (face.v1 != i) neighbors.add(model.vertices[face.v1])
                        if (face.v2 != i) neighbors.add(model.vertices[face.v2])
                        if (face.v3 != i) neighbors.add(model.vertices[face.v3])
                    }
                }
                
                if (neighbors.isNotEmpty()) {
                    // Calculate average neighbor position
                    var avgX = 0f
                    var avgY = 0f
                    var avgZ = 0f
                    for (neighbor in neighbors) {
                        avgX += neighbor.x
                        avgY += neighbor.y
                        avgZ += neighbor.z
                    }
                    val avgPos = Vector3(
                        avgX / neighbors.size,
                        avgY / neighbors.size,
                        avgZ / neighbors.size
                    )
                    
                    // Apply falloff
                    val normalizedDist = distance / radius
                    val falloff = 1f - normalizedDist
                    
                    // Blend original position with average based on strength and falloff
                    val blendFactor = strength * falloff * 0.5f
                    val newPos = Vector3(
                        vertex.x + (avgPos.x - vertex.x) * blendFactor,
                        vertex.y + (avgPos.y - vertex.y) * blendFactor,
                        vertex.z + (avgPos.z - vertex.z) * blendFactor
                    )
                    
                    newPositions[i] = newPos
                    affectedVertices.add(i)
                }
            }
        }
        
        // Apply new positions
        for ((i, newPos) in newPositions) {
            model.vertices[i] = newPos
        }
        
        model.recalculateNormalsForVertices(affectedVertices)
    }
    
    override fun getName() = "Smooth"
}

class FlattenTool : Tool {
    override fun apply(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float, dragDirection: Vector3) {
        val affectedVertices = mutableSetOf<Int>()
        
        // Calculate average normal of affected vertices to define plane
        var avgNormalX = 0f
        var avgNormalY = 0f
        var avgNormalZ = 0f
        var count = 0
        
        for (i in model.vertices.indices) {
            val vertex = model.vertices[i]
            val distance = (vertex - hitPoint).length()
            
            if (distance < radius) {
                avgNormalX += model.normals[i].x
                avgNormalY += model.normals[i].y
                avgNormalZ += model.normals[i].z
                count++
            }
        }
        
        if (count == 0) return
        
        val planeNormal = Vector3(
            avgNormalX / count,
            avgNormalY / count,
            avgNormalZ / count
        ).normalize()
        
        // Flatten vertices toward plane
        for (i in model.vertices.indices) {
            val vertex = model.vertices[i]
            val distance = (vertex - hitPoint).length()
            
            if (distance < radius) {
                // Project vertex onto plane
                val toVertex = vertex - hitPoint
                val distToPlane = toVertex.dot(planeNormal)
                val projection = vertex - (planeNormal * distToPlane)
                
                // Apply falloff
                val normalizedDist = distance / radius
                val falloff = 1f - normalizedDist
                
                // Interpolate toward projection
                val blendFactor = strength * falloff
                model.vertices[i] = Vector3(
                    vertex.x + (projection.x - vertex.x) * blendFactor,
                    vertex.y + (projection.y - vertex.y) * blendFactor,
                    vertex.z + (projection.z - vertex.z) * blendFactor
                )
                affectedVertices.add(i)
            }
        }
        
        model.recalculateNormalsForVertices(affectedVertices)
    }
    
    override fun getName() = "Flatten"
}

class PinchTool : Tool {
    override fun apply(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float, dragDirection: Vector3) {
        val affectedVertices = mutableSetOf<Int>()
        
        for (i in model.vertices.indices) {
            val vertex = model.vertices[i]
            val distance = (vertex - hitPoint).length()
            
            if (distance < radius && distance > 0.001f) {
                // Quadratic falloff for sharp concentration
                val normalizedDist = distance / radius
                val falloff = (1f - normalizedDist * normalizedDist) * (1f - normalizedDist * normalizedDist)
                
                // Pull toward hit point
                val direction = (hitPoint - vertex).normalize()
                val offset = direction * (strength * falloff * 0.15f)
                model.vertices[i] = vertex + offset
                affectedVertices.add(i)
            }
        }
        
        model.recalculateNormalsForVertices(affectedVertices)
    }
    
    override fun getName() = "Pinch"
}

class InflateTool : Tool {
    override fun apply(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float, dragDirection: Vector3) {
        val originalNormals = model.normals.toList()
        val affectedVertices = mutableSetOf<Int>()
        
        for (i in model.vertices.indices) {
            val vertex = model.vertices[i]
            val distance = (vertex - hitPoint).length()
            
            if (distance < radius) {
                val normalizedDist = distance / radius
                val falloff = 1f - normalizedDist
                
                // Push along normal (ignore drag direction)
                val normal = originalNormals[i]
                val offset = normal * (strength * falloff * 0.1f)
                model.vertices[i] = vertex + offset
                affectedVertices.add(i)
            }
        }
        
        model.recalculateNormalsForVertices(affectedVertices)
    }
    
    override fun getName() = "Inflate"
}
