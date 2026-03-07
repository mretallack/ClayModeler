# Clay Modeling App - Implementation Tasks

## Phase 1: Project Setup & Foundation (Week 1)

### Task 1.1: Project Initialization
- [ ] Create Android project structure with Gradle Kotlin DSL
- [ ] Set up `settings.gradle.kts` with repository configuration
- [ ] Create `gradle/libs.versions.toml` with all dependencies
- [ ] Configure `build.gradle.kts` (project and app module)
- [ ] Set up `local.properties` with SDK path
- [ ] Create `.gitignore` for Android project
- [ ] Verify project builds successfully

**Expected Outcome:** Empty Android project that compiles

### Task 1.2: Core Data Structures
- [ ] Create `Vector3` class (x, y, z with math operations)
- [ ] Create `Face` class (3 vertex indices)
- [ ] Create `ClayModel` class with vertices, faces, normals lists
- [ ] Implement `ClayModel.initialize()` for icosphere generation
- [ ] Add unit tests for Vector3 math operations
- [ ] Add unit tests for icosphere generation

**Expected Outcome:** ClayModel can generate initial sphere mesh

### Task 1.3: Basic UI Layout
- [ ] Create `MainActivity` with MVVM structure
- [ ] Create `ModelingViewModel` with LiveData
- [ ] Design XML layout with GLSurfaceView and tool buttons
- [ ] Implement portrait layout
- [ ] Implement landscape layout (tools on side)
- [ ] Add Material Design theme and colors
- [ ] Test orientation changes preserve state

**Expected Outcome:** UI layout displays correctly in both orientations

### Task 1.4: App Icon and Launcher
- [ ] Design app icon (clay sphere or sculpting tool)
- [ ] Create adaptive icon (foreground + background)
- [ ] Generate all required densities (mdpi to xxxhdpi)
- [ ] Configure launcher icon in AndroidManifest.xml
- [ ] Test icon appears correctly on different launchers
- [ ] Add icon to F-Droid metadata

**Expected Outcome:** App has professional icon

### Task 1.5: Storage Permissions Setup
- [ ] Add storage permissions to AndroidManifest.xml
- [ ] Implement runtime permission request for Android 6+
- [ ] Configure scoped storage for Android 10+
- [ ] Set up MediaStore API for Downloads access
- [ ] Handle permission denied gracefully
- [ ] Test on Android 8, 10, 12, 14

**Expected Outcome:** App can access storage correctly on all Android versions

## Phase 2: 3D Rendering (Week 2)

### Task 2.1: OpenGL Renderer Setup
- [ ] Create `ModelRenderer` implementing GLSurfaceView.Renderer
- [ ] Implement `onSurfaceCreated()` with OpenGL initialization
- [ ] Implement `onSurfaceChanged()` with viewport setup
- [ ] Create vertex and fragment shaders (Phong lighting)
- [ ] Compile and link shader program
- [ ] Set up projection and view matrices
- [ ] Add error handling for OpenGL operations

**Expected Outcome:** Black screen with OpenGL context initialized

### Task 2.2: Mesh Rendering
- [ ] Create Vertex Buffer Objects (VBO) for vertices
- [ ] Create VBO for normals
- [ ] Create Element Buffer Object (EBO) for faces
- [ ] Implement `onDrawFrame()` to render mesh
- [ ] Add terracotta clay color and lighting
- [ ] Test rendering with default sphere
- [ ] Verify 30+ FPS performance

**Expected Outcome:** Clay sphere renders with proper lighting

### Task 2.3: Camera Controls
- [ ] Implement camera orbit around model center
- [ ] Add pinch-to-zoom gesture detection
- [ ] Add single-finger drag for rotation
- [ ] Add two-finger drag for pan
- [ ] Implement camera matrix calculations
- [ ] Add smooth interpolation for gestures
- [ ] Test camera controls feel natural

**Expected Outcome:** User can view model from all angles

## Phase 3: Tool System (Week 3)

### Task 3.1: Tool Architecture
- [ ] Create `Tool` interface with `apply()` method
- [ ] Create `ToolEngine` to manage active tool
- [ ] Implement tool selection in ViewModel
- [ ] Add tool state to UI (highlight selected tool)
- [ ] Create tool cursor preview overlay
- [ ] Test tool switching

**Expected Outcome:** Tool selection system works

### Task 3.2: Ray Casting
- [ ] Implement screen-to-world ray calculation
- [ ] Create octree for spatial acceleration
- [ ] Implement ray-triangle intersection
- [ ] Find closest hit point on mesh
- [ ] Return hit point and surface normal
- [ ] Add unit tests for ray casting
- [ ] Optimize for performance

**Expected Outcome:** Touch input accurately hits mesh surface

### Task 3.3: Remove Clay Tool
- [ ] Implement `RemoveClayTool` class
- [ ] Find vertices within brush radius
- [ ] Calculate falloff curve (1 - (d/r)²)
- [ ] Move vertices inward along normals
- [ ] Update VBOs with modified vertices
- [ ] Recalculate normals for affected area
- [ ] Test tool feels responsive
- [ ] Add unit tests for vertex modification

**Expected Outcome:** User can carve clay from sphere

### Task 3.4: Add Clay Tool
- [ ] Implement `AddClayTool` class
- [ ] Move vertices outward along normals
- [ ] Use same falloff as Remove tool
- [ ] Smooth blending with existing surface
- [ ] Test tool builds up clay naturally
- [ ] Add unit tests

**Expected Outcome:** User can add clay to model

### Task 3.5: Pull Clay Tool
- [ ] Implement `PullClayTool` class
- [ ] Calculate drag direction from touch movement
- [ ] Blend drag direction with surface normal
- [ ] Move vertices along blended direction
- [ ] Maintain surface smoothness
- [ ] Test tool creates natural protrusions
- [ ] Add unit tests

**Expected Outcome:** User can pull clay outward

### Task 3.6: View Mode Tool
- [ ] Implement `ViewModeTool` class
- [ ] Disable mesh modification when active
- [ ] Enable all camera gestures
- [ ] Add zoom level indicator UI
- [ ] Add reset camera button (FAB)
- [ ] Test all gestures work in view mode
- [ ] Ensure pivot point is model center

**Expected Outcome:** User can examine model without editing

### Task 3.7: Tool Cursor Implementation
- [ ] Create circular overlay shader for cursor
- [ ] Position cursor at ray-cast hit point
- [ ] Scale cursor based on brush size setting
- [ ] Render cursor with 50% opacity white fill
- [ ] Add 2dp primary color border
- [ ] Update cursor in real-time as slider changes
- [ ] Hide cursor in View mode
- [ ] Test cursor visibility on different backgrounds

**Expected Outcome:** Visual preview of tool size on model surface

## Phase 4: Tool Settings (Week 4)

### Task 4.1: Brush Size Control
- [ ] Add slider UI for brush size
- [ ] Connect slider to ViewModel
- [ ] Update tool cursor size in real-time
- [ ] Apply size to tool calculations
- [ ] Add haptic feedback on change
- [ ] Test range (min to max)

**Expected Outcome:** Brush size adjusts tool effect area

### Task 4.2: Strength Control
- [ ] Add slider UI for strength
- [ ] Connect slider to ViewModel
- [ ] Apply strength multiplier to tools
- [ ] Test subtle vs dramatic changes
- [ ] Ensure smooth gradation

**Expected Outcome:** Strength controls how much clay moves

## Phase 5: Undo/Redo (Week 4)

### Task 5.1: History Management
- [ ] Implement undo stack in ViewModel (max 20)
- [ ] Implement redo stack
- [ ] Clone model on each tool application
- [ ] Add undo/redo buttons to UI
- [ ] Enable/disable buttons based on stack state
- [ ] Add unit tests for undo/redo logic
- [ ] Test memory usage with full stack

**Expected Outcome:** User can undo/redo actions

## Phase 6: File Management (Week 5)

### Task 6.1: Clay File Format
- [ ] Implement binary file writer with header
- [ ] Write metadata as JSON
- [ ] Write vertex, face, normal data
- [ ] Calculate and write CRC32 checksum
- [ ] Implement atomic file write (temp + rename)
- [ ] Add unit tests for file format
- [ ] Test round-trip save/load

**Expected Outcome:** Models save to .clay format

### Task 6.2: File Loading
- [ ] Implement binary file reader
- [ ] Validate magic number and version
- [ ] Verify checksum
- [ ] Parse JSON metadata
- [ ] Read vertex, face, normal data
- [ ] Reconstruct ClayModel object
- [ ] Handle corrupted files gracefully
- [ ] Add unit tests for loading
- [ ] Test with various file sizes

**Expected Outcome:** Models load from .clay files

### Task 6.3: Save Dialog
- [ ] Create save dialog UI
- [ ] Add filename text input
- [ ] Validate filename (no special chars)
- [ ] Show confirmation on success
- [ ] Handle save errors (storage full, etc.)
- [ ] Test save workflow

**Expected Outcome:** User can save models with custom names

### Task 6.4: Load Dialog
- [ ] Create load dialog UI
- [ ] List saved models from storage
- [ ] Generate thumbnails (128x128 PNG)
- [ ] Show creation date for each model
- [ ] Handle empty state (no models)
- [ ] Load selected model
- [ ] Handle load errors
- [ ] Test load workflow

**Expected Outcome:** User can load previously saved models

### Task 6.5: Auto-Save
- [ ] Implement auto-save coroutine (1 minute)
- [ ] Save to autosave.clay silently
- [ ] Track last modified time
- [ ] Check for autosave on app start
- [ ] Show restore dialog if autosave exists
- [ ] Delete autosave after manual save
- [ ] Test crash recovery
- [ ] Add unit tests for auto-save logic

**Expected Outcome:** Work auto-saves every minute

### Task 6.6: Thumbnail Generation
- [ ] Implement OpenGL screenshot capture
- [ ] Render model to offscreen framebuffer (256x256)
- [ ] Convert framebuffer to Bitmap
- [ ] Scale bitmap to 128x128
- [ ] Save as PNG to thumbnails directory
- [ ] Generate thumbnail on save
- [ ] Update thumbnail on model changes
- [ ] Handle thumbnail generation errors
- [ ] Add unit tests for thumbnail creation

**Expected Outcome:** Each saved model has a thumbnail preview

## Phase 7: STL Export (Week 6)

### Task 7.1: STL Export Implementation
- [ ] Implement coordinate system conversion (Y-up to Z-up)
- [ ] Implement scale to millimeters
- [ ] Calculate face normals
- [ ] Ensure counter-clockwise winding
- [ ] Write binary STL format (little-endian)
- [ ] Add progress tracking for large models
- [ ] Save to Downloads using MediaStore API
- [ ] Add unit tests for STL format
- [ ] Test with 3D printing software

**Expected Outcome:** Models export as valid STL files

### Task 7.2: Mesh Validation
- [ ] Implement manifold check (edge counting)
- [ ] Implement degenerate triangle detection
- [ ] Check normal orientation (outward-facing)
- [ ] Skip degenerate triangles on export
- [ ] Fix inverted normals automatically
- [ ] Add unit tests for validation
- [ ] Test with non-manifold meshes

**Expected Outcome:** Export validates and repairs meshes

### Task 7.3: Export Dialog
- [ ] Create export dialog UI
- [ ] Add size options (50/100/150/200/custom mm)
- [ ] Add quality settings (Low/Medium/High)
- [ ] Add validation checkbox
- [ ] Show progress bar during export
- [ ] Display success message with file location
- [ ] Handle export errors
- [ ] Test export workflow

**Expected Outcome:** User can export with custom settings

### Task 7.4: ASCII STL Export (Debug)
- [ ] Implement ASCII STL writer
- [ ] Format: "solid [name]" header
- [ ] Write facets with normals and vertices
- [ ] Add "endsolid" footer
- [ ] Add checkbox in export dialog (debug mode only)
- [ ] Test ASCII format with text editor
- [ ] Verify compatibility with slicing software

**Expected Outcome:** Debug builds can export ASCII STL for inspection

## Phase 8: Testing & Quality (Week 7)

### Task 8.1: Unit Tests
- [ ] Write ClayModel tests (90%+ coverage)
- [ ] Write Tool tests (90%+ coverage)
- [ ] Write FileManager tests (85%+ coverage)
- [ ] Write file format validation tests (95%+ coverage)
- [ ] Write STL export tests (90%+ coverage)
- [ ] Write ViewModel tests (85%+ coverage)
- [ ] Achieve 80%+ overall coverage
- [ ] All tests pass in CI

**Expected Outcome:** Comprehensive test coverage

### Task 8.2: Integration Tests
- [ ] Test save/load workflow
- [ ] Test STL export workflow
- [ ] Test undo/redo with multiple operations
- [ ] Test auto-save and restore
- [ ] Test orientation changes
- [ ] All integration tests pass

**Expected Outcome:** Critical workflows validated

### Task 8.3: Performance Testing
- [ ] Profile rendering performance (target 30+ FPS)
- [ ] Test with 50K vertex model
- [ ] Measure memory usage
- [ ] Test on low-end device (Android 8.0, 2GB RAM)
- [ ] Optimize bottlenecks
- [ ] Verify no memory leaks

**Expected Outcome:** App performs well on target devices

### Task 8.4: Device Testing
- [ ] Test on phone (5" screen)
- [ ] Test on tablet (10" screen)
- [ ] Test on different manufacturers (Samsung, Pixel, OnePlus)
- [ ] Test on Android 8.0, 10, 12, 14
- [ ] Fix device-specific issues

**Expected Outcome:** App works across devices

## Phase 9: Polish & UX (Week 8)

### Task 9.1: Accessibility
- [ ] Add content descriptions to all buttons
- [ ] Ensure 48dp minimum touch targets
- [ ] Verify color contrast ratios (WCAG AA)
- [ ] Test with TalkBack screen reader
- [ ] Add haptic feedback where appropriate

**Expected Outcome:** App is accessible

### Task 9.2: Error Handling
- [ ] Add error dialogs for all failure cases
- [ ] Show helpful error messages
- [ ] Handle low storage gracefully
- [ ] Handle corrupted files
- [ ] Test all error paths

**Expected Outcome:** Errors handled gracefully

### Task 9.3: ACRA Setup
- [ ] Add ACRA dependencies
- [ ] Configure ACRA in Application class
- [ ] Create crash report dialog
- [ ] Test crash reporting
- [ ] Verify no automatic upload
- [ ] Test manual report sharing

**Expected Outcome:** Crash reports saved locally

### Task 9.4: Documentation
- [ ] Write README.md with app description
- [ ] Add build instructions
- [ ] Document file formats
- [ ] Create user guide (basic usage)
- [ ] Add LICENSE file
- [ ] Add PRIVACY.md (no data collection)

**Expected Outcome:** Project is documented

### Task 9.5: Menu Implementation
- [ ] Create navigation drawer or popup menu
- [ ] Add menu items: New Model, Save, Load, Export, Settings, Help, About
- [ ] Implement menu item click handlers
- [ ] Connect menu to ViewModel actions
- [ ] Add menu icons
- [ ] Test menu on phone and tablet
- [ ] Handle back button to close menu

**Expected Outcome:** User can access all app functions via menu

### Task 9.6: New Model Action
- [ ] Add "New Model" menu item
- [ ] Show confirmation dialog if unsaved changes
- [ ] Clear current model and undo/redo stacks
- [ ] Reset camera to default position
- [ ] Generate new sphere
- [ ] Reset tool settings to defaults
- [ ] Test new model workflow

**Expected Outcome:** User can start fresh model

### Task 9.7: Settings Screen
- [ ] Create settings activity/fragment (if needed)
- [ ] Add preference for default sphere subdivision level
- [ ] Add preference for auto-save interval
- [ ] Add preference for default export size
- [ ] Save preferences using SharedPreferences
- [ ] Apply settings on app start
- [ ] Test settings persistence

**Expected Outcome:** User can configure app preferences (optional for MVP)

### Task 9.8: Help Screen
- [ ] Create help activity/dialog
- [ ] Add basic usage instructions
- [ ] Explain each tool with icons
- [ ] Add gesture guide (pinch, drag, etc.)
- [ ] Include tips for 3D printing
- [ ] Add scrollable text view
- [ ] Test help is accessible and clear

**Expected Outcome:** User can learn how to use the app

### Task 9.9: About Screen
- [ ] Create about dialog
- [ ] Display app name and version
- [ ] Add developer credits
- [ ] Include license information (GPL/MIT/Apache)
- [ ] Add link to source code repository
- [ ] Add privacy statement (no data collection)
- [ ] Test about dialog displays correctly

**Expected Outcome:** User can see app information

### Task 9.10: Snackbar Notifications
- [ ] Implement Snackbar helper class
- [ ] Add success snackbar for save/export
- [ ] Add error snackbar for failures
- [ ] Add warning snackbar for validation issues
- [ ] Configure 3-second auto-dismiss
- [ ] Add swipe-to-dismiss
- [ ] Test snackbars don't overlap
- [ ] Ensure snackbars are accessible

**Expected Outcome:** User receives feedback for actions

### Task 9.11: UI Animations
- [ ] Implement tool selection animation (150ms scale)
- [ ] Add dialog fade-in animation (200ms)
- [ ] Add dialog scale animation (0.8 → 1.0)
- [ ] Implement snackbar slide-up (250ms)
- [ ] Add button ripple effects
- [ ] Add undo/redo button pulse on action
- [ ] Test animations feel smooth
- [ ] Ensure animations respect accessibility settings

**Expected Outcome:** UI feels polished and responsive

### Task 9.12: Double-tap Reset in View Mode
- [ ] Implement double-tap gesture detection
- [ ] Reset camera to default position on double-tap
- [ ] Animate camera transition smoothly
- [ ] Only active in View mode
- [ ] Add haptic feedback on reset
- [ ] Test double-tap doesn't conflict with other gestures

**Expected Outcome:** User can quickly reset view

### Task 9.13: Zoom Level Indicator
- [ ] Create zoom level text overlay (bottom-right)
- [ ] Format as "1.0x" with 14sp white text
- [ ] Add text shadow for visibility
- [ ] Update in real-time during pinch
- [ ] Only show in View mode
- [ ] Fade out after 2 seconds of no interaction
- [ ] Test visibility on different backgrounds

**Expected Outcome:** User knows current zoom level

### Task 9.14: CI/CD Pipeline
- [ ] Create `.github/workflows/android-ci.yml`
- [ ] Configure Gradle wrapper validation
- [ ] Add build job (assembleDebug)
- [ ] Add lint job (lintDebug)
- [ ] Add test job (testDebugUnitTest)
- [ ] Add coverage reporting (Jacoco)
- [ ] Upload APK artifact
- [ ] Test CI pipeline

**Expected Outcome:** CI pipeline runs on push/PR

## Phase 10: Release Preparation

### Task 10.1: Final Testing
- [ ] Full regression test on all features
- [ ] Test all user workflows end-to-end
- [ ] Verify all acceptance criteria met
- [ ] Fix any remaining bugs

**Expected Outcome:** App is stable and complete

### Task 10.2: Release Build
- [ ] Configure release build type
- [ ] Set up signing configuration
- [ ] Build release APK
- [ ] Test release build
- [ ] Verify APK size is reasonable

**Expected Outcome:** Release APK ready

### Task 10.3: F-Droid Preparation
- [ ] Verify no proprietary dependencies
- [ ] Ensure reproducible builds
- [ ] Create F-Droid metadata
- [ ] Test F-Droid build process

**Expected Outcome:** App ready for F-Droid

### Task 10.4: Proguard/R8 Configuration
- [ ] Create proguard-rules.pro
- [ ] Add keep rules for OpenGL classes
- [ ] Add keep rules for ACRA
- [ ] Add keep rules for serialization classes
- [ ] Test release build with minification
- [ ] Verify app works after obfuscation
- [ ] Check APK size reduction

**Expected Outcome:** Release build is optimized and functional

### Task 10.5: App Signing Setup
- [ ] Generate release keystore
- [ ] Configure signing in build.gradle.kts
- [ ] Store keystore securely (not in repo)
- [ ] Document signing process
- [ ] Test signed release build
- [ ] Verify signature with jarsigner

**Expected Outcome:** App can be signed for release

### Task 10.6: Final Polish
- [ ] Review all UI strings for consistency
- [ ] Check all icons are correct size/density
- [ ] Verify all colors match design spec
- [ ] Test all animations are smooth
- [ ] Ensure no debug logs in release
- [ ] Check APK size is reasonable (<10MB)
- [ ] Verify app name and package name

**Expected Outcome:** App is polished and ready for users

## Summary

**Total Tasks:** ~120
**Estimated Duration:** 8-10 weeks
**Team Size:** 1 developer

**Key Milestones:**
- Week 2: Sphere renders and rotates
- Week 4: All tools working with undo/redo
- Week 6: Save/load and STL export complete
- Week 8: Polished and ready for release

**Risk Mitigation:**
- Week 1-2 is proof of concept (validate OpenGL performance)
- Can adjust scope if timeline slips
- Core features prioritized over polish

**Added Tasks (Complete Coverage):**
- Phase 1: App icon, storage permissions (2 tasks)
- Phase 3: Tool cursor implementation (1 task)
- Phase 6: Thumbnail generation (1 task)
- Phase 7: ASCII STL export (1 task)
- Phase 9: Menu, navigation, UI polish (9 tasks)
- Phase 10: Proguard, signing, final polish (3 tasks)

**Coverage:**
- All 16 user stories: ✅ 100%
- All NFRs: ✅ 100%
- All acceptance criteria: ✅ 100%
- UI/UX details: ✅ 100%
- Release preparation: ✅ 100%
