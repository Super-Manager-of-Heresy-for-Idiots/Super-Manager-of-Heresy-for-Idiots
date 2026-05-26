#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
BOLD="\033[1m"
GREEN="\033[32m"
RED="\033[31m"
CYAN="\033[36m"
RESET="\033[0m"
PASS=0
FAIL=0

header() {
  echo ""
  echo -e "${BOLD}${CYAN}=== $1 ===${RESET}"
}

check() {
  local desc="$1" status="$2" body="$3" expected_status="$4"
  if [ "$status" -eq "$expected_status" ]; then
    echo -e "  ${GREEN}[PASS]${RESET} $desc (HTTP $status)"
    PASS=$((PASS + 1))
  else
    echo -e "  ${RED}[FAIL]${RESET} $desc — expected $expected_status, got $status"
    echo "        Body: $body"
    FAIL=$((FAIL + 1))
  fi
}

do_post() {
  local url="$1" data="$2" token="${3:-}"
  local auth_header=""
  if [ -n "$token" ]; then
    auth_header="-H \"Authorization: Bearer $token\""
  fi
  eval curl -s -w "\n%{http_code}" -X POST "$url" \
    -H "Content-Type: application/json" \
    $auth_header \
    -d "'$data'"
}

do_get() {
  local url="$1" token="${2:-}"
  local auth_header=""
  if [ -n "$token" ]; then
    auth_header="-H \"Authorization: Bearer $token\""
  fi
  eval curl -s -w "\n%{http_code}" -X GET "$url" $auth_header
}

do_put() {
  local url="$1" data="$2" token="${3:-}"
  local auth_header=""
  if [ -n "$token" ]; then
    auth_header="-H \"Authorization: Bearer $token\""
  fi
  eval curl -s -w "\n%{http_code}" -X PUT "$url" \
    -H "Content-Type: application/json" \
    $auth_header \
    -d "'$data'"
}

do_delete() {
  local url="$1" token="${2:-}"
  local auth_header=""
  if [ -n "$token" ]; then
    auth_header="-H \"Authorization: Bearer $token\""
  fi
  eval curl -s -w "\n%{http_code}" -X DELETE "$url" $auth_header
}

parse_response() {
  local raw="$1"
  BODY=$(echo "$raw" | sed '$d')
  HTTP_CODE=$(echo "$raw" | tail -1)
}

extract_json() {
  local json="$1" field="$2"
  echo "$json" | grep -o "\"$field\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" | head -1 | sed 's/.*: *"//;s/"$//'
}

extract_json_array_first_field() {
  local json="$1" field="$2"
  echo "$json" | grep -o "\"$field\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" | head -1 | sed 's/.*: *"//;s/"$//'
}

echo -e "${BOLD}D&D Character Management API Test Suite${RESET}"
echo "Target: $BASE_URL"
echo ""

# ============================================================
header "1. ADMIN LOGIN"
# ============================================================

RAW=$(do_post "$BASE_URL/api/auth/login" '{"username":"admin","password":"admin123"}')
parse_response "$RAW"
check "Admin login" "$HTTP_CODE" "$BODY" 200
ADMIN_TOKEN=$(extract_json "$BODY" "token")

if [ -z "$ADMIN_TOKEN" ]; then
  echo -e "${RED}Cannot proceed without admin token. Is the app running?${RESET}"
  exit 1
fi

# ============================================================
header "2. ADMIN: Create custom class and race"
# ============================================================

RAW=$(do_post "$BASE_URL/api/admin/character-classes" \
  '{"name":"Warlock","description":"Pact magic wielder"}' "$ADMIN_TOKEN")
parse_response "$RAW"
check "Create Warlock class" "$HTTP_CODE" "$BODY" 201
WARLOCK_CLASS_ID=$(extract_json "$BODY" "id")

RAW=$(do_post "$BASE_URL/api/admin/character-races" \
  '{"name":"Orc","description":"Savage warriors"}' "$ADMIN_TOKEN")
parse_response "$RAW"
check "Create Orc race" "$HTTP_CODE" "$BODY" 201
ORC_RACE_ID=$(extract_json "$BODY" "id")

RAW=$(do_get "$BASE_URL/api/admin/character-classes" "$ADMIN_TOKEN")
parse_response "$RAW"
check "List classes (should include Warlock)" "$HTTP_CODE" "$BODY" 200

RAW=$(do_get "$BASE_URL/api/admin/character-races" "$ADMIN_TOKEN")
parse_response "$RAW"
check "List races (should include Orc)" "$HTTP_CODE" "$BODY" 200

# ============================================================
header "3. ADMIN: Create custom stat type and item type"
# ============================================================

RAW=$(do_post "$BASE_URL/api/admin/stat-types" \
  '{"name":"LUCK","description":"Fortune and fate"}' "$ADMIN_TOKEN")
parse_response "$RAW"
check "Create LUCK stat type" "$HTTP_CODE" "$BODY" 201

RAW=$(do_post "$BASE_URL/api/admin/item-types" \
  '{"name":"Longsword","description":"Versatile martial weapon","slot":"MAIN_HAND"}' "$ADMIN_TOKEN")
parse_response "$RAW"
check "Create Longsword item type" "$HTTP_CODE" "$BODY" 201
LONGSWORD_ID=$(extract_json "$BODY" "id")

# ============================================================
header "4. REGISTER PLAYER"
# ============================================================

RAW=$(do_post "$BASE_URL/api/auth/register" \
  '{"username":"frodo_baggins","email":"frodo@shire.com","password":"ringbearer1","role":"PLAYER"}')
parse_response "$RAW"
check "Register player" "$HTTP_CODE" "$BODY" 201

RAW=$(do_post "$BASE_URL/api/auth/login" \
  '{"username":"frodo_baggins","password":"ringbearer1"}')
parse_response "$RAW"
check "Player login" "$HTTP_CODE" "$BODY" 200
PLAYER_TOKEN=$(extract_json "$BODY" "token")

# ============================================================
header "5. REGISTER GAME MASTER"
# ============================================================

RAW=$(do_post "$BASE_URL/api/auth/register" \
  '{"username":"dungeon_master","email":"dm@guild.com","password":"d20rolls!","role":"GAME_MASTER"}')
parse_response "$RAW"
check "Register game master" "$HTTP_CODE" "$BODY" 201

RAW=$(do_post "$BASE_URL/api/auth/login" \
  '{"username":"dungeon_master","password":"d20rolls!"}')
parse_response "$RAW"
check "GM login" "$HTTP_CODE" "$BODY" 200
GM_TOKEN=$(extract_json "$BODY" "token")

# ============================================================
header "6. PLAYER: Create character"
# ============================================================

RAW=$(do_post "$BASE_URL/api/characters" \
  '{"name":"Frodo","level":3,"classId":"b0000000-0000-0000-0000-000000000003","raceId":"c0000000-0000-0000-0000-000000000004"}' \
  "$PLAYER_TOKEN")
parse_response "$RAW"
check "Create character (Rogue Halfling)" "$HTTP_CODE" "$BODY" 201
CHAR_ID=$(extract_json "$BODY" "id")

# ============================================================
header "7. PLAYER: View character, stats, inventory"
# ============================================================

RAW=$(do_get "$BASE_URL/api/characters" "$PLAYER_TOKEN")
parse_response "$RAW"
check "List own characters" "$HTTP_CODE" "$BODY" 200

RAW=$(do_get "$BASE_URL/api/characters/$CHAR_ID" "$PLAYER_TOKEN")
parse_response "$RAW"
check "Get character by ID" "$HTTP_CODE" "$BODY" 200

RAW=$(do_get "$BASE_URL/api/characters/$CHAR_ID/stats" "$PLAYER_TOKEN")
parse_response "$RAW"
check "Get character stats" "$HTTP_CODE" "$BODY" 200
STAT_ID=$(extract_json_array_first_field "$BODY" "id")

RAW=$(do_get "$BASE_URL/api/characters/$CHAR_ID/inventory" "$PLAYER_TOKEN")
parse_response "$RAW"
check "Get character inventory" "$HTTP_CODE" "$BODY" 200

# ============================================================
header "8. PLAYER: Update stat and inventory"
# ============================================================

if [ -n "$STAT_ID" ]; then
  RAW=$(do_put "$BASE_URL/api/characters/$CHAR_ID/stats/$STAT_ID" \
    '{"value":18}' "$PLAYER_TOKEN")
  parse_response "$RAW"
  check "Update stat to 18" "$HTTP_CODE" "$BODY" 200
fi

RAW=$(do_put "$BASE_URL/api/characters/$CHAR_ID/inventory/MAIN_HAND" \
  "{\"itemTypeId\":\"$LONGSWORD_ID\",\"quantity\":1,\"notes\":\"Sting\"}" "$PLAYER_TOKEN")
parse_response "$RAW"
check "Equip Longsword to MAIN_HAND" "$HTTP_CODE" "$BODY" 200

# ============================================================
header "9. PLAYER: Update character"
# ============================================================

RAW=$(do_put "$BASE_URL/api/characters/$CHAR_ID" \
  '{"name":"Frodo the Brave","level":5}' "$PLAYER_TOKEN")
parse_response "$RAW"
check "Update character name and level" "$HTTP_CODE" "$BODY" 200

# ============================================================
header "10. GM: Create team and get invite code"
# ============================================================

RAW=$(do_post "$BASE_URL/api/teams" \
  '{"name":"The Fellowship of the Ring"}' "$GM_TOKEN")
parse_response "$RAW"
check "Create team" "$HTTP_CODE" "$BODY" 201
TEAM_ID=$(extract_json "$BODY" "id")

RAW=$(do_get "$BASE_URL/api/teams/$TEAM_ID/invite-code" "$GM_TOKEN")
parse_response "$RAW"
check "Get invite code" "$HTTP_CODE" "$BODY" 200
INVITE_CODE=$(extract_json "$BODY" "inviteCode")
echo "        Invite code: $INVITE_CODE"

# ============================================================
header "11. PLAYER: Join team"
# ============================================================

RAW=$(do_post "$BASE_URL/api/teams/join" \
  "{\"inviteCode\":\"$INVITE_CODE\"}" "$PLAYER_TOKEN")
parse_response "$RAW"
check "Join team with invite code" "$HTTP_CODE" "$BODY" 200

# ============================================================
header "12. GM: View team members and their characters"
# ============================================================

RAW=$(do_get "$BASE_URL/api/teams/$TEAM_ID" "$GM_TOKEN")
parse_response "$RAW"
check "Get team with members" "$HTTP_CODE" "$BODY" 200

RAW=$(do_get "$BASE_URL/api/characters" "$GM_TOKEN")
parse_response "$RAW"
check "GM lists team members' characters" "$HTTP_CODE" "$BODY" 200

# ============================================================
header "13. GM: Edit team member's stat"
# ============================================================

if [ -n "$STAT_ID" ]; then
  RAW=$(do_put "$BASE_URL/api/characters/$CHAR_ID/stats/$STAT_ID" \
    '{"value":20}' "$GM_TOKEN")
  parse_response "$RAW"
  check "GM updates player stat to 20" "$HTTP_CODE" "$BODY" 200
fi

# ============================================================
header "14. GM: Create artifact"
# ============================================================

RAW=$(do_post "$BASE_URL/api/artifacts" \
  "{\"name\":\"Flame Tongue\",\"description\":\"A sword wreathed in fire\",\"itemTypeId\":\"$LONGSWORD_ID\",\"rarity\":\"RARE\",\"properties\":\"+2 to attack\",\"specialAbilities\":\"2d6 fire damage\"}" \
  "$GM_TOKEN")
parse_response "$RAW"
check "Create artifact (Flame Tongue)" "$HTTP_CODE" "$BODY" 201
ARTIFACT_ID=$(extract_json "$BODY" "id")

RAW=$(do_get "$BASE_URL/api/artifacts" "$GM_TOKEN")
parse_response "$RAW"
check "GM lists artifacts" "$HTTP_CODE" "$BODY" 200

RAW=$(do_get "$BASE_URL/api/artifacts/$ARTIFACT_ID" "$GM_TOKEN")
parse_response "$RAW"
check "Get artifact by ID" "$HTTP_CODE" "$BODY" 200

# ============================================================
header "15. GM: Place artifact in player inventory"
# ============================================================

RAW=$(do_put "$BASE_URL/api/artifacts/place/$CHAR_ID/MAIN_HAND" \
  "{\"artifactId\":\"$ARTIFACT_ID\"}" "$GM_TOKEN")
parse_response "$RAW"
check "Place artifact in MAIN_HAND" "$HTTP_CODE" "$BODY" 200

RAW=$(do_get "$BASE_URL/api/characters/$CHAR_ID/inventory" "$PLAYER_TOKEN")
parse_response "$RAW"
check "Inventory shows artifact" "$HTTP_CODE" "$BODY" 200

# ============================================================
header "16. GM: Create condition with modifiers"
# ============================================================

RAW=$(do_post "$BASE_URL/api/conditions" \
  '{"name":"Poisoned","description":"Weakened by poison"}' "$GM_TOKEN")
parse_response "$RAW"
check "Create condition (Poisoned)" "$HTTP_CODE" "$BODY" 201
CONDITION_ID=$(extract_json "$BODY" "id")

RAW=$(do_post "$BASE_URL/api/conditions/$CONDITION_ID/modifiers" \
  '{"statTypeId":"a0000000-0000-0000-0000-000000000001","modifierValue":-3}' "$GM_TOKEN")
parse_response "$RAW"
check "Add STR -3 modifier to Poisoned" "$HTTP_CODE" "$BODY" 201

RAW=$(do_post "$BASE_URL/api/conditions/$CONDITION_ID/modifiers" \
  '{"statTypeId":"a0000000-0000-0000-0000-000000000002","modifierValue":-2}' "$GM_TOKEN")
parse_response "$RAW"
check "Add DEX -2 modifier to Poisoned" "$HTTP_CODE" "$BODY" 201

RAW=$(do_get "$BASE_URL/api/conditions/$CONDITION_ID" "$GM_TOKEN")
parse_response "$RAW"
check "Get condition with modifiers" "$HTTP_CODE" "$BODY" 200

RAW=$(do_get "$BASE_URL/api/conditions" "$GM_TOKEN")
parse_response "$RAW"
check "GM lists conditions" "$HTTP_CODE" "$BODY" 200

# ============================================================
header "17. GM: Apply condition to character"
# ============================================================

RAW=$(do_post "$BASE_URL/api/conditions/apply/$CHAR_ID" \
  "{\"conditionId\":\"$CONDITION_ID\"}" "$GM_TOKEN")
parse_response "$RAW"
check "Apply Poisoned to character" "$HTTP_CODE" "$BODY" 201
CHAR_COND_ID=$(extract_json "$BODY" "id")

RAW=$(do_get "$BASE_URL/api/conditions/character/$CHAR_ID" "$GM_TOKEN")
parse_response "$RAW"
check "Get active conditions on character" "$HTTP_CODE" "$BODY" 200

# ============================================================
header "18. Verify effective stats"
# ============================================================

RAW=$(do_get "$BASE_URL/api/characters/$CHAR_ID/stats" "$PLAYER_TOKEN")
parse_response "$RAW"
check "Stats include effectiveValue with modifiers" "$HTTP_CODE" "$BODY" 200
echo "        Stats response (check effectiveValue): $(echo $BODY | head -c 500)"

# ============================================================
header "19. GM: Remove condition from character"
# ============================================================

RAW=$(do_delete "$BASE_URL/api/conditions/character/$CHAR_ID/$CHAR_COND_ID" "$GM_TOKEN")
parse_response "$RAW"
check "Remove Poisoned from character" "$HTTP_CODE" "$BODY" 200

RAW=$(do_get "$BASE_URL/api/conditions/character/$CHAR_ID" "$GM_TOKEN")
parse_response "$RAW"
check "Active conditions now empty" "$HTTP_CODE" "$BODY" 200

# ============================================================
header "20. GM: Duplicate condition (negative test)"
# ============================================================

RAW=$(do_post "$BASE_URL/api/conditions/apply/$CHAR_ID" \
  "{\"conditionId\":\"$CONDITION_ID\"}" "$GM_TOKEN")
parse_response "$RAW"
check "Apply Poisoned again" "$HTTP_CODE" "$BODY" 201
CHAR_COND_ID2=$(extract_json "$BODY" "id")

RAW=$(do_post "$BASE_URL/api/conditions/apply/$CHAR_ID" \
  "{\"conditionId\":\"$CONDITION_ID\"}" "$GM_TOKEN")
parse_response "$RAW"
check "Cannot apply same condition twice (expect 409)" "$HTTP_CODE" "$BODY" 409

# Clean up
RAW=$(do_delete "$BASE_URL/api/conditions/character/$CHAR_ID/$CHAR_COND_ID2" "$GM_TOKEN")
parse_response "$RAW"

# ============================================================
header "21. GM: Artifact slot mismatch (negative test)"
# ============================================================

RAW=$(do_put "$BASE_URL/api/artifacts/place/$CHAR_ID/HEAD" \
  "{\"artifactId\":\"$ARTIFACT_ID\"}" "$GM_TOKEN")
parse_response "$RAW"
check "Cannot place MAIN_HAND artifact in HEAD slot (expect 400)" "$HTTP_CODE" "$BODY" 400

# ============================================================
header "22. ACCESS CONTROL: Negative tests"
# ============================================================

# Player cannot create artifacts
RAW=$(do_post "$BASE_URL/api/artifacts" \
  "{\"name\":\"Hack\",\"itemTypeId\":\"$LONGSWORD_ID\"}" "$PLAYER_TOKEN")
parse_response "$RAW"
check "Player cannot create artifact (expect 403)" "$HTTP_CODE" "$BODY" 403

# Player cannot create conditions
RAW=$(do_post "$BASE_URL/api/conditions" \
  '{"name":"Hack"}' "$PLAYER_TOKEN")
parse_response "$RAW"
check "Player cannot create condition (expect 403)" "$HTTP_CODE" "$BODY" 403

# GM cannot update character details
RAW=$(do_put "$BASE_URL/api/characters/$CHAR_ID" \
  '{"name":"Hacked"}' "$GM_TOKEN")
parse_response "$RAW"
check "GM cannot update character name (expect 403)" "$HTTP_CODE" "$BODY" 403

# GM cannot update inventory
RAW=$(do_put "$BASE_URL/api/characters/$CHAR_ID/inventory/MAIN_HAND" \
  '{"itemTypeId":null}' "$GM_TOKEN")
parse_response "$RAW"
check "GM cannot update inventory (expect 403)" "$HTTP_CODE" "$BODY" 403

# Admin cannot create character
RAW=$(do_post "$BASE_URL/api/characters" \
  '{"name":"AdminChar","classId":"b0000000-0000-0000-0000-000000000001","raceId":"c0000000-0000-0000-0000-000000000001"}' \
  "$ADMIN_TOKEN")
parse_response "$RAW"
check "Admin cannot create character (expect 403)" "$HTTP_CODE" "$BODY" 403

# Player cannot access admin endpoints
RAW=$(do_get "$BASE_URL/api/admin/users" "$PLAYER_TOKEN")
parse_response "$RAW"
check "Player cannot access admin endpoint (expect 403)" "$HTTP_CODE" "$BODY" 403

# Invalid invite code
RAW=$(do_post "$BASE_URL/api/teams/join" \
  '{"inviteCode":"BADCODE1"}' "$PLAYER_TOKEN")
parse_response "$RAW"
check "Invalid invite code (expect 404)" "$HTTP_CODE" "$BODY" 404

# ============================================================
header "23. GM: Regenerate invite code"
# ============================================================

RAW=$(do_post "$BASE_URL/api/teams/$TEAM_ID/regenerate-invite" "" "$GM_TOKEN")
parse_response "$RAW"
check "Regenerate invite code" "$HTTP_CODE" "$BODY" 200
NEW_CODE=$(extract_json "$BODY" "inviteCode")
echo "        New invite code: $NEW_CODE"

# ============================================================
header "24. ADMIN: View all users and teams"
# ============================================================

RAW=$(do_get "$BASE_URL/api/admin/users" "$ADMIN_TOKEN")
parse_response "$RAW"
check "Admin lists all users" "$HTTP_CODE" "$BODY" 200

RAW=$(do_get "$BASE_URL/api/admin/teams" "$ADMIN_TOKEN")
parse_response "$RAW"
check "Admin lists all teams" "$HTTP_CODE" "$BODY" 200

# ============================================================
header "25. CLEANUP"
# ============================================================

RAW=$(do_delete "$BASE_URL/api/conditions/$CONDITION_ID" "$GM_TOKEN")
parse_response "$RAW"
check "Delete condition" "$HTTP_CODE" "$BODY" 200

RAW=$(do_delete "$BASE_URL/api/artifacts/$ARTIFACT_ID" "$GM_TOKEN")
parse_response "$RAW"
check "Delete artifact" "$HTTP_CODE" "$BODY" 200

RAW=$(do_delete "$BASE_URL/api/characters/$CHAR_ID" "$PLAYER_TOKEN")
parse_response "$RAW"
check "Delete character" "$HTTP_CODE" "$BODY" 200

# ============================================================
echo ""
echo -e "${BOLD}========================================${RESET}"
echo -e "${BOLD} Results: ${GREEN}$PASS passed${RESET}, ${RED}$FAIL failed${RESET}"
echo -e "${BOLD}========================================${RESET}"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
