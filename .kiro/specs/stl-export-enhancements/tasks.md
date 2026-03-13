# STL Export Enhancements - Tasks

## Phase 1: Data Models & Configuration

### Task 1.1: Create export configuration data models
- [ ] Create `export/ExportConfiguration.kt` with `ExportConfiguration`, `AttachmentType` enum
- [ ] Create `export/BaseConfig.kt` with `BaseConfig`, `BaseShape` enum
- [ ] Create `export/KeyringConfig.kt` with `KeyringConfig`, `LoopPosition`, `LoopSize` enums
- [ ] Create `export/HookConfig.kt` with `HookConfig`, `HookType`, `HookPosition` enums
- [ ] Create `export/PlacementResult.kt` with position, normal, rotation, scale fields
- **Expected outcome**: All config data classes compile and have sensible defaults

### Task 1.2: Create preset storage
- [ ] Create `export/PresetManager.kt` - save/load/delete presets as JSON in app private storage
- [ ] Use Gson or kotlinx.serialization for JSON handling
- [ ] Handle corrupted preset files gracefully (delete and recreate)
- **Expected outcome**: Presets persist across app restarts

---

## Phase 2: Geometry Generation

### Task 2.1: Base geometry generator
- [ ] Create `export/geometry/BaseGenerator.kt`
- [ ] Implement circular base: generate cylinder mesh from radius, height, segment count
- [ ] Implement rectangular base: generate box mesh from width, depth, height
- [ ] Implement custom base: trace model outline at Z-min, offset by margin, extrude downward
- [ ] Auto-calculate base dimensions from model bounding box + margin
- [ ] Position base aligned to model's lowest Y point
- [ ] Generate smooth fillet vertices at model-to-base transition
- [ ] Validate base height is minimum 2mm, clamp if user sets lower
- **Expected outcome**: Given a ClayModel and BaseConfig, produces a list of vertices and faces for the base

### Task 2.2: Keyring loop geometry generator
- [ ] Create `export/geometry/LoopGenerator.kt`
- [ ] Generate torus mesh from inner diameter and wall thickness
- [ ] Enforce minimum 2mm wall thickness, clamp if user scales below
- [ ] Generate reinforcement gusset connecting loop to model surface
- [ ] Orient loop perpendicular to surface normal at PlacementResult position
- [ ] Validate clearance: loop opening is not blocked by nearby model geometry
- **Expected outcome**: Given a ClayModel, KeyringConfig, and PlacementResult, produces loop mesh

### Task 2.3: Wall hook geometry generator
- [ ] Create `export/geometry/HookGenerator.kt`
- [ ] Implement keyhole slot: inverted keyhole shape (8mm head, 4mm slot)
- [ ] Implement mounting holes: two 4mm countersunk holes, 20-30mm spacing
- [ ] Implement hanging loop: large torus (15mm inner diameter) on back surface
- [ ] Auto-position at center of mass when HookPosition.AUTO
- **Expected outcome**: Given a ClayModel, HookConfig, and PlacementResult, produces hook mesh

### Task 2.4: Geometry merger
- [ ] Create `export/geometry/GeometryMerger.kt`
- [ ] Combine vertex and face lists, adjusting face indices for the attachment mesh
- [ ] Remove duplicate vertices within tolerance
- [ ] Implement manifold validation: each edge shared by exactly 2 faces, no inverted normals
- [ ] Return merged ClayModel ready for STL export
- **Expected outcome**: Merges clay model + attachment into single valid mesh

---

## Phase 3: Surface Picker & Placement

### Task 3.1: Surface picker for touch-to-place
- [ ] Create `export/placement/SurfacePicker.kt`
- [ ] Reuse existing `RayCaster.screenToWorldRay()` for ray generation
- [ ] Reuse existing `RayCaster.raycast()` for mesh intersection
- [ ] Return `PlacementResult` with hit position, surface normal, face index
- [ ] Handle miss (tap on empty space) by returning null
- **Expected outcome**: Tap on 3D preview returns exact surface point and normal

### Task 3.2: Placement controller - core gestures
- [ ] Create `export/placement/PlacementController.kt`
- [ ] Handle single tap → place attachment via SurfacePicker
- [ ] Handle drag → slide attachment along surface (continuous ray-cast)
- [ ] Handle two-finger rotate → update PlacementResult.rotation
- [ ] Handle pinch → update PlacementResult.scale within min/max bounds
- [ ] Distinguish between placement gestures (touch on attachment) and camera gestures (touch on empty space)
- **Expected outcome**: User can place, move, rotate, and resize attachments via touch

### Task 3.2b: Placement controller - secondary gestures
- [ ] Handle long press → snap to nearest preset position
- [ ] Handle double tap → remove attachment
- [ ] Handle three-finger tap → trigger undo via PlacementUndoManager
- **Expected outcome**: Full secondary gesture set working

### Task 3.2c: Snap guides
- [ ] Implement optional snap-to-grid overlay on model surface
- [ ] Toggle snap guides on/off in placement toolbar
- [ ] Snap attachment position to nearest grid point when enabled
- **Expected outcome**: Optional grid snapping for symmetrical placement

### Task 3.3: Placement validation
- [ ] Create `export/placement/PlacementValidator.kt`
- [ ] Check attachment doesn't intersect model interior
- [ ] Check minimum surface area at attachment point
- [ ] Check keyring loop opening clearance
- [ ] Check wall hook has sufficient flat area
- [ ] Return validation result with reason string if invalid
- [ ] Suggest nearest valid position when invalid
- **Expected outcome**: Validates placement and provides feedback

### Task 3.4: Undo/redo system
- [ ] Create `export/placement/PlacementUndoManager.kt`
- [ ] Track placement actions: place, move, rotate, resize, remove
- [ ] Maintain undo stack (max 20 actions) and redo stack
- [ ] New action clears redo stack
- [ ] Expose undo/redo methods and canUndo/canRedo state
- **Expected outcome**: Full undo/redo for all placement actions

---

## Phase 4: Wizard UI

### Task 4.1: Wizard activity and navigation
- [ ] Create `ui/wizard/ExportWizardActivity.kt` with ViewPager2 or FragmentStatePagerAdapter
- [ ] Apply Material Design 3 theming (Material Components library)
- [ ] Implement step indicator (dots or progress bar) showing current step
- [ ] Implement Next/Back/Cancel navigation buttons
- [ ] Preserve wizard state on device rotation (ViewModel)
- [ ] Pass current ClayModel into wizard via intent/ViewModel
- **Expected outcome**: Navigable 5-step wizard shell with Material Design styling

### Task 4.2: Step 1 - Model Review fragment
- [ ] Create `ui/wizard/ModelReviewFragment.kt` and layout
- [ ] Display 3D preview of current model (reuse ModelRenderer)
- [ ] Show model dimensions (bounding box in mm) and vertex/face count
- [ ] Add scale slider (0.5x to 3x) with live dimension update
- [ ] Show size warnings: < 20mm suggest scaling up, > 200mm warn about print time
- **Expected outcome**: User can review and scale their model

### Task 4.3: Step 2 - Attachment Selection fragment
- [ ] Create `ui/wizard/AttachmentSelectionFragment.kt` and layout
- [ ] Radio group: None / Base / Keyring Loop / Wall Hook
- [ ] Each option has icon and short description
- [ ] Show saved presets as quick-select chips at top
- [ ] Validate attachment combinations (base + loop OK, base + hook warns)
- **Expected outcome**: User selects attachment type or preset

### Task 4.4a: Step 3 - Configuration fragment shell
- [ ] Create `ui/wizard/ConfigurationFragment.kt` and layout
- [ ] Dynamic form container: swap visible controls based on selected attachment type
- [ ] Add undo/redo toolbar buttons wired to PlacementUndoManager
- [ ] Add tooltips (info icons) on each option explaining what it does and suggested values
- [ ] Small screen: collapse preset buttons to dropdown, enable precision mode
- **Expected outcome**: Configuration step shell that swaps content based on attachment type

### Task 4.4b: Step 3 - Base configuration
- [ ] Add base config controls: shape selector (circular/rectangular/custom), width/depth/height sliders, margin slider
- [ ] Auto-populate suggested dimensions from model bounding box
- [ ] Show live 3D preview of model with base (no touch-to-place needed, base auto-positions)
- **Expected outcome**: User can configure base shape and dimensions

### Task 4.4c: Step 3 - Keyring loop configuration & placement
- [ ] Add keyring config controls: size selector (small/medium/large)
- [ ] Add preset position buttons (top/left/right/front/back) around 3D preview
- [ ] Integrate PlacementController for touch-to-place on 3D preview
- [ ] Show visual feedback: green valid, red invalid with reason text
- **Expected outcome**: User can configure and place keyring loop on model

### Task 4.4d: Step 3 - Wall hook configuration & placement
- [ ] Add hook config controls: type selector (keyhole/holes/loop)
- [ ] Add preset position buttons around 3D preview
- [ ] Integrate PlacementController for touch-to-place on 3D preview
- [ ] Show visual feedback: green valid, red invalid with reason text
- **Expected outcome**: User can configure and place wall hook on model

### Task 4.5: Step 4 - Preview fragment
- [ ] Create `ui/wizard/PreviewFragment.kt` and layout
- [ ] Render merged model (clay + attachment) using ModelRenderer
- [ ] Highlight attachment in accent color (semi-transparent)
- [ ] Add orbit/zoom/pan controls
- [ ] Show dimensions overlay
- [ ] Add "Back to Edit" button to return to configuration step
- **Expected outcome**: User inspects final result from all angles

### Task 4.6: Step 5 - Export fragment
- [ ] Create `ui/wizard/ExportFragment.kt` and layout
- [ ] Show summary: attachment type, dimensions, vertex count, estimated file size
- [ ] File name input field
- [ ] "Save as Preset" checkbox with name input
- [ ] Export button triggers STLExporter with merged model
- [ ] Show progress indicator during export
- [ ] Show success message with file path, or error with explanation
- **Expected outcome**: User exports final STL file

---

## Phase 5: Integrate with Existing App

### Task 5.1: Add wizard entry point
- [ ] Add "Export with Options..." menu item in MainActivity alongside existing export
- [ ] Keep existing ExportDialog as "Quick Export" for backwards compatibility
- [ ] Launch ExportWizardActivity with current ClayModel
- **Expected outcome**: Both quick export and wizard export available from menu

### Task 5.2: Update STLExporter for merged models
- [ ] Extend `STLExporter.exportBinary()` to accept optional `ExportConfiguration`
- [ ] When configuration has attachments, run GeometryGenerator → GeometryMerger before export
- [ ] Add manifold validation step before writing STL
- [ ] Preserve existing export path (no config = current behavior)
- **Expected outcome**: STLExporter handles both simple and enhanced exports

### Task 5.3: Accessibility pass
- [ ] Add content descriptions to all wizard UI elements
- [ ] Ensure TalkBack navigates wizard steps correctly
- [ ] Add toolbar buttons as alternatives to all placement gestures
- [ ] Add keyboard/d-pad navigation for placement (arrow keys nudge, Enter confirms)
- [ ] Add haptic feedback on surface snap and invalid placement
- [ ] Verify 48dp minimum touch targets throughout
- **Expected outcome**: Wizard is fully accessible

---

## Phase 6: Testing

### Task 6.1: Unit tests - Geometry generation
- [ ] Test BaseGenerator: circular, rectangular, dimensions, alignment
- [ ] Test LoopGenerator: torus dimensions, orientation, reinforcement
- [ ] Test HookGenerator: keyhole dimensions, hole spacing, loop size
- [ ] Test GeometryMerger: vertex combining, face index adjustment, manifold validation
- [ ] Test ManifoldValidator: valid mesh, holes, duplicates, inverted normals
- **Expected outcome**: All geometry logic has unit test coverage

### Task 6.2: Unit tests - Configuration and placement
- [ ] Test ExportConfiguration defaults and serialization
- [ ] Test PresetManager save/load/delete/corruption handling
- [ ] Test SurfacePicker hit/miss scenarios
- [ ] Test PlacementValidator rules for each attachment type
- [ ] Test PlacementUndoManager stack behavior
- **Expected outcome**: All config and placement logic has unit test coverage

### Task 6.3: Integration tests - Wizard flow
- [ ] Test wizard navigation: forward, back, cancel, rotation preservation
- [ ] Test attachment selection and configuration persistence across steps
- [ ] Test end-to-end: select attachment → configure → preview → export
- [ ] Test preset save and load within wizard
- **Expected outcome**: Wizard flow works end-to-end

### Task 6.4: Integration tests - STL export
- [ ] Export test models with each attachment type, validate STL is manifold
- [ ] Verify exported file sizes are reasonable
- [ ] Test export with saved preset
- [ ] Test size warnings for tiny and huge models
- **Expected outcome**: Exported STL files are valid and printable

### Task 6.5: Create test data
- [ ] Create test .clay models: sphere, cube, owl, thin, flat, tiny, huge
- [ ] Store in `app/src/test/resources/` or `app/src/androidTest/assets/`
- [ ] Create expected output references for regression testing
- **Expected outcome**: Repeatable test data for all test suites

---

## Phase 7: Polish

### Task 7.1: Performance optimization
- [ ] Generate attachment geometry on background coroutine
- [ ] Use simplified mesh (50% vertices) for preview rendering
- [ ] Cache generated geometry until configuration changes
- [ ] Debounce slider inputs (300ms) before regenerating preview
- [ ] Profile memory usage, ensure < 200MB for typical models
- **Expected outcome**: Preview updates < 500ms, export < 5 seconds

### Task 7.2: Error handling
- [ ] Handle non-manifold merge result: retry with simplified attachment, show error if still fails
- [ ] Handle file write failure: check permissions, suggest alternative location
- [ ] Handle out of memory: offer simplified export with reduced mesh
- [ ] Handle preview generation failure: auto-recover by falling back to model-only preview, show toast
- [ ] Show clear, actionable error messages throughout wizard
- **Expected outcome**: All failure paths handled gracefully with user feedback

### Task 7.3: Manual testing and print validation
- [ ] Test on low-end (2GB RAM), mid-range, and high-end devices
- [ ] Test on tablet for layout verification
- [ ] Export owl model with base, keyring loop, and wall hook separately
- [ ] Import each STL into Cura and PrusaSlicer, verify no errors
- [ ] 3D print at least one model with base and one with keyring loop
- [ ] Verify base stands upright, keyring fits through loop
- **Expected outcome**: Real-world validation of printability
