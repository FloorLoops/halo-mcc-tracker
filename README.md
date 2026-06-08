<div align="center">

# 🛡️ UNSC TERMINAL
### Halo MCC 100% Achievement Tracker

*Every achievement. Every game. One terminal.*

![version](https://img.shields.io/badge/version-1.0-5c8a3a?style=for-the-badge)
![platform](https://img.shields.io/badge/Android%20·%20native-2d5a87?style=for-the-badge)
![database](https://img.shields.io/badge/database-690%20·%207110G-d4a017?style=for-the-badge)
![sync](https://img.shields.io/badge/Xbox%20Live-sync-107c10?style=for-the-badge)

<br>

<img src="screenshots/home.png" width="270" alt="Home — rank + campaign progress">&nbsp;&nbsp;
<img src="screenshots/games.png" width="270" alt="Achievements — real icons, search, filters">

</div>

---

**UNSC Terminal** is a native Android tracker for 100%ing *Halo: The Master Chief Collection*. Full achievement database imported from Halopedia, real achievement art (greyed while locked, full color when unlocked), per-achievement guides, and Xbox Live sync.

🌐 **Web version:** [floorloops.github.io/halo-mcc-tracker](https://floorloops.github.io/halo-mcc-tracker/) · 📱 **Android:** grab the APK from [Releases](../../releases)

## ✨ Features

- 🗂️ **690 achievements / 7,110G** across CE Anniversary, Halo 2 + H2A MP, Halo 3, ODST, Reach, Halo 4, and MCC General
- 🖼️ **Real achievement art** — official icons load live with disk caching; locked = greyscale, unlocked = full color
- ⚡ **Xbox Live sync** — paste a free [OpenXBL](https://xbl.io) key → SYNC NOW pulls your true unlock state
- 📖 **GUIDES button on every achievement** — jump to its Halopedia page, TrueAchievements guide, or YouTube solutions
- 🎖️ **UNSC rank ladder** — Recruit → Master Chief as your completion climbs
- 📌 **Pins** · 🔎 **search** · 🏷️ **11 type filters** (story / skull / terminal / speed / LASO / legendary…) · ✅ ALL/TODO/DONE
- ⏱️ **Session timer** with check-off counter — structure the grind
- 💾 **Offline-first** — progress stored locally, one-tap clipboard backup, zero accounts, zero telemetry

## 📦 Get it

| Channel | What you get |
|---|---|
| [**Releases**](../../releases) | `UNSCTerminal-v1.0.apk` — native Android app (~65 KB, sideload) |
| [**Web**](https://floorloops.github.io/halo-mcc-tracker/) | Browser version — desktop-friendly, same database lineage |
| `src/` | Full source — single-file Java, zero dependencies, no-Gradle build |

## 🗺️ Roadmap

| Phase | Focus | Status |
|---|---|---|
| **v1.0** | Native app · 690-achievement database · real icons · Xbox Live sync · guide links | ✅ shipped |
| **v1.1** | Exact-700 reconciliation vs TrueAchievements · achievement icons fill their frame (crop-to-fit) · full UNSC rank ladder view · **estimated time-to-100%** · richer stats (per-type, per-difficulty, pace) | 🔜 next |
| **v1.2** | Per-game Halo icons · game-asset backgrounds + overall design pass · Halo SFX (mutable) · animations & transitions | planned |
| **v1.3** | Per-achievement written walkthroughs · direct solution videos · path/collectible screenshots (skulls, terminals) | planned |
| **v1.4** | Optimal completion-order engine + least-pain LASO routing | planned |
| **v2.0** | Generic **"100% Checklist"** edition for Google Play (original branding) | planned |

## 🏗️ Building

No Gradle, no Android Studio: `aapt2 → ecj → d8 → zipalign → apksigner`. The entire app is one Java file + one JSON database.

## ⚖️ Disclaimer

Unofficial fan-made tool, free, for personal use. *Halo* and *The Master Chief Collection* are trademarks of Microsoft Corporation / Halo Studios (343 Industries); achievement names, descriptions, and artwork remain Microsoft's property (icons served from [Halopedia](https://www.halopedia.org)'s gallery). This project is not affiliated with or endorsed by Microsoft. Any commercially distributed version will use original generic branding and assets only.

## 📜 License

Code © 2026 Parliament Four. All rights reserved.

---

<div align="center"><i>"Were it so easy."</i></div>
