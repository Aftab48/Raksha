# Raksha — Design System

## Design Philosophy

Designed for a panicked user operating with one thumb in low visibility.
Every decision prioritises clarity, speed, and calm confidence over decoration.
The interface should feel like a trusted, silent guardian — not a consumer app.

---

## Theme

**Mode:** Dark only
**Tone:** Deep, organic calm. Minimal. High contrast. Purposeful.
**Personality:** Confident, quiet, protective. Never alarming in idle state. Instantly alarming in active state.

---

## Color Palette

### Base
| Token | Hex | Usage |
|---|---|---|
| `color-background` | `#050F0D` | Primary background |
| `color-surface` | `#0A1A16` | Cards, sheets, bottom bars |
| `color-surface-elevated` | `#112420` | Modals, elevated surfaces |
| `color-border` | `#1A3830` | Dividers, outlines |

### Primary (Cyan/Green — Protection)
| Token | Hex | Usage |
|---|---|---|
| `color-primary` | `#00E5B0` | Shield toggle active, CTAs, links |
| `color-primary-dim` | `#00A87D` | Pressed states |
| `color-primary-subtle` | `#00E5B015` | Background tint on active states |

### Danger (Red — Emergency)
| Token | Hex | Usage |
|---|---|---|
| `color-danger` | `#FF3B3B` | SOS button, active alert states |
| `color-danger-dim` | `#C42B2B` | Pressed SOS |
| `color-danger-subtle` | `#FF3B3B15` | Alert card backgrounds |
| `color-danger-pulse` | `#FF3B3B` | Pulsing animation during active SOS |

### Safety (Green — Safe routes/zones)
| Token | Hex | Usage |
|---|---|---|
| `color-safe` | `#00C97A` | Safe route indicator, resolved events |
| `color-safe-subtle` | `#00C97A15` | Safe zone heatmap tint |

### Warning (Amber — Moderate risk)
| Token | Hex | Usage |
|---|---|---|
| `color-warning` | `#F5A623` | Moderate risk routes, caution indicators |

### Text
| Token | Hex | Usage |
|---|---|---|
| `color-text-primary` | `#E8F5F2` | Body text, screen titles |
| `color-text-secondary` | `#6B9E92` | Labels, timestamps, subtitles |
| `color-text-disabled` | `#2D5048` | Disabled states |
| `color-text-inverse` | `#050F0D` | Text on light/colored backgrounds |

---

## Typography

### Font Families
| Role | Font | Fallback |
|---|---|---|
| Display / Headlines | `Poppins` | sans-serif |
| Body / UI Labels | `Inter` | sans-serif |
| Monospace (timestamps, coords) | `IBM Plex Mono` | monospace |

### Scale
| Token | Size | Weight | Line Height | Usage |
|---|---|---|---|---|
| `text-display` | 32sp | 700 | 1.1 | Screen hero text |
| `text-heading-1` | 24sp | 600 | 1.2 | Section titles |
| `text-heading-2` | 18sp | 600 | 1.3 | Card headers |
| `text-body-lg` | 16sp | 400 | 1.5 | Primary body text |
| `text-body-md` | 14sp | 400 | 1.5 | Secondary body, descriptions |
| `text-label` | 12sp | 500 | 1.4 | Tags, chips, status labels |
| `text-mono` | 12sp | 400 | 1.4 | Timestamps, coordinates |
| `text-sos` | 20sp | 700 | 1.2 | SOS button label only |

---

## Spacing

Base unit: **4dp**

| Token | Value | Usage |
|---|---|---|
| `space-xs` | 4dp | Tight internal padding |
| `space-sm` | 8dp | Component internal spacing |
| `space-md` | 16dp | Standard padding, gaps |
| `space-lg` | 24dp | Section spacing |
| `space-xl` | 32dp | Screen-level margins |
| `space-2xl` | 48dp | Hero sections |

---

## Corner Radius

| Token | Value | Usage |
|---|---|---|
| `radius-sm` | 8dp | Small chips, tags |
| `radius-md` | 12dp | Cards, input fields |
| `radius-lg` | 16dp | Bottom sheets, modals |
| `radius-xl` | 24dp | Primary CTA buttons |
| `radius-full` | 999dp | Pills, FABs, toggle |

---

## Elevation & Shadow

All shadows use `#000000` with varying alpha and blur.

| Token | Usage |
|---|---|
| `elevation-low` | Cards on background |
| `elevation-mid` | Bottom sheets |
| `elevation-high` | Modals, active overlays |

---

## Iconography

- Style: Outlined, 24dp default, 2dp stroke weight
- Library: Material Symbols Outlined (or Lucide Android)
- SOS icon: Custom shield with exclamation — filled, 32dp, `color-danger`
- Shield active icon: Custom shield with checkmark — filled, 32dp, `color-primary`

---

## Component Library

### SOS Button
- Shape: Circle, 80dp diameter
- Color: `color-danger` fill
- Label: "SOS" — `text-sos`, `color-text-primary`
- Active state: Pulsing red ring animation (3 concentric rings, expanding outward, 1.5s loop)
- Placement: Fixed bottom center, 24dp from screen bottom
- Shadow: `elevation-high` with `color-danger` tint

### Shield Toggle
- Style: Large pill toggle, 200dp wide
- Off state: `color-surface-elevated` background, `color-text-secondary` label "Shield Off"
- On state: `color-primary` background, `color-text-inverse` label "Shield Active"
- Transition: 300ms animated fill
- Icon: Shield icon left of label

### Route Card
- Background: `color-surface`
- Corner: `radius-md`
- Left border: 4dp vertical stripe — `color-safe` (safest) or `color-warning` (moderate)
- Contents: Route name, safety score badge, ETA, distance
- Safety score badge: Pill — `color-safe-subtle` background, `color-safe` text

### Safety Score Badge
- Shape: Pill, `radius-full`
- Safe (score < 0.4): `color-safe` text, `color-safe-subtle` background
- Moderate (0.4–0.7): `color-warning` text, amber subtle background
- Risky (> 0.7): `color-danger` text, `color-danger-subtle` background

### Contact Card
- Background: `color-surface`
- Corner: `radius-md`
- Avatar: Initials circle, `color-primary-subtle` background, `color-primary` text
- Delete action: Swipe left to reveal red delete zone

### Alert Banner (Active SOS)
- Full screen takeover
- Background: `#0A0E14` with `color-danger` 10% overlay
- Pulsing red border on screen edges
- Large "ALERT ACTIVE" headline — `text-display`, `color-danger`
- Cancel button: `color-surface-elevated` background, clearly labelled "Cancel Alert"

---

## Map Styling

Use a custom dark map style (Google Maps JSON) with:
- All base map elements: deep dark green-black
- Roads: `#1A3830`
- Labels: `#6B9E92`
- Water: `#051510`
- Parks: `#0A1F18`

### Heatmap Colors
- Low risk: `#2ECC7140` (transparent green)
- Medium risk: `#F5A62360` (transparent amber)
- High risk: `#FF3B3B80` (transparent red)

---

## Motion & Animation

### Principles
- Fast in, slow out (ease-out preferred)
- Never animate more than 2 elements simultaneously
- Emergency states animate more aggressively than idle states

### Transitions
| Interaction | Duration | Easing |
|---|---|---|
| Screen navigation | 280ms | EaseInOut |
| Shield toggle | 300ms | EaseOut |
| Card appear | 200ms | EaseOut |
| Bottom sheet | 350ms | Spring (damping 0.8) |

### SOS Pulse Animation
```
Repeating, infinite loop:
Ring 1: scale 1.0 → 1.4, opacity 0.6 → 0, duration 1.5s
Ring 2: scale 1.0 → 1.4, opacity 0.6 → 0, duration 1.5s, delay 0.5s
Ring 3: scale 1.0 → 1.4, opacity 0.6 → 0, duration 1.5s, delay 1.0s
```

---

## Accessibility

- Minimum touch target: 48dp x 48dp
- All interactive elements labelled with `contentDescription`
- Color is never the only indicator of state (always paired with icon or text)
- SOS button: extra large touch target (80dp), no accidental trigger protection — requires 1.5s long press for manual trigger
- Text contrast ratio: minimum 4.5:1 for body, 3:1 for large text

---

## Do / Don't

| Do | Don't |
|---|---|
| Use teal for protective/active states | Use teal for danger states |
| Use red only for emergency/danger | Use red for decorative purposes |
| Keep screens uncluttered | Add non-essential information to active SOS screen |
| Use `IBM Plex Mono` for all coordinates and timestamps | Use `Poppins` for data fields |
| Keep the SOS button always visible on home screen | Hide SOS behind navigation |
