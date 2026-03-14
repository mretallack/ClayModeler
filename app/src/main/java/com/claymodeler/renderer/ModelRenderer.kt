package com.claymodeler.renderer

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.claymodeler.R
import com.claymodeler.model.ClayModel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ModelRenderer(private val context: Context) : GLSurfaceView.Renderer {
    
    private var shaderProgram = 0
    private var vbo = 0
    private var normalVbo = 0
    private var ebo = 0
    private var glReady = false
    
    private val mvpMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val normalMatrix = FloatArray(16)
    private var aspectRatio = 1f
    
    val camera = Camera()
    private val rayCaster = RayCaster()
    private var octree: Octree? = null
    
    private var model: ClayModel? = null
    private var vertexCount = 0
    
    // FPS tracking
    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    private var currentFps = 0f
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Set clear color to black
        GLES30.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        
        // Enable depth testing
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LESS)
        
        // Disable face culling to show cross-sections when camera is inside model
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        
        // Create shader program
        shaderProgram = createShaderProgram()
        
        // Initialize matrices
        Matrix.setIdentityM(modelMatrix, 0)
        
        glReady = true
        
        // Upload initial model if set
        if (model != null) {
            uploadModelToGPU()
        }
        
        checkGLError("onSurfaceCreated")
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        
        aspectRatio = width.toFloat() / height.toFloat()
        
        // Fixed projection matrix with wide range for large models
        Matrix.frustumM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, 1f, 50f)
        
        checkGLError("onSurfaceChanged")
    }
    
    var showGroundGrid = false
    private var gridVbo = 0
    private var gridNormalVbo = 0
    private var gridVertexCount = 0
    private var gridY = -1f
    var showLightIndicator = false
    private var lightIndicatorVbo = 0
    private var lightIndicatorNormalVbo = 0
    private var lightIndicatorVertexCount = 0

    override fun onDrawFrame(gl: GL10?) {
        // Clear buffers
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        
        // Update FPS
        frameCount++
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFpsTime >= 1000) {
            currentFps = frameCount * 1000f / (currentTime - lastFpsTime)
            frameCount = 0
            lastFpsTime = currentTime
            android.util.Log.d("ModelRenderer", "FPS: $currentFps")
        }
        
        if (model == null || vertexCount == 0) {
            return
        }
        
        // Use shader program
        GLES30.glUseProgram(shaderProgram)
        
        // Get view matrix from camera
        val viewMatrix = camera.getViewMatrix()
        
        // Calculate MVP matrix
        val tempMatrix = FloatArray(16)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
        
        // Calculate normal matrix (inverse transpose of model matrix)
        Matrix.invertM(normalMatrix, 0, modelMatrix, 0)
        Matrix.transposeM(normalMatrix, 0, normalMatrix, 0)
        
        // Set uniforms
        val mvpMatrixHandle = GLES30.glGetUniformLocation(shaderProgram, "uMVPMatrix")
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        
        val modelMatrixHandle = GLES30.glGetUniformLocation(shaderProgram, "uModelMatrix")
        GLES30.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0)
        
        val normalMatrixHandle = GLES30.glGetUniformLocation(shaderProgram, "uNormalMatrix")
        GLES30.glUniformMatrix4fv(normalMatrixHandle, 1, false, normalMatrix, 0)
        
        val lightPosHandle = GLES30.glGetUniformLocation(shaderProgram, "uLightPos")
        val lightPos = model?.lightPosition ?: com.claymodeler.model.Vector3(2f, 3f, 2f)
        GLES30.glUniform3f(lightPosHandle, lightPos.x, lightPos.y, lightPos.z)
        
        val lightIntensityHandle = GLES30.glGetUniformLocation(shaderProgram, "uLightIntensity")
        val lightIntensity = model?.lightIntensity ?: 1f
        GLES30.glUniform1f(lightIntensityHandle, lightIntensity)
        
        val viewPosHandle = GLES30.glGetUniformLocation(shaderProgram, "uViewPos")
        GLES30.glUniform3f(viewPosHandle, 0f, 0f, 4f)
        
        val clayColorHandle = GLES30.glGetUniformLocation(shaderProgram, "uClayColor")
        GLES30.glUniform3f(clayColorHandle, 0.82f, 0.41f, 0.12f) // Terracotta
        
        // Bind VBOs
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, 0)
        
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, normalVbo)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 0, 0)
        
        // Bind EBO and draw
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, vertexCount, GLES30.GL_UNSIGNED_INT, 0)
        
        // Cleanup
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)

        // Draw grid with identity model matrix (stays fixed as ground reference)
        if (showGroundGrid && gridVbo != 0 && gridVertexCount > 0) {
            val identityModel = FloatArray(16)
            Matrix.setIdentityM(identityModel, 0)
            val gridMvp = FloatArray(16)
            val gridTemp = FloatArray(16)
            Matrix.multiplyMM(gridTemp, 0, camera.getViewMatrix(), 0, identityModel, 0)
            Matrix.multiplyMM(gridMvp, 0, projectionMatrix, 0, gridTemp, 0)

            val mvpH = GLES30.glGetUniformLocation(shaderProgram, "uMVPMatrix")
            GLES30.glUniformMatrix4fv(mvpH, 1, false, gridMvp, 0)
            val modelH = GLES30.glGetUniformLocation(shaderProgram, "uModelMatrix")
            GLES30.glUniformMatrix4fv(modelH, 1, false, identityModel, 0)
            val colorH = GLES30.glGetUniformLocation(shaderProgram, "uClayColor")
            GLES30.glUniform3f(colorH, 0.45f, 0.45f, 0.45f)

            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, gridVbo)
            GLES30.glEnableVertexAttribArray(0)
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, 0)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, gridNormalVbo)
            GLES30.glEnableVertexAttribArray(1)
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 0, 0)

            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, gridVertexCount)

            GLES30.glDisableVertexAttribArray(0)
            GLES30.glDisableVertexAttribArray(1)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
            GLES30.glGetError() // clear any grid errors
        }

        // Draw light indicator
        if (showLightIndicator) {
            if (lightIndicatorVbo == 0) buildLightIndicator()
            val lightPos = model?.lightPosition ?: com.claymodeler.model.Vector3(2f, 3f, 2f)
            val lightModel = FloatArray(16)
            Matrix.setIdentityM(lightModel, 0)
            Matrix.translateM(lightModel, 0, lightPos.x, lightPos.y, lightPos.z)
            Matrix.scaleM(lightModel, 0, 0.15f, 0.15f, 0.15f)
            val lightMvp = FloatArray(16)
            val lightTemp = FloatArray(16)
            Matrix.multiplyMM(lightTemp, 0, camera.getViewMatrix(), 0, lightModel, 0)
            Matrix.multiplyMM(lightMvp, 0, projectionMatrix, 0, lightTemp, 0)

            val mvpH = GLES30.glGetUniformLocation(shaderProgram, "uMVPMatrix")
            GLES30.glUniformMatrix4fv(mvpH, 1, false, lightMvp, 0)
            val modelH = GLES30.glGetUniformLocation(shaderProgram, "uModelMatrix")
            GLES30.glUniformMatrix4fv(modelH, 1, false, lightModel, 0)
            val colorH = GLES30.glGetUniformLocation(shaderProgram, "uClayColor")
            GLES30.glUniform3f(colorH, 1f, 0.9f, 0.3f) // yellow
            val lightIntH = GLES30.glGetUniformLocation(shaderProgram, "uLightIntensity")
            GLES30.glUniform1f(lightIntH, 3f) // bright so it's always visible

            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, lightIndicatorVbo)
            GLES30.glEnableVertexAttribArray(0)
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, 0)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, lightIndicatorNormalVbo)
            GLES30.glEnableVertexAttribArray(1)
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 0, 0)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, lightIndicatorVertexCount)
            GLES30.glDisableVertexAttribArray(0)
            GLES30.glDisableVertexAttribArray(1)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)

            // Restore model lighting
            GLES30.glUniform1f(lightIntH, model?.lightIntensity ?: 1f)
            GLES30.glGetError()
        }
        
        checkGLError("onDrawFrame")
    }
    
    fun setModel(newModel: ClayModel) {
        model = newModel
        octree = Octree(newModel)
        if (showGroundGrid) buildGridData(newModel)
        uploadModelToGPU()
    }
    
    fun updateModel() {
        uploadModelToGPU()
    }
    
    fun performRaycast(screenX: Float, screenY: Float, screenWidth: Int, screenHeight: Int): RayHit? {
        val m = model ?: return null
        val (rayOrigin, rayDirection) = rayCaster.screenToWorldRay(
            screenX, screenY, screenWidth, screenHeight,
            camera.getViewMatrix(), projectionMatrix
        )
        return rayCaster.raycast(rayOrigin, rayDirection, m, octree)
    }
    
    fun getFps(): Float = currentFps
    
    /** Rotate the model matrix (instant, no vertex recalculation) */
    fun rotateModelMatrix(deltaX: Float, deltaY: Float) {
        val temp = FloatArray(16)
        Matrix.setIdentityM(temp, 0)
        Matrix.rotateM(temp, 0, deltaX * 0.5f, 0f, 1f, 0f) // horizontal = Y axis
        Matrix.rotateM(temp, 0, deltaY * 0.5f, 1f, 0f, 0f) // vertical = X axis
        val result = FloatArray(16)
        Matrix.multiplyMM(result, 0, temp, 0, modelMatrix, 0)
        System.arraycopy(result, 0, modelMatrix, 0, 16)
    }

    /** Get current model matrix rotation angles for baking into vertices */
    fun getModelMatrix(): FloatArray = modelMatrix.copyOf()

    /** Reset model matrix to identity */
    fun resetModelMatrix() {
        Matrix.setIdentityM(modelMatrix, 0)
    }

    fun rotateCamera(deltaX: Float, deltaY: Float) {
        camera.rotate(deltaX, deltaY)
    }
    
    fun zoomCamera(scaleFactor: Float) {
        camera.zoom(scaleFactor)
    }
    
    fun panCamera(deltaX: Float, deltaY: Float) {
        camera.pan(deltaX, deltaY)
    }
    
    fun resetCamera() {
        camera.reset()
    }
    
    private fun buildLightIndicator() {
        val stacks = 8; val slices = 8
        val verts = mutableListOf<Float>()
        val norms = mutableListOf<Float>()
        for (i in 0 until stacks) {
            val phi1 = (Math.PI * i / stacks).toFloat()
            val phi2 = (Math.PI * (i + 1) / stacks).toFloat()
            for (j in 0 until slices) {
                val th1 = (2 * Math.PI * j / slices).toFloat()
                val th2 = (2 * Math.PI * (j + 1) / slices).toFloat()
                fun v(p: Float, t: Float) {
                    val x = kotlin.math.sin(p) * kotlin.math.cos(t)
                    val y = kotlin.math.cos(p)
                    val z = kotlin.math.sin(p) * kotlin.math.sin(t)
                    verts.addAll(listOf(x, y, z)); norms.addAll(listOf(x, y, z))
                }
                v(phi1, th1); v(phi2, th1); v(phi2, th2)
                v(phi1, th1); v(phi2, th2); v(phi1, th2)
            }
        }
        lightIndicatorVertexCount = verts.size / 3
        val vBuf = java.nio.ByteBuffer.allocateDirect(verts.size * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
        vBuf.put(verts.toFloatArray()); vBuf.position(0)
        val nBuf = java.nio.ByteBuffer.allocateDirect(norms.size * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
        nBuf.put(norms.toFloatArray()); nBuf.position(0)
        val b = IntArray(2); GLES30.glGenBuffers(2, b, 0)
        lightIndicatorVbo = b[0]; lightIndicatorNormalVbo = b[1]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, lightIndicatorVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, verts.size * 4, vBuf, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, lightIndicatorNormalVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, norms.size * 4, nBuf, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    private fun buildGridData(m: ClayModel) {
        var minY = Float.MAX_VALUE
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        for (v in m.vertices) {
            if (v.y < minY) minY = v.y
            if (v.x < minX) minX = v.x; if (v.x > maxX) maxX = v.x
            if (v.z < minZ) minZ = v.z; if (v.z > maxZ) maxZ = v.z
        }
        val extent = maxOf(maxX - minX, maxZ - minZ) * 1.5f
        val cx = (minX + maxX) / 2f
        val cz = (minZ + maxZ) / 2f
        val lines = 11
        val step = extent / (lines - 1)
        val half = extent / 2f
        val t = extent * 0.005f
        val y = minY - 0.002f

        val verts = mutableListOf<Float>()
        val norms = mutableListOf<Float>()
        for (i in 0 until lines) {
            val offset = -half + i * step
            // Along X
            verts.addAll(listOf(cx - half, y, cz + offset - t, cx + half, y, cz + offset - t, cx + half, y, cz + offset + t))
            verts.addAll(listOf(cx - half, y, cz + offset - t, cx + half, y, cz + offset + t, cx - half, y, cz + offset + t))
            // Along Z
            verts.addAll(listOf(cx + offset - t, y, cz - half, cx + offset + t, y, cz - half, cx + offset + t, y, cz + half))
            verts.addAll(listOf(cx + offset - t, y, cz - half, cx + offset + t, y, cz + half, cx + offset - t, y, cz + half))
            repeat(12) { norms.addAll(listOf(0f, 1f, 0f)) }
        }
        gridVertexCount = verts.size / 3

        val vertBuf = java.nio.ByteBuffer.allocateDirect(verts.size * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
        vertBuf.put(verts.toFloatArray()); vertBuf.position(0)
        val normBuf = java.nio.ByteBuffer.allocateDirect(norms.size * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
        normBuf.put(norms.toFloatArray()); normBuf.position(0)

        if (gridVbo == 0) {
            val b = IntArray(2); GLES30.glGenBuffers(2, b, 0); gridVbo = b[0]; gridNormalVbo = b[1]
        }
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, gridVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, verts.size * 4, vertBuf, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, gridNormalVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, norms.size * 4, normBuf, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    private fun uploadGrid(m: ClayModel) {
        if (!glReady) return
        var minY = Float.MAX_VALUE
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        for (v in m.vertices) {
            if (v.y < minY) minY = v.y
            if (v.x < minX) minX = v.x; if (v.x > maxX) maxX = v.x
            if (v.z < minZ) minZ = v.z; if (v.z > maxZ) maxZ = v.z
        }
        gridY = minY
        val extent = maxOf(maxX - minX, maxZ - minZ) * 1.5f
        val cx = (minX + maxX) / 2f
        val cz = (minZ + maxZ) / 2f
        val lines = 11
        val step = extent / (lines - 1)
        val half = extent / 2f
        val thickness = extent * 0.004f // thin but visible

        // Build grid as flat quads (2 triangles each)
        val verts = mutableListOf<Float>()
        val norms = mutableListOf<Float>()
        fun addQuad(x1: Float, z1: Float, x2: Float, z2: Float, dx: Float, dz: Float) {
            // Two triangles forming a thin quad
            val y = minY - 0.001f // slightly below model to avoid z-fighting
            verts.addAll(listOf(x1 - dz, y, z1 + dx,  x2 - dz, y, z2 + dx,  x2 + dz, y, z2 - dx))
            verts.addAll(listOf(x1 - dz, y, z1 + dx,  x2 + dz, y, z2 - dx,  x1 + dz, y, z1 - dx))
            repeat(6) { norms.addAll(listOf(0f, 1f, 0f)) }
        }

        for (i in 0 until lines) {
            val offset = -half + i * step
            // Line along X (thickness in Z)
            addQuad(cx - half, cz + offset, cx + half, cz + offset, 0f, thickness)
            // Line along Z (thickness in X)
            addQuad(cx + offset, cz - half, cx + offset, cz + half, thickness, 0f)
        }

        gridVertexCount = verts.size / 3

        val vertBuf = java.nio.ByteBuffer.allocateDirect(verts.size * 4)
            .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
        vertBuf.put(verts.toFloatArray()); vertBuf.position(0)

        val normBuf = java.nio.ByteBuffer.allocateDirect(norms.size * 4)
            .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
        normBuf.put(norms.toFloatArray()); normBuf.position(0)

        if (gridVbo == 0) {
            val b = IntArray(2); GLES30.glGenBuffers(2, b, 0)
            gridVbo = b[0]; gridNormalVbo = b[1]
        }
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, gridVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, verts.size * 4, vertBuf, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, gridNormalVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, norms.size * 4, normBuf, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    private fun uploadModelToGPU() {
        if (!glReady) return
        val m = model ?: return
        
        // Prepare vertex data
        val vertexData = FloatArray(m.vertices.size * 3)
        for (i in m.vertices.indices) {
            vertexData[i * 3] = m.vertices[i].x
            vertexData[i * 3 + 1] = m.vertices[i].y
            vertexData[i * 3 + 2] = m.vertices[i].z
        }
        
        // Prepare normal data
        val normalData = FloatArray(m.normals.size * 3)
        for (i in m.normals.indices) {
            normalData[i * 3] = m.normals[i].x
            normalData[i * 3 + 1] = m.normals[i].y
            normalData[i * 3 + 2] = m.normals[i].z
        }
        
        // Prepare index data
        val indexData = IntArray(m.faces.size * 3)
        for (i in m.faces.indices) {
            indexData[i * 3] = m.faces[i].v1
            indexData[i * 3 + 1] = m.faces[i].v2
            indexData[i * 3 + 2] = m.faces[i].v3
        }
        
        vertexCount = indexData.size
        
        // Create buffers
        val vertexBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertexData)
        vertexBuffer.position(0)
        
        val normalBuffer = ByteBuffer.allocateDirect(normalData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(normalData)
        normalBuffer.position(0)
        
        val indexBuffer = ByteBuffer.allocateDirect(indexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
            .put(indexData)
        indexBuffer.position(0)
        
        // Upload to GPU
        if (vbo == 0) {
            val buffers = IntArray(3)
            GLES30.glGenBuffers(3, buffers, 0)
            vbo = buffers[0]
            normalVbo = buffers[1]
            ebo = buffers[2]
        }
        
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexData.size * 4, vertexBuffer, GLES30.GL_STATIC_DRAW)
        
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, normalVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, normalData.size * 4, normalBuffer, GLES30.GL_STATIC_DRAW)
        
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexData.size * 4, indexBuffer, GLES30.GL_STATIC_DRAW)
        
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
        
        checkGLError("uploadModelToGPU")
    }
    
    private fun createShaderProgram(): Int {
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, R.raw.vertex_shader)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, R.raw.fragment_shader)
        
        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)
        
        // Check link status
        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val error = GLES30.glGetProgramInfoLog(program)
            GLES30.glDeleteProgram(program)
            throw RuntimeException("Error linking program: $error")
        }
        
        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
        
        return program
    }
    
    private fun loadShader(type: Int, resourceId: Int): Int {
        val code = context.resources.openRawResource(resourceId)
            .bufferedReader()
            .use { it.readText() }
        
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, code)
        GLES30.glCompileShader(shader)
        
        // Check compile status
        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val error = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            throw RuntimeException("Error compiling shader: $error")
        }
        
        return shader
    }
    
    private fun checkGLError(op: String) {
        val error = GLES30.glGetError()
        if (error != GLES30.GL_NO_ERROR) {
            throw RuntimeException("$op: glError $error")
        }
    }
}
