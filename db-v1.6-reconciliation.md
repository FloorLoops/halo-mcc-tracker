# Halo MCC canonical achievement DB rebuild — report

Built from Halopedia pageid 186393, sections 1-10 (fetched "2026-07-06"), reconciled against src/data.json.

## Final gate
- Achievements: **700**
- Total Gamerscore: **7000**
- All 700 entries have non-empty `img` and `wiki`. Unique ids: 700.

## Per-game counts / GS

| game | count | GS |
|---|---|---|
| mcc | 85 | 1000 |
| ce | 95 | 915 |
| h2 | 157 | 1425 |
| h3 | 89 | 805 |
| odst | 99 | 990 |
| reach | 100 | 1000 |
| h4 | 75 | 865 |
| **total** | **700** | **7000** |

## Reconciliation
- Matched (kept existing entry; gs/game/desc set from canonical): **690**
- Added: **10**
- Dropped: **0**

### Added (all Halo 3, Season 7/8-era additions missing from old DB)
- Dirge of Madrigal (5 GS, id `h3_new1`, type `story`)
- Primate (5 GS, id `h3_new2`, type `story`)
- Haplorrhini (5 GS, id `h3_new3`, type `story`)
- Can't Keep Him Down (5 GS, id `h3_new4`, type `story`)
- Flipyap (5 GS, id `h3_new5`, type `story`)
- Heading to his Destiny (5 GS, id `h3_new6`, type `story`)
- Missing Link (5 GS, id `h3_new7`, type `story`)
- Orbital Skull (5 GS, id `h3_new8`, type `skull`)
- Sandbox Skull (5 GS, id `h3_new9`, type `skull`)
- Brainpan (10 GS, id `h3_new10`, type `skull`)

### Dropped
- (none)

## Name-normalization collisions
- None (700 unique normalized names in canonical; 1:1 with Exophase Xbox list).

## Anomalies / corrections

1. **Wiki GS typos (14 entries, +165 GS)** — the Halopedia tables sum to 7,165. Cross-checked all 700 entries against the live Xbox achievement list (Exophase full dump, which sums to exactly 7,000 and matches the canonical name set 1:1). The following wiki values were corrected to the real Xbox Gamerscore (wiki → real):
   - [h2] BLASTacular!: 5 → 10
   - [h2] Monumental Thirst!: 5 → 10
   - [h2] MVP: 5 → 10
   - [odst] Tayari Plaza: 30 → 10
   - [odst] Uplift Reserve: 30 → 10
   - [odst] Kizingo Boulevard: 30 → 10
   - [odst] ONI Alpha Site: 30 → 10
   - [odst] NMPD HQ: 30 → 10
   - [odst] Kikowani Station: 30 → 10
   - [odst] Data Hive: 50 → 10
   - [odst] Coastal Highway: 50 → 10
   - [odst] Audiophile: 5 → 10
   - [odst] Record Store Owner: 5 → 10
   - [odst] Well... Maybe One or Two: 10 → 20
   Original wiki values preserved in canonical.json as `gs_wiki_original`.
2. `BLASTacular!` wiki cell was malformed (`|1{{Gamerscore|5}}`); real value is 10.
3. Old DB was 690 achievements / 7,110 GS: it carried the same 14 wiki GS typos and was missing the 10 newest Halo 3 achievements. Both issues are fixed in data-new.json.
4. Icon image URLs verified against the known-good LASOMaster hash-path example.
