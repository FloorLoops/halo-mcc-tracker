<div align="center">

# 🛡️ UNSC TERMINAL
### Halo MCC 100% Achievement Tracker

*Every achievement. Every game. One terminal.*

![version](https://img.shields.io/badge/version-1.2.1-5c8a3a?style=for-the-badge)
![platform](https://img.shields.io/badge/Android%20·%20native-2d5a87?style=for-the-badge)
![database](https://img.shields.io/badge/database-690%20·%207110G-d4a017?style=for-the-badge)
![sync](https://img.shields.io/badge/Xbox%20Live-sync-107c10?style=for-the-badge)

<br>

<img src="screenshots/home.png" width="270" alt="Home — rank + campaign progress">&nbsp;&nbsp;
<img src="screenshots/games.png" width="270" alt="Achievements — real icons, search, filters">

</div>

---

**UNSC Terminal** is a native Android tracker for 100%ing *Halo: The Master Chief Collection*. Full achievement database imported from Halopedia, real achievement art (greyed while locked, full color when unlocked), per-achievement guides, and Xbox Live sync.

> 🎖️ *Design inspired by the old **Halo Waypoint** app* — the companion that tracked your career, service record, and challenges. This is a love-letter to that experience, reimagined as a completionist's tracker.

🌐 **Web version:** [floorloops.github.io/halo-mcc-tracker](https://floorloops.github.io/halo-mcc-tracker/) · 📱 **Android:** grab the APK from [Releases](../../releases)

## ✨ Features

- 🗂️ **~690 achievement database** across CE Anniversary, Halo 2 + H2A MP, Halo 3, ODST, Reach, Halo 4, and MCC General *(Halopedia-sourced; exact reconciliation to the official 700 / 7,000G is on the roadmap)*
- 🖼️ **Real achievement art** — official icons load live with disk caching; locked = greyscale, unlocked = full color
- ⚡ **Xbox Live sync** — paste a free [OpenXBL](https://xbl.io) key → SYNC NOW pulls your true unlock state + unlock dates
- 📖 **GUIDES button on every achievement** — jump to its Halopedia page, TrueAchievements guide, or YouTube solutions
- 🎖️ **UNSC rank ladder** — Recruit → Master Chief as your completion climbs
- 📌 **Pins** · 🔎 **search** · 🏷️ **type filters** (story / skull / terminal / speed / LASO / legendary…) · ✅ ALL/TODO/DONE
- 🏆 **100+ in-app achievements** with animated unlock banners, sounds, replay, and an app-rank that climbs as you earn them — plus hidden secrets to discover
- ⏱️ **Session timer** with check-off counter
- 💾 **Offline-first** — progress stored locally, one-tap clipboard backup, zero accounts, zero telemetry

## 📦 Get it

| Channel | What you get |
|---|---|
| [**Releases**](../../releases) | `UNSCTerminal-v1.2.1.apk` — native Android app (sideload) |
| [**Web**](https://floorloops.github.io/halo-mcc-tracker/) | Browser version — desktop-friendly, same database lineage |
| `src/` | Full source — single-file Java, zero dependencies, no-Gradle build |

## 🗺️ Roadmap

| Phase | Focus | Status |
|---|---|---|
| **v1.0** | Native app · ~690-achievement database · real icons · guide links | ✅ shipped |
| **v1.1.5** | **Xbox Live sync** (+ unlock dates) · **100+ in-app achievements** with animated unlock banners, sounds, replay & a climbing app-rank · hidden easter eggs · UNSC rank ladder · estimated time-to-100% · per-type stats · in-app roadmap · "What's New" update review · Grunt API key field · Halo Waypoint-inspired | ✅ shipped |
| **v1.2.1** | Exact-700 reconciliation to the official Xbox list (Xbox sync adds any achievements the local DB is missing) · **difficulty-weighted time-to-completion** (a LASO playlist counts as 20+ hrs, not 1) | ✅ shipped |
| **v1.2** | **Overhauled ranking** (XP-weighted — heavier achievements lift your rank more than flat %) · **choose your rank style: modern MCC / Halo 3 / Halo: Reach** · **Focus Mode** — "best next targets" ranked by gamerscore-per-hour · **smart breakdown** (closest game to 100%, easiest category to clear, XP earned) | ✅ shipped |
| **v1.2.5** | **Native UI glow-up** — bring the native app up to the web version's look (UNSC HUD styling, glows, depth, polish) | planned |
| **v1.3** | **Career stats** — medals, headshots, kills, accuracy, playtime pulled from Xbox/Halo stats API · per-game Halo icons · game-asset backgrounds + overall design pass · **in-app feedback button that emails feature requests straight to the dev** | planned |
| **v1.3.5** | **Achievement artwork viewer** — tap to view the full high-res achievement art (requires scraping + bundling HQ image sets) · optimization & full review pass | planned |
| **v1.4** | Halo SFX (mutable) · animations & transitions | planned |
| **v1.5** | Packaged notification sound · minor tweaks | planned |
| **v1.6** | Home-screen widgets | planned |
| **v1.7** | General tips & pointers blended from YouTube / Halopedia / TrueAchievements | planned |
| **v1.8** | Per-achievement written walkthroughs · solution videos · path/collectible screenshots | planned |
| **v1.9** | Optimal completion-order engine + least-pain LASO routing | planned |
| **v2.0** | Generic **"100% Checklist"** edition for Google Play (original branding & assets) | planned |

## 🏗️ Building

No Gradle, no Android Studio: `aapt2 → ecj → d8 → zipalign → apksigner`. The entire app is one Java file + one JSON database.

## ⚖️ Disclaimer

Unofficial fan-made tool, free, for personal use. *Halo* and *The Master Chief Collection* are trademarks of Microsoft Corporation / Halo Studios (343 Industries); achievement names, descriptions, and artwork remain Microsoft's property (icons served from [Halopedia](https://www.halopedia.org)'s gallery). This project is not affiliated with or endorsed by Microsoft. Any commercially distributed version will use original generic branding and assets only.

## 📜 License

Code © 2026 Parliament Four. All rights reserved.

---

<div align="center"><i>"Were it so easy."</i></div>
