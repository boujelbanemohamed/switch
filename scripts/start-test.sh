#!/usr/bin/env bash
#
# start-test.sh — Démarre toute la plateforme Switch et lance les tests E2E.
#
# Usage:
#   ./start-test.sh            # démarre tout + lance les tests E2E (headless)
#   ./start-test.sh --ui       # démarre tout + ouvre l'interface Playwright interactive
#   ./start-test.sh --api      # démarre tout + lance uniquement le smoke-test API
#   ./start-test.sh --no-tests # démarre tout sans lancer les tests (pour tester à la main)
#
# Prérequis : docker, docker-compose, java 21, maven (ou ./mvnw), node 20+, npm
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND="$ROOT/backend"
FRONTEND="$ROOT/frontend"
MODE="${1:-}"

# Couleurs
G='\033[0;32m'; Y='\033[0;33m'; R='\033[0;31m'; N='\033[0m'
log()  { echo -e "${G}[start-test]${N} $*"; }
warn() { echo -e "${Y}[start-test]${N} $*"; }
err()  { echo -e "${R}[start-test]${N} $*"; }

BACKEND_PID=""
FRONTEND_PID=""

cleanup() {
  log "Arrêt des processus..."
  [ -n "$FRONTEND_PID" ] && kill "$FRONTEND_PID" 2>/dev/null || true
  [ -n "$BACKEND_PID" ]  && kill "$BACKEND_PID"  2>/dev/null || true
  # On laisse les conteneurs Docker tourner ; pour tout arrêter : docker-compose down
}
trap cleanup EXIT INT TERM

# 1. Variables d'environnement -------------------------------------------------
if [ ! -f "$ROOT/.env" ]; then
  log "Création de .env depuis .env.example + génération des clés"
  cp "$ROOT/.env.example" "$ROOT/.env"
  {
    echo "JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')"
    echo "PAN_HASH_KEY=$(openssl rand -base64 32 | tr -d '\n')"
    echo "PCI_ENCRYPTION_KEY=$(openssl rand -base64 32 | tr -d '\n')"
    echo "PIN_ENCRYPTION_KEY=$(openssl rand -base64 32 | tr -d '\n')"
  } >> "$ROOT/.env"
fi
set -a; source "$ROOT/.env"; set +a

# 2. Infra : Postgres + Kafka + Zookeeper en Docker ----------------------------
log "Démarrage de Postgres, Kafka, Zookeeper (Docker)..."
( cd "$ROOT" && docker-compose up -d postgres kafka switch-zookeeper )

log "Attente de Postgres (santé)..."
for i in $(seq 1 30); do
  if docker exec switch-postgres pg_isready -U switch_user -d switch_db >/dev/null 2>&1; then
    log "Postgres prêt."; break
  fi
  sleep 2
  [ "$i" = "30" ] && { err "Postgres n'a pas démarré à temps"; exit 1; }
done

# 3. Backend en local ----------------------------------------------------------
log "Démarrage du backend (profil dev)..."
cd "$BACKEND"
MVN="./mvnw"; [ -x "$MVN" ] || MVN="mvn"
$MVN spring-boot:run -Dspring-boot.run.profiles=dev > "$ROOT/backend.log" 2>&1 &
BACKEND_PID=$!

log "Attente du backend sur http://localhost:8085/actuator/health ..."
for i in $(seq 1 60); do
  if curl -sf http://localhost:8085/actuator/health >/dev/null 2>&1; then
    log "Backend prêt."; break
  fi
  sleep 3
  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
    err "Le backend s'est arrêté. Dernières lignes de backend.log :"
    tail -40 "$ROOT/backend.log"; exit 1
  fi
  [ "$i" = "60" ] && { err "Backend trop lent. Voir backend.log"; tail -40 "$ROOT/backend.log"; exit 1; }
done

# 4. Frontend en local ---------------------------------------------------------
log "Installation des dépendances frontend..."
cd "$FRONTEND"
npm install --silent

# --api : on saute le frontend, on teste juste l'API
if [ "$MODE" = "--api" ]; then
  log "Lancement du smoke-test API..."
  bash "$ROOT/scripts/api-smoke.sh"
  exit 0
fi

log "Démarrage du frontend (Vite)..."
npm run dev > "$ROOT/frontend.log" 2>&1 &
FRONTEND_PID=$!

log "Attente du frontend sur http://localhost:3000 ..."
for i in $(seq 1 30); do
  if curl -sf http://localhost:3000 >/dev/null 2>&1; then
    log "Frontend prêt."; break
  fi
  sleep 2
  [ "$i" = "30" ] && { err "Frontend trop lent. Voir frontend.log"; tail -20 "$ROOT/frontend.log"; exit 1; }
done

# 5. Tests ---------------------------------------------------------------------
log "Installation des navigateurs Playwright (si nécessaire)..."
npx playwright install chromium >/dev/null 2>&1 || true

case "$MODE" in
  --no-tests)
    log "Plateforme démarrée. Front: http://localhost:3000 — API: http://localhost:8085"
    log "Teste à la main. Ctrl+C pour tout arrêter."
    wait "$BACKEND_PID"
    ;;
  --ui)
    log "Ouverture de l'interface Playwright interactive..."
    npx playwright test --ui
    ;;
  *)
    log "Lancement de tous les tests E2E (headless)..."
    npx playwright test || true
    log "Rapport HTML : cd frontend && npx playwright show-report"
    ;;
esac
