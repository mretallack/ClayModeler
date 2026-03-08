# ClayModeler Enhancements - Design

## Architecture Overview

This enhancement adds three major features to ClayModeler:
1. Four new sculpting tools with specialized algorithms
2. Lighting control system with adjustable parameters
3. Example model library with asset management

## Component Design

### 1. Additional Sculpting Tools

#### Tool Implementations

**SmoothTool**
```kotlin
class SmoothTool : Tool {
    override fun apply(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float, dragDirection: Vector3) {
        // Algorithm:
        // 1. Find vertices within radius
        // 2. For each vertex, calculate average position of neighbors
        // 3. Move vertex toward average position based on strength
        // 4. Apply falloff for smooth transition
    }
}
```

Algorithm:
- Laplacian smoothing approach
- For each affected vertex, compute weighted average of neighboring vertices
- Blend original position with averaged position based on strength and falloff
- Preserve volume by limiting displacement magnitude

**FlattenTool**
```kotlin
class FlattenTool : Tool {
    private var flattenPlane: Plane? = null
    
    override fun apply(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float, dragDirection: Vector3) {
        // Algorithm:
        // 1. On first application, define plane from hit point and normal
        // 2. Project vertices onto plane
        // 3. Move vertices toward projected position based on strength
        // 4. Reset plane on touch release
    }
}
```

Algorithm:
- Define plane using hit point and average normal of affected vertices
- Project each vertex onto plane
- Interpolate between current position and projection based on strength
- Use linear falloff from brush center

**PinchTool**
```kotlin
class PinchTool : Tool {
    override fun apply(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float, dragDirection: Vector3) {
        // Algorithm:
        // 1. Find vertices within radius
        // 2. Calculate direction from vertex to hit point
        // 3. Move vertex toward hit point with strong falloff
        // 4. Use quadratic falloff for concentrated effect
    }
}
```

Algorithm:
- Pull vertices toward brush center (hit point)
- Use quadratic falloff: `(1 - (d/r)²)²` for sharp concentration
- Higher strength multiplier for dramatic effect
- Clamp maximum displacement to prevent collapse

**InflateTool**
```kotlin
class InflateTool : Tool {
    override fun apply(model: ClayModel, hitPoint: Vector3, strength: Float, radius: Float, dragDirection: Vector3) {
        // Algorithm:
        // 1. Find vertices within radius
        // 2. Push vertices along their normals uniformly
        // 3. Use smooth falloff for rounded expansion
        // 4. Similar to Add tool but ignores drag direction
    }
}
```

Algorithm:
- Push vertices along surface normals (like Add tool)
- Ignore drag direction - always use normals
- Use smooth falloff for rounded appearance
- Consistent displacement magnitude across brush area

#### UI Integration

**Toolbar Layout:**
```
[Remove] [Add] [Pull] [Smooth] [Flatten] [Pinch] [Inflate] [View]
```

**Tool Icons:**
- Smooth: Wavy lines smoothing out
- Flatten: Horizontal plane/ruler
- Pinch: Inward arrows converging
- Inflate: Outward arrows expanding

**Implementation:**
- Add buttons to `activity_main.xml` and `activity_main.xml` (landscape)
- Register tools in `ModelingViewModel`
- Update `ToolEngine` to support new tools
- Ensure all tools work with existing undo/redo system

### 2. Lighting Control System

#### Lighting Architecture

**Current Lighting (Fragment Shader):**
```glsl
// Fixed light direction
vec3 lightDir = normalize(vec3(0.5, 1.0, 0.3));
float diff = max(dot(normal, lightDir), 0.0);
vec3 ambient = 0.3 * color;
vec3 diffuse = diff * color;
vec3 finalColor = ambient + diffuse;
```

**Enhanced Lighting:**
```glsl
uniform vec3 u_LightPosition;  // Light position in world space
uniform float u_LightIntensity; // 0.0 to 2.0

// Calculate light direction from fragment position
vec3 lightDir = normalize(u_LightPosition - v_Position);
float diff = max(dot(normal, lightDir), 0.0);

vec3 ambient = 0.3 * color * u_LightIntensity;
vec3 diffuse = diff * color * u_LightIntensity;
vec3 finalColor = ambient + diffuse;
```

#### Lighting Settings UI

**Settings Dialog:**
```xml
<LinearLayout>
    <TextView text="Light Position" />
    <SeekBar id="slider_light_x" /> <!-- -5.0 to 5.0 -->
    <SeekBar id="slider_light_y" /> <!-- -5.0 to 5.0 -->
    <SeekBar id="slider_light_z" /> <!-- -5.0 to 5.0 -->
    
    <TextView text="Light Intensity" />
    <SeekBar id="slider_light_intensity" /> <!-- 0.0 to 2.0 -->
    
    <Button text="Reset to Defaults" />
</LinearLayout>
```

**Default Values:**
- Position: (2.0, 3.0, 2.0)
- Intensity: 1.0

**Persistence:**
- Store in ClayModel (per-model lighting)
- Save/load with .clay file
- New models start with default lighting
- Each model remembers its optimal lighting setup

#### Implementation Classes

**ClayModel.kt Updates:**
```kotlin
class ClayModel {
    // Existing fields...
    var lightPosition: Vector3 = Vector3(2f, 3f, 2f)
    var lightIntensity: Float = 1f
    
    fun resetLighting() {
        lightPosition = Vector3(2f, 3f, 2f)
        lightIntensity = 1f
    }
}
```

**FileManager.kt Updates:**
```kotlin
// Save lighting to .clay file metadata
metadata["light_x"] = model.lightPosition.x.toString()
metadata["light_y"] = model.lightPosition.y.toString()
metadata["light_z"] = model.lightPosition.z.toString()
metadata["light_intensity"] = model.lightIntensity.toString()

// Load lighting from .clay file metadata
model.lightPosition = Vector3(
    metadata["light_x"]?.toFloatOrNull() ?: 2f,
    metadata["light_y"]?.toFloatOrNull() ?: 3f,
    metadata["light_z"]?.toFloatOrNull() ?: 2f
)
model.lightIntensity = metadata["light_intensity"]?.toFloatOrNull() ?: 1f
```

**ModelRenderer Updates:**
- Add uniforms for light position and intensity
- Update shader to use uniforms
- Add methods to update lighting parameters
- Apply lighting changes in `onDrawFrame()`

### 3. Example Models System

#### Asset Management

**Example Model Storage:**
```
app/src/main/assets/examples/
├── sphere.clay          # Basic sphere (starting model)
├── cube.clay            # Cube with smooth edges
├── vase.clay            # Simple vase shape
├── character.clay       # Basic character head
├── abstract.clay        # Abstract sculpture
└── examples.json        # Metadata
```

**examples.json:**
```json
{
  "examples": [
    {
      "filename": "sphere.clay",
      "name": "Basic Sphere",
      "description": "Starting point - practice basic tools",
      "difficulty": "beginner"
    },
    {
      "filename": "cube.clay",
      "name": "Rounded Cube",
      "description": "Demonstrates flatten and smooth tools",
      "difficulty": "beginner"
    },
    {
      "filename": "vase.clay",
      "name": "Simple Vase",
      "description": "Shows pull and smooth techniques",
      "difficulty": "intermediate"
    },
    {
      "filename": "character.clay",
      "name": "Character Head",
      "description": "Basic character modeling with pinch details",
      "difficulty": "intermediate"
    },
    {
      "filename": "abstract.clay",
      "name": "Abstract Form",
      "description": "Creative use of all tools",
      "difficulty": "advanced"
    }
  ]
}
```

#### Example Browser UI

**ExampleBrowserDialog.kt:**
```kotlin
class ExampleBrowserDialog(context: Context, onLoad: (String) -> Unit) : Dialog(context) {
    // RecyclerView with example cards
    // Each card shows: name, description, difficulty badge
    // Tap to load (with unsaved work warning)
}
```

**Layout:**
```xml
<RecyclerView>
    <!-- Item layout -->
    <CardView>
        <TextView id="example_name" />
        <TextView id="example_description" />
        <TextView id="example_difficulty" /> <!-- Badge: Beginner/Intermediate/Advanced -->
    </CardView>
</RecyclerView>
```

#### Example Loading Flow

```
User taps "Examples" menu
    ↓
Check if current model has unsaved changes
    ↓
If unsaved: Show warning dialog
    ↓
Display ExampleBrowserDialog
    ↓
User selects example
    ↓
Load .clay file from assets
    ↓
Parse and create ClayModel
    ↓
Update ViewModel
    ↓
Clear undo/redo stacks
    ↓
Render new model
```

#### Implementation Classes

**ExampleManager.kt:**
```kotlin
class ExampleManager(private val context: Context) {
    data class ExampleInfo(
        val filename: String,
        val name: String,
        val description: String,
        val difficulty: String
    )
    
    fun loadExampleList(): List<ExampleInfo> {
        // Parse examples.json from assets
    }
    
    fun loadExample(filename: String): ClayModel {
        // Load .clay file from assets/examples/
        // Parse using FileManager
    }
}
```

**Menu Integration:**
- Add "Examples" option to menu
- Show ExampleBrowserDialog on selection
- Handle loading with unsaved work check

## Data Flow

### Tool Application Flow
```
User drags with new tool
    ↓
MainActivity.handleTouchEvent()
    ↓
Calculate drag direction
    ↓
Tool.apply(model, hitPoint, strength, radius, dragDirection)
    ↓
Tool-specific algorithm modifies vertices
    ↓
model.recalculateNormalsForVertices()
    ↓
renderer.updateModel()
    ↓
Render updated geometry
```

### Lighting Update Flow
```
User adjusts lighting slider
    ↓
LightingSettingsDialog updates LightingSettings
    ↓
Save to SharedPreferences
    ↓
Pass to ModelRenderer
    ↓
Update shader uniforms
    ↓
Re-render with new lighting
```

### Example Loading Flow
```
User selects example
    ↓
ExampleManager.loadExample(filename)
    ↓
Read .clay from assets
    ↓
FileManager.parse()
    ↓
Create ClayModel
    ↓
ViewModel.setModel()
    ↓
Clear undo/redo
    ↓
Renderer displays example
```

## UI/UX Considerations

### Tool Selection
- Toolbar may become crowded with 8 tools
- Consider scrollable toolbar or tool categories
- Maintain consistent tool button size and spacing

### Lighting Controls
- Real-time preview essential for good UX
- Sliders should have clear labels and value indicators
- Reset button should be prominent

### Example Browser
- Clear visual hierarchy (name > description > difficulty)
- Difficulty badges with color coding (green/yellow/red)
- Preview thumbnails would enhance UX (future enhancement)

## Performance Considerations

### Tool Performance
- Smooth tool requires neighbor lookups - use spatial indexing
- Flatten tool needs plane calculation - cache per stroke
- All tools must maintain 30+ FPS on target devices

### Lighting Performance
- Uniform updates are cheap (negligible overhead)
- Real-time preview should not impact frame rate

### Example Loading
- Asset loading is I/O bound
- Load examples on background thread
- Show loading indicator for large models

## Testing Strategy

### Unit Tests

#### Tool Tests

**SmoothToolTest.kt:**
```kotlin
class SmoothToolTest : FunSpec({
    test("smooth tool averages neighboring vertices") {
        // Create model with known vertex positions
        // Apply smooth tool
        // Verify vertices moved toward average of neighbors
    }
    
    test("smooth tool respects strength parameter") {
        // Apply with strength 0.5
        // Verify displacement is half of maximum
    }
    
    test("smooth tool respects radius") {
        // Apply tool
        // Verify only vertices within radius are affected
    }
    
    test("smooth tool preserves overall volume") {
        // Calculate volume before smoothing
        // Apply smooth tool multiple times
        // Verify volume change is minimal
    }
})
```

**FlattenToolTest.kt:**
```kotlin
class FlattenToolTest : FunSpec({
    test("flatten tool creates planar surface") {
        // Apply flatten tool
        // Verify affected vertices lie on same plane
    }
    
    test("flatten tool uses hit point normal for plane") {
        // Apply on known surface
        // Verify plane orientation matches surface normal
    }
    
    test("flatten tool respects falloff") {
        // Apply tool
        // Verify vertices at edge are less flattened
    }
})
```

**PinchToolTest.kt:**
```kotlin
class PinchToolTest : FunSpec({
    test("pinch tool pulls vertices toward center") {
        // Apply pinch tool
        // Verify all vertices moved toward hit point
    }
    
    test("pinch tool uses quadratic falloff") {
        // Apply tool
        // Verify displacement follows (1-(d/r)²)² curve
    }
    
    test("pinch tool creates sharp concentration") {
        // Apply pinch
        // Verify center vertices moved more than edge vertices
    }
})
```

**InflateToolTest.kt:**
```kotlin
class InflateToolTest : FunSpec({
    test("inflate tool pushes along normals") {
        // Apply inflate tool
        // Verify vertices moved along their normals
    }
    
    test("inflate tool ignores drag direction") {
        // Apply with drag direction
        // Verify movement is along normals, not drag
    }
    
    test("inflate tool creates uniform expansion") {
        // Apply tool
        // Verify all affected vertices displaced equally
    }
})
```

#### Lighting Tests

**ClayModelLightingTest.kt:**
```kotlin
class ClayModelLightingTest : FunSpec({
    test("new model has default lighting") {
        val model = ClayModel()
        model.initialize(3)
        
        model.lightPosition shouldBe Vector3(2f, 3f, 2f)
        model.lightIntensity shouldBe 1f
    }
    
    test("reset lighting restores defaults") {
        val model = ClayModel()
        model.lightPosition = Vector3(5f, 5f, 5f)
        model.lightIntensity = 2f
        
        model.resetLighting()
        
        model.lightPosition shouldBe Vector3(2f, 3f, 2f)
        model.lightIntensity shouldBe 1f
    }
})
```

**FileManagerLightingTest.kt:**
```kotlin
class FileManagerLightingTest : FunSpec({
    test("save includes lighting in metadata") {
        val model = ClayModel()
        model.lightPosition = Vector3(1f, 2f, 3f)
        model.lightIntensity = 1.5f
        
        // Save model
        // Parse saved file
        // Verify metadata contains light_x, light_y, light_z, light_intensity
    }
    
    test("load restores lighting from metadata") {
        // Create .clay file with lighting metadata
        val model = fileManager.load("test")
        
        model.lightPosition shouldBe Vector3(1f, 2f, 3f)
        model.lightIntensity shouldBe 1.5f
    }
    
    test("load uses defaults if lighting missing") {
        // Create .clay file without lighting metadata
        val model = fileManager.load("test")
        
        model.lightPosition shouldBe Vector3(2f, 3f, 2f)
        model.lightIntensity shouldBe 1f
    }
})
```

#### Example Manager Tests

**ExampleManagerTest.kt:**
```kotlin
class ExampleManagerTest : FunSpec({
    test("load example list from JSON") {
        // Parse examples.json
        // Verify correct number of examples
        // Verify metadata fields present
    }
    
    test("load example model from assets") {
        // Load sphere.clay
        // Verify ClayModel created
        // Verify vertices and faces loaded
    }
    
    test("handle missing example file gracefully") {
        // Attempt to load non-existent file
        // Verify exception thrown or null returned
    }
    
    test("validate example metadata") {
        // Load examples
        // Verify all have required fields
        // Verify difficulty values are valid
    }
})
```

### Integration Tests

#### Tool Integration Tests

**ToolIntegrationTest.kt:**
```kotlin
class ToolIntegrationTest : FunSpec({
    test("all tools work with undo/redo") {
        // Apply each new tool
        // Undo
        // Verify model restored
        // Redo
        // Verify tool reapplied
    }
    
    test("tools respect brush size setting") {
        // Set brush size to 0.5
        // Apply each tool
        // Verify affected area matches brush size
    }
    
    test("tools respect strength setting") {
        // Set strength to 0.3
        // Apply each tool
        // Verify displacement scaled appropriately
    }
    
    test("switching between tools preserves model") {
        // Apply tool A
        // Switch to tool B
        // Apply tool B
        // Verify both modifications present
    }
})
```

#### Lighting Integration Tests

**LightingIntegrationTest.kt:**
```kotlin
class LightingIntegrationTest : FunSpec({
    test("lighting saves and loads with model") {
        // Set custom lighting
        // Save model
        // Load model
        // Verify lighting restored
    }
    
    test("different models have independent lighting") {
        // Set lighting for model A
        // Save model A
        // Create new model B with different lighting
        // Load model A
        // Verify model A lighting restored (not model B's)
    }
    
    test("lighting updates apply to renderer") {
        // Change light position
        // Verify renderer receives update
        // Verify shader uniforms updated
    }
    
    test("reset lighting restores defaults") {
        // Modify lighting
        // Reset
        // Verify renderer uses default values
    }
    
    test("new model starts with default lighting") {
        // Create new model
        // Verify default lighting applied to renderer
    }
})
```

#### Example Loading Integration Tests

**ExampleLoadingTest.kt:**
```kotlin
class ExampleLoadingTest : FunSpec({
    test("loading example clears undo stack") {
        // Make modifications
        // Load example
        // Verify undo stack empty
    }
    
    test("loading example updates renderer") {
        // Load example
        // Verify renderer displays new model
        // Verify vertex count matches example
    }
    
    test("loading example prompts for unsaved changes") {
        // Modify model
        // Attempt to load example
        // Verify warning dialog shown
    }
    
    test("all example files load successfully") {
        // Iterate through all examples
        // Load each one
        // Verify no errors
        // Verify valid models created
    }
})
```

### UI Tests (Manual)

#### Tool UI Tests
- [ ] All 8 tools visible in toolbar (portrait and landscape)
- [ ] Tool icons are distinct and recognizable
- [ ] Selected tool highlights correctly
- [ ] Tool cursor updates for each tool
- [ ] Tools respond to touch immediately

#### Lighting UI Tests
- [ ] Lighting dialog accessible from menu
- [ ] Sliders update lighting in real-time
- [ ] Value labels show current settings
- [ ] Reset button restores defaults
- [ ] Dialog dismisses and saves settings

#### Example Browser UI Tests
- [ ] Examples menu option present
- [ ] Example browser displays all examples
- [ ] Example cards show name, description, difficulty
- [ ] Difficulty badges color-coded correctly
- [ ] Tapping example loads it
- [ ] Unsaved work warning appears when needed

### Performance Tests

**ToolPerformanceTest.kt:**
```kotlin
class ToolPerformanceTest : FunSpec({
    test("smooth tool maintains 30+ FPS") {
        // Create high-poly model (10k vertices)
        // Apply smooth tool continuously
        // Measure frame time
        // Verify < 33ms per frame
    }
    
    test("all tools complete within 16ms") {
        // Apply each tool
        // Measure execution time
        // Verify < 16ms (60 FPS target)
    }
})
```

**LightingPerformanceTest.kt:**
```kotlin
class LightingPerformanceTest : FunSpec({
    test("lighting update completes within 16ms") {
        // Change lighting
        // Measure update time
        // Verify < 16ms
    }
})
```

**ExampleLoadingPerformanceTest.kt:**
```kotlin
class ExampleLoadingPerformanceTest : FunSpec({
    test("example loads within 500ms") {
        // Load each example
        // Measure load time
        // Verify < 500ms
    }
})
```

### Test Coverage Goals

- **Unit Tests:** 80%+ coverage for new classes
- **Integration Tests:** All major workflows covered
- **UI Tests:** All user-facing features manually verified
- **Performance Tests:** All performance requirements validated

### Continuous Integration

- Run unit tests on every commit
- Run integration tests on pull requests
- Performance tests run nightly
- UI tests run before releases

## Future Enhancements

- Tool presets (save/load tool configurations)
- Custom lighting colors
- Multiple light sources
- User-created example sharing
- Example model thumbnails
- Tool tutorials/guides
