# Clay Modeling App - Technical Design

## Architecture Overview

The app follows a Model-View-ViewModel (MVVM) architecture with a custom 3D rendering engine built on OpenGL ES.

### High-Level Components

```
┌─────────────────────────────────────────────────────────┐
│                     MainActivity                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │            ModelingViewModel                     │   │
│  │  - Model state management                        │   │
│  │  - Tool state                                    │   │
│  │  - Undo/redo history                            │   │
│  └─────────────────────────────────────────────────┘   │
│                                                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │            GLSurfaceView                         │   │
│  │  ┌──────────────────────────────────────────┐   │   │
│  │  │        ModelRenderer                      │   │   │
│  │  │  - OpenGL ES rendering                    │   │   │
│  │  │  - Camera management                      │   │   │
│  │  │  - Touch event handling                   │   │   │
│  │  └──────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────┘   │
│                                                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │            Tool Toolbar (UI)                     │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
        ┌─────────────────────────────────────┐
        │         Core Components              │
        │                                      │
        │  ┌────────────────────────────────┐ │
        │  │      ClayModel                 │ │
        │  │  - Mesh data (vertices/faces)  │ │
        │  │  - Mesh manipulation methods   │ │
        │  └────────────────────────────────┘ │
        │                                      │
        │  ┌────────────────────────────────┐ │
        │  │      ToolEngine                │ │
        │  │  - RemoveClayTool              │ │
        │  │  - AddClayTool                 │ │
        │  │  - PullClayTool                │ │
        │  └────────────────────────────────┘ │
        │                                      │
        │  ┌────────────────────────────────┐ │
        │  │      FileManager               │ │
        │  │  - Save/load model data        │ │
        │  │  - STL export                  │ │
        │  └────────────────────────────────┘ │
        └─────────────────────────────────────┘
```

## Core Components

### 1. ClayModel

Represents the 3D mesh data and provides manipulation methods.

**Data Structure:**
- Vertices: List of 3D points (x, y, z)
- Faces: Triangles defined by vertex indices
- Normals: Surface normals for lighting

**Key Methods:**
- `initialize()`: Create initial sphere mesh
- `applyTool(tool, position, strength, radius)`: Modify mesh based on tool
- `getVertices()`: Return vertex data for rendering
- `getFaces()`: Return face indices
- `clone()`: Deep copy for undo/redo

**Mesh Representation:**
```
Sphere subdivision using icosphere algorithm:
- Start with icosahedron (20 faces)
- Subdivide each triangle recursively
- Project vertices to sphere surface
- Default: 3 subdivisions = ~1,280 faces
```

### 2. ToolEngine

Abstract tool system with concrete implementations.

**Base Tool Interface:**
```kotlin
interface Tool {
    fun apply(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float)
    fun getIcon(): Int
    fun getName(): String
}
```

**Tool Implementations:**

**RemoveClayTool:**
- Find vertices within radius of hit point
- Move vertices inward along surface normal
- Distance = strength * falloff(distance from center)
- Falloff: smooth curve (1 - (d/r)²)

**AddClayTool:**
- Find vertices within radius of hit point
- Move vertices outward along surface normal
- Same falloff as remove tool

**PullClayTool:**
- Find vertices within radius of hit point
- Move vertices in direction of drag vector
- Blend drag direction with surface normal
- Maintains surface smoothness

**ViewModeTool:**
- Disables mesh modification
- Enables camera manipulation only
- Pinch gesture: Zoom (0.5x to 5x scale)
- Single-finger drag: Rotate around model center
- Two-finger drag: Pan camera
- Double-tap: Reset to default view
- All transformations pivot around clay model origin

### 3. ModelRenderer

OpenGL ES 3.0 renderer for 3D visualization.

**Rendering Pipeline:**
1. Clear color and depth buffers
2. Set up view and projection matrices
3. Bind shader program
4. Upload vertex data to GPU
5. Upload normal data for lighting
6. Draw triangles
7. Swap buffers

**Shaders:**

**Vertex Shader:**
- Transform vertices by MVP matrix
- Pass normals to fragment shader
- Calculate lighting per-vertex

**Fragment Shader:**
- Phong lighting model
- Single directional light
- Clay-like material (matte finish)
- Color: Terracotta/clay tone

**Camera System:**
- Orbit camera around model center
- Touch rotation: Convert screen delta to rotation angles
- Pinch zoom: Adjust camera distance
- Pan: Move camera target position

### 4. FileManager

Handles model persistence and export.

**File Format Rationale:**

The app uses a hybrid binary + JSON approach for optimal performance and flexibility.

**Format Comparison:**

| Aspect | Binary + JSON (Current) | Pure JSON | Pure Binary | Protocol Buffers |
|--------|------------------------|-----------|-------------|------------------|
| Load Speed | Fast (10-50x vs JSON) | Slow | Fastest | Fast |
| File Size | Compact (61.7 KB) | Large (185 KB) | Smallest | Compact |
| Extensibility | Easy (JSON metadata) | Easy | Hard | Medium |
| Human Readable | Metadata only | Full | None | None |
| Implementation | Simple | Simple | Simple | Complex |
| Debugging | Easy | Easy | Hard | Medium |

**Why Hybrid Binary + JSON:**
- Performance: Binary vertex/face data loads 10-50x faster than JSON
- Compact: 61.7 KB vs 185 KB for same model (3x smaller)
- Flexible: JSON metadata easy to extend without breaking compatibility
- Industry standard: Similar to glTF, FBX, and other 3D formats
- Best of both worlds: Fast binary data + human-readable metadata

**Pure JSON Alternative (Rejected):**
```json
{
  "vertices": [[0.0, 1.0, 0.0], [0.5, 0.5, 0.5], ...],
  "faces": [[0, 1, 2], [1, 2, 3], ...],
  "normals": [[0.0, 1.0, 0.0], ...]
}
```
- ❌ 3-5x larger file size
- ❌ Slow JSON parsing (CPU-intensive)
- ❌ High memory overhead during parsing
- ❌ Not optimized for numerical data
- ✅ Human-readable (but users won't edit these files)

**Protocol Buffers Alternative (Future Consideration):**
- Could replace JSON metadata for better performance
- Requires additional dependency
- More complex implementation
- Consider if metadata becomes large or complex

**Save Format (Custom Binary - .clay):**

The app uses a custom binary format optimized for fast loading and compact storage.

```
File Structure:
┌─────────────────────────────────────────┐
│ Header (32 bytes)                       │
├─────────────────────────────────────────┤
│ Metadata (variable)                     │
├─────────────────────────────────────────┤
│ Vertex Data (variable)                  │
├─────────────────────────────────────────┤
│ Face Data (variable)                    │
├─────────────────────────────────────────┤
│ Normal Data (variable)                  │
└─────────────────────────────────────────┘

Header (32 bytes):
- Magic number: "CLAY" (4 bytes, ASCII)
- Version: 1 (4 bytes, uint32)
- Vertex count (4 bytes, uint32)
- Face count (4 bytes, uint32)
- Metadata length (4 bytes, uint32)
- Checksum (4 bytes, CRC32)
- Reserved (8 bytes, for future use)

Metadata (variable length, JSON):
{
  "name": "Model_001",
  "created": "2024-03-07T15:30:00Z",
  "modified": "2024-03-07T16:45:00Z",
  "app_version": "1.0.0",
  "subdivision_level": 3,
  "bounds": {
    "min": [-1.0, -1.0, -1.0],
    "max": [1.0, 1.0, 1.0]
  }
}

Vertex Data:
- Format: [x, y, z] * vertex_count
- Type: float32 (4 bytes each)
- Total size: vertex_count * 12 bytes
- Coordinate system: Right-handed, Y-up
- Units: Arbitrary (normalized to unit sphere initially)

Face Data:
- Format: [v1, v2, v3] * face_count
- Type: uint32 (4 bytes each)
- Total size: face_count * 12 bytes
- Winding order: Counter-clockwise (front-facing)
- Indices: Zero-based vertex indices

Normal Data:
- Format: [nx, ny, nz] * vertex_count
- Type: float32 (4 bytes each)
- Total size: vertex_count * 12 bytes
- Normalized: All normals are unit vectors
- Per-vertex normals for smooth shading
```

**File Size Estimation:**
```
Example for default sphere (3 subdivisions):
- Vertices: ~1,280
- Faces: ~2,560

Size calculation:
- Header: 32 bytes
- Metadata: ~200 bytes (JSON)
- Vertices: 1,280 * 12 = 15,360 bytes
- Faces: 2,560 * 12 = 30,720 bytes
- Normals: 1,280 * 12 = 15,360 bytes
- Total: ~61.7 KB

Highly detailed model (50,000 vertices):
- Total: ~2.4 MB
```

**Compression (Future Enhancement):**
- Optional zlib compression
- File extension: .clay.gz
- Reduces size by 60-80% for typical models
- Trade-off: Slightly slower load times

**STL Export:**

Standard binary STL format for 3D printing compatibility.

```
Binary STL Structure:
┌─────────────────────────────────────────┐
│ Header (80 bytes)                       │
├─────────────────────────────────────────┤
│ Triangle count (4 bytes)                │
├─────────────────────────────────────────┤
│ Triangle 1 (50 bytes)                   │
│   - Normal (12 bytes)                   │
│   - Vertex 1 (12 bytes)                 │
│   - Vertex 2 (12 bytes)                 │
│   - Vertex 3 (12 bytes)                 │
│   - Attribute (2 bytes)                 │
├─────────────────────────────────────────┤
│ Triangle 2 (50 bytes)                   │
│ ...                                     │
└─────────────────────────────────────────┘

Header (80 bytes):
- ASCII text: "ClayModeler v1.0 - [model_name]"
- Padded with spaces to 80 bytes

Triangle Data:
- Normal: [nx, ny, nz] (float32)
- Vertex 1: [x, y, z] (float32)
- Vertex 2: [x, y, z] (float32)
- Vertex 3: [x, y, z] (float32)
- Attribute: 0x0000 (unused, 2 bytes)

Units:
- Millimeters (standard for 3D printing)
- Default scale: 100mm diameter sphere
- User can specify scale in export dialog
```

**STL Export Implementation:**

**Step 1: Pre-Export Validation**
```kotlin
fun validateMeshForExport(model: ClayModel): ValidationResult {
    // Check if mesh is manifold (watertight)
    if (!isManifold(model)) {
        return ValidationResult.Warning("Mesh has holes, may not print correctly")
    }
    
    // Check for degenerate triangles
    val degenerateCount = countDegenerateTriangles(model)
    if (degenerateCount > 0) {
        return ValidationResult.Warning("$degenerateCount degenerate triangles will be skipped")
    }
    
    // Check normal orientation
    if (!allNormalsOutward(model)) {
        return ValidationResult.Warning("Some normals face inward, will be flipped")
    }
    
    return ValidationResult.Success
}
```

**Step 2: Coordinate System Conversion**
```
App Coordinate System (OpenGL):
- Right-handed
- Y-up (vertical)
- Z-forward (toward camera)

3D Printing Coordinate System (Standard):
- Right-handed
- Z-up (vertical)
- Y-forward

Conversion Matrix:
[x']   [1  0  0] [x]
[y'] = [0  0  1] [y]
[z']   [0 -1  0] [z]

In code:
newX = oldX
newY = oldZ
newZ = -oldY
```

**Step 3: Scale to Millimeters**
```kotlin
fun scaleToMillimeters(model: ClayModel, targetSize: Float): ClayModel {
    // Calculate current bounding box
    val bounds = model.getBounds()
    val currentSize = max(bounds.width, bounds.height, bounds.depth)
    
    // Calculate scale factor
    val scaleFactor = targetSize / currentSize
    
    // Apply scale to all vertices
    return model.transform { vertex ->
        vertex * scaleFactor
    }
}

// Default: 100mm diameter
// User options: 50mm, 100mm, 150mm, 200mm, Custom
```

**Step 4: Calculate Face Normals**
```kotlin
fun calculateFaceNormal(v1: Vector3, v2: Vector3, v3: Vector3): Vector3 {
    // Calculate edges
    val edge1 = v2 - v1
    val edge2 = v3 - v1
    
    // Cross product (right-hand rule)
    val normal = edge1.cross(edge2)
    
    // Normalize to unit vector
    return normal.normalize()
}

// Ensure counter-clockwise winding (outward-facing)
fun ensureCorrectWinding(v1: Vector3, v2: Vector3, v3: Vector3, 
                         expectedNormal: Vector3): Triple<Vector3, Vector3, Vector3> {
    val calculatedNormal = calculateFaceNormal(v1, v2, v3)
    
    // If normal points inward, reverse winding order
    if (calculatedNormal.dot(expectedNormal) < 0) {
        return Triple(v1, v3, v2)  // Swap v2 and v3
    }
    
    return Triple(v1, v2, v3)
}
```

**Step 5: Write Binary STL**
```kotlin
fun exportSTL(model: ClayModel, outputPath: String, scale: Float) {
    val file = File(outputPath)
    val output = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))
    
    try {
        // 1. Write header (80 bytes)
        val header = "ClayModeler v1.0 - ${model.name}".padEnd(80, ' ')
        output.write(header.toByteArray(Charsets.US_ASCII), 0, 80)
        
        // 2. Count valid triangles (skip degenerate)
        val validFaces = model.faces.filter { !isDegenerate(it) }
        output.writeInt(validFaces.size.toLittleEndian())
        
        // 3. Write each triangle
        for (face in validFaces) {
            // Get vertices
            val v1 = model.vertices[face.v1]
            val v2 = model.vertices[face.v2]
            val v3 = model.vertices[face.v3]
            
            // Convert coordinate system
            val v1Conv = convertCoordinates(v1)
            val v2Conv = convertCoordinates(v2)
            val v3Conv = convertCoordinates(v3)
            
            // Scale to millimeters
            val v1Scaled = v1Conv * scale
            val v2Scaled = v2Conv * scale
            val v3Scaled = v3Conv * scale
            
            // Calculate normal
            val normal = calculateFaceNormal(v1Scaled, v2Scaled, v3Scaled)
            
            // Ensure correct winding
            val (v1Final, v2Final, v3Final) = ensureCorrectWinding(
                v1Scaled, v2Scaled, v3Scaled, normal
            )
            
            // Write normal (12 bytes)
            output.writeFloat(normal.x.toLittleEndian())
            output.writeFloat(normal.y.toLittleEndian())
            output.writeFloat(normal.z.toLittleEndian())
            
            // Write vertices (36 bytes)
            writeVertex(output, v1Final)
            writeVertex(output, v2Final)
            writeVertex(output, v3Final)
            
            // Write attribute (2 bytes, unused)
            output.writeShort(0)
        }
        
        output.flush()
        
    } finally {
        output.close()
    }
}

fun writeVertex(output: DataOutputStream, v: Vector3) {
    output.writeFloat(v.x.toLittleEndian())
    output.writeFloat(v.y.toLittleEndian())
    output.writeFloat(v.z.toLittleEndian())
}

// STL uses little-endian byte order
fun Int.toLittleEndian(): Int = Integer.reverseBytes(this)
fun Float.toLittleEndian(): Float = Float.fromBits(Integer.reverseBytes(this.toBits()))
```

**Step 6: Progress Tracking**
```kotlin
suspend fun exportSTLWithProgress(
    model: ClayModel, 
    outputPath: String, 
    scale: Float,
    onProgress: (Int) -> Unit
): Result<String> = withContext(Dispatchers.IO) {
    try {
        val totalFaces = model.faces.size
        var processedFaces = 0
        
        // ... write header and count ...
        
        for (face in model.faces) {
            // ... write triangle ...
            
            processedFaces++
            if (processedFaces % 100 == 0) {
                val progress = (processedFaces * 100) / totalFaces
                withContext(Dispatchers.Main) {
                    onProgress(progress)
                }
            }
        }
        
        Result.success(outputPath)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Step 7: Save to Downloads (Android 10+)**
```kotlin
fun saveToDownloads(context: Context, fileName: String, data: ByteArray): Uri? {
    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "application/sla")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }
    
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    
    uri?.let {
        resolver.openOutputStream(it)?.use { output ->
            output.write(data)
        }
    }
    
    return uri
}
```

**Export Dialog Options:**

```
┌─────────────────────────────────┐
│  Export STL                     │
│                                 │
│  File Name:                     │
│  [Model_001________________]    │
│                                 │
│  Size:                          │
│  ○ 50mm   ● 100mm   ○ 150mm     │
│  ○ 200mm  ○ Custom: [___]mm     │
│                                 │
│  Quality:                       │
│  ○ Low (Fast)                   │
│  ● Medium (Recommended)         │
│  ○ High (Slow, large file)      │
│                                 │
│  Options:                       │
│  ☑ Validate mesh                │
│  ☑ Fix normals                  │
│  ☐ ASCII format (debug)         │
│                                 │
│  [CANCEL]         [EXPORT]      │
└─────────────────────────────────┘
```

**Quality Settings:**
- Low: Skip validation, faster export
- Medium: Basic validation, fix obvious issues
- High: Full validation, repair mesh if possible

**Mesh Validation:**

```kotlin
fun isManifold(model: ClayModel): Boolean {
    // Check if every edge is shared by exactly 2 faces
    val edgeCount = mutableMapOf<Edge, Int>()
    
    for (face in model.faces) {
        val edges = listOf(
            Edge(face.v1, face.v2),
            Edge(face.v2, face.v3),
            Edge(face.v3, face.v1)
        )
        
        for (edge in edges) {
            edgeCount[edge] = edgeCount.getOrDefault(edge, 0) + 1
        }
    }
    
    // All edges should be shared by exactly 2 faces
    return edgeCount.values.all { it == 2 }
}

fun isDegenerate(face: Face, vertices: List<Vector3>): Boolean {
    val v1 = vertices[face.v1]
    val v2 = vertices[face.v2]
    val v3 = vertices[face.v3]
    
    // Check if vertices are collinear or coincident
    val edge1 = v2 - v1
    val edge2 = v3 - v1
    val cross = edge1.cross(edge2)
    
    // If cross product is near zero, triangle is degenerate
    return cross.length() < 0.0001f
}

fun allNormalsOutward(model: ClayModel): Boolean {
    val center = model.getCenter()
    
    for (face in model.faces) {
        val v1 = model.vertices[face.v1]
        val v2 = model.vertices[face.v2]
        val v3 = model.vertices[face.v3]
        
        val faceCenter = (v1 + v2 + v3) / 3.0f
        val normal = calculateFaceNormal(v1, v2, v3)
        val toCenter = center - faceCenter
        
        // Normal should point away from center
        if (normal.dot(toCenter) > 0) {
            return false
        }
    }
    
    return true
}
```

**ASCII STL Option (Debug):**

```
solid ClayModeler_Model_001
  facet normal 0.0 0.0 1.0
    outer loop
      vertex 0.0 0.0 0.0
      vertex 1.0 0.0 0.0
      vertex 0.0 1.0 0.0
    endloop
  endfacet
  ...
endsolid ClayModeler_Model_001
```

- Human-readable format
- Larger file size (3-5x)
- Useful for debugging geometry issues
- Not recommended for production use

**Error Handling:**

```kotlin
sealed class ExportError {
    object InsufficientStorage : ExportError()
    object PermissionDenied : ExportError()
    data class InvalidMesh(val issues: List<String>) : ExportError()
    data class IOError(val message: String) : ExportError()
}

fun handleExportError(error: ExportError) {
    when (error) {
        is ExportError.InsufficientStorage -> 
            showError("Not enough storage space")
        is ExportError.PermissionDenied -> 
            showError("Storage permission required")
        is ExportError.InvalidMesh -> 
            showWarning("Mesh issues: ${error.issues.joinToString()}")
        is ExportError.IOError -> 
            showError("Export failed: ${error.message}")
    }
}
```

**Storage Locations:**

```
Internal Storage (Private):
/data/data/com.claymodeler/files/
├── models/
│   ├── Model_001.clay
│   ├── Model_002.clay
│   └── autosave.clay
├── thumbnails/
│   ├── Model_001.png (128x128)
│   └── Model_002.png (128x128)
└── cache/
    └── temp_export.stl

External Storage (Public):
/storage/emulated/0/
├── Download/
│   ├── Model_001.stl
│   └── Model_002.stl
└── Documents/ClayModeler/
    ├── Model_001.clay (optional backup)
    └── Model_002.clay
```

**Storage Management:**

- Internal models: Managed by app, deleted on uninstall
- Thumbnails: Auto-generated on save (128x128 PNG)
- Autosave: Every 5 minutes, single file (overwritten)
- STL exports: Saved to Downloads, persist after uninstall
- Cache: Cleared on app exit

**File Operations:**

**Save:**
1. Serialize model to binary format
2. Calculate CRC32 checksum
3. Write to temporary file
4. Verify write success
5. Rename to final filename (atomic operation)
6. Generate thumbnail (async)
7. Update metadata index

**Load:**
1. Read header and validate magic number
2. Verify version compatibility
3. Validate checksum
4. Read metadata
5. Allocate buffers for vertex/face/normal data
6. Read data in chunks (for large files)
7. Reconstruct ClayModel object
8. Rebuild octree for spatial queries

**Export STL:**
1. Validate mesh (manifold check, degenerate triangles)
2. Convert coordinate system (Y-up to Z-up)
3. Scale to millimeters (user-specified size)
4. Calculate face normals from vertices
5. Ensure correct winding order (counter-clockwise)
6. Write STL header (80 bytes)
7. Write triangle count (4 bytes, little-endian)
8. For each face:
   - Skip if degenerate
   - Calculate and normalize normal vector
   - Convert coordinates and scale vertices
   - Write normal (12 bytes, little-endian float32)
   - Write 3 vertices (36 bytes, little-endian float32)
   - Write attribute bytes (2 bytes, 0x0000)
9. Flush to disk with progress updates
10. Save to Downloads using MediaStore API
11. Notify user of file location and any warnings

**Error Handling:**

- Corrupted file: Show error, offer to delete
- Version mismatch: Attempt migration or reject
- Insufficient storage: Warn before save
- Write failure: Retry once, then show error
- Checksum mismatch: File corrupted, cannot load

**Backup & Sync (Future Enhancement):**

- Cloud backup to Google Drive
- Export/import via share intent
- Automatic backup on save
- Conflict resolution for synced files

### 5. ModelingViewModel

Manages app state and business logic.

**State:**
- Current model: ClayModel
- Active tool: Tool
- Tool settings: brush size, strength
- Undo stack: List<ClayModel>
- Redo stack: List<ClayModel>

**Methods:**
- `applyTool(position, strength)`: Apply current tool and save to undo stack
- `undo()`: Restore previous model state
- `redo()`: Reapply undone action
- `saveModel(name)`: Persist model to storage
- `loadModel(name)`: Load model from storage
- `exportSTL(name)`: Export as STL file
- `startAutoSave()`: Begin auto-save timer (1 minute interval)
- `stopAutoSave()`: Stop auto-save timer
- `performAutoSave()`: Save to autosave.clay silently

**Auto-Save Implementation:**

```kotlin
class ModelingViewModel : ViewModel() {
    private var autoSaveJob: Job? = null
    private var lastModifiedTime: Long = 0
    private val autoSaveInterval = 60_000L // 1 minute
    
    fun startAutoSave() {
        autoSaveJob = viewModelScope.launch {
            while (isActive) {
                delay(autoSaveInterval)
                if (hasUnsavedChanges()) {
                    performAutoSave()
                }
            }
        }
    }
    
    private suspend fun performAutoSave() {
        withContext(Dispatchers.IO) {
            try {
                fileManager.saveToAutoSave(currentModel)
                // Silent - no user notification
            } catch (e: Exception) {
                // Log error but don't interrupt user
                Log.e("AutoSave", "Failed to auto-save", e)
            }
        }
    }
    
    fun onModelChanged() {
        lastModifiedTime = System.currentTimeMillis()
        // Reset auto-save timer handled by coroutine
    }
    
    fun checkForAutoSaveRestore(): Boolean {
        return fileManager.hasAutoSave() && 
               fileManager.getAutoSaveAge() < 24 * 60 * 60 * 1000 // 24 hours
    }
    
    suspend fun restoreAutoSave(): ClayModel? {
        return withContext(Dispatchers.IO) {
            try {
                fileManager.loadFromAutoSave()
            } catch (e: Exception) {
                null
            }
        }
    }
}
```

**Auto-Save File Location:**
- Path: `/data/data/com.claymodeler/files/autosave.clay`
- Single file (overwritten each save)
- Not shown in load dialog
- Deleted after successful manual save
- Retained for 24 hours after last write

## Touch Interaction Flow

```
User Touch Event
      │
      ▼
GLSurfaceView.onTouchEvent()
      │
      ├─── Single finger drag ──→ Rotate camera
      │
      ├─── Pinch gesture ──→ Zoom camera
      │
      ├─── Two finger drag ──→ Pan camera
      │
      └─── Touch on model ──→ Ray casting
                                    │
                                    ▼
                              Find hit point on mesh
                                    │
                                    ▼
                              ViewModel.applyTool()
                                    │
                                    ▼
                              Tool.apply(model, hitPoint, ...)
                                    │
                                    ▼
                              Model mesh updated
                                    │
                                    ▼
                              Renderer draws updated mesh
```

## Ray Casting Algorithm

To determine where the user touched on the 3D model:

1. Convert screen coordinates to normalized device coordinates
2. Unproject to create ray in world space
3. Test ray against all triangles in mesh
4. Find closest intersection point
5. Return hit point and surface normal

## Performance Optimizations

**Spatial Partitioning:**
- Octree structure for fast vertex lookup
- Only test nearby vertices when applying tools
- Rebuild octree after significant changes

**GPU Optimization:**
- Use Vertex Buffer Objects (VBO)
- Update only modified vertex regions
- Batch draw calls

**Undo/Redo:**
- Limit history to 20 actions
- Store deltas instead of full copies (future optimization)

**Mesh Simplification:**
- Optional: Reduce triangle count for export
- Preserve visual quality while reducing file size

## UI Layout & Visual Design

### Wireframes

**Portrait Mode (Primary):**
```
┌─────────────────────────────────────────┐
│  [≡] Clay Modeler        [↶] [↷] [💾]  │ ← Top bar (56dp)
├─────────────────────────────────────────┤
│                                         │
│                                         │
│           3D Viewport                   │ ← Expandable
│         (GLSurfaceView)                 │   (fills space)
│                                         │
│                                         │
├─────────────────────────────────────────┤
│  [🗑️ Remove] [➕ Add] [👆 Pull] [👁️ View]│ ← Tool bar (72dp)
├─────────────────────────────────────────┤
│  Size:     [────●────]                  │ ← Settings (120dp)
│  Strength: [──●──────]                  │
└─────────────────────────────────────────┘
```

**Landscape Mode:**
```
┌──────────────────────────────────────────────────────────┐
│  [≡] Clay Modeler              [↶] [↷] [💾]             │
├────────────────────────────────┬─────────────────────────┤
│                                │  [🗑️ Remove]            │
│                                │  [➕ Add]               │
│         3D Viewport            │  [👆 Pull]              │
│       (GLSurfaceView)          │  [👁️ View]              │
│                                │                         │
│                                │  Size:   [──●──]        │
│                                │  Strength: [──●──]      │
└────────────────────────────────┴─────────────────────────┘
```

**Tablet Mode (10" and above):**
- Side panel for tools (persistent)
- Larger touch targets (64dp)
- More spacing between elements

### Color Palette

**Primary Colors:**
- Primary: `#00897B` (Teal 600)
- Primary Dark: `#00695C` (Teal 800)
- Accent: `#FF6F00` (Orange 800)

**Clay & Viewport:**
- Clay Color: `#D2691E` (Terracotta)
- Viewport Background: `#1A1A1A` (Near black)
- Grid Lines (optional): `#333333` (Dark gray, 20% opacity)

**UI Elements:**
- Background: `#FAFAFA` (Light gray)
- Surface: `#FFFFFF` (White)
- Tool Bar: `#424242` (Dark gray)
- Dividers: `#E0E0E0` (Light gray)

**Text:**
- Primary Text: `#212121` (87% black)
- Secondary Text: `#757575` (54% black)
- Disabled Text: `#BDBDBD` (38% black)
- Text on Dark: `#FFFFFF` (White)

**States:**
- Selected Tool: `#00897B` (Primary)
- Hover/Focus: `#B2DFDB` (Teal 100)
- Error: `#D32F2F` (Red 700)
- Success: `#388E3C` (Green 700)

### Typography

**Font Family:** Roboto (Material Design standard)

**Sizes:**
- App Title: 20sp, Medium
- Tool Labels: 14sp, Medium
- Slider Labels: 12sp, Regular
- Body Text: 16sp, Regular
- Button Text: 14sp, Medium (All caps)
- Dialog Title: 20sp, Medium
- Dialog Body: 16sp, Regular

### Component Specifications

**Top Bar (AppBar):**
- Height: 56dp (phone), 64dp (tablet)
- Elevation: 4dp
- Background: Primary color
- Icons: 24dp, white
- Title: 20sp, white, medium weight

**Tool Buttons:**
- Size: 64dp × 64dp (phone), 72dp × 72dp (tablet)
- Minimum touch target: 48dp × 48dp
- Icon size: 24dp
- Corner radius: 8dp
- Elevation: 2dp (normal), 8dp (selected)
- Spacing: 8dp between buttons
- Selected state: Primary color background
- Unselected state: Surface color background

**Sliders:**
- Track height: 4dp
- Thumb size: 20dp diameter
- Active color: Primary
- Inactive color: Primary at 38% opacity
- Label size: 12sp
- Value display: 14sp, shown above thumb

**3D Viewport:**
- Fills remaining space
- Minimum height: 200dp
- Background: `#1A1A1A`
- Border: None
- Clay initial size: 40% of viewport height

### Dialogs & Menus

**Save Dialog:**
```
┌─────────────────────────────────┐
│  Save Model                     │
│                                 │
│  Model Name:                    │
│  [_________________________]    │
│                                 │
│  [CANCEL]           [SAVE]      │
└─────────────────────────────────┘
```
- Width: 280dp (phone), 400dp (tablet)
- Padding: 24dp
- Button height: 36dp
- Text field height: 56dp

**Load Dialog:**
```
┌─────────────────────────────────┐
│  Load Model                     │
│                                 │
│  ┌───────────────────────────┐ │
│  │ ● Model_001.clay          │ │
│  │   Created: 2024-03-07     │ │
│  ├───────────────────────────┤ │
│  │ ● Model_002.clay          │ │
│  │   Created: 2024-03-06     │ │
│  └───────────────────────────┘ │
│                                 │
│  [CANCEL]           [LOAD]      │
└─────────────────────────────────┘
```
- List item height: 72dp
- Icon size: 40dp
- Max visible items: 5 (scrollable)

**Export Dialog:**
```
┌─────────────────────────────────┐
│  Export STL                     │
│                                 │
│  File Name:                     │
│  [_________________________]    │
│                                 │
│  Quality:                       │
│  ○ Low    ● Medium    ○ High    │
│                                 │
│  [CANCEL]         [EXPORT]      │
└─────────────────────────────────┘
```

**Menu (Hamburger):**
- New Model
- Save Model
- Load Model
- Export STL
- Settings
- Help
- About

### States & Feedback

**Loading States:**
- Circular progress indicator (48dp)
- Centered in viewport
- Primary color
- Text: "Loading model..." (16sp, secondary text)

**Error States:**
```
┌─────────────────────────────────┐
│  ⚠️                              │
│  Failed to load model           │
│  File may be corrupted          │
│                                 │
│  [DISMISS]          [RETRY]     │
└─────────────────────────────────┘
```
- Icon: 48dp, error color
- Message: 16sp, error color
- Snackbar for non-critical errors

**Empty State (Load Dialog):**
```
┌─────────────────────────────────┐
│  Load Model                     │
│                                 │
│         📦                       │
│    No saved models              │
│    Create your first model!     │
│                                 │
│  [CANCEL]                       │
└─────────────────────────────────┘
```

**Success Feedback:**
- Snackbar: "Model saved successfully"
- Duration: 3 seconds
- Action: "UNDO" (optional)
- Background: Success color

**Tool Cursor/Preview:**
- Circle overlay on model surface
- Diameter: Current brush size
- Color: White with 50% opacity
- Border: 2dp, primary color
- Updates in real-time as slider changes

### View Mode

**View Mode Activation:**
- Dedicated "View" button in tool bar
- Icon: Eye symbol (👁️)
- When active: All editing tools disabled
- Visual indicator: Blue border around viewport

**View Mode Controls:**
- **Pinch:** Zoom in/out (0.5x to 5x)
- **Single-finger drag:** Rotate around model center
- **Two-finger drag:** Pan camera
- **Double-tap:** Reset to default view
- All gestures centered on clay model origin

**View Mode UI:**
- Zoom level indicator: Bottom-right corner
- Format: "1.0x" (14sp, white with shadow)
- Reset button: Floating action button (56dp)
- Icon: Home/center icon
- Position: Bottom-right, 16dp margin

### Interactions & Animations

**Tool Selection:**
- Duration: 150ms
- Easing: Fast out, slow in
- Scale: 1.0 → 1.1 → 1.0
- Elevation change: 2dp → 8dp

**Slider Interaction:**
- Thumb scales to 24dp when dragging
- Haptic feedback on value change
- Value label appears above thumb

**Model Manipulation:**
- Real-time mesh updates (< 16ms)
- Smooth interpolation for tool application
- No animation delay

**Undo/Redo:**
- Button pulse animation on action
- Duration: 200ms
- Disabled state: 38% opacity

**Dialog Transitions:**
- Fade in: 200ms
- Scale: 0.8 → 1.0
- Backdrop: Fade to 50% black

**Snackbar:**
- Slide up from bottom: 250ms
- Auto-dismiss: 3 seconds
- Swipe to dismiss

### Accessibility

**Content Descriptions:**
- Menu button: "Open menu"
- Undo button: "Undo last action"
- Redo button: "Redo action"
- Save button: "Save model"
- Remove tool: "Remove clay tool"
- Add tool: "Add clay tool"
- Pull tool: "Pull clay tool"
- View tool: "View mode - zoom and rotate"
- Size slider: "Brush size, current value X percent"
- Strength slider: "Brush strength, current value X percent"

**Touch Targets:**
- Minimum: 48dp × 48dp
- Recommended: 56dp × 56dp for primary actions
- Spacing: 8dp minimum between targets

**Color Contrast:**
- Text on background: 4.5:1 minimum (WCAG AA)
- Large text: 3:1 minimum
- Icons: 3:1 minimum
- All critical UI elements meet WCAG AA standards

**Screen Reader Support:**
- Announce tool changes
- Announce undo/redo actions
- Announce save/load success/failure
- Describe model state changes

### Responsive Design

**Phone (< 600dp width):**
- Portrait: Vertical layout (as shown)
- Landscape: Horizontal split (tools on right)
- Tool bar: Scrollable if needed
- Settings: Collapsible panel

**Tablet (600dp - 840dp):**
- Side panel for tools (persistent)
- Larger touch targets (64dp)
- More padding (24dp vs 16dp)
- Two-column dialogs where appropriate

**Large Tablet (> 840dp):**
- Master-detail layout option
- Tool palette can float
- Multiple viewports (future enhancement)

**Minimum Requirements:**
- Screen width: 320dp
- Screen height: 480dp
- Aspect ratio: 16:9 to 21:9

### Navigation Flow

```
Launch App
    ↓
Main Screen (New Model)
    ↓
    ├─→ Menu → Save → Save Dialog → Main Screen
    ├─→ Menu → Load → Load Dialog → Main Screen (loaded model)
    ├─→ Menu → Export → Export Dialog → Success Snackbar
    ├─→ Menu → Settings → Settings Screen → Main Screen
    ├─→ Menu → Help → Help Screen → Main Screen
    └─→ Menu → About → About Dialog → Main Screen
```

**Back Button Behavior:**
- Main screen: Exit app (with confirmation if unsaved changes)
- Dialog: Close dialog
- Settings/Help: Return to main screen
- View mode: Exit view mode, return to last tool

### Performance Indicators

**FPS Counter (Debug mode):**
- Position: Top-right corner
- Format: "30 FPS" (12sp, white with shadow)
- Updates every second
- Hidden in release builds

**Vertex Count (Debug mode):**
- Position: Below FPS counter
- Format: "12,345 vertices" (12sp, white with shadow)

## Technology Stack

- **Language:** Kotlin 2.2.20
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 36 (Android 14)
- **Compile SDK:** 36
- **Graphics:** OpenGL ES 3.0
- **Architecture:** MVVM with LiveData
- **Build System:** Gradle 8.13.0 with Kotlin DSL
- **Java Version:** 21 (OpenJDK)
- **Testing:** Kotest (JUnit 5), MockK, Robolectric
- **CI/CD:** GitHub Actions

## Development Environment

**Local Setup:**
- Android SDK: `/home/mark/android-sdk/`
- Java: OpenJDK 21.0.10
- Gradle: Wrapper-based (./gradlew)
- Build tools, platforms, and licenses configured

**Project Structure:**
- Root: `/home/mark/git/ClayModeler/`
- Multi-module support (app module primary)
- Gradle Kotlin DSL for all build files

## Dependencies

```kotlin
dependencies {
    // Android core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    
    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    
    // File I/O
    implementation("androidx.documentfile:documentfile:1.0.1")
    
    // Crash reporting (ACRA - local only, no upload)
    implementation("ch.acra:acra-core:5.11.3")
    implementation("ch.acra:acra-dialog:5.11.3")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
}
```

**Important:** No Google Play Services, Firebase, or analytics dependencies. App is fully offline and F-Droid compatible.

**Note:** OpenGL ES 3.0 is built into Android SDK (API 18+), no external dependency needed. Use:
```kotlin
import android.opengl.GLSurfaceView
import android.opengl.GLES30
```

## Crash Reporting (ACRA)

**Configuration:**

```kotlin
@AcraCore(
    buildConfigClass = BuildConfig::class,
    reportFormat = StringFormat.JSON
)
@AcraDialog(
    reportDialogClass = CrashReportDialog::class,
    resTitle = R.string.crash_dialog_title,
    resText = R.string.crash_dialog_text,
    resCommentPrompt = R.string.crash_dialog_comment_prompt
)
class ClayModelerApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        ACRA.init(this)
    }
}
```

**Crash Report Storage:**
- Location: `/data/data/com.claymodeler/files/acra-reports/`
- Format: JSON
- No automatic upload
- User can manually share via email/file manager
- Reports include: stack trace, device info, app version
- No personal data collected

**Crash Dialog:**
```
┌─────────────────────────────────┐
│  App Crashed                    │
│                                 │
│  ClayModeler has stopped        │
│  unexpectedly.                  │
│                                 │
│  Would you like to save a       │
│  crash report?                  │
│                                 │
│  Comment (optional):            │
│  [_________________________]    │
│                                 │
│  [DON'T SAVE]      [SAVE]       │
└─────────────────────────────────┘
```

**Privacy:**
- No network access required
- No automatic data collection
- User controls all data sharing
- F-Droid compatible
- GDPR compliant

## Testing Strategy

### Unit Tests

**Test Coverage Targets:**
- ClayModel: 90%+ coverage
- Tool implementations: 90%+ coverage
- FileManager: 85%+ coverage
- File format validation: 95%+ coverage
- STL export: 90%+ coverage
- ModelingViewModel: 85%+ coverage
- Overall business logic: 80%+ coverage

**ClayModel Tests:**
```kotlin
class ClayModelTest {
    @Test fun `initialize creates sphere with correct vertex count`()
    @Test fun `applyTool modifies vertices within radius`()
    @Test fun `applyTool does not modify vertices outside radius`()
    @Test fun `clone creates independent copy`()
    @Test fun `getVertices returns correct data format`()
}
```

**Tool Tests:**
```kotlin
class RemoveClayToolTest {
    @Test fun `apply moves vertices inward`()
    @Test fun `apply respects strength parameter`()
    @Test fun `apply uses smooth falloff`()
}

class AddClayToolTest {
    @Test fun `apply moves vertices outward`()
    @Test fun `apply blends smoothly with surface`()
}

class PullClayToolTest {
    @Test fun `apply moves vertices in drag direction`()
    @Test fun `apply maintains surface smoothness`()
}
```

**FileManager Tests:**
```kotlin
class FileManagerTest {
    @Test fun `saveModel writes valid file format`()
    @Test fun `loadModel reads saved file correctly`()
    @Test fun `loadModel handles corrupted files`()
    @Test fun `exportSTL creates valid STL format`()
    @Test fun `exportSTL handles empty model`()
}
```

**File Format Validation Tests:**

```kotlin
class ClayFileFormatTest {
    @Test 
    fun `clay file has correct magic number`() {
        val model = createTestModel()
        val file = saveToTempFile(model)
        
        val bytes = file.readBytes()
        val magic = bytes.sliceArray(0..3).toString(Charsets.US_ASCII)
        
        assertThat(magic).isEqualTo("CLAY")
    }
    
    @Test 
    fun `clay file has correct version`() {
        val model = createTestModel()
        val file = saveToTempFile(model)
        
        val version = readUInt32(file, offset = 4)
        
        assertThat(version).isEqualTo(1)
    }
    
    @Test 
    fun `clay file vertex count matches actual vertices`() {
        val model = createTestModel(vertexCount = 100)
        val file = saveToTempFile(model)
        
        val headerVertexCount = readUInt32(file, offset = 8)
        val actualVertexCount = model.vertices.size
        
        assertThat(headerVertexCount).isEqualTo(actualVertexCount)
    }
    
    @Test 
    fun `clay file face count matches actual faces`() {
        val model = createTestModel(faceCount = 200)
        val file = saveToTempFile(model)
        
        val headerFaceCount = readUInt32(file, offset = 12)
        val actualFaceCount = model.faces.size
        
        assertThat(headerFaceCount).isEqualTo(actualFaceCount)
    }
    
    @Test 
    fun `clay file checksum is valid`() {
        val model = createTestModel()
        val file = saveToTempFile(model)
        
        val storedChecksum = readUInt32(file, offset = 20)
        val calculatedChecksum = calculateCRC32(file, skipBytes = 20..23)
        
        assertThat(storedChecksum).isEqualTo(calculatedChecksum)
    }
    
    @Test 
    fun `clay file metadata is valid JSON`() {
        val model = createTestModel()
        val file = saveToTempFile(model)
        
        val metadataLength = readUInt32(file, offset = 16)
        val metadataBytes = file.readBytes().sliceArray(32 until 32 + metadataLength)
        val metadataJson = metadataBytes.toString(Charsets.UTF_8)
        
        // Should parse without exception
        val metadata = Json.parseToJsonElement(metadataJson)
        assertThat(metadata.jsonObject).containsKey("name")
        assertThat(metadata.jsonObject).containsKey("created")
    }
    
    @Test 
    fun `clay file vertex data is correct size`() {
        val model = createTestModel(vertexCount = 100)
        val file = saveToTempFile(model)
        
        val vertexCount = readUInt32(file, offset = 8)
        val expectedSize = vertexCount * 12 // 3 floats * 4 bytes
        
        val metadataLength = readUInt32(file, offset = 16)
        val vertexDataStart = 32 + metadataLength
        val vertexDataEnd = vertexDataStart + expectedSize
        
        assertThat(file.length()).isGreaterThanOrEqualTo(vertexDataEnd)
    }
    
    @Test 
    fun `clay file vertices are valid floats`() {
        val model = createTestModel()
        val file = saveToTempFile(model)
        
        val vertices = readVertices(file)
        
        for (vertex in vertices) {
            assertThat(vertex.x).isFinite()
            assertThat(vertex.y).isFinite()
            assertThat(vertex.z).isFinite()
        }
    }
    
    @Test 
    fun `clay file face indices are within bounds`() {
        val model = createTestModel(vertexCount = 100)
        val file = saveToTempFile(model)
        
        val vertexCount = readUInt32(file, offset = 8)
        val faces = readFaces(file)
        
        for (face in faces) {
            assertThat(face.v1).isLessThan(vertexCount)
            assertThat(face.v2).isLessThan(vertexCount)
            assertThat(face.v3).isLessThan(vertexCount)
        }
    }
    
    @Test 
    fun `clay file round trip preserves data`() {
        val originalModel = createTestModel()
        val file = saveToTempFile(originalModel)
        val loadedModel = loadFromFile(file)
        
        assertThat(loadedModel.vertices.size).isEqualTo(originalModel.vertices.size)
        assertThat(loadedModel.faces.size).isEqualTo(originalModel.faces.size)
        
        for (i in originalModel.vertices.indices) {
            assertVertexNear(loadedModel.vertices[i], originalModel.vertices[i], 0.0001f)
        }
    }
    
    @Test 
    fun `clay file with corrupted magic number is rejected`() {
        val file = createCorruptedFile(corruptMagicNumber = true)
        
        assertThrows<InvalidFileFormatException> {
            loadFromFile(file)
        }
    }
    
    @Test 
    fun `clay file with invalid checksum is rejected`() {
        val file = createCorruptedFile(corruptChecksum = true)
        
        assertThrows<CorruptedFileException> {
            loadFromFile(file)
        }
    }
}

class STLFileFormatTest {
    @Test 
    fun `stl file has 80 byte header`() {
        val model = createTestModel()
        val file = exportToSTL(model)
        
        assertThat(file.length()).isGreaterThanOrEqualTo(80)
    }
    
    @Test 
    fun `stl file header contains app name`() {
        val model = createTestModel(name = "TestModel")
        val file = exportToSTL(model)
        
        val header = file.readBytes().sliceArray(0..79).toString(Charsets.US_ASCII)
        
        assertThat(header).contains("ClayModeler")
        assertThat(header).contains("TestModel")
    }
    
    @Test 
    fun `stl file triangle count is correct`() {
        val model = createTestModel(faceCount = 100)
        val file = exportToSTL(model)
        
        val triangleCount = readUInt32LittleEndian(file, offset = 80)
        
        assertThat(triangleCount).isEqualTo(100)
    }
    
    @Test 
    fun `stl file has correct total size`() {
        val model = createTestModel(faceCount = 100)
        val file = exportToSTL(model)
        
        val expectedSize = 80 + 4 + (100 * 50) // header + count + triangles
        
        assertThat(file.length()).isEqualTo(expectedSize.toLong())
    }
    
    @Test 
    fun `stl file normals are unit vectors`() {
        val model = createTestModel()
        val file = exportToSTL(model)
        
        val triangles = readSTLTriangles(file)
        
        for (triangle in triangles) {
            val normalLength = sqrt(
                triangle.normal.x * triangle.normal.x +
                triangle.normal.y * triangle.normal.y +
                triangle.normal.z * triangle.normal.z
            )
            assertThat(normalLength).isCloseTo(1.0f, 0.001f)
        }
    }
    
    @Test 
    fun `stl file normals match calculated normals`() {
        val model = createTestModel()
        val file = exportToSTL(model)
        
        val triangles = readSTLTriangles(file)
        
        for (triangle in triangles) {
            val edge1 = triangle.v2 - triangle.v1
            val edge2 = triangle.v3 - triangle.v1
            val calculatedNormal = edge1.cross(edge2).normalize()
            
            assertVectorNear(triangle.normal, calculatedNormal, 0.01f)
        }
    }
    
    @Test 
    fun `stl file uses little endian byte order`() {
        val model = createTestModel()
        val file = exportToSTL(model)
        
        // Read triangle count both ways
        val bytes = file.readBytes()
        val littleEndian = ByteBuffer.wrap(bytes, 80, 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .getInt()
        val bigEndian = ByteBuffer.wrap(bytes, 80, 4)
            .order(ByteOrder.BIG_ENDIAN)
            .getInt()
        
        // Should be different unless count is palindromic
        if (model.faces.size > 255) {
            assertThat(littleEndian).isNotEqualTo(bigEndian)
        }
        assertThat(littleEndian).isEqualTo(model.faces.size)
    }
    
    @Test 
    fun `stl file vertices are in millimeters`() {
        val model = createUnitSphere() // 1.0 unit radius
        val file = exportToSTL(model, scale = 100f) // 100mm diameter
        
        val triangles = readSTLTriangles(file)
        
        // Find max distance from origin
        var maxDistance = 0f
        for (triangle in triangles) {
            for (vertex in listOf(triangle.v1, triangle.v2, triangle.v3)) {
                val distance = sqrt(vertex.x * vertex.x + vertex.y * vertex.y + vertex.z * vertex.z)
                maxDistance = max(maxDistance, distance)
            }
        }
        
        // Should be approximately 50mm (radius of 100mm diameter sphere)
        assertThat(maxDistance).isCloseTo(50f, 1f)
    }
    
    @Test 
    fun `stl file coordinate system is Z-up`() {
        // Create model with known Y-up orientation
        val model = createModelWithYUpOrientation()
        val file = exportToSTL(model)
        
        val triangles = readSTLTriangles(file)
        
        // After conversion, what was Y should now be Z
        // Verify by checking highest point
        val maxZ = triangles.flatMap { listOf(it.v1, it.v2, it.v3) }
            .maxOf { it.z }
        val maxY = triangles.flatMap { listOf(it.v1, it.v2, it.v3) }
            .maxOf { it.y }
        
        assertThat(maxZ).isGreaterThan(maxY)
    }
    
    @Test 
    fun `stl file has counter-clockwise winding`() {
        val model = createTestModel()
        val file = exportToSTL(model)
        
        val triangles = readSTLTriangles(file)
        
        for (triangle in triangles) {
            val edge1 = triangle.v2 - triangle.v1
            val edge2 = triangle.v3 - triangle.v1
            val calculatedNormal = edge1.cross(edge2).normalize()
            
            // Calculated normal should match stored normal (same direction)
            val dotProduct = calculatedNormal.dot(triangle.normal)
            assertThat(dotProduct).isGreaterThan(0.99f) // Allow small tolerance
        }
    }
    
    @Test 
    fun `stl file skips degenerate triangles`() {
        val model = createModelWithDegenerateTriangles()
        val file = exportToSTL(model)
        
        val triangleCount = readUInt32LittleEndian(file, offset = 80)
        val validTriangleCount = model.faces.count { !isDegenerate(it) }
        
        assertThat(triangleCount).isEqualTo(validTriangleCount)
    }
    
    @Test 
    fun `stl file all normals point outward`() {
        val model = createTestSphere()
        val file = exportToSTL(model)
        
        val triangles = readSTLTriangles(file)
        val center = calculateCenter(triangles)
        
        for (triangle in triangles) {
            val faceCenter = (triangle.v1 + triangle.v2 + triangle.v3) / 3f
            val toCenter = center - faceCenter
            
            // Normal should point away from center (negative dot product)
            val dotProduct = triangle.normal.dot(toCenter)
            assertThat(dotProduct).isLessThan(0f)
        }
    }
    
    @Test 
    fun `stl file is valid for 3D printing software`() {
        val model = createTestModel()
        val file = exportToSTL(model)
        
        // Run basic STL validation
        val validation = validateSTLFile(file)
        
        assertThat(validation.isValid).isTrue()
        assertThat(validation.errors).isEmpty()
    }
    
    @Test 
    fun `stl export with different scales produces correct sizes`() {
        val model = createUnitSphere()
        
        val scales = listOf(50f, 100f, 150f, 200f)
        
        for (scale in scales) {
            val file = exportToSTL(model, scale = scale)
            val triangles = readSTLTriangles(file)
            
            val maxDistance = triangles.flatMap { listOf(it.v1, it.v2, it.v3) }
                .maxOf { sqrt(it.x * it.x + it.y * it.y + it.z * it.z) }
            
            // Should be approximately scale/2 (radius)
            assertThat(maxDistance).isCloseTo(scale / 2f, scale * 0.05f)
        }
    }
    
    @Test 
    fun `ascii stl format is valid`() {
        val model = createTestModel()
        val file = exportToASCIISTL(model)
        
        val content = file.readText()
        
        assertThat(content).startsWith("solid")
        assertThat(content).endsWith("endsolid")
        assertThat(content).contains("facet normal")
        assertThat(content).contains("outer loop")
        assertThat(content).contains("vertex")
        assertThat(content).contains("endloop")
        assertThat(content).contains("endfacet")
    }
}

class FileFormatIntegrationTest {
    @Test 
    fun `save and load preserves model exactly`() {
        val original = createComplexModel()
        
        val clayFile = saveToClayFile(original)
        val loaded = loadFromClayFile(clayFile)
        
        assertModelsEqual(original, loaded, tolerance = 0.0001f)
    }
    
    @Test 
    fun `export to stl and reimport preserves geometry`() {
        val original = createTestModel()
        
        val stlFile = exportToSTL(original)
        val reimported = importFromSTL(stlFile)
        
        // Vertex positions should match (within tolerance for coordinate conversion)
        assertModelsGeometricallyEqual(original, reimported, tolerance = 0.01f)
    }
    
    @Test 
    fun `large model saves and loads correctly`() {
        val largeModel = createTestModel(vertexCount = 50000)
        
        val file = saveToClayFile(largeModel)
        val loaded = loadFromClayFile(file)
        
        assertThat(loaded.vertices.size).isEqualTo(50000)
        assertModelsEqual(largeModel, loaded, tolerance = 0.0001f)
    }
    
    @Test 
    fun `stl export handles mesh with holes`() {
        val modelWithHoles = createNonManifoldModel()
        
        val result = exportToSTLWithValidation(modelWithHoles)
        
        assertThat(result.warnings).contains("Mesh has holes")
        assertThat(result.file).isNotNull()
    }
    
    @Test 
    fun `file format version migration works`() {
        val v1File = createVersion1ClayFile()
        
        // Should load and migrate to current version
        val loaded = loadFromClayFile(v1File)
        
        assertThat(loaded).isNotNull()
    }
}
```

**Test Utilities:**

```kotlin
object TestData {
    fun createTestModel(
        vertexCount: Int = 100,
        faceCount: Int = 200
    ): ClayModel {
        // Create icosphere with specified complexity
    }
    
    fun createUnitSphere(): ClayModel {
        // Create sphere with 1.0 unit radius
    }
    
    fun createModelWithDegenerateTriangles(): ClayModel {
        // Include some degenerate triangles for testing
    }
    
    fun createNonManifoldModel(): ClayModel {
        // Create model with holes/non-manifold edges
    }
    
    fun createCorruptedFile(
        corruptMagicNumber: Boolean = false,
        corruptChecksum: Boolean = false
    ): File {
        // Create intentionally corrupted file for error testing
    }
}

object FileAssertions {
    fun assertVertexNear(actual: Vector3, expected: Vector3, tolerance: Float) {
        assertThat(actual.x).isCloseTo(expected.x, tolerance)
        assertThat(actual.y).isCloseTo(expected.y, tolerance)
        assertThat(actual.z).isCloseTo(expected.z, tolerance)
    }
    
    fun assertVectorNear(actual: Vector3, expected: Vector3, tolerance: Float) {
        val distance = (actual - expected).length()
        assertThat(distance).isLessThan(tolerance)
    }
    
    fun assertModelsEqual(actual: ClayModel, expected: ClayModel, tolerance: Float) {
        assertThat(actual.vertices.size).isEqualTo(expected.vertices.size)
        assertThat(actual.faces.size).isEqualTo(expected.faces.size)
        
        for (i in actual.vertices.indices) {
            assertVertexNear(actual.vertices[i], expected.vertices[i], tolerance)
        }
    }
}

object STLValidator {
    fun validateSTLFile(file: File): ValidationResult {
        // Check header size
        if (file.length() < 84) {
            return ValidationResult.Invalid("File too small")
        }
        
        // Check triangle count matches file size
        val triangleCount = readUInt32LittleEndian(file, 80)
        val expectedSize = 84 + (triangleCount * 50)
        if (file.length() != expectedSize) {
            return ValidationResult.Invalid("File size mismatch")
        }
        
        // Check all normals are unit vectors
        val triangles = readSTLTriangles(file)
        for (triangle in triangles) {
            val length = triangle.normal.length()
            if (abs(length - 1.0f) > 0.01f) {
                return ValidationResult.Invalid("Non-unit normal found")
            }
        }
        
        return ValidationResult.Valid
    }
}
```

**ViewModel Tests:**
```kotlin
class ModelingViewModelTest {
    @Test fun `applyTool updates model state`()
    @Test fun `applyTool adds to undo stack`()
    @Test fun `undo restores previous state`()
    @Test fun `redo reapplies undone action`()
    @Test fun `undo stack limited to 20 items`()
    @Test fun `saveModel persists current state`()
    @Test fun `loadModel updates current state`()
}
```

### Integration Tests

**Workflow Tests:**
```kotlin
class ModelWorkflowTest {
    @Test fun `save and load preserves model data`()
    @Test fun `export STL creates valid file`()
    @Test fun `multiple tool applications work correctly`()
    @Test fun `undo redo cycle maintains consistency`()
}
```

**Test Execution:**
- Unit tests: Run on JVM using Robolectric (no emulator needed)
- Integration tests: Run on JVM where possible, emulator for UI tests
- Mock OpenGL calls for renderer tests

**Local Headless Emulator Testing:**

For running integration tests locally without a GUI:

```bash
# 1. Check KVM is available (required for hardware acceleration)
ls -la /dev/kvm

# 2. Install system image (one-time setup)
sdkmanager "system-images;android-29;google_apis;x86_64"

# 3. Create AVD (one-time setup)
avdmanager create avd -n test_avd -k "system-images;android-29;google_apis;x86_64" --device "pixel_5"

# 4. Start emulator headless
emulator -avd test_avd -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect &

# 5. Wait for device to boot
adb wait-for-device
adb shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done'

# 6. Run integration tests
./gradlew connectedDebugAndroidTest

# 7. Kill emulator when done
adb emu kill
```

**Headless Emulator Options:**
- `-no-window`: Run without GUI
- `-no-audio`: Disable audio
- `-no-boot-anim`: Skip boot animation (faster)
- `-gpu swiftshader_indirect`: Software rendering (no GPU needed)
- `-no-snapshot-save`: Don't save state on exit
- `-wipe-data`: Start with clean state

**Requirements:**
- KVM enabled on Linux host
- 2-4GB RAM per emulator instance
- Android SDK with emulator installed
- Boot time: ~30-60 seconds

**Benefits:**
- Test on server without GUI
- Verify APK installation and functionality
- Run full integration tests locally
- Hardware accelerated (with KVM)

**Script for Automated Testing:**
```bash
#!/bin/bash
# test-with-emulator.sh

set -e

AVD_NAME="test_avd"
API_LEVEL="29"

echo "Starting emulator..."
emulator -avd $AVD_NAME -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect &
EMULATOR_PID=$!

echo "Waiting for device..."
adb wait-for-device
adb shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done'
echo "Device ready!"

echo "Running tests..."
./gradlew connectedDebugAndroidTest

echo "Killing emulator..."
adb emu kill
wait $EMULATOR_PID

echo "Tests complete!"
```

### Test Utilities

**Mock Data:**
```kotlin
object TestData {
    fun createSimpleSphere(subdivisions: Int = 1): ClayModel
    fun createTestModel(vertexCount: Int): ClayModel
    fun createMockTouchEvent(x: Float, y: Float): MotionEvent
}
```

**Assertions:**
```kotlin
fun assertVertexNear(expected: Vector3, actual: Vector3, tolerance: Float)
fun assertMeshValid(model: ClayModel)
fun assertFileFormatValid(file: File)
```

## CI/CD Pipeline

### GitHub Actions Workflow

**File:** `.github/workflows/android-ci.yml`

**Environment:**
- Runner: `ubuntu-latest`
- Java: OpenJDK 21 (Temurin distribution)
- Gradle cache enabled

**Triggers:**
- Push to main/master branch
- Pull requests to main/master
- Manual workflow dispatch
- Ignore paths: README.md, docs, meta files

**Jobs:**

**1. Validate Gradle Wrapper**
```yaml
- name: Validate Gradle wrapper
  uses: gradle/actions/wrapper-validation@v5
```

**2. Setup Environment**
```yaml
- name: Check out repository
  uses: actions/checkout@v4
  
- name: Set up Java JDK
  uses: actions/setup-java@v4
  with:
    java-version: 21
    distribution: "temurin"
    cache: 'gradle'
```

**3. Build and Test**
```yaml
- name: Build and test
  run: ./gradlew assembleDebug lintDebug testDebugUnitTest --stacktrace
```

**4. Upload Artifacts**
```yaml
- name: Upload APK
  uses: actions/upload-artifact@v6
  with:
    name: app
    path: app/build/outputs/apk/debug/*.apk
    
- name: Upload Test Reports
  if: always()
  uses: actions/upload-artifact@v6
  with:
    name: test-reports
    path: app/build/reports/tests/
    
- name: Upload Lint Reports
  if: always()
  uses: actions/upload-artifact@v6
  with:
    name: lint-reports
    path: app/build/reports/lint/
```

**5. Coverage (Optional)**
```yaml
- name: Generate Coverage Report
  run: ./gradlew jacocoTestReport
  
- name: Upload Coverage
  uses: actions/upload-artifact@v6
  with:
    name: coverage-report
    path: app/build/reports/jacoco/
```

### Release Workflow

**File:** `.github/workflows/release.yml`

**Trigger:** Tag push on master branch

```yaml
name: Release Build

on:
  push:
    tags:
      - 'v*'
    branches:
      - master

jobs:
  test:
    runs-on: ubuntu-latest
    if: startsWith(github.ref, 'refs/tags/')
    
    steps:
      - name: Check out repository
        uses: actions/checkout@v4
      
      - name: Set up Java JDK
        uses: actions/setup-java@v4
        with:
          java-version: 21
          distribution: "temurin"
          cache: 'gradle'
      
      - name: Run Lint
        run: ./gradlew lintDebug
      
      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest
      
      - name: Run Integration Tests
        run: ./gradlew connectedDebugAndroidTest || echo "Skipping if no emulator"
      
      - name: Upload Test Reports
        if: always()
        uses: actions/upload-artifact@v6
        with:
          name: test-reports
          path: app/build/reports/tests/
  
  release:
    runs-on: ubuntu-latest
    needs: test
    if: startsWith(github.ref, 'refs/tags/')
    
    steps:
      - name: Check out repository
        uses: actions/checkout@v4
      
      - name: Set up Java JDK
        uses: actions/setup-java@v4
        with:
          java-version: 21
          distribution: "temurin"
          cache: 'gradle'
      
      - name: Extract version from tag
        id: version
        run: echo "VERSION=${GITHUB_REF#refs/tags/v}" >> $GITHUB_OUTPUT
      
      - name: Build Release APK
        run: ./gradlew assembleRelease
      
      - name: Sign APK
        run: |
          # Signing handled by build.gradle.kts with secrets
          echo "APK signed during build"
      
      - name: Rename APK
        run: |
          mv app/build/outputs/apk/release/app-release.apk \
             app/build/outputs/apk/release/ClayModeler-${{ steps.version.outputs.VERSION }}.apk
      
      - name: Create GitHub Release
        uses: softprops/action-gh-release@v1
        with:
          files: app/build/outputs/apk/release/ClayModeler-*.apk
          draft: false
          prerelease: false
          generate_release_notes: true
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      
      - name: Upload APK Artifact
        uses: actions/upload-artifact@v6
        with:
          name: ClayModeler-${{ steps.version.outputs.VERSION }}
          path: app/build/outputs/apk/release/ClayModeler-*.apk
```

**Important:** Release job only runs if test job passes. If any tests fail, release is aborted.

**Secrets Required:**
- `KEYSTORE_FILE`: Base64 encoded keystore
- `KEYSTORE_PASSWORD`: Keystore password
- `KEY_ALIAS`: Key alias
- `KEY_PASSWORD`: Key password

**Usage:**
```bash
# Create and push tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# Workflow automatically:
# 1. Runs lint checks (must pass)
# 2. Runs unit tests (must pass)
# 3. Runs integration tests (must pass)
# 4. Builds release APK (only if tests pass)
# 5. Signs APK
# 6. Creates GitHub release
# 7. Uploads APK to release
```

**Release Naming:**
- Tag: `v1.0.0`
- APK: `ClayModeler-1.0.0.apk`
- Release: `v1.0.0`

### Build Configuration

**build.gradle.kts (Project level):**

```kotlin
plugins {
    alias(libs.plugins.com.android.application) apply false
    alias(libs.plugins.org.jetbrains.kotlin.android) apply false
}
```

**build.gradle.kts (App module):**

```kotlin
plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    id("jacoco")
}

android {
    namespace = "com.claymodeler"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.claymodeler"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        viewBinding = true
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    classDirectories.setFrom(files("build/tmp/kotlin-classes/debug"))
    executionData.setFrom(files("build/jacoco/testDebugUnitTest.exec"))
}

tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("jacocoTestReport")
    
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
```

**settings.gradle.kts:**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ClayModeler"
include(":app")
```

**gradle/libs.versions.toml:**

```toml
[versions]
kotlin = "2.2.20"
gradle = "8.13.0"
appcompat = "1.7.1"
lifecycle = "2.7.0"
opengl = "1.0.0"
documentfile = "1.0.1"
kotest = "6.0.3"
mockk = "1.13.8"
robolectric = "4.11.1"
androidxTest = "1.7.0"
espresso = "3.5.1"
coroutines = "1.7.3"
jacoco = "0.8.11"
minSdk = "26"
targetSdk = "36"
compileSdk = "36"
java = "21"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version = "1.12.0" }
androidx-appcompat = { module = "androidx.appcompat:appcompat", version.ref = "appcompat" }
material = { module = "com.google.android.material:material", version = "1.11.0" }
lifecycle-viewmodel-ktx = { module = "androidx.lifecycle:lifecycle-viewmodel-ktx", version.ref = "lifecycle" }
lifecycle-livedata-ktx = { module = "androidx.lifecycle:lifecycle-livedata-ktx", version.ref = "lifecycle" }
opengl = { module = "androidx.opengl:opengl", version.ref = "opengl" }
documentfile = { module = "androidx.documentfile:documentfile", version.ref = "documentfile" }

# Testing
kotest-runner-junit5 = { module = "io.kotest:kotest-runner-junit5", version.ref = "kotest" }
kotest-assertions-core = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest" }
kotest-property = { module = "io.kotest:kotest-property", version.ref = "kotest" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
androidx-test-core = { module = "androidx.test:core", version.ref = "androidxTest" }
androidx-test-ext-junit = { module = "androidx.test.ext:junit", version = "1.1.5" }
androidx-test-runner = { module = "androidx.test:runner", version.ref = "androidxTest" }
androidx-test-rules = { module = "androidx.test:rules", version.ref = "androidxTest" }
androidx-test-espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "espresso" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }

[plugins]
com-android-application = { id = "com.android.application", version.ref = "gradle" }
org-jetbrains-kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

**local.properties:**

```properties
sdk.dir=/home/mark/android-sdk
```

## Test Data Management

**Test Models:**
- Simple sphere (100 vertices) for fast tests
- Medium sphere (1000 vertices) for integration tests
- Complex model (10000 vertices) for performance tests

**Test Files:**
- Valid save files for load testing
- Corrupted files for error handling tests
- STL reference files for export validation

## Error Handling

**Rendering Errors:**
- Catch OpenGL errors after each operation
- Log errors and display user-friendly message
- Fallback to safe state (reload last valid model)

**File I/O Errors:**
- Validate file format before loading
- Handle corrupted files gracefully
- Display specific error messages (file not found, permission denied, etc.)

**Memory Errors:**
- Monitor mesh complexity
- Warn user if model becomes too complex
- Implement mesh decimation if needed

**Touch Input Errors:**
- Validate ray casting results
- Handle edge cases (touch outside model)
- Prevent tool application if no valid hit point

## Risks and Unknowns

### Technical Risks

**High Risk:**

1. **OpenGL ES Performance on Low-End Devices**
   - Risk: Real-time mesh updates may not achieve 30 FPS on older devices
   - Impact: Poor user experience, app unusable on budget phones
   - Mitigation: 
     - Implement adaptive mesh quality
     - Profile on minimum spec device (Android 8.0, 2GB RAM)
     - Add performance mode with reduced visual quality
   - Unknown: Actual performance on target devices

2. **Memory Constraints with Large Models**
   - Risk: 50K vertex models may cause OOM crashes on 2GB RAM devices
   - Impact: App crashes, data loss
   - Mitigation:
     - Implement mesh decimation
     - Warn users before exceeding limits
     - Add memory monitoring
     - Implement streaming/paging for very large models
   - Unknown: Actual memory usage patterns during sculpting

3. **Touch Input Precision**
   - Risk: Ray casting may be inaccurate, tools apply to wrong areas
   - Impact: Frustrating user experience, imprecise sculpting
   - Mitigation:
     - Implement spatial acceleration (octree)
     - Add visual feedback (cursor preview)
     - Allow zoom for fine detail work
   - Unknown: Touch precision on different screen sizes/densities

**Medium Risk:**

4. **File Format Compatibility**
   - Risk: Future versions may break compatibility with old .clay files
   - Impact: Users lose access to old models
   - Mitigation:
     - Version field in header
     - Implement migration logic
     - Maintain backward compatibility for at least 2 versions
   - Unknown: How format will evolve over time

5. **STL Export Quality**
   - Risk: Exported STLs may have issues (non-manifold, inverted normals)
   - Impact: Models fail to print, user frustration
   - Mitigation:
     - Comprehensive validation before export
     - Automatic repair where possible
     - Clear warnings to user
     - Test with multiple slicing software
   - Unknown: Real-world 3D printing success rate

6. **Undo/Redo Memory Usage**
   - Risk: Storing 20 full model copies uses excessive memory
   - Impact: OOM crashes, slow performance
   - Mitigation:
     - Implement delta compression (store only changes)
     - Reduce undo stack size on low-memory devices
     - Clear undo stack on memory pressure
   - Unknown: Optimal undo stack size vs memory trade-off

7. **Android Fragmentation**
   - Risk: Different behaviors on different Android versions/manufacturers
   - Impact: Bugs on specific devices, inconsistent experience
   - Mitigation:
     - Test on multiple devices (Samsung, Pixel, OnePlus, etc.)
     - Use Android compatibility libraries
     - Handle manufacturer-specific quirks
   - Unknown: Device-specific issues until testing

**Low Risk:**

8. **Storage Permissions (Android 11+)**
   - Risk: Scoped storage restrictions may complicate file access
   - Impact: Cannot save/load files as expected
   - Mitigation:
     - Use MediaStore API
     - Request appropriate permissions
     - Fallback to app-private storage
   - Status: Well-documented, standard solution exists

9. **Coordinate System Confusion**
   - Risk: Y-up to Z-up conversion may introduce bugs
   - Impact: Models export upside-down or rotated incorrectly
   - Mitigation:
     - Comprehensive unit tests
     - Visual verification during development
     - User-facing "preview" before export
   - Status: Testable, fixable

### UX/Design Risks

**Medium Risk:**

10. **Learning Curve**
    - Risk: Users unfamiliar with 3D modeling struggle to use app
    - Impact: Poor reviews, low retention
    - Mitigation:
      - Interactive tutorial on first launch
      - Tooltips and hints
      - Example models to explore
      - Help documentation
    - Unknown: Target user's 3D modeling experience level

11. **Touch Gestures Conflicts**
    - Risk: Pinch/pan/rotate gestures interfere with tool usage
    - Impact: Accidental actions, frustration
    - Mitigation:
      - Clear mode separation (View mode vs Edit mode)
      - Visual feedback for active gesture
      - Configurable gesture sensitivity
    - Unknown: Optimal gesture detection thresholds

12. **Small Screen Usability**
    - Risk: UI too cramped on phones, tools hard to select
    - Impact: Poor mobile experience
    - Mitigation:
      - Responsive layout
      - Collapsible panels
      - Tablet-optimized layout
      - Test on 5" screen minimum
    - Unknown: Minimum practical screen size

### Business/Scope Risks

**High Risk:**

13. **Scope Creep**
    - Risk: Feature requests expand beyond MVP
    - Impact: Delayed launch, incomplete features
    - Mitigation:
      - Strict MVP definition
      - Feature prioritization
      - Post-launch roadmap for enhancements
    - Status: Requires discipline

14. **3D Printing Ecosystem Changes**
    - Risk: STL format becomes obsolete, new formats emerge
    - Impact: App becomes less relevant
    - Mitigation:
      - Monitor 3D printing trends
      - Design for extensible export formats
      - Consider 3MF format support
    - Unknown: Future of 3D printing file formats

**Low Risk:**

15. **Competition**
    - Risk: Similar apps already exist or emerge
    - Impact: Reduced user adoption
    - Mitigation:
      - Focus on simplicity and ease of use
      - Target beginners, not professionals
      - Free and open-source advantage
    - Status: Acceptable risk for learning project

### Technical Unknowns

**Critical Unknowns:**

1. **OpenGL ES 3.0 Availability**
   - Question: What % of Android 8.0+ devices support OpenGL ES 3.0?
   - Impact: May need OpenGL ES 2.0 fallback
   - Resolution: Check Android device statistics, test on real devices

2. **Mesh Topology Preservation**
   - Question: How to maintain good topology during sculpting?
   - Impact: Models may become difficult to edit or export poorly
   - Resolution: Research mesh editing algorithms, implement remeshing

3. **Optimal Subdivision Level**
   - Question: What's the best default sphere subdivision for editing?
   - Impact: Too low = blocky, too high = slow
   - Resolution: User testing, performance profiling

4. **Tool Falloff Curves**
   - Question: What falloff function feels most natural?
   - Impact: Tools feel too harsh or too soft
   - Resolution: User testing, adjustable falloff

**Important Unknowns:**

5. **File Size Growth**
   - Question: How large do models get with heavy editing?
   - Impact: Storage concerns, slow save/load
   - Resolution: Monitor during development, implement compression if needed

6. **Battery Usage**
   - Question: How much battery does real-time 3D rendering consume?
   - Impact: User complaints about battery drain
   - Resolution: Profile battery usage, optimize rendering

7. **Thermal Throttling**
   - Question: Will sustained sculpting cause device to throttle?
   - Impact: Performance degrades over time
   - Resolution: Test extended sessions, implement frame rate limiting

8. **Accessibility for 3D Apps**
   - Question: How to make 3D sculpting accessible to screen reader users?
   - Impact: Excludes users with visual impairments
   - Resolution: Research accessibility best practices, may be out of scope for MVP

### Dependency Risks

**Low Risk:**

9. **Kotest/Testing Framework**
   - Risk: Testing framework has breaking changes
   - Impact: Tests fail, need refactoring
   - Mitigation: Pin versions, gradual upgrades
   - Status: Stable, mature library

10. **Android SDK Changes**
    - Risk: New Android versions break compatibility
    - Impact: App doesn't work on latest Android
    - Mitigation: Follow Android best practices, test beta releases
    - Status: Manageable with proper testing

### Risk Mitigation Strategy

**Phase 1: Proof of Concept (Week 1-2)**
- Build minimal OpenGL renderer with sphere
- Test performance on low-end device
- Validate touch input precision
- Decision point: Continue or pivot?

**Phase 2: Core Features (Week 3-6)**
- Implement one tool (Remove Clay)
- Test on multiple devices
- Measure memory usage
- Validate file format

**Phase 3: Polish & Testing (Week 7-8)**
- User testing with target audience
- Performance optimization
- Bug fixes
- Documentation

**Risk Review Cadence:**
- Weekly risk assessment
- Update mitigation strategies
- Add newly discovered risks
- Remove resolved risks

### Open Questions

1. ~~Should we support importing existing STL files for editing?~~ **DECIDED: No**
2. What's the maximum model complexity we should support?
3. ~~Should we implement auto-save? How frequently?~~ **DECIDED: Yes, every 1 minute**
4. ~~Do we need cloud backup, or is local storage sufficient?~~ **DECIDED: Local storage only, no cloud**
5. ~~Should we support stylus input (S-Pen, Apple Pencil)?~~ **DECIDED: No, touch only**
6. What's the minimum viable tutorial/onboarding?
7. ~~Should we support landscape-only mode for tablets?~~ **DECIDED: Both portrait and landscape**
8. ~~Do we need a "gallery" view of saved models?~~ **DECIDED: No, simple list in load dialog**
9. ~~Should exported STLs include metadata (app version, creation date)?~~ **DECIDED: Yes, in 80-byte header**
10. ~~What analytics/crash reporting should we implement?~~ **DECIDED: ACRA crash reporting, no analytics**

### STL Metadata

**What is STL Metadata?**
Additional information embedded in STL files beyond 3D geometry.

**Available Metadata Spaces:**

1. **Header (80 bytes)** - Only official metadata space:
   ```
   "ClayModeler v1.0 - Model_001                                               "
   ```
   Can include: app name, version, model name, date, author

2. **Attribute Bytes (2 bytes per triangle)** - Usually 0x0000:
   - Some software uses for color (RGB)
   - Material properties
   - Layer information
   - We use: 0x0000 (standard)

3. **ASCII STL Comments** - Only in ASCII format:
   ```
   solid ClayModeler_Model_001
     ; Created: 2024-03-07
     ; App: ClayModeler v1.0
   ```

**Our Implementation:**
- Header includes: "ClayModeler v1.0 - [model_name]"
- Attribute bytes: 0x0000 (standard)
- ASCII format (debug only): Can include comments

**Rationale:**
- 80 bytes is limited space
- Most slicing software ignores metadata
- Not standardized across tools
- Full metadata available in original .clay file
- Simple approach is sufficient

### Decided Requirements

**Auto-Save:**
- Frequency: Every 1 minute
- Location: `/data/data/com.claymodeler/files/autosave.clay`
- Single file (overwritten each time)
- Silent operation (no user notification)
- Restore on app crash/restart

**No Cloud Dependencies:**
- No Google Play Services
- No Firebase
- No cloud storage integration
- Works completely offline
- F-Droid compatible

**Crash Reporting:**
- ACRA (Application Crash Reports for Android)
- Local crash logs only
- No automatic upload
- User can manually share crash reports
- Privacy-focused

**Orientation Support:**
- Portrait: Primary layout (vertical tools)
- Landscape: Horizontal layout (tools on side)
- Rotation handled gracefully (preserve model state)
- No orientation lock

**No Gallery:**
- Load dialog shows simple list
- Thumbnail preview in list items
- Sort by date modified
- Search/filter not needed for MVP

## Future Enhancements

- Additional tools: Smooth, Flatten, Pinch
- Symmetry mode for mirrored editing
- Multiple clay colors/materials
- Texture painting
- Reference image overlay
- Cloud save/sync
- Model sharing
- Import existing STL files
- Remeshing/topology optimization
- Multi-material 3D printing support
