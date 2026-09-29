# L2Journey with Docker Compose

Run Login Server, Game Server, and MariaDB for **local development** using Docker Compose. Images are built from source.

## Requirements

- Docker and Docker Compose
- No local Java or Ant needed (build runs inside the image)

## Quick start

1. **Optional:** Copy `.env.example` to `.env` and change passwords/ports if needed.
   ```bash
   cp .env.example .env
   ```
   On Windows PowerShell: `Copy-Item .env.example .env`

2. **Build and start** all services:
   ```bash
   docker compose up -d --build
   ```
   To include **phpMyAdmin** (optional profile `pma`):
   ```bash
   docker compose --profile pma up -d --build
   ```

3. **Logs:**
   ```bash
   docker compose logs -f
   # or per service:
   docker compose logs -f loginserver
   docker compose logs -f gameserver
   docker compose logs -f db
   ```

4. **Stop:**
   ```bash
   docker compose down
   ```

## Services

| Service       | Port(s)     | Description        |
|---------------|-------------|--------------------|
| **db**        | `MYSQL_PORT` → 3306 (default **3306**) | MariaDB 11; creates database and user only |
| **db_init**   | —           | One-off: runs prebuilt `Database_Installer.jar` then updates SQL |
| **loginserver** | `LOGIN_PORT` → 2106 (default **2106**), **9014** | Login Server (client + game server registration) |
| **gameserver**  | `GAME_PORT` → 7777 (default **7777**) | Game Server        |
| **phpmyadmin**  | `127.0.0.1:PHPMYADMIN_PORT` → 80 (default **8080**); profile **`pma`** only | phpMyAdmin → MariaDB `db` (not started by default) |

Host-side ports are set in `.env` (`MYSQL_PORT`, `LOGIN_PORT`, `GAME_PORT`, `PHPMYADMIN_PORT` when using phpMyAdmin); see `.env.example`.

### phpMyAdmin (optional, local dev only)

Enable the Compose profile **`pma`** when bringing the stack up, for example:

```bash
docker compose --profile pma up -d --build
```

Open **http://127.0.0.1:8080** (or whatever you set in `PHPMYADMIN_PORT`). The UI is bound to localhost only. The container connects to MariaDB at host **`db`** on the internal network. **Auto-login** uses `MYSQL_USER` and `MYSQL_PASSWORD` from your environment (same as the app DB user)—convenient for local development; do not rely on this pattern for production or untrusted networks.

- **Database:** MariaDB creates the `l2journey` database (or `MYSQL_DATABASE`) and user from `MYSQL_USER` / `MYSQL_PASSWORD` (default `l2j` / `l2j`). Table setup is done by **db_init**: it runs the prebuilt `dist/db_installer/Database_Installer.jar` (login → game scopes), then applies SQL from `sql/updates`. Login and Game servers start only after db_init has finished.
- **Config overrides:** `docker/config/login/database.ini`, `docker/config/game/database.ini`, and `docker/config/game/server.ini` are mounted so the servers use host `db` and `loginserver` inside the Docker network. **Login and Game read DB settings from the mounted `database.ini` files only** (not from `L2J_DATABASE_*` in compose, which the Java process does not use). To change DB name, user, or password, edit those `database.ini` files so they match MariaDB and your `.env` (or the defaults in `.env.example`).

## Connecting to MariaDB

From the host, use the **published** port: it equals `MYSQL_PORT` from `.env` (default **3306**). Use the same database name, user, and password as in `.env` (`MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`) and keep them aligned with `docker/config/*/database.ini`.

Example with defaults (`MYSQL_PORT=3306`, user `l2j`, password `l2j`, database `l2journey`):

```bash
mysql -h 127.0.0.1 -P 3306 -u l2j -pl2j l2journey
```

If you changed `MYSQL_PORT`, use that value for `-P`.

Or open a client **inside** the `db` container (always connects to port 3306 inside the network namespace).

```bash
docker compose exec db mariadb -u l2j -pl2j l2journey
```

Use your actual `MYSQL_USER`, `MYSQL_PASSWORD`, and `MYSQL_DATABASE` if they differ from the defaults.

## Rebuilding after code changes

```bash
docker compose up -d --build loginserver gameserver
```

Only the Java servers are rebuilt; the database and data are unchanged.

## Debug from Cursor (breakpoints/step)

Remote debugging is opt-in. By default, the stack starts without JDWP enabled.

1. Copy `.env.example` to `.env` (if not already done), then uncomment and keep:
   - `L2J_LOGIN_OPTS=... -agentlib:jdwp=...suspend=n,address=*:5006`
   - `L2J_GAME_OPTS=... -agentlib:jdwp=...suspend=n,address=*:5005`

2. Start or rebuild:

```bash
docker compose up -d --build
```

3. In Cursor, use `.vscode/launch.json`:
   - `Attach GameServer (Docker :5005)`
   - `Attach LoginServer (Docker :5006)`
   - or `Attach Both Servers (Docker)`

Notes:
- Debug ports are mapped to localhost only (`127.0.0.1`) in compose.
- `suspend=n` means services start normally; attach debugger any time.

## Data persistence

- MariaDB data is stored in the `mariadb-data` volume. To start with a **clean DB** (re-run db_init and recreate all tables), remove the volume and bring the stack up again:
  ```bash
  docker compose down -v
  docker compose up -d --build
  ```

## Troubleshooting

- **Login/Game server can’t connect to DB:** Ensure `db` is healthy and `db_init` has run (and exited): `docker compose ps`. Check `docker/config/*/database.ini` use host `db`, user `l2j`, password `l2j` (or match your `.env`).
- **Game server can’t register with Login (wrong hexid):** Login only accepts hexids already in the `gameservers` table. `db_init` syncs that table from `dist/game/config/ipconfig/hexid.txt`. Compose mounts that host file into `db_init` (`/opt/l2j/hexid.txt`) and `gameserver` (`config/ipconfig/hexid.txt`), so a re-run reads the current file rather than the copy baked into the images. After editing it, run `docker compose run --rm db_init`, then restart login and game. Ensure `docker/config/game/server.ini` has `LoginHost = loginserver` and `LoginPort = 9014`.
- **db_init failed:** Check `docker compose logs db_init`. After fixing the cause, re-run the installer with `docker compose run --rm db_init` (starts `db` if needed and runs a fresh one-off container). Use `docker compose up -d --build db_init` when you changed the `db_init` image or compose service definition and want Compose to recreate that service. For a **clean** database (wipe data and re-run installers from scratch), use `docker compose down -v` then `docker compose up -d --build`.
- **Debugger won’t attach:** Confirm JDWP flags are enabled in `.env` (`L2J_LOGIN_OPTS` / `L2J_GAME_OPTS`), then restart services. Verify debug ports (`5006`/`5005`) are not already used by another process.
- **Breakpoints are not hit:** Ensure you rebuilt containers from current source and attached to the correct service/port. Source mismatch between local files and running jars can prevent breakpoint binding.
- **Build fails (Ant/Java):** Build runs with JDK 25 and Ant inside the image; no local Java is required. If the build still fails, confirm `build.xml`, `java/`, and `dist/` are present and that `ant compile jar` succeeds locally with Java 25.
