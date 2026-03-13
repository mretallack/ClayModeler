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

#### GeometryGenerator Tests
```kotlin
class GeometryGeneratorTest {
    @Test
    fun `circular base generation creates correct vertex count`()
    
    @Test
    fun `rectangular base aligns with model bounds`()
    
    @Test
    fun `base height is minimum 2mm`()
    
    @Test
    fun `base margin is applied correctly`()
    
    @Test
    fun `keyring loop has correct inner diameter`()
    
    @Test
    fun `keyring loop wall thickness is minimum 2mm`()
    
    @Test
    fun `keyring loop orientation is perpendicular to surface`()
    
    @Test
    fun `wall hook keyhole dimensions are correct`()
    
    @Test
    fun `mounting holes are properly spaced`()
    
    @Test
    fun `hanging loop is larger than keyring loop`()
}
```

#### GeometryMerger Tests
```kotlin
class GeometryMergerTest {
    @Test
    fun `merge combines vertex lists correctly`()
    
    @Test
    fun `merge adjusts face indices after combining`()
    
    @Test
    fun `merge removes duplicate vertices`()
    
    @Test
    fun `merge result is manifold`()
    
    @Test
    fun `merge handles empty attachment mesh`()
    
    @Test
    fun `merge throws exception for non-manifold result`()
    
    @Test
    fun `merge preserves model normals`()
}
```

#### Manifold Validation Tests
```kotlin
class ManifoldValidatorTest {
    @Test
    fun `valid mesh passes manifold check`()
    
    @Test
    fun `mesh with hole fails manifold check`()
    
    @Test
    fun `mesh with duplicate vertices fails check`()
    
    @Test
    fun `mesh with inverted normals is detected`()
    
    @Test
    fun `each edge is shared by exactly two faces`()
    
    @Test
    fun `self-intersecting mesh is detected`()
}
```

#### STLExporter Tests
```kotlin
class STLExporterTest {
    @Test
    fun `binary STL header is 80 bytes`()
    
    @Test
    fun `triangle count is written correctly`()
    
    @Test
    fun `normals are calculated correctly`()
    
    @Test
    fun `vertices are written in correct order`()
    
    @Test
    fun `file size matches expected size`()
    
    @Test
    fun `export handles large meshes without OOM`()
    
    @Test
    fun `export validates mesh before writing`()
    
    @Test
    fun `export throws exception for invalid path`()
}
```

#### Configuration Tests
```kotlin
class ExportConfigurationTest {
    @Test
    fun `default configuration has no attachment`()
    
    @Test
    fun `base config validates minimum dimensions`()
    
    @Test
    fun `keyring config rejects invalid position`()
    
    @Test
    fun `hook config defaults to auto position`()
    
    @Test
    fun `configuration serializes to JSON correctly`()
    
    @Test
    fun `configuration deserializes from JSON correctly`()
}
```

### Integration Tests

#### Wizard Flow Tests
```kotlin
class ExportWizardFlowTest {
    @Test
    fun `wizard starts at model review step`()
    
    @Test
    fun `next button advances to attachment selection`()
    
    @Test
    fun `back button returns to previous step`()
    
    @Test
    fun `cancel button exits wizard without saving`()
    
    @Test
    fun `wizard skips configuration step when no attachment selected`()
    
    @Test
    fun `wizard shows preview after configuration`()
    
    @Test
    fun `wizard completes and exports file`()
    
    @Test
    fun `wizard state is preserved on rotation`()
}
```

#### Configuration Persistence Tests
```kotlin
class ConfigurationPersistenceTest {
    @Test
    fun `preset is saved to storage`()
    
    @Test
    fun `preset is loaded from storage`()
    
    @Test
    fun `multiple presets are stored correctly`()
    
    @Test
    fun `preset with same name overwrites existing`()
    
    @Test
    fun `deleted preset is removed from storage`()
    
    @Test
    fun `corrupted preset file is handled gracefully`()
}
```

#### Preview Rendering Tests
```kotlin
class PreviewRenderingTest {
    @Test
    fun `preview renders clay model`()
    
    @Test
    fun `preview renders attachment in different color`()
    
    @Test
    fun `preview updates when configuration changes`()
    
    @Test
    fun `preview supports orbit controls`()
    
    @Test
    fun `preview shows dimensions overlay`()
    
    @Test
    fun `preview handles rendering errors gracefully`()
    
    @Test
    fun `preview debounces rapid configuration changes`()
}
```

#### End-to-End Export Tests
```kotlin
class EndToEndExportTest {
    @Test
    fun `export owl model with circular base`()
    
    @Test
    fun `export owl model with keyring loop`()
    
    @Test
    fun `export owl model with wall hook`()
    
    @Test
    fun `export small model with size warning`()
    
    @Test
    fun `export large model with size warning`()
    
    @Test
    fun `export using saved preset`()
    
    @Test
    fun `exported STL is valid and manifold`()
    
    @Test
    fun `exported file size is reasonable`()
}
```

### Manual Testing

#### Model Variety Tests
- **Simple sphere**: Test with basic geometry
- **Complex owl**: Test with detailed organic model
- **Tall thin model**: Test base stability recommendations
- **Wide flat model**: Test keyring loop placement
- **Asymmetric model**: Test auto-positioning of hooks

#### Size Variation Tests
- **Tiny (10mm)**: Verify minimum size warnings
- **Small (30mm)**: Test keyring loop sizing
- **Medium (80mm)**: Test standard configurations
- **Large (150mm)**: Test base proportions
- **Huge (250mm)**: Verify performance and warnings

#### Attachment Combinations
- Base only
- Keyring loop only
- Wall hook only
- Base + keyring loop (invalid combination - should warn)
- Base + wall hook (invalid combination - should warn)

#### 3D Print Validation
For each attachment type, print and verify:
- **Base**: Model stands upright without tipping
- **Keyring loop**: Standard keyring fits through loop
- **Wall hook (keyhole)**: Hangs securely on nail/screw
- **Wall hook (holes)**: Screws fit mounting holes
- **Wall hook (loop)**: Supports model weight when hanging

#### Slicer Compatibility
Import exported STL files into:
- **Cura**: Verify no errors, slices correctly
- **PrusaSlicer**: Check manifold validation passes
- **Simplify3D**: Ensure proper mesh recognition
- **OrcaSlicer**: Validate geometry integrity

#### Device Testing
Test on:
- **Low-end device** (2GB RAM): Check performance, memory usage
- **Mid-range device** (4GB RAM): Verify smooth operation
- **High-end device** (8GB+ RAM): Test with complex models
- **Tablet**: Verify UI layout on larger screen
- **Different Android versions**: 10, 11, 12, 13, 14

#### Edge Cases
- **Empty model**: Should prevent export
- **Single vertex model**: Should show error
- **Non-manifold input**: Should attempt repair or warn
- **Extremely high poly count**: Should offer simplification
- **Model at origin**: Base generation should work
- **Model far from origin**: Should center before adding base
- **Inverted normals**: Should detect and offer to fix

#### UI/UX Testing
- **Wizard navigation**: All buttons work correctly
- **Input validation**: Invalid values show helpful errors
- **Preview responsiveness**: Smooth rotation and zoom
- **Progress indicators**: Show during long operations
- **Error messages**: Clear and actionable
- **Tooltips**: Helpful and accurate
- **Accessibility**: TalkBack navigation works

#### Performance Benchmarks
- **Preview generation**: < 500ms for typical model
- **Export time**: < 5 seconds for typical model
- **Memory usage**: < 200MB for typical model
- **UI responsiveness**: No frame drops during preview
- **File size**: Reasonable for mesh complexity

### Automated Testing Pipeline

#### Pre-commit Checks
```bash
./gradlew test                    # Run all unit tests
./gradlew ktlintCheck            # Code style validation
```

#### CI/CD Pipeline
```yaml
- Run unit tests
- Run integration tests
- Generate code coverage report (target: 80%+)
- Build APK
- Run instrumented tests on emulator
- Validate STL exports with external tool
- Performance regression tests
```

#### Test Coverage Goals
- **Unit tests**: 85%+ coverage
- **Integration tests**: 70%+ coverage
- **Critical paths**: 100% coverage (export, merge, validation)

### Test Data

#### Sample Models
Maintain repository of test models:
- `test_sphere.clay` - Simple 100-vertex sphere
- `test_cube.clay` - Basic cube with 8 vertices
- `test_owl.clay` - Complex 5000-vertex organic model
- `test_thin.clay` - Tall thin model (stability test)
- `test_flat.clay` - Wide flat model (base test)
- `test_tiny.clay` - 5mm model (size warning test)
- `test_huge.clay` - 300mm model (size warning test)

#### Expected Outputs
For each test model, maintain:
- Expected STL file (for regression testing)
- Expected vertex count after attachment
- Expected file size range
- Expected manifold validation result

### Regression Testing

After each change:
1. Export all test models with all attachment types
2. Compare output STL files with expected results
3. Validate manifold status hasn't changed
4. Check file sizes are within expected range
5. Verify no performance degradation

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
