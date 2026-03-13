# STL Export Enhancements - Requirements

## Overview

Clay sculptures like the owl model are difficult to balance when 3D printed. Users need the ability to add bases, stands, or attachment points (like keyring loops) during STL export to make their models practical for display or everyday use.

## User Stories

### US-1: Export Wizard
**As a** user exporting a clay model to STL  
**I want** a guided wizard interface  
**So that** I can easily configure export options without being overwhelmed

WHEN a user initiates STL export  
THE SYSTEM SHALL display a multi-step wizard interface

WHEN the wizard is displayed  
THE SYSTEM SHALL show progress indicators for each step

WHEN a user completes all wizard steps  
THE SYSTEM SHALL generate the STL file with selected options

### US-2: Base Platform Options
**As a** user creating a sculpture for display  
**I want** to add a flat base platform to my model  
**So that** it can stand upright on a shelf or desk

WHEN a user selects "Add Base" in the export wizard  
THE SYSTEM SHALL offer base type options: circular, rectangular, or custom shape

WHEN a user selects a base type  
THE SYSTEM SHALL allow adjustment of base dimensions (width, depth, height)

WHEN a user confirms base settings  
THE SYSTEM SHALL automatically merge the base with the model at the lowest point

WHEN the base is added  
THE SYSTEM SHALL ensure the combined model is manifold and printable

### US-3: Keyring Attachment
**As a** user creating a small sculpture for a keychain  
**I want** to add a keyring loop to my model  
**So that** it can be attached to keys or bags

WHEN a user selects "Add Keyring Loop" in the export wizard  
THE SYSTEM SHALL allow placement of the loop at top, side, or custom position

WHEN a user selects loop placement  
THE SYSTEM SHALL offer loop size options: small (5mm), medium (8mm), large (12mm)

WHEN a user confirms loop settings  
THE SYSTEM SHALL generate a reinforced loop attachment integrated with the model

WHEN the loop is added  
THE SYSTEM SHALL ensure the attachment point is structurally sound for printing

### US-4: Wall Hook Attachment
**As a** user creating a decorative piece  
**I want** to add a wall mounting hook to my model  
**So that** it can be hung on a wall

WHEN a user selects "Add Wall Hook" in the export wizard  
THE SYSTEM SHALL offer hook types: keyhole slot, mounting holes, or hanging loop

WHEN a user selects a hook type  
THE SYSTEM SHALL position it on the back of the model automatically

WHEN a user confirms hook settings  
THE SYSTEM SHALL integrate the mounting feature with the model geometry

### US-5: Preview Before Export
**As a** user configuring export options  
**I want** to see a real-time preview of my model with attachments  
**So that** I can verify the result before exporting

WHEN a user modifies any export option  
THE SYSTEM SHALL update the 3D preview in real-time

WHEN the preview is displayed  
THE SYSTEM SHALL highlight added features (base, loop, hook) in a different color

WHEN a user rotates the preview  
THE SYSTEM SHALL allow inspection from all angles

### US-6: Export Presets
**As a** frequent user of specific export configurations  
**I want** to save and reuse export presets  
**So that** I don't have to reconfigure options each time

WHEN a user completes export configuration  
THE SYSTEM SHALL offer an option to save as preset

WHEN a user saves a preset  
THE SYSTEM SHALL store the configuration with a user-defined name

WHEN a user starts the export wizard  
THE SYSTEM SHALL display available presets for quick selection

### US-7: Size Optimization
**As a** user preparing models for printing  
**I want** the system to suggest optimal sizes  
**So that** my model prints successfully with appropriate detail

WHEN a user enters the export wizard  
THE SYSTEM SHALL analyze the model dimensions

WHEN the model is very small (< 20mm)  
THE SYSTEM SHALL suggest scaling up for better printability

WHEN the model is very large (> 200mm)  
THE SYSTEM SHALL warn about print time and material usage

WHEN a user adds a keyring loop  
THE SYSTEM SHALL ensure minimum wall thickness of 2mm for strength

## Acceptance Criteria

### AC-1: Wizard Flow
- Wizard has clear steps: Model Review → Attachment Selection → Configuration → Preview → Export
- User can navigate back/forward through steps
- User can cancel at any point without saving
- Final step shows summary of all selections

### AC-2: Base Generation
- Base automatically aligns with model's lowest point
- Base thickness is at least 2mm for structural integrity
- Base edges are smooth and printable
- Model-to-base connection is seamless (no gaps)

### AC-3: Keyring Loop
- Loop opening is large enough for standard keyring (minimum 5mm inner diameter)
- Loop attachment is reinforced with minimum 2mm wall thickness
- Loop orientation is perpendicular to model for proper hanging

### AC-4: File Quality
- Exported STL is manifold (watertight)
- No inverted normals
- No intersecting geometry
- File size is optimized (binary STL format)

### AC-5: Performance
- Preview updates within 500ms of option change
- Export completes within 5 seconds for typical models
- Wizard remains responsive during preview generation

## Non-Functional Requirements

### NFR-1: Usability
- Wizard interface follows Material Design guidelines
- All options have helpful tooltips
- Preview is interactive (rotate, zoom, pan)

### NFR-2: Compatibility
- Generated STL files work with common slicers (Cura, PrusaSlicer, Simplify3D)
- Base and attachments are optimized for FDM printing
- Support structures are minimized

### NFR-3: Reliability
- System validates all geometry before export
- User receives clear error messages if export fails
- Automatic recovery if preview generation fails

## Out of Scope

- Advanced CAD operations (boolean operations beyond basic merge)
- Custom text engraving on base
- Multi-material export
- Automatic support structure generation
- Direct integration with slicing software
