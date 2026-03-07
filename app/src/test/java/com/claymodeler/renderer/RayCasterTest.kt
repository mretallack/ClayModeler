package com.claymodeler.renderer

import com.claymodeler.model.ClayModel
import com.claymodeler.model.Vector3
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class RayCasterTest : FunSpec({
    
    test("raycast hits sphere center") {
        val rayCaster = RayCaster()
        val model = ClayModel()
        model.initialize(subdivisions = 0)
        
        val rayOrigin = Vector3(0f, 0f, 5f)
        val rayDirection = Vector3(0f, 0f, -1f)
        
        val hit = rayCaster.raycast(rayOrigin, rayDirection, model)
        
        hit shouldNotBe null
        hit!!.distance shouldNotBe 0f
    }
    
    test("raycast misses sphere") {
        val rayCaster = RayCaster()
        val model = ClayModel()
        model.initialize(subdivisions = 0)
        
        val rayOrigin = Vector3(10f, 10f, 5f)
        val rayDirection = Vector3(0f, 0f, -1f)
        
        val hit = rayCaster.raycast(rayOrigin, rayDirection, model)
        
        hit shouldBe null
    }
    
    test("octree can be created and queried") {
        val model = ClayModel()
        model.initialize(subdivisions = 2)
        
        val octree = Octree(model)
        val rayOrigin = Vector3(0f, 0f, 5f)
        val rayDirection = Vector3(0f, 0f, -1f)
        
        val candidates = octree.query(rayOrigin, rayDirection)
        
        // Octree should return some candidate faces
        candidates.size shouldNotBe 0
    }
    
    test("octree query returns candidate faces") {
        val model = ClayModel()
        model.initialize(subdivisions = 1)
        
        val octree = Octree(model)
        val rayOrigin = Vector3(0f, 0f, 5f)
        val rayDirection = Vector3(0f, 0f, -1f)
        
        val candidates = octree.query(rayOrigin, rayDirection)
        
        candidates.size shouldNotBe 0
    }
})
