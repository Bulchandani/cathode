# Cathode Visual System

The Cathode UI emulates a CRT/tube television: phosphor-green primary, void background, monospace typography, scanline overlay, barrel-distorted viewport corners, pulsing focus glow, and channel-switch static for major transitions.

Density target: **IPTV Smarters** — high information density, multi-column layouts where they help, no lean-back-spacious whitespace.

## Color tokens

| Token | Hex | Usage |
|---|---|---|
| `Void` | `#0A0A0F` | Base background |
| `DimGrey` | `#1A1A22` | Surfaces (tile fills, cards, input backgrounds) |
| `PhosphorGreen` | `#4AF626` | Primary text, focus glow, accents |
| `PhosphorGreenDim` | `#2BA014` | Disabled states, secondary borders, dim text |
| `Amber` | `#FFB000` | Secondary accent (warnings, "now playing", highlight) |
| `AlarmRed` | `#FF3D3D` | Errors, critical state |
| `OffWhite` | `#E8E8E8` | Body text, value text in forms |
| `ScanlineWhite` | `#E8E8E8 @ 8%` | Scanline overlay |

## Typography

- **VT323** (Google Fonts, OFL) — display sizes (32sp+), big numbers, signage. Slightly degraded edges, very on-brand for CRT.
- **IBM Plex Mono** (IBM, OFL) — body and data (12–24sp). Clean, readable, consistent.

| Style | Family | Size | Weight | Letter spacing |
|---|---|---|---|---|
| `display` | VT323 | 56sp | Regular | 4sp |
| `headline` | VT323 | 32sp | Regular | 2sp |
| `section` | IBM Plex Mono | 20sp | Bold | 1sp |
| `body` | IBM Plex Mono | 16sp | Regular | 0 |
| `data` | IBM Plex Mono | 14sp | Regular | 0.5sp |
| `caption` | IBM Plex Mono | 12sp | Regular | 0.5sp |

## Glow (focused element)

Pulsing breathing glow on the focused element only.

- **Animation:** sine-wave alpha, 2.0s period
- **Alpha:** oscillates between **0.20 and 0.55**
- **Color:** `PhosphorGreen` at the chosen alpha
- **Blur radius:** 24dp
- **Spread:** 4dp outside the element
- **Border:** 1.5dp solid `PhosphorGreen` on the element itself when focused (steady, not pulsing)

Unfocused: 0.5dp `PhosphorGreenDim` border, no glow.

## Scanlines

Full vintage:

- 1px line every 3px
- alpha 8%
- color `ScanlineWhite`
- drawn full-screen, above content but below the player surface

## Curvature distortion

Barrel distortion at viewport edges. **API 31+ only** (RenderEffect shader). Below API 31 we degrade to a stronger vignette + darker corners.

- Distortion strength: ~3% radial bow at corners
- Applied to the entire viewport composition

## Vignette

Always on. Subtle radial darkening at corners.

- Inner radius: 60% of viewport
- Outer alpha: 35% black at corners

## Channel-switch static

120ms procedural noise overlay during major transitions:

- Hub → Player
- Hub → Settings
- Player ← Hub
- Channel change inside Live TV

While static plays, the next screen's content pre-renders in the background. When static ends, the new content snaps in with no further delay.

Implementation: Compose Canvas drawing random pixels each frame (frame rate uncapped, GPU clamps to 60fps).

## Motion vocabulary

| Motion | Duration | Easing |
|---|---|---|
| Focus border + glow ramp | 200ms | LinearEasing |
| Glow pulse | 2000ms | Sine |
| Channel-switch static | 120ms | Linear (constant noise) |
| Tile press feedback | 80ms | FastOutSlowIn |
| Player overlay auto-hide | After 5s idle, fade out 300ms | LinearEasing |

## Component library (Phase 1 implementation)

- `CathodeBox` — base container with stroke + glow when focused
- `CathodeButton` — phosphor-green button, glow on focus
- `CathodeField` — D-pad-friendly input with on-focus glow
- `CathodeStatic` — channel-switch noise overlay
- `CathodeScanlines` — strengthened scanline overlay
- `CathodeVignette` — corner darkening
- `CathodeCurvature` — barrel distortion (API 31+)

## Layout density (Smarters reference)

- Section gap: 16dp (was 24dp in early scaffold; tighten)
- Inner padding inside containers: 12dp horizontal, 8dp vertical
- Form field height: 56dp (D-pad reachable, finger-friendly)
- Channel/movie tile: 200dp wide × 120dp tall (16:9-ish)
- EPG cell: 240dp wide × 60dp tall per program
- Sidebar width: 240dp on 1920dp screens

## Devices and graceful degradation

| Device | Scanlines | Glow | Curvature | Static | Notes |
|---|---|---|---|---|---|
| Fire TV Stick 4K Max (Android 11) | Full | Full | Vignette fallback (no shader) | Full | Most common target |
| Fire TV Stick Lite (Android 9) | Full | Full | Vignette fallback | Reduced | Watch perf |
| Fire TV Cube (Android 11) | Full | Full | Vignette fallback | Full | High-power target |
| Modern Android tablet (Android 13+) | Full | Full | Real shader | Full | Best fidelity |
