package com.claymodeler.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.shouldBeWithinPercentageOf
import io.kotest.matchers.shouldBe

class Vector3Test : FunSpec({
    
    test("addition works correctly") {
        val v1 = Vector3(1f, 2f, 3f)
        val v2 = Vector3(4f, 5f, 6f)
        val result = v1 + v2
        
        result.x shouldBe 5f
        result.y shouldBe 7f
        result.z shouldBe 9f
    }
    
    test("subtraction works correctly") {
        val v1 = Vector3(5f, 7f, 9f)
        val v2 = Vector3(1f, 2f, 3f)
        val result = v1 - v2
        
        result.x shouldBe 4f
        result.y shouldBe 5f
        result.z shouldBe 6f
    }
    
    test("scalar multiplication works correctly") {
        val v = Vector3(1f, 2f, 3f)
        val result = v * 2f
        
        result.x shouldBe 2f
        result.y shouldBe 4f
        result.z shouldBe 6f
    }
    
    test("scalar division works correctly") {
        val v = Vector3(2f, 4f, 6f)
        val result = v / 2f
        
        result.x shouldBe 1f
        result.y shouldBe 2f
        result.z shouldBe 3f
    }
    
    test("dot product works correctly") {
        val v1 = Vector3(1f, 2f, 3f)
        val v2 = Vector3(4f, 5f, 6f)
        val result = v1.dot(v2)
        
        result shouldBe 32f // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
    }
    
    test("cross product works correctly") {
        val v1 = Vector3(1f, 0f, 0f)
        val v2 = Vector3(0f, 1f, 0f)
        val result = v1.cross(v2)
        
        result.x shouldBe 0f
        result.y shouldBe 0f
        result.z shouldBe 1f
    }
    
    test("length calculation works correctly") {
        val v = Vector3(3f, 4f, 0f)
        val length = v.length()
        
        length shouldBe 5f // sqrt(9 + 16) = 5
    }
    
    test("normalize creates unit vector") {
        val v = Vector3(3f, 4f, 0f)
        val normalized = v.normalize()
        
        normalized.length().shouldBeWithinPercentageOf(1f, 0.01)
    }
    
    test("normalize preserves direction") {
        val v = Vector3(3f, 4f, 0f)
        val normalized = v.normalize()
        
        normalized.x.shouldBeWithinPercentageOf(0.6f, 0.01)
        normalized.y.shouldBeWithinPercentageOf(0.8f, 0.01)
        normalized.z shouldBe 0f
    }
})
