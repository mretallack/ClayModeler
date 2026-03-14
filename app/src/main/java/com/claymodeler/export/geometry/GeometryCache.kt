package com.claymodeler.export.geometry

import com.claymodeler.export.ExportConfiguration
import com.claymodeler.model.ClayModel
import kotlinx.coroutines.*

class GeometryCache {

    private var cachedConfig: ExportConfiguration? = null
    private var cachedMesh: GeneratedMesh? = null
    private var debounceJob: Job? = null

    fun get(config: ExportConfiguration): GeneratedMesh? =
        if (config == cachedConfig) cachedMesh else null

    fun put(config: ExportConfiguration, mesh: GeneratedMesh) {
        cachedConfig = config
        cachedMesh = mesh
    }

    fun invalidate() {
        cachedConfig = null
        cachedMesh = null
    }

    /**
     * Debounced geometry generation on a background coroutine.
     * Cancels previous pending generation if called again within delayMs.
     */
    fun generateDebounced(
        scope: CoroutineScope,
        config: ExportConfiguration,
        model: ClayModel,
        delayMs: Long = 300L,
        generator: (ClayModel, ExportConfiguration) -> GeneratedMesh,
        onResult: (GeneratedMesh) -> Unit
    ) {
        // Check cache first
        get(config)?.let { onResult(it); return }

        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(delayMs)
            val mesh = withContext(Dispatchers.Default) {
                generator(model, config)
            }
            put(config, mesh)
            onResult(mesh)
        }
    }

    /**
     * Generate a simplified mesh with reduced vertex count for preview.
     */
    companion object {
        fun simplify(mesh: GeneratedMesh, factor: Float = 0.5f): GeneratedMesh {
            if (factor >= 1f) return mesh
            // Simple decimation: skip every Nth face
            val step = (1f / factor).toInt().coerceAtLeast(2)
            val keptFaces = mesh.faces.filterIndexed { i, _ -> i % step == 0 }
            return GeneratedMesh(mesh.vertices, keptFaces.toMutableList())
        }
    }
}
