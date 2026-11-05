# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Fixed

- Clicking Quest on Mitraell, Laferon, or Vulkus shows the no-quest dialog instead of doing nothing when the tutorial is their only quest
- A phantom that dies revives on a hunt spot for its level instead of its race creation point, and it no longer farms monsters 10 or more levels below it (such as starter Gremlins). The hunt band is recorded only after that relocate actually lands

## [0.2.0] - 2026-11-03

SonarCloud **Reliability Rating A** (closes [Milestone #3](https://gitlab.com/l2journey/l2journey/-/milestones/3)). Open bugs on `main` are **0**.
Overall coverage is **7.1%** ([Milestone #4](https://gitlab.com/l2journey/l2journey/-/milestones/4) and [Milestone #5](https://gitlab.com/l2journey/l2journey/-/milestones/5)), **25** code smells remain ([Milestone #8](https://gitlab.com/l2journey/l2journey/-/milestones/8)), and `sonarcloud-check` still has `allow_failure: true` ([Milestone #6](https://gitlab.com/l2journey/l2journey/-/milestones/6)). Those milestones stay open and are not claimed here.

### Added

- Community Board name/title color service with a configurable RGB palette, item fee, persistent name/title preferences restored after PvP/faction login defaults without a schema migration, and payment refunds on database failure

- Community Board Subclass Master: localized add, activate and confirmed removal actions, configurable item fee, existing subclass eligibility rules, and transactional cleanup

- `//geomap` reports the geodata region id and world bounds at the GM position; `//geocell` reports the geo cell and world XYZ
- Phantom spoil and sweep for Scavenger, Bounty Hunter, and Fortune Seeker. The bot sweeps its own corpse before it picks up other ground loot, and waits out Spoil or Sweeper reuse instead of abandoning the corpse
- Phantom summons follow the class pet, and an idle summon is ordered to attack again
- Bishop, Elven Elder, and Shillien Elder phantoms cast Inquisitor and hunt with Surrender to the Divine and Divine Punishment
- New phantom clans stop at five, counting only clans led by a phantom, and each new clan receives a pledge crest
- Resting phantoms in town exchange a short Spanish greeting. The reply is not a farm or trade line
- Phantom mood (neutral, annoyed, angry, cheerful, tired): reaction delay, rare AFK, short distractions, potion misses, and a casual-chat rate that follows the mood. Level-up sets cheerful; repeated deaths set annoyed, then angry
- Phantom fighter attacks for Orc Monk (Stunning Fist), Warder (Penetrating Shot), and Inspector (Triple Thrust)
- Phantom self-buffs: Rapid Shot for bow classes, including Silver Ranger and Moonlight Sentinel; Furious Soul for Warder, Arbalester, and Trickster; Combat Aura for tanks
- Phantom chat replies wait for a short typing delay. The Spanish lines are unchanged
- Two level 20–25 phantom farm routes
- Brazilian Portuguese dialogs for the Talking Island guards: Captain Gilbert (30039), Leon (30040), Arnold (30041), Abellos (30042), Johnstone (30043), Chiperan (30044), Kenyos (30045), and Singa (30046)

### Changed

- An Inquisitor phantom closes and casts Surrender to the Divine, then Divine Punishment, the same way other damage mages cast. It uses its normal attack when those stance spells are not available
- A mage becomes a Cleric, Elven Oracle, or Shillien Oracle only when its level cap is at least 40. A Cleric then becomes a Bishop
- A phantom class change grants that class's skills immediately
- Phantom hair is one random accessory from the pool, which does not include Demon Horns. A bot already wearing Demon Horns is given a replacement once
- The periodic phantom town visit plays Blessed Scroll of Escape once, waits 2 seconds, then lands on geodata height, keeping the authored Z when nearest-Z is more than 1500 off
- Phantom hunt relocate, travel, and spawn skip peace zones and towns. If every try is inside one, the bot goes to that level's route farm spot and waits out the normal return cooldown
- Phantoms ignore quest, chest, and box targets, and only treat a mob as taken when another player is attacking or casting it

### Fixed

- NPC physical attack range uses the template for every NPC, including the archer chase and siege guards
- Battle Roar fills the HP it adds, including the Attack enchant
- Blocked abnormal slots stay blocked while another active effect still holds them
- A party-room invite is refused while the inviter is already waiting for a reply from an online target
- Possessor of a Precious Soul 4 teleports from Caradine on the first ready click and grants the nobless reward only once
- Data loaders skip missing summon-effect map entries and shortcut, option, and UI attributes instead of throwing on null (issue #26)
- A blocked movement step no longer throws when movement was cancelled mid-check (issue #45)
- Cursed-weapon monster drops no longer abort when the killer is outside every map region (issue #11)
- Map regions register Krateis Cube (17,17), Border of Nightmare (16,13), and the Heine hunting square (24,22) so position lookups no longer miss those tiles (issue #12)
- Warders kite and have their bolts restocked. Crossbow classes no longer try to cast Rapid Shot
- The login server selects the game-server host whose subnet contains the client address. The previous comparison never matched, so every client was sent the default host
- Copying a fish for a fight returns a copy of that fish. `Fish.clone()` no longer returns null (issue #42)
- Birthday mail replaces `$c1` with the character name and `$s1` with the age. Those tokens were handed to a regular-expression replace and stayed in the letter
- A clan that already has an academy is told so when someone tries to create another. The check looked at the resolved pledge type, which is 0 when no slot is free, so the academy message never appeared (issue #35)
- A dimensional rift whose room or waiting room is missing from the data logs the gap and skips that teleport
- Missing values are skipped or rejected in pet stats, actor stats and status, siege and siege-guard AI, creature AI, transforms and pets, item conditions and formulas, and in map-region, Kratei's Cube, effect, spawn, attack-stance, item-packet, and system-message call sites (issues #20, #21, #22, #23, #24, #25, #28, #29, #30)
- An empty list passed to a quest random pick returns 0 instead of reading past the end (issue #15)
- The account manager, Dashboard control server, and Game Server register leave their input loops when the session ends (issue #14)
- An interrupted wait in shutdown, Olympiad, Seven Signs Festival, Underground Coliseum, game time, database backup, and the Dashboard keeps the interrupt flag (issue #31)
- Database backup names and template placeholders are read with a plain scan, so a long crafted string no longer hits a super-linear regular expression (issue #44)
- Skills that had no effect now match their descriptions:
	- Presentation - SSQ2 General Purpose 6 (6802) hides the target for a day;
	- Charming, Dance, and Greeting (6909–6911) play the GM animation;
	- Stigma Aftermath (6912) drains 500 HP and 300 MP each second for 10 seconds in a 100 radius;
	- NPC Haste (6914) raises attack speed by 10% to 100% for 10 minutes;
	- Kanna's Splendor and Kallesin's Splendor (6917–6918) require an equipped agathion;
	- Challenge Acceptance (6919) raises P. Atk. and M. Atk. by 30% and attack and casting speed by 15%, and cuts P. Def. and M. Def. in half for 5 minutes;
	- Blessing of Fire (8279) and Blessing of Earth (8280) add 20 element power for 10 minutes;
	- Inventory Slot Add (8281) adds one inventory slot;
	- Cloak Slot Add (8282) opens the cloak slot;
	- Arcana Sigil (8299) raises magic success resistance by 2% and lowers magic cancel by 13;
	- Dynasty Jewel Leather Armor (8366–8368) and the Halloween masks (8372–8377) apply their set bonuses;
	- Provided gift of cheers (9176) gives one Gran Kain's Firework and waits 24 hours
- Sonic Buster (9) level 2 power is 118. Backstab (30) level 22 power is 3271
- Punch of Doom (81) level 2 power is 6632 and its critical rate is 20; the stray stun on that skill is removed
- Double Shot (19) Power enchant step 18 is 5661
- Skill values in `00100–00999` follow the audited tables where a magic level, power, MP cost, reuse, hit time, or enchant step was duplicated or skipped

## [0.1.0] - 2026-10-27

First tagged release. Headline: SonarCloud **Security Rating A** (closes [Milestone #1](https://gitlab.com/l2journey/l2journey/-/milestones/1)). Open vulnerabilities on `main` are **0**. Reliability Rating A and the other Sonar milestones remain open and are not claimed here.

### Security

- XML parsers reject external entities (issue #5)
- Accepted SHA-1 digests for `accounts.password`, secondary PIN storage, and the GameGuard reply check (issue #6)
- Accepted raw RSA `nopadding` for official-client and gameserver wire crypto (issue #7)
- Clan-hall auction, UI-key, and Olympiad queries use bound parameters (issue #8)
- Login Blowfish keys come from `SecureRandom`; secondary-auth SQL constants renamed off hard-coded password patterns (issue #9)
- Debug stack traces disabled in production paths; Dashboard no longer trusts OS PATH lookups (issue #10)

### Added

- `admin_npcinfo` command to reopen the GM NPC Info panel (used by Effects → Back)
- JaCoCo coverage on `ant test`, fed to SonarQube Cloud and GitLab (`coverage` + JaCoCo report artifacts)
- GM Shop Life Stones buylist: Level 85 and 86 Life Stones (normal, mid, high, top) and Accessory Type Life Stones
- Large Dragon Bone in the GM Store Miscellaneous consumables buylist
- SonarQube Cloud analysis in GitLab CI (`sonarcloud-check`) with `sonar-project.properties`
- Dedicated `config/npcs/phantom.ini` with `PhantomEnable` (full system off) and `PhantomAutoStart` (boot spawn)
- Empty-list boot restore of `phantom_ai` characters into `phantomplayers.xml`, plus `admin_phantom_restore_db`
- Phantom cursed-sword hunts: wielders attack players (real players first), a configurable squad (`PhantomCursedHunterLimit`) chases the holder, and one hunter loots the drop on death
- GitLab CI job `package` runs a full `ant` distribution build, including `Dashboard.jar`, after unit tests

### Changed

- Drop Calculator rebuilt around an immutable, reload-aware core index and Community Board UI: paginated item/NPC lookup, sorting, drop/spoil sources, NPC stats/skills and radar; percentage/group chances and player rates are calculated in Journey units

- Community Board Services replaces the Blacksmith shortcut and groups Warehouse, Symbol Maker, Blacksmith and Subclass Master in rows, with PT-BR/EN labels and per-service visibility

- Datapack scripts compile with `--release 25`; Java 8 `-source` / `-target` is removed
- Script file manager forwards JDK module APIs so `--release` compilation works
- Datapack scripts compile with `--release 25`. Java 8 `-source` / `-target` is removed; a Java 8-style script and a Java 25 constructor still compile
- `PhantomCursedHunterLimit` moved from `npc.ini` into `phantom.ini`
- Phantom players pick up their own ground loot before selecting the next farm target
- Geodata pathfinding defaults to live routes (`PathFinding = 2`), and the server corrects player coordinates when the client drifts (`CoordSynchronize = 2`)
- Phantom farm travel and stuck relocation use Blessed SoE return animation before teleporting
- Portuguese village-master HTML restyled to the project title layout (`noscrollbar` for short dialogs)
- Elpies runs every day at 20:00; the schedule is loaded from `config/events/elpiesschedule.xml`
- Community Board home subtitle and tagline follow the player's language. The static Brazilian Portuguese `home.html` is removed
- `CommunityPremiumSystem` in `communityboard.ini` defaults to false, matching its comment and `EnablePremiumSystem`

### Fixed

- Subclass Master HTML uses shallow tables, six choices per page and escaped names; removes raw ampersands and oversized fixed-height page layout for High Five client compatibility

- Treasure Hunter Search (20000), Treasure Hunter Hammer Punch (20001), Dragon Kick (20002), Dragon Slash (20003), and Dragon Dash (20004) skill definitions and chest interaction
- Continuous effect ticks no longer keep running after the effected creature is despawned/removed from the world
- Castle chamberlain vault withdraw rejects non-positive amounts instead of treating them as a successful empty withdraw
- GM Effects page Back button returns to the prior NPC Info / character / summon panel instead of always opening the GM's own character info
- Community Board Premium Manager and Premium Buffs are hidden, and premium purchases are refused, when `EnablePremiumSystem` is false
- Quest "Seven Signs, One Who Seeks the Power of the Seal" (10296): Eris's second dialog option before the battle with Etis van Etina is not freezing the client anymore; After defeating Etis van Etina, Elcadia now returns to the player when the closing scene ends
- NullPointerException when talking to Sophia in the "Seven Signs, Forbidden Book of the Elmore-Aden Kingdom" quest
- Dead Orfen no longer teleports players, starts Paralysis, or moves her corpse to the nest
- Deleting a character also removes its saved `character_summons` rows
- Ferries dock straight at Talking Island and Gludin. The Talking Island berth line is aligned, the Gludin arrival is one straight run into the harbor, and a finished route places the ship on the server position so the client stops instead of steering around the berth
- Community Board epic boss status is read from `GrandBossManager` when the board opens, so kills and respawns no longer wait for the hourly cache
- Olympiad weekly-change countdown log used period-end locals (`countDown` / `numMins`) instead of weekly-change locals (`countDown2` / `numMins2`), so logged days/hours/mins were wrong
- Closed broken `<title>` tags on Tetrarch Thifiell village-master pages (`30358.htm`, `30358-1.htm`)
- Added missing `noscrollbar` on High Priestess Levian (`30037.htm`) and Tetrarch Thifiell (`30358.htm`)
- Cursed-sword phantom wielders no longer take the periodic town break or stand idle in peace zones; they are sent back to hunting spots outside town with a Return cooldown
- Phantom gear progression no longer strips D-grade Mithril Stockings from mages (levels 20–39), which had recreated the kit every AI tick until OOM
- Gear progression skips while a cursed weapon is equipped so the sword is not destroyed
- Event and agathion skills that had `targetType` `NONE` and no effects now apply their described buffs: Gift of Thanks (23156), Donateloph's / Raphaeloph's / Leonardoph's / Michelangeloph's Blessing (23157–23160), Rudolphina's Blessing - Maintain Vitality (23161), Turkey's Big Mistake (23163), Inadequate Affection (23165), and Ball Trapping Buff of Cheers (23183, 23185, 23187, 23189, 23191, 23193, 23195, 23197, 23199)
- Selected skills in `21200–21299`, `22000–22099`, `22100–22199`, `22200–22299`, and `22300–22399` that had no effects now apply them, including cookies, milk buffs, PC Cafe energy, Summon Friend, resists, Energy Replenish, Sweet Chocolate, Meteor, and the Blessed Scroll of Escape. Item 22289 (Juju Agathion's Gift) is an extractable capsule
- Selected weapon, jewelry, talisman, and agathion skills in `08300–08399`, `08400–08499`, `08500–08599`, `09000–09099`, `09100–09199`, and `21100–21199` that were empty now match their descriptions, including weapon special abilities, drop-rate jewelry, Blessing of Victory, Nevit hourglasses, Freya's Refreshing Breeze, and agathion seal cancels
- Monster skills Snake (14156), Long Snake (14157), Crawler Spear (14158), Arcane Jet (14164), Arcane Bust (14165), Black Powder (14167), and Throw Grenade (14168) now have their attack effects
- Cursed-weapon holders ignore flag, karma, clan, and ally target protections. Enslave (3331) is a 10-second slow (run, attack, and cast speed ×0.7)
- A fort siege ends when the rebels recapture the fortress
- The private-store whole message (package sale) is opcode `0x4A`. Opcode `0x49` is the empty PvP-match-record packet
- Attack and damage script events are a new object per hit, so a listener no longer sees the next hit overwrite the event it was given
- GM Effects and Skills paging stays on the NPC it was opened for
- Destroying an item refreshes appearance immediately, so hair and hair2 slots stop showing the destroyed item
- Spawn startup no longer hangs when a territory point is outside geo data
- Rainbow Springs Chateau shows its GoToArena error pages again, and Lovers Jubilee shows its dialog pages again
- Quest 421 (Little Wings, Big Adventure) tracks the four trees with bit flags instead of the memo state modulo
- Underground Coliseum no longer schedules a negative ThreadPool delay when a delayed start task runs after the period has already ended; the expired period is skipped and the next start is scheduled instead
- Seven Signs no longer schedules a negative ThreadPool delay when a period-change task runs after the next boundary (e.g. host sleep); the change advances immediately and the official calendar stays aligned
- Olympiad no longer schedules a negative ThreadPool delay when a delayed competition start task runs after the period has already ended; the expired period is skipped and the next competition is scheduled instead
- Level-80 phantom mages no longer pick ground channel skills (Volcano, Cyclone, Raging Waves, Gehenna) as offensive fallback, which caused a NullPointerException in `Creature.checkDoCastConditions()` when no skill world position was set (reported by fCodiff)
- `Creature.checkDoCastConditions()` rejects a player ground cast when `getCurrentSkillWorldPosition()` is null, before the peace-zone check calls `getX()` (reported by fCodiff)
- Aggro-distance check leashes only the mobs the config includes. With `AggroDistanceCheckRaids` and `AggroDistanceCheckInstances` left false, raids and instance monsters keep fighting past the leash instead of stalling in combat (reported by fCodiff)
- Phantom equipment kits no longer include Tattoo of Divine (item 496), which overwrote the chest slot (reported by fCodiff)
- Clan hall siege admin errors no longer reopen the hall HTML page after the system message (reported by fCodiff)
- `sendInventoryUpdate` skips players in offline mode, so phantoms no longer retain every inventory change in memory (reported by fCodiff)

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

[Unreleased]: https://gitlab.com/l2journey/l2journey/-/compare/v0.2.0...HEAD
[0.2.0]: https://gitlab.com/l2journey/l2journey/-/releases/v0.2.0
[0.1.0]: https://gitlab.com/l2journey/l2journey/-/releases/v0.1.0
[2026-09-30]: https://gitlab.com/l2journey/l2journey/-/commits/main
