# CHANGELOG

## Notes
- Keep each entry short: version, user-visible change, verification, and remaining risk.
- Dates below use the local workspace date.

## Entries
- 2026-03-11 `v081`: Localized the manufacturer battery guide catalog into Korean product copy while keeping the asset JSON ASCII-safe with Unicode escapes. The catalog still uses `Build.MANUFACTURER` plus `Build.BRAND`, and unsupported devices still fall back to the shared in-app Android guide. Verification: `:app:testDebugUnitTest`, `:app:assembleDebug`.
- 2026-03-11 `v080`: Strengthened battery guide matching by checking both `Build.MANUFACTURER` and `Build.BRAND`, so derived brands like POCO, Redmi, Honor, iQOO, and Pixel map more reliably to vendor guidance. Unsupported devices now fall back to the shared in-app Android battery guide. Repaired the corrupted battery guide catalog, added metadata fields (`last_verified_at`, `source`), and added catalog tests for brand aliases and fallback lookup. Verification: `:app:testDebugUnitTest`, `:app:assembleDebug`.