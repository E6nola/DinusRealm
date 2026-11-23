# DinusRealm Design System — "Neo-Swiss Acid-Lime & Matte Charcoal"

> Aesthetic direction: Ultra-clean Neo-Swiss minimalist Bento system unifying both Light Mode (`#FFFFFF` + `#F3F4F6`) and Dark Mode (`#0E0F11` + `#17191D`) around **Electric Citrus Lime** (`#BDF90B` / `#B8F50A`), **Matte Charcoal** (`#1D2024` / `#22262E`), crisp dashed dividers, minimal underline tabs, and **Logo #29 (`Lime Bento D`)**.

---

## 1. Dual Theme Color Tokens (Zero Navy / Purple AI Slop)

### Light Mode — **"Pure Alabaster & Citrus"**
| Token | Hex | Semantic Use |
| :--- | :--- | :--- |
| `background` | `#FFFFFF` | Pure crisp canvas & flat navigation bar |
| `surface` | `#F3F4F6` | Cool Alabaster Bento cards & schedule tiles |
| `surfaceVariant` | `#E7EAEE` | Pill tracks & subtle input containers |
| `border` | `#E4E7EC` | Hairline dividers |
| `ink` | `#16181A` | Deep Charcoal primary typography |
| `inkMuted` | `#767C85` | Secondary cool gray labels |
| `lime` / `onLime` | `#BDF90B` / `#141618` | Hero telemetry card, primary CTAs, active toggles, tab underlines |
| `cardActive` / `onCardActive` | `#1D2024` / `#FFFFFF` | Featured Bento tile (`Presensi`) & active/next class card |
| `dashedDivider` | `#DFE3E8` / `#343942` | 1dp dashed internal Bento card separator |

### Dark Mode — **"Matte Obsidian & Citrus"**
| Token | Hex | Semantic Use |
| :--- | :--- | :--- |
| `background` | `#0E0F11` | True neutral Matte Obsidian background |
| `surface` | `#17191D` | Matte Graphite Bento cards |
| `surfaceVariant` | `#22252B` | Elevated icon wells & subtle pill tracks |
| `border` | `#262A32` | Clean 1dp dark card outline |
| `ink` / `inkMuted` | `#F4F5F7` / `#8C929D` | Crisp neutral typography |
| `lime` / `onLime` | `#B8F50A` / `#121417` | Electric Citrus highlights & primary CTA ink |
| `cardActive` | `#22262E` | Featured Bento tile with subtle lime accent border |
| `mint` / `mango` / `strawberry` | `#4ADE80` / `#FBBF24` / `#FB7185` | Strictly semantic status indicators (Hadir/Lunas, Reminder, Unpaid/Alpha) |

---

## 2. Brand Mark & Signature Components

- **App Icon (Logo #29 · `Lime Bento D`)**: Pure white adaptive background (`#FFFFFF`) carrying the 3-piece geometric Bento `D`: Matte Charcoal vertical pillar (`#1D2024`), Electric Citrus Lime top-right tile (`#BDF90B`), Cool Alabaster bottom-right tile (`#E5E8EE`) with `#1D2024` dot, plus Material You Android 13+ monochrome layer (`ic_launcher_mono.xml`).
- **`LimeHeroBanner`**: `28.dp` rounded `#BDF90B` hero card displaying live room/schedule headline, `46.dp` white circle action badge, instant charcoal `[Absen Sekarang]` trigger when open, and a 3-column academic telemetry strip (`IPK Kumulatif`, `SKS / Matkul`, `Keuangan`).
- **`SoftGridTile`**: `24.dp` Bento tile featuring a circular icon well, title/metric stack, dashed rule, status caption, and tactile mini toggle switch (`Presensi` highlighted in Matte Charcoal).
- **`CurvedHeroHeader`**: Flat Neo-Swiss screen header matching `background` with circular back button, bold title, hairline bottom separator, and an optional `#BDF90B` right KPI pill.
- **`SorbetSegmentTabs` & Flat Bottom Bar**: Clean horizontal text tabs and flat 4-item navigation bar highlighted solely by an Acid-Lime underline pill indicator.
