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
    
    // OpenGL utilities
    implementation("androidx.opengl:opengl:1.0.0")
    
    // File I/O
    implementation("androidx.documentfile:documentfile:1.0.1")
    
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

## Testing Strategy

### Unit Tests

**Test Coverage Targets:**
- ClayModel: 90%+ coverage
- Tool implementations: 90%+ coverage
- FileManager: 85%+ coverage
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

## Future Enhancements

- Additional tools: Smooth, Flatten, Pinch
- Symmetry mode for mirrored editing
- Multiple clay colors/materials
- Texture painting
- Reference image overlay
- Cloud save/sync
- Model sharing
