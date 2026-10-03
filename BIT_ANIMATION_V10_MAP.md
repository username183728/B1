# Bit Animation v10 — split assets

The app now uses separate Lottie JSON assets derived from `bit_face_v10.json`.

- `bit_idle.json` — normal: blink/look + sleep/wake (0–450)
- `bit_tap.json` — single tap reaction (450–480)
- `bit_bump.json` — bump reaction (480–560); used directly on the 3rd tap, so the TAP animation is skipped
- `bit_dizzy.json` — shake reaction (560–660)
- `bit_love.json` — love reaction (660–720)
- `bit_error.json` — error / exclamation (720–770)
- `bit_sad.json` — sad reaction (830–900)
- `bit_wink.json` — recovery / one-eye wink (900–940)

The purple `BG (ungu)` Lottie layer is removed from the animation assets. The Android app background therefore shows through and follows the existing light/dark theme.

`bit_angry.json` is intentionally not included or referenced. The 3-tap reaction ends at BUMP and returns to IDLE.
