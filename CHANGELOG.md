# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- Dedicated `config/npcs/phantom.ini` with `PhantomEnable` (full system off) and `PhantomAutoStart` (boot spawn)
- Unit tests for phantom start/create early-return when `PhantomEnable` is false
- Empty-list boot restore of `phantom_ai` characters into `phantomplayers.xml`, plus `admin_phantom_restore_db`
- Phantom cursed-sword hunts: wielders attack players (real players first), a configurable squad (`PhantomCursedHunterLimit`) chases the holder, and one hunter loots the drop on death
- Unit tests for phantom cursed hunts, return relocation, and related progression guards (`ant test`)
- Unit test that the script compiler accepts an ordinary Java 8 script and a Java 25 constructor

### Changed

- Datapack scripts compile with `--release 25`; Java 8 `-source` / `-target` is removed
- Script file manager forwards JDK module APIs so `--release` compilation works
- `PhantomCursedHunterLimit` moved from `npc.ini` into `phantom.ini`
- Phantom players pick up their own ground loot before selecting the next farm target
- Geodata pathfinding defaults to live routes (`PathFinding = 2`), and the server corrects player coordinates when the client drifts (`CoordSynchronize = 2`)
- Phantom farm travel and stuck relocation use Blessed SoE return animation before teleporting
- Portuguese village-master HTML restyled to the project title layout (`noscrollbar` for short dialogs)
- README documents PT-BR HTML coverage and how to switch language in game
- Elpies runs every day at 20:00; the schedule is loaded from `config/events/elpiesschedule.xml`

### Fixed

- Ferries dock straight at Talking Island and Gludin. The Talking Island berth line is aligned, the Gludin arrival is one straight run into the harbor, and a finished route places the ship on the server position so the client stops instead of steering around the berth
- Community Board epic boss status is read from `GrandBossManager` when the board opens, so kills and respawns no longer wait for the hourly cache
- Olympiad weekly-change countdown log used period-end locals (`countDown` / `numMins`) instead of weekly-change locals (`countDown2` / `numMins2`), so logged days/hours/mins were wrong
- Closed broken `<title>` tags on Tetrarch Thifiell village-master pages (`30358.htm`, `30358-1.htm`)
- Added missing `noscrollbar` on High Priestess Levian (`30037.htm`) and Tetrarch Thifiell (`30358.htm`)
- Cursed-sword phantom wielders no longer take the periodic town break or stand idle in peace zones; they are sent back to hunting spots outside town with a Return cooldown
- Phantom gear progression no longer strips D-grade Mithril Stockings from mages (levels 20–39), which had recreated the kit every AI tick until OOM
- Gear progression skips while a cursed weapon is equipped so the sword is not destroyed
- Event and agathion skills that had `targetType` `NONE` and no effects now apply their described buffs: Gift of Thanks (23156), Donateloph's / Raphaeloph's / Leonardoph's / Michelangeloph's Blessing (23157–23160), Rudolphina's Blessing - Maintain Vitality (23161), Turkey's Big Mistake (23163), Inadequate Affection (23165), and Ball Trapping Buff of Cheers (23183, 23185, 23187, 23189, 23191, 23193, 23195, 23197, 23199)

## [2026-09-30]

### Added

- Npc Buffer and Npc Buffer Premium (NPCs 12 and 15)
- Visual Dressme system
- User panel and server statistics panel
- Project Community Board and Community Buffer
- Admin commands to view or delete items in a player's bag, and to view player and clan warehouses
- Damage balance system and buff-return mod
- Agathion energy system (Soul Avatar)
- Daily Reward mod
- Hitman event
- Custom NPCs: GM Shop (36603), Delevel Manager (36604), Achievement Manager (70000), Noblesse Master (1003000)
- Collectible Archives NPC (70001; in progress)
- Portuguese (PT-BR) HTML translations and project HTML styling (in progress)
- Phantom player system with AI, equipment, hunting, and progression
- Phantom custom party support
- Ketra / Varka war custom event
- Global announce for crafters
- Server dashboard (in progress)
- Java 25 build target
- Docker Compose support for local Login, Game, and MariaDB
- Ant unit tests and GitLab CI job
- Updated database installer

### Changed

- Mana Potion and Mana Drug behave as potions
- PcBang / PcCafe exchanges use Adventure Guildsman NPCs
- Delevel Manager reworked
- Retail Community Board tabs rebuilt

### Fixed

- Monster self-buffs and several boss AIs (Cruma Tower, Den of Evil, Forest of the Dead)
- Global chat announcements and HexId validation for additional worlds
- Missing agathion skills; Chain Heal matches its description
- Monster movement on geodata and visual pathing around obstacles
- Pet regeneration, support or attack modes, and skill levels vs the UI
- Dark Cloud Mansion last room; Delusion Chamber last-room teleport
- Area effect chances; Four Sepulchers blank messages
- CombineTalisman; Elrokian HTMLs; Noblesse Master fee
- Boss cancel rate no longer always 100%
- CTF and Olympiad scoreboards
- Hot Springs bath areas remove disease effects gradually
- NPC map positions; Sel Mahum message range limited near the NPC
- Pet buffs applied individually without duplicates
- Quest, item, and skill fixes (including quests 288 and 423)
- Client system text for Mana Drug / Mana Potion, Bear Spirit Totem, Eye of Watchman Silence / Sleep animations, and several skill icons
- Docker Compose build and GameServer hexid registration on JDK 25
- GM feedback when an admin command is missing required arguments
- Phantom Manager panel buttons and English labels; phantom kits no longer request missing item 25502
- Fishing: Pumping and Reeling require a hooked fish, day or night bait rules, fish HP regen, and bait slot sync
- Buffs no longer processed twice; trade inventory sync after trade ends
- Quest items drop only while the quest is in progress
- Queen Ant and Queen Shyeed AI
- Community Board blacksmith and multisell
- Autoplay Sweeper and Sweeper Festival; Sweeper MP recovery
- Nornil's Garden exit; transmog display; follow distance
- Castle supply delivery reputation and interruption messages
- Duplicate Fantasy Isle spawns; guard movement

### Removed

- FakePlayers system
