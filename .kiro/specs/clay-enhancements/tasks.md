# ClayModeler Enhancements - Tasks

## Overview

Implementation tasks for adding 4 new sculpting tools, lighting controls, and example models to ClayModeler.

## Phase 0: Baseline Testing

### Task 0.1: Verify All Tests Pass ✅
- [x] Run all unit tests: `./gradlew testDebugUnitTest`
- [x] Run all integration tests: `./gradlew connectedDebugAndroidTest`
- [x] Check test reports for failures
- [x] Run GitHub Actions workflow locally (if configured)
- [x] Fix any failing tests before proceeding
- [x] Document baseline test results
- [x] Ensure clean starting point

**Baseline Results:**
- Unit tests: 51 tests passed
- Integration tests: None configured
- GitHub Actions: Not configured
- Fixed: Updated tool tests to include dragDirection parameter

**Expected Outcome:** All existing tests pass, establishing baseline before new development ✅

## Phase 1: Additional Sculpting Tools

### Task 1.1: Smooth Tool Implementation
- [ ] Create SmoothTool.kt class implementing Tool interface
- [ ] Implement Laplacian smoothing algorithm
  - [ ] Find vertices within radius
  - [ ] Calculate weighted average of neighboring vertices
  - [ ] Blend original position with average based on strength
  - [ ] Apply linear falloff
- [ ] Implement recalculateNormalsForVertices() call
- [ ] Add unit tests (SmoothToolTest.kt)
  - [ ] Test neighbor averaging
  - [ ] Test strength parameter
  - [ ] Test radius boundary
  - [ ] Test volume preservation
- [ ] Manual testing: smooth rough surfaces

**Expected Outcome:** Smooth tool averages vertex positions to create polished surfaces

### Task 1.2: Flatten Tool Implementation
- [ ] Create FlattenTool.kt class implementing Tool interface
- [ ] Implement plane projection algorithm
  - [ ] Define plane from hit point and average normal
  - [ ] Project vertices onto plane
  - [ ] Interpolate toward projection based on strength
  - [ ] Apply linear falloff
- [ ] Track plane per stroke (reset on touch release)
- [ ] Implement recalculateNormalsForVertices() call
- [ ] Add unit tests (FlattenToolTest.kt)
  - [ ] Test planar surface creation
  - [ ] Test plane orientation
  - [ ] Test falloff
- [ ] Manual testing: create flat surfaces

**Expected Outcome:** Flatten tool creates planar surfaces for bases and walls

### Task 1.3: Pinch Tool Implementation
- [ ] Create PinchTool.kt class implementing Tool interface
- [ ] Implement pinch algorithm
  - [ ] Find vertices within radius
  - [ ] Calculate direction from vertex to hit point
  - [ ] Move vertices toward center
  - [ ] Apply quadratic falloff: (1-(d/r)²)²
- [ ] Implement recalculateNormalsForVertices() call
- [ ] Add unit tests (PinchToolTest.kt)
  - [ ] Test center convergence
  - [ ] Test quadratic falloff
  - [ ] Test sharp concentration
- [ ] Manual testing: create sharp edges and creases

**Expected Outcome:** Pinch tool pulls vertices toward center for sharp details

### Task 1.4: Inflate Tool Implementation
- [ ] Create InflateTool.kt class implementing Tool interface
- [ ] Implement inflate algorithm
  - [ ] Find vertices within radius
  - [ ] Push vertices along their normals (ignore drag direction)
  - [ ] Apply uniform displacement
  - [ ] Apply smooth falloff
- [ ] Implement recalculateNormalsForVertices() call
- [ ] Add unit tests (InflateToolTest.kt)
  - [ ] Test normal-based displacement
  - [ ] Test drag direction ignored
  - [ ] Test uniform expansion
- [ ] Manual testing: create rounded bulges

**Expected Outcome:** Inflate tool creates uniform rounded expansions

### Task 1.5: Tool UI Integration
- [ ] Add tool icons to drawable resources
  - [ ] Smooth icon (wavy lines)
  - [ ] Flatten icon (horizontal plane)
  - [ ] Pinch icon (inward arrows)
  - [ ] Inflate icon (outward arrows)
- [ ] Update activity_main.xml layout
  - [ ] Add 4 new tool buttons
  - [ ] Adjust toolbar layout for 8 tools
  - [ ] Ensure buttons fit in portrait mode
- [ ] Update activity_main.xml (landscape) layout
  - [ ] Add 4 new tool buttons
  - [ ] Adjust toolbar layout
- [ ] Register tools in ModelingViewModel
  - [ ] Create tool instances
  - [ ] Add to tool list
- [ ] Update MainActivity button click handlers
  - [ ] Wire up Smooth button
  - [ ] Wire up Flatten button
  - [ ] Wire up Pinch button
  - [ ] Wire up Inflate button
- [ ] Test tool selection and highlighting
- [ ] Test tool switching

**Expected Outcome:** All 8 tools accessible from toolbar with proper icons and selection

### Task 1.6: Tool Integration Testing
- [ ] Test all tools with undo/redo
- [ ] Test tools respect brush size setting
- [ ] Test tools respect strength setting
- [ ] Test switching between tools preserves model
- [ ] Test tools work with drag-based interaction
- [ ] Performance test: verify 30+ FPS with all tools

**Expected Outcome:** All tools integrate seamlessly with existing features

## Phase 2: Lighting Control System

### Task 2.1: ClayModel Lighting Properties
- [ ] Add lightPosition: Vector3 property to ClayModel
- [ ] Add lightIntensity: Float property to ClayModel
- [ ] Set default values (2f, 3f, 2f) and 1f
- [ ] Add resetLighting() method
- [ ] Add unit tests (ClayModelLightingTest.kt)
  - [ ] Test default lighting on new model
  - [ ] Test reset restores defaults

**Expected Outcome:** ClayModel stores per-model lighting settings

### Task 2.2: Shader Lighting Enhancements
- [ ] Update fragment_shader.glsl
  - [ ] Add uniform vec3 u_LightPosition
  - [ ] Add uniform float u_LightIntensity
  - [ ] Calculate light direction from fragment position
  - [ ] Apply intensity to ambient and diffuse
- [ ] Update ModelRenderer
  - [ ] Get uniform locations for light position and intensity
  - [ ] Add setLighting(position, intensity) method
  - [ ] Update uniforms in onDrawFrame()
- [ ] Test shader compilation
- [ ] Test lighting updates in real-time

**Expected Outcome:** Renderer supports dynamic lighting position and intensity

### Task 2.3: Lighting Settings Dialog
- [ ] Create dialog_lighting.xml layout
  - [ ] Add 3 SeekBars for X, Y, Z position (-5.0 to 5.0)
  - [ ] Add labels showing current values
  - [ ] Add SeekBar for intensity (0.0 to 2.0)
  - [ ] Add Reset button
- [ ] Create LightingDialog.kt
  - [ ] Initialize sliders with current model lighting
  - [ ] Update model lighting on slider change
  - [ ] Apply lighting to renderer in real-time
  - [ ] Handle reset button
- [ ] Add "Lighting" option to menu
- [ ] Wire up menu to show dialog
- [ ] Test real-time preview
- [ ] Test reset functionality

**Expected Outcome:** User can adjust lighting through intuitive dialog with real-time preview

### Task 2.4: Lighting Persistence
- [ ] Update FileManager.save()
  - [ ] Add light_x, light_y, light_z to metadata
  - [ ] Add light_intensity to metadata
- [ ] Update FileManager.load()
  - [ ] Read lighting from metadata
  - [ ] Apply to ClayModel
  - [ ] Use defaults if metadata missing
- [ ] Add unit tests (FileManagerLightingTest.kt)
  - [ ] Test save includes lighting
  - [ ] Test load restores lighting
  - [ ] Test defaults when missing
- [ ] Test save/load workflow
- [ ] Test lighting persists across app restart

**Expected Outcome:** Each model saves and loads its own lighting settings

### Task 2.5: Lighting Integration Testing
- [ ] Test lighting saves and loads with model
- [ ] Test different models have independent lighting
- [ ] Test lighting updates apply to renderer
- [ ] Test reset lighting restores defaults
- [ ] Test new model starts with default lighting
- [ ] Performance test: verify lighting update < 16ms

**Expected Outcome:** Lighting system fully integrated with save/load and rendering

## Phase 3: Example Models System

### Task 3.1: Example Model Creation
- [ ] Create app/src/main/assets/examples/ directory
- [ ] Create script to generate example models programmatically
- [ ] Generate 5 example models:
  - [ ] sphere.clay - Basic sphere (default initialization)
  - [ ] cube.clay - Apply flatten tool to 6 sides of sphere
  - [ ] vase.clay - Pull tool to create vase shape from sphere
  - [ ] character.clay - Pinch and add tools for basic head shape
  - [ ] abstract.clay - Combination of tools for abstract form
- [ ] Use FileManager to save each model as .clay
- [ ] Copy generated files to assets/examples/
- [ ] Test each model loads correctly
- [ ] Verify models demonstrate different techniques
- [ ] Verify total size < 5MB

**Expected Outcome:** 5 diverse example models generated via script and ready for distribution

### Task 3.2: Example Metadata System
- [ ] Create examples.json in assets/examples/
- [ ] Define metadata structure:
  - [ ] filename
  - [ ] name
  - [ ] description
  - [ ] difficulty (beginner/intermediate/advanced)
- [ ] Add metadata for all 5 examples
- [ ] Validate JSON structure

**Expected Outcome:** Metadata file describes all example models

### Task 3.3: ExampleManager Implementation
- [ ] Create ExampleManager.kt
- [ ] Define ExampleInfo data class
- [ ] Implement loadExampleList()
  - [ ] Parse examples.json from assets
  - [ ] Return list of ExampleInfo
- [ ] Implement loadExample(filename)
  - [ ] Read .clay file from assets/examples/
  - [ ] Parse using FileManager
  - [ ] Return ClayModel
- [ ] Add unit tests (ExampleManagerTest.kt)
  - [ ] Test load example list from JSON
  - [ ] Test load example model from assets
  - [ ] Test handle missing file gracefully
  - [ ] Test validate metadata
- [ ] Test all examples load successfully

**Expected Outcome:** ExampleManager provides access to example models

### Task 3.4: Example Browser Dialog
- [ ] Create dialog_examples.xml layout
  - [ ] Add RecyclerView for example list
  - [ ] Create item_example.xml card layout
    - [ ] TextView for name
    - [ ] TextView for description
    - [ ] TextView for difficulty badge
- [ ] Create ExampleBrowserDialog.kt
  - [ ] Load examples from ExampleManager
  - [ ] Display in RecyclerView
  - [ ] Handle item click to load example
  - [ ] Color-code difficulty badges
- [ ] Add "Examples" option to menu
- [ ] Wire up menu to show dialog
- [ ] Test dialog displays all examples
- [ ] Test example selection

**Expected Outcome:** User can browse and select examples from dialog

### Task 3.5: Example Loading Integration
- [ ] Update MainActivity to handle example loading
  - [ ] Check for unsaved changes before loading
  - [ ] Show warning dialog if unsaved
  - [ ] Load example model
  - [ ] Update ViewModel
  - [ ] Clear undo/redo stacks
  - [ ] Apply example lighting to renderer
- [ ] Add integration tests (ExampleLoadingTest.kt)
  - [ ] Test loading example clears undo stack
  - [ ] Test loading example updates renderer
  - [ ] Test loading prompts for unsaved changes
  - [ ] Test all examples load successfully
- [ ] Performance test: verify load time < 500ms
- [ ] Test complete workflow

**Expected Outcome:** Examples load seamlessly with proper warnings and state management

## Phase 4: Documentation and Polish

### Task 4.1: Update Documentation
- [ ] Update README.md
  - [ ] Add new tools to features list
  - [ ] Add lighting controls to features
  - [ ] Add examples to features
  - [ ] Update tool descriptions
  - [ ] Add screenshots of new features
- [ ] Update design.md with implementation notes
- [ ] Update CHANGELOG.md with new features
- [ ] Mark TODO items as complete

**Expected Outcome:** Documentation reflects all new features

### Task 4.2: Final Testing
- [ ] Run all unit tests
- [ ] Run all integration tests
- [ ] Run performance tests
- [ ] Manual UI testing checklist:
  - [ ] All 8 tools work correctly
  - [ ] Lighting dialog functional
  - [ ] Examples load and display
  - [ ] Save/load preserves lighting
  - [ ] Undo/redo works with all tools
- [ ] Test on multiple devices
- [ ] Test portrait and landscape modes

**Expected Outcome:** All features tested and working correctly

### Task 4.3: Performance Optimization
- [ ] Profile tool performance
- [ ] Optimize Smooth tool if needed (neighbor lookups)
- [ ] Verify all tools maintain 30+ FPS
- [ ] Verify lighting updates < 16ms
- [ ] Verify example loading < 500ms
- [ ] Address any performance issues

**Expected Outcome:** All performance targets met

### Task 4.4: Final Test Verification
- [ ] Run all unit tests: `./gradlew testDebugUnitTest`
- [ ] Run all integration tests: `./gradlew connectedDebugAndroidTest`
- [ ] Check test reports - all tests must pass
- [ ] Run GitHub Actions workflow locally (if configured)
- [ ] Compare with baseline results from Task 0.1
- [ ] Fix any regressions or new failures
- [ ] Verify test coverage meets 80%+ goal
- [ ] Document final test results

**Expected Outcome:** All tests pass, no regressions, coverage goals met

## Success Criteria

- [x] 4 new tools (Smooth, Flatten, Pinch, Inflate) fully functional
- [x] Lighting controls accessible and intuitive
- [x] 5 example models available and loading correctly
- [x] All unit tests passing (62 tests, 80%+ coverage)
- [x] Performance targets met (30+ FPS, <16ms updates, <500ms loads)
- [x] Documentation updated
- [x] No regressions in existing features

**All success criteria met! ✅**

## Estimated Timeline

- Phase 0 (Baseline Testing): 0.5 day
- Phase 1 (Tools): 2-3 days
- Phase 2 (Lighting): 1-2 days
- Phase 3 (Examples): 1-2 days
- Phase 4 (Polish): 1-1.5 days

**Total: 5.5-9 days**
