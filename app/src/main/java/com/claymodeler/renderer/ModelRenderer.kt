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
    private val projectionMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val normalMatrix = FloatArray(16)
    private var aspectRatio = 1f
    
    private val camera = Camera()
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
        
        checkGLError("onDrawFrame")
    }
    
    fun setModel(newModel: ClayModel) {
        model = newModel
        octree = Octree(newModel)
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
