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
- Store in SharedPreferences
- Load on app start
- Apply to renderer

#### Implementation Classes

**LightingSettings.kt:**
```kotlin
data class LightingSettings(
    var position: Vector3 = Vector3(2f, 3f, 2f),
    var intensity: Float = 1f
) {
    fun reset() {
        position = Vector3(2f, 3f, 2f)
        intensity = 1f
    }
    
    fun save(prefs: SharedPreferences)
    fun load(prefs: SharedPreferences)
}
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
- Test each tool's vertex modification algorithm
- Test lighting settings persistence
- Test example metadata parsing

### Integration Tests
- Test tool switching and application
- Test lighting changes with rendering
- Test example loading workflow

### Manual Testing
- Verify tool visual effects match expectations
- Verify lighting changes are intuitive
- Verify examples load correctly and demonstrate techniques

## Future Enhancements

- Tool presets (save/load tool configurations)
- Custom lighting colors
- Multiple light sources
- User-created example sharing
- Example model thumbnails
- Tool tutorials/guides
