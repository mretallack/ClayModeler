# STL Export Enhancements - Design

## Architecture Overview

The STL export enhancement feature adds a wizard-based workflow for configuring and generating 3D printable models with bases, keyring loops, and wall hooks. The system extends the existing STL export functionality with geometry generation and merging capabilities.

## Component Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    ExportWizardActivity                      │
│  - Manages wizard flow and navigation                       │
│  - Coordinates between fragments                            │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│AttachmentType│    │Configuration │    │   Preview    │
│  Fragment    │    │   Fragment   │    │  Fragment    │
└──────────────┘    └──────────────┘    └──────────────┘
        │                     │                     │
        └─────────────────────┴─────────────────────┘
                              │
                              ▼
                  ┌──────────────────────┐
                  │  ExportConfiguration │
                  │  - Data class        │
                  │  - Holds all options │
                  └──────────────────────┘
                              │
                              ▼
                  ┌──────────────────────┐
                  │  GeometryGenerator   │
                  │  - Base generation   │
                  │  - Loop generation   │
                  │  - Hook generation   │
                  └──────────────────────┘
                              │
                              ▼
                  ┌──────────────────────┐
                  │   GeometryMerger     │
                  │  - Mesh combination  │
                  │  - Manifold check    │
                  └──────────────────────┘
                              │
                              ▼
                  ┌──────────────────────┐
                  │    STLExporter       │
                  │  - Binary STL write  │
                  │  - File validation   │
                  └──────────────────────┘
```

## Data Models

### ExportConfiguration
```kotlin
data class ExportConfiguration(
    val attachmentType: AttachmentType = AttachmentType.NONE,
    val baseConfig: BaseConfig? = null,
    val keyringConfig: KeyringConfig? = null,
    val hookConfig: HookConfig? = null,
    val scale: Float = 1.0f,
    val presetName: String? = null
)

enum class AttachmentType {
    NONE, BASE, KEYRING_LOOP, WALL_HOOK
}
```

### BaseConfig
```kotlin
data class BaseConfig(
    val shape: BaseShape = BaseShape.CIRCULAR,
    val width: Float = 50f,  // mm
    val depth: Float = 50f,  // mm
    val height: Float = 3f,  // mm
    val margin: Float = 5f   // mm from model edge
)

enum class BaseShape {
    CIRCULAR, RECTANGULAR, CUSTOM
}
```

### KeyringConfig
```kotlin
data class KeyringConfig(
    val position: LoopPosition = LoopPosition.TOP,
    val size: LoopSize = LoopSize.MEDIUM,
    val customPosition: Vector3? = null
)

enum class LoopPosition {
    TOP, SIDE_LEFT, SIDE_RIGHT, CUSTOM
}

enum class LoopSize(val innerDiameter: Float, val thickness: Float) {
    SMALL(5f, 2f),
    MEDIUM(8f, 2.5f),
    LARGE(12f, 3f)
}
```

### HookConfig
```kotlin
data class HookConfig(
    val type: HookType = HookType.KEYHOLE,
    val position: HookPosition = HookPosition.AUTO
)

enum class HookType {
    KEYHOLE,      // Keyhole slot for nail/screw
    HOLES,        // Two mounting holes
    HANGING_LOOP  // Integrated loop on back
}

enum class HookPosition {
    AUTO,   // System determines best position
    TOP,
    CENTER,
    CUSTOM
}
```

## Geometry Generation

### Base Generation Algorithm

1. **Analyze Model Bounds**
   - Calculate bounding box of clay model
   - Find lowest point (Z-min)
   - Determine model footprint in XY plane

2. **Generate Base Shape**
   - **Circular**: Create cylinder with radius = max(width, depth) / 2 + margin
   - **Rectangular**: Create box with dimensions = bounds + margin
   - **Custom**: Trace model outline at Z-min, offset by margin

3. **Position Base**
   - Align base top surface with model Z-min
   - Center base under model centroid

4. **Create Transition**
   - Generate smooth fillet between model and base (radius = 1-2mm)
   - Ensures printability without support

### Keyring Loop Generation

1. **Determine Attachment Point**
   - **TOP**: Highest point of model
   - **SIDE**: Point on side with best structural support
   - **CUSTOM**: User-specified position

2. **Generate Loop Geometry**
   - Create torus with specified inner diameter and thickness
   - Orient perpendicular to model surface at attachment point

3. **Create Reinforcement**
   - Generate triangular gusset connecting loop to model
   - Minimum 2mm thickness for strength
   - Smooth transition to avoid stress concentration

4. **Validate Clearance**
   - Ensure loop doesn't intersect with model
   - Verify minimum 1mm clearance for keyring insertion

### Wall Hook Generation

1. **Keyhole Slot**
   - Create inverted keyhole shape on back surface
   - Dimensions: 8mm wide head, 4mm narrow slot, 10mm depth
   - Position at center of mass for balanced hanging

2. **Mounting Holes**
   - Create two 4mm diameter holes
   - Spacing: 20-30mm apart depending on model size
   - Countersink for screw heads

3. **Hanging Loop**
   - Similar to keyring loop but positioned on back
   - Larger size (15mm inner diameter)
   - Reinforced attachment

## Mesh Operations

### Geometry Merger

The merger combines the clay model mesh with generated attachment geometry:

```kotlin
class GeometryMerger {
    fun merge(clayModel: Mesh, attachment: Mesh): Mesh {
        // 1. Combine vertex lists
        val vertices = clayModel.vertices + attachment.vertices
        
        // 2. Combine face lists (adjust indices)
        val faces = clayModel.faces + attachment.faces.map { 
            it.offset(clayModel.vertices.size) 
        }
        
        // 3. Remove internal faces at intersection
        val cleaned = removeInternalFaces(vertices, faces)
        
        // 4. Validate manifold
        if (!isManifold(cleaned)) {
            throw GeometryException("Merged mesh is not manifold")
        }
        
        return cleaned
    }
}
```

### Manifold Validation

Ensures exported STL is watertight:
- Each edge is shared by exactly 2 faces
- No duplicate vertices
- All normals point outward
- No self-intersections

## UI/UX Design

### Wizard Flow

**Step 1: Model Review**
- Display current clay model
- Show dimensions and vertex count
- Option to scale model

**Step 2: Attachment Selection**
- Radio buttons: None / Base / Keyring / Wall Hook
- Icon and description for each option
- "Learn More" links with examples

**Step 3: Configuration**
- Dynamic form based on selected attachment
- Real-time validation of inputs
- Suggested values based on model size

**Step 4: Preview**
- 3D view with orbit controls
- Attachment highlighted in different color
- Dimensions overlay
- "Regenerate" button if configuration changes

**Step 5: Export**
- Summary of all settings
- File name input
- Save location picker
- "Save as Preset" checkbox
- Export button

### Preview Rendering

Use existing OpenGL renderer with modifications:
- Render clay model in primary color
- Render attachments in accent color (semi-transparent)
- Add grid plane for scale reference
- Display dimensions as text overlays

## File Format

### STL Binary Format

```
UINT8[80]    – Header (unused)
UINT32       – Number of triangles

foreach triangle:
    REAL32[3] – Normal vector
    REAL32[3] – Vertex 1
    REAL32[3] – Vertex 2
    REAL32[3] – Vertex 3
    UINT16    – Attribute byte count (unused)
```

### Preset Storage

Store presets as JSON in app's private storage:

```json
{
  "presets": [
    {
      "name": "Small Keyring",
      "attachmentType": "KEYRING_LOOP",
      "keyringConfig": {
        "position": "TOP",
        "size": "SMALL"
      },
      "scale": 0.8
    }
  ]
}
```

## Error Handling

### Geometry Errors
- **Non-manifold result**: Retry with simplified attachment
- **Intersection failure**: Adjust attachment position automatically
- **Invalid dimensions**: Show validation error with suggested fix

### Export Errors
- **File write failure**: Check permissions, suggest alternative location
- **Out of memory**: Reduce mesh resolution, offer simplified export
- **Invalid mesh**: Run repair algorithm, warn user of potential issues

## Performance Considerations

### Preview Generation
- Generate attachment geometry on background thread
- Use simplified mesh for preview (reduce vertex count by 50%)
- Cache generated geometry until configuration changes
- Debounce slider inputs (300ms delay)

### Export Optimization
- Use binary STL format (smaller file size)
- Optimize vertex order for better compression
- Remove duplicate vertices
- Merge coplanar faces where possible

## Testing Strategy

### Unit Tests
- Geometry generation algorithms
- Mesh merging logic
- Manifold validation
- STL file writing

### Integration Tests
- Wizard navigation flow
- Configuration persistence
- Preview rendering
- File export end-to-end

### Manual Testing
- Test with various model sizes and shapes
- Verify printability with actual 3D prints
- Test on different Android devices
- Validate STL files in multiple slicers

## Implementation Considerations

### Existing Code Integration
- Extend current `STLExporter` class
- Reuse `ClayModel` mesh representation
- Integrate with existing file picker
- Maintain compatibility with direct export (bypass wizard)

### Dependencies
- No new external libraries required
- Use existing OpenGL ES for preview
- Leverage Kotlin coroutines for async operations
- Material Design components for wizard UI

### Backwards Compatibility
- Keep existing "Quick Export" option
- Wizard is opt-in via "Export with Options" menu item
- Old STL files remain compatible

## Future Enhancements

- Custom text engraving on base
- Automatic support structure generation
- Multi-part export (separate base and model)
- Integration with online 3D printing services
- AR preview using device camera
