package com.claymodeler.export

import com.claymodeler.export.geometry.*
import com.claymodeler.model.ClayModel

sealed class ExportResult {
    data class Success(val path: String) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

class ExportErrorHandler {

    fun safeExport(
        exporter: STLExporter,
        model: ClayModel,
        filename: String,
        config: ExportConfiguration
    ): ExportResult {
        return try {
            if (config.attachmentType == AttachmentType.NONE) {
                val path = exporter.exportBinary(model, filename, config.sizeInMm)
                ExportResult.Success(path)
            } else {
                val path = exporter.exportWithConfiguration(model, filename, config)
                ExportResult.Success(path)
            }
        } catch (e: OutOfMemoryError) {
            // Try simplified export
            try {
                val path = exporter.exportBinary(model, filename, config.sizeInMm, validate = false)
                ExportResult.Success("$path (simplified — attachment omitted due to memory)")
            } catch (_: Exception) {
                ExportResult.Error("Out of memory. Try reducing model complexity.")
            }
        } catch (e: SecurityException) {
            ExportResult.Error("Permission denied. Check storage permissions.")
        } catch (e: IllegalStateException) {
            ExportResult.Error("Could not create file. Try a different filename or location.")
        } catch (e: Exception) {
            ExportResult.Error("Export failed: ${e.message ?: "Unknown error"}")
        }
    }

    fun safeMerge(
        model: ClayModel,
        attachment: GeneratedMesh
    ): Pair<ClayModel, String?> {
        return try {
            val merger = GeometryMerger()
            val merged = merger.merge(model, attachment)
            val validation = merger.validateManifold(merged)
            val warning = if (!validation.valid) {
                "Mesh has issues: ${validation.issues.joinToString(", ")}. Print quality may be affected."
            } else null
            Pair(merged, warning)
        } catch (_: Exception) {
            // Fall back to model without attachment
            Pair(model, "Could not merge attachment. Exporting model only.")
        }
    }
}
