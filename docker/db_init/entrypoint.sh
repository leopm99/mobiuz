#!/bin/sh
set -e
DB_HOST="${DB_HOST:-db}"
DB_PORT="${DB_PORT:-3306}"

INSTALLER_JAR="/opt/l2j/db_installer/Database_Installer.jar"
SQL_DIR="/opt/l2j/sql"

run_installer() {
  scope="$1"
  java -jar "$INSTALLER_JAR" \
    --console \
    -h "$DB_HOST" -p "$DB_PORT" -u root -pw "$MYSQL_ROOT_PASSWORD" \
    -d "$MYSQL_DATABASE" \
    --sql-dir "$SQL_DIR" \
    --scope "$scope"
}

table_exists() {
  t="$1"
  [ "$(mysql -h "$DB_HOST" -P "$DB_PORT" -u root -p"$MYSQL_ROOT_PASSWORD" -N -s -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${MYSQL_DATABASE}' AND table_name='${t}';")" = "1" ]
}

column_exists() {
  t="$1"
  c="$2"
  [ "$(mysql -h "$DB_HOST" -P "$DB_PORT" -u root -p"$MYSQL_ROOT_PASSWORD" -N -s -e "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='${MYSQL_DATABASE}' AND table_name='${t}' AND column_name='${c}';")" = "1" ]
}

echo "Waiting for MariaDB TCP ${DB_HOST}:${DB_PORT}..."
for i in $(seq 1 30); do
  if mysqladmin ping --protocol=tcp -h "$DB_HOST" -P "$DB_PORT" -u root -p"$MYSQL_ROOT_PASSWORD" --silent >/dev/null 2>&1; then
    echo "MariaDB is ready."
    break
  fi
  echo "MariaDB not ready yet ($i/30), retrying..."
  sleep 1
done

if table_exists "account_data" || table_exists "gameservers"; then
  echo "Login schema already present, skipping Database_Installer (login)."
else
  echo "Running Database_Installer (login)..."
  run_installer login
fi

if table_exists "items" || table_exists "characters"; then
  echo "Game schema already present, skipping Database_Installer (game)."
else
  echo "Running Database_Installer (game)..."
  run_installer game
fi

if [ -d /opt/l2j/sql/updates ] && [ -n "$(find /opt/l2j/sql/updates -maxdepth 1 -name '*.sql' 2>/dev/null)" ]; then
  echo "Running updates SQL..."
  for f in $(find /opt/l2j/sql/updates -maxdepth 1 -name '*.sql' | sort); do
    base="$(basename "$f")"
    if [ "$base" = "Update_character_shortcuts.sql" ] && column_exists "character_shortcuts" "character_type"; then
      echo "Skipping $f (character_shortcuts.character_type already exists)"
      continue
    fi
    if [ "$base" = "update_items_dressme.sql" ] && column_exists "items" "visual_item_id"; then
      echo "Skipping $f (items.visual_item_id already exists)"
      continue
    fi
    if [ "$base" = "update_items_agathion_energy.sql" ] && column_exists "items" "agathion_energy"; then
      echo "Skipping $f (items.agathion_energy already exists)"
      continue
    fi
    echo "Applying $f"
    mysql -h "$DB_HOST" -P "$DB_PORT" -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < "$f"
  done
fi

# LoginServer rejects unknown hexids. Keep gameservers in sync with the GameServer hexid.txt.
HEXID_FILE="/opt/l2j/hexid.txt"
if table_exists "gameservers" && [ -f "$HEXID_FILE" ]; then
  HEXID="$(grep -E '^HexID=' "$HEXID_FILE" | cut -d= -f2- | tr -d '\r')"
  SERVER_ID="$(grep -E '^ServerID=' "$HEXID_FILE" | cut -d= -f2- | tr -d '\r')"
  SERVER_ID="${SERVER_ID:-1}"
  case "$SERVER_ID" in
    ''|*[!0-9]*) SERVER_ID=1 ;;
  esac
  if [ -n "$HEXID" ]; then
    echo "Syncing gameservers registration (server_id=${SERVER_ID}) to match hexid.txt..."
    mysql -h "$DB_HOST" -P "$DB_PORT" -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e \
      "INSERT INTO gameservers (server_id, hexid, host) VALUES (${SERVER_ID}, '${HEXID}', '')
       ON DUPLICATE KEY UPDATE hexid=VALUES(hexid);"
  else
    echo "Warning: HexID missing from ${HEXID_FILE}; skipping gameservers sync."
  fi
fi

echo "Database installation complete."
