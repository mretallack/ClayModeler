# Interactive Attachment Placement - Tasks

## Task 1: Add placement mode toggle to WizardPreviewView
- [x] Add `placementMode: Boolean` property (default false)
- [x] Add `onPlacementChanged: ((PlacementResult?) -> Unit)?` callback
- [x] Add a floating toggle button (camera/hand icon) overlaid on the GL view
- [x] When placement mode is active, show accent-colored border around preview
- [x] When placement mode is inactive, touch events go to camera controls (existing behavior)
- **Expected outcome**: Toggle button switches between camera and placement modes visually

## Task 2: Wire placement mode touch events to raycasting
- [x] In placement mode ACTION_DOWN: raycast from touch point to model, call `onPlacementChanged` with result
- [x] In placement mode ACTION_MOVE: raycast continuously, call `onPlacementChanged` with updated position (drag-to-slide)
- [x] In placement mode two-finger rotate: compute rotation angle delta, update `PlacementResult.rotation`
- [x] In placement mode pinch: update `PlacementResult.scale` (clamped 0.5–3.0)
- [x] Store the raw (un-merged) model separately so raycasts always hit the original model surface, not the attachment
- **Expected outcome**: Dragging finger across model in placement mode moves the attachment smoothly along the surface

## Task 3: Live preview rebuild on placement change
- [x] In ConfigurationFragment, set `onPlacementChanged` callback on the WizardPreviewView
- [x] When callback fires: update `vm.placement`, regenerate attachment mesh, merge with model, call `setModel()` on preview
- [x] Debounce rebuilds to max ~15fps (skip rebuild if <66ms since last) to keep UI smooth
- [x] Run geometry generation on background thread, upload to GL on main thread
- [x] Show validation text (valid/invalid) below preview after each placement change
- **Expected outcome**: Attachment visually follows the user's finger across the model surface in real-time

## Task 4: Rotation gesture for attachment orientation
- [x] Detect two-finger rotation angle (atan2 of finger delta) in placement mode
- [x] Apply rotation delta to `PlacementResult.rotation`
- [x] Rebuild preview with rotated attachment
- [x] Add haptic tick feedback on each 15° increment to give tactile rotation feel
- **Expected outcome**: Two-finger twist rotates the attachment around its surface normal

## Task 5: Update ConfigurationFragment to integrate placement mode
- [x] Keep existing preset buttons (Top/Left/Right/Front/Back) as quick-place shortcuts
- [x] Preset button tap sets placement AND switches preview to camera mode so user can inspect
- [x] Remove undo/redo toolbar (simplify UI — placement mode replaces it)
- [x] Show "Drag to place" hint text when placement mode is first activated
- [x] Hide hint after first successful placement
- **Expected outcome**: User can either tap a preset button or toggle to placement mode and drag

## Task 6: Test and install
- [x] Add unit test: raycast in placement mode returns valid PlacementResult
- [x] Add unit test: rotation delta correctly updates PlacementResult.rotation
- [x] Add unit test: scale clamp works within bounds
- [x] Verify existing tests still pass
- [x] Build and install on connected phone
- **Expected outcome**: All tests pass, app installed on phone with working drag-to-place
