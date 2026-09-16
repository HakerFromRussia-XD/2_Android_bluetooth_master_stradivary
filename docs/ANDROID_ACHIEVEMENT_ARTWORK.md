# Android achievement artwork

Source: https://disk.yandex.ru/d/rjLTu0kdZdyiog (downloaded 2026-09-16).

All PNGs are copied byte-for-byte into `app/src/main/res/drawable-nodpi`. Every card now has the former Marathon height: a 40 dp header and an artwork slot of `(contentWidth - 10 dp) * 822 / 1024`, followed by the existing counter and progress bar. The artwork slot spans the same left/right edges as the progress bar. Each alpha-cropped illustration fits inside it, bottom-centered without stretching or cutting off content; narrower artwork therefore retains empty space at the sides. The artwork-to-counter and counter-to-progress-bar gaps are both 4 dp.

PNG regions are decoded on `Dispatchers.IO`, using power-of-two sampling followed by scaling to the actual slot size in physical pixels. A serialized decoder prevents concurrent allocations and a 16 MiB LRU cache reuses screen-sized bitmaps during scrolling. Evicted images are not recycled while Compose may still display them. There are no generated lower-resolution asset files. Previously `ImageBitmap.imageResource` decoded the full original image synchronously during composition.

The source contains 14 images for the 14 enabled achievements. Named files are matched by title (`Точно в цель` corresponds to `PRECISION`). Numbered exports are matched by their depicted scene. The phone scene maps to `ALWAYS_CONNECTED`. The existing disabled `SQUARE_EYES` achievement remains disabled.

| Achievement | Original PNG | Android drawable | SHA-256 |
| --- | --- | --- | --- |
| BIONIC | Бионик.png | `achievement_bionic` | `497bfee1949c052cf5e9166df7f80e5fb3aa250134589536e815d94ea10b3499` |
| CYBORG | Киборг.png | `achievement_cyborg` | `76a76fe692397d827caadec7737820e6233f0548988dcc5311f9acb4a1408208` |
| STREAK | Серия.png | `achievement_streak` | `ef3bf53c3d7141db0b38d8ef952aa7751ee258175337132ee3284956536b5df8` |
| LONG_HAUL | Марафон.png | `achievement_long_haul` | `65be210d3d9fd337d8e79733219114517bd4be1ee5c5efaf3f11fe5f4130127d` |
| SCIENTIST | achievement-flex-panda-clean (1).png | `achievement_scientist` | `8df355571ab142db8efbcd96c90508d589377d8c2f3e4a5ab10ebac64897dbd4` |
| DAILY_CHALLENGE | achievement-flex-panda-clean (4).png | `achievement_daily_challenge` | `7d8b9ada0c7c167ce268da91d2c909ae6d1b56b1b74a96a2840534a61a8e8235` |
| PRECISION | Точно в цель.png | `achievement_precision` | `612d1a0967290dde68566ab1002b0a9045220af5b7a993f45e6c86c54e57335d` |
| POWER | achievement-flex-panda-clean (3).png | `achievement_power` | `828a7a9cd23ddbfc7ce887d7a99c3c085ab25926f9481863e5b2a197d415c0f6` |
| GET_A_GRIP | achievement-flex-panda-clean (6).png | `achievement_get_a_grip` | `199eda761025aafd658e1918cde48db104320767642a89284c6a38359acbe452` |
| ALTER_EGO | Новое имя.png | `achievement_alter_ego` | `00b7fae971f44fc94f6656c85c7233f94968504f2f6292ea5f99279a7d79f2a6` |
| ANNIVERSARY | achievement-flex-panda-clean (7).png | `achievement_anniversary` | `61d0aa2cb0d7f5ed2a52ff57aa5a47974bc430e75987a9317ee01fe9ae92f458` |
| PERSONALISATION | achievement-flex-panda-clean (5).png | `achievement_personalisation` | `62137271e2ed26cc90e50b8f5bc2df102c50dda2e7bca48673a7e0ded4792cf7` |
| CHAMPION | achievement-flex-panda-clean (2).png | `achievement_champion` | `8e7a4cabf0da3b6cb0afa8cd84ffe9e1e2b0f6d15712a7336cac16efed2af2fa` |
| ALWAYS_CONNECTED | achievement-flex-panda-clean.png | `achievement_phone` | `953be665567312260ca1a78a6782c34454c1b6482eca14db11203e95a61c331e` |

## Verification

- `:app:assembleDebug` and `:app:assembleDebugAndroidTest` passed on the merged application baseline.
- `AchievementsCatalogTest` passed (1 test), using an isolated test source set to avoid unrelated legacy test compilation failures.
- A temporary Android instrumentation check on an Android 14 / Pixel 5 emulator opened the screen and the Bronze/Silver/Gold description, scrolled the grid and captured the top and final rows. It passed (1 test). The sensor screen needed a manual profile tap to let instrumentation reach an idle screen.
- The 14 packaged PNG SHA-256 hashes match the downloads; the catalog's alpha bounds match every original bitmap.
- The profile entry was also verified as visible and clickable (`achievementsItem`); tapping it opened the updated screen.
- No physical BLE device was used for artwork verification.


## Uniform cards and scrolling check (2026-09-16)

- `:app:assembleRelease` passed. `AchievementsCatalogTest` and `AchievementArtworkSizeTest` passed (3 tests total) using the isolated achievement test source set.
- Updated signed release installed with `adb install -r` on phone `dcbf845e`, 1080×2400, density 440; installed APK SHA-256 matches the local build: `48c39128e169bba442b465168af28799e57ee9ef3d62738f2e9d18355a2d28e1`.
- Real phone screenshot verified equal card heights and intact illustrations. The full-width slot follows the progress bar, while the image preserves its proportions within that slot.
- Same 24-swipe sequence on the achievement screen, three passes down/up: before 1,142 frames, median 18 ms, p95 23 ms, p99 24 ms; after 1,139 frames, median 12 ms, p95 15 ms, p99 18 ms. Modern janky-frame metric was 0 before and 8 (0.70%) after; legacy metric was 92.38% before and 4.30% after. Frame durations improved, but this is not proof of zero jank. Before used an already warm process; after included first traversal of uncached lower rows. Layout and image-loading changes were measured together.
- Performance logs and physical-device screenshots are retained with the task artifacts. This UI check does not validate BLE firmware updating.

## Shared navigation bar (2026-09-16)

Achievements now uses the same activity status bar, back-button mode, slide transition and preserved profile view as Games. Removed the duplicate Compose header and fragment lifecycle overrides that hid/restored the activity bar. Release build passed and installed with data preserved on `dcbf845e`. Physical-device checks confirmed the shared bar on both screens and toolbar Back returning from achievements to the profile.
