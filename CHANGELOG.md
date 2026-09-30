# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- Empty-list boot restore of `phantom_ai` characters into `phantomplayers.xml`, plus `admin_phantom_restore_db`

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
