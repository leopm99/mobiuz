# L2Journey

L2Journey is a High Five login and game server for Lineage II. It is derived from [L2J Mobius](https://gitlab.com/MobiusDevelopment/L2J_Mobius) (`CT_2.6_HighFive`) and adds project-specific systems, fixes, and PT-BR content.

Supported client protocol revisions: `267`, `268`, `271`, `273`.

Lineage II is a trademark of NCSOFT Corporation. This project is an unofficial emulator and is not affiliated with NCSOFT.

## Requirements

- **Local build:** JDK 25 and Apache Ant 1.8.2 or newer
- **Docker:** Docker and Docker Compose only (no local JDK or Ant required)

Geodata and the client system pack are not in this repository. Download them from the links below.

## Quick start

### Docker (recommended for local development)

```bash
cp .env.example .env
docker compose up -d --build
```

On Windows PowerShell: `Copy-Item .env.example .env`

See [README_docker.md](README_docker.md) for ports, phpMyAdmin, database setup, debugging, and troubleshooting.

### Native build

```bash
ant jar
```

Jars are written under `build/dist/libs/`. Configure login and game servers under `dist/login/config/` and `dist/game/config/`, then run the database installer and start the servers from `dist/`.

Run the unit tests with:

```bash
ant test
```

Test sources live under `test/java/`. Dependencies (JUnit, Mockito) are in `test/lib/` and are used automatically by the Ant `test` target and the GitLab CI `unit-test` job.
## Downloads

| Resource | Link |
|----------|------|
| Source | https://gitlab.com/l2journey/l2journey |
| System pack | https://drive.google.com/drive/folders/1zooE7c60Ez0po7lvlirLpV3j4wCaOSMA?usp=sharing |
| Geodata | https://drive.google.com/drive/folders/1-7pJRBCnmEGQZPE2Murd5sz2_q8cR3tG?usp=drive_link |

## Repository layout

| Path | Contents |
|------|----------|
| `java/` | Login server, game server, and commons sources |
| `dist/` | Datapack, configs, scripts, and runtime layout |
| `sql/` | Database schemas and updates |
| `docker/` | Compose config overrides for containerized runs |
| `test/` | Unit tests |

## Project features

- Npc Buffer and Npc Buffer Premium
- Dressme, user panel, server statistics panel
- Project Community Board and Community Buffer
- Admin bag and warehouse inspection (and bag item deletion)
- Damage balance, buff return, Agathion energy (Soul Avatar)
- Daily Reward and Hitman event
- Phantom player system (including custom parties)
- Ketra / Varka war event
- Docker Compose support
- PT-BR HTML translations and project HTML styling (in progress)

## Portuguese (PT-BR)

Player-dialog HTML coverage (excludes `dist/game/data/html/admin/`):

`█░░░░░░░░░ 9% (1,967 / 21,235 HTML files)`

Recount with PowerShell from the repository root:

```powershell
$roots = @('dist/game/data/html','dist/game/data/scripts')
$en = Get-ChildItem $roots -Recurse -Include *.htm,*.html | Where-Object { $_.FullName -notmatch '\\html\\admin\\' }
$hit = @($en | Where-Object { Test-Path ($_.FullName -replace '\\data\\','\data\lang\br\data\') })
'{0}/{1}' -f $hit.Count, @($en).Count
```

This bar is HTML file coverage only. It does not include NPC name localisation or system messages.

### Changing language in game

Multilingual support is enabled (`MultiLangEnable = True` in `dist/game/config/player/userpanel.ini`). Allowed languages are `en` and `br`. New characters start in English.

1. Open the user panel with `.user`, `.panel`, or `.cp`.
2. Under **Multilingual Support**, the current language is shown. Click **Change** to open the language list.
3. Or type `.lang` and choose **BR**. `.lang br` selects Portuguese directly.

The choice is stored on the character. If a Portuguese HTML file is missing, the server falls back to the English datapack path (`data/html/...` or `data/scripts/...`).

### Custom NPCs

| ID | NPC |
|----|-----|
| 12 | Buffer |
| 15 | Buffer Premium |
| 36603 | GM Shop |
| 36604 | Delevel Manager |
| 70000 | Achievement Manager |
| 70001 | Collectible Archives (in progress) |
| 1003000 | Noblesse Master |

## License

This distribution is licensed under the **GNU General Public License v3** (or later). See [LICENSE](LICENSE).

L2Journey follows the same dual-license rule as L2J Mobius: **the license header in each source file governs that file**. New or rewritten files may use the MIT License; those terms are in [LICENSE.MIT](LICENSE.MIT). Until every GPL-3 file is rewritten, the distribution as a whole remains GPL-3.

Many MIT-style headers in this tree still say that L2J Mobius portions were “shared under the MIT License.” That wording is broader than upstream’s per-file rule. Prefer the header on each file and [upstream’s license section](https://gitlab.com/MobiusDevelopment/L2J_Mobius/-/blob/master/README.md#license).

Third-party jars under `dist/libs/` keep their own licenses. Game client assets and NCSOFT content are not covered by this project’s licenses.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for features, fixes, and removals.
