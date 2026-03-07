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

**Save Format (Custom Binary):**
```
Header:
- Magic number: "CLAY" (4 bytes)
- Version: 1 (4 bytes)
- Vertex count (4 bytes)
- Face count (4 bytes)

Data:
- Vertices: [x, y, z] * count (float32)
- Faces: [v1, v2, v3] * count (int32)
```

**STL Export:**
- Binary STL format
- 80-byte header
- Triangle count (4 bytes)
- For each triangle:
  - Normal vector (12 bytes)
  - 3 vertices (36 bytes)
  - Attribute byte count (2 bytes)

**Storage Locations:**
- Internal storage: `/data/data/com.claymodeler/files/models/`
- STL exports: `/storage/emulated/0/Download/`

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

## UI Layout

```
┌─────────────────────────────────────────┐
│  [≡] Clay Modeler        [↶] [↷] [💾]  │ ← Top bar
├─────────────────────────────────────────┤
│                                         │
│                                         │
│           3D Viewport                   │
│         (GLSurfaceView)                 │
│                                         │
│                                         │
├─────────────────────────────────────────┤
│  [Remove] [Add] [Pull]                  │ ← Tool bar
├─────────────────────────────────────────┤
│  Size:   [────●────]                    │ ← Settings
│  Strength: [──●──────]                  │
└─────────────────────────────────────────┘
```

## Technology Stack

- **Language:** Kotlin
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)
- **Graphics:** OpenGL ES 3.0
- **Architecture:** MVVM with LiveData
- **Build System:** Gradle with Kotlin DSL

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
    
    // OpenGL utilities
    implementation("androidx.opengl:opengl:1.0.0")
    
    // File I/O
    implementation("androidx.documentfile:documentfile:1.0.1")
}
```

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

## Future Enhancements

- Additional tools: Smooth, Flatten, Pinch
- Symmetry mode for mirrored editing
- Multiple clay colors/materials
- Texture painting
- Reference image overlay
- Cloud save/sync
- Model sharing
