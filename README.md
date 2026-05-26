# Switch Platform - ISO 8583 & ISO 20022 Transaction Switch

Plateforme de switch de transactions compatible **ISO 8583 v2** (1987/1993) et **ISO 20022** (pacs.008, pain.001, camt.053).

## Architecture

```
switch-platform/
├── backend/          # Java 21 + Spring Boot 3.x
│   ├── src/main/
│   │   ├── java/com/switch/platform/
│   │   │   ├── iso8583/       # Moteur de parsing/encodage ISO 8583 (j8583)
│   │   │   ├── iso20022/      # Moteur XML ISO 20022 (DOM/JAXB)
│   │   │   ├── router/        # Moteur de routage par règles
│   │   │   ├── service/       # Switch Core, Participants, Monitoring
│   │   │   ├── controller/    # API REST (Switch + Admin)
│   │   │   ├── model/         # Entités JPA
│   │   │   ├── repository/    # Accès données
│   │   │   └── config/        # Configuration, CORS, DataSeeder
│   │   └── resources/
│   │       ├── db/migration/  # Flyway migrations
│   │       ├── iso8583-config.xml
│   │       └── application.yml
│   └── pom.xml
├── frontend/         # React 19 + TypeScript + Tailwind CSS
│   ├── src/
│   │   ├── pages/    # Dashboard, Transactions, Participants, etc.
│   │   ├── components/
│   │   ├── services/ # Client API
│   │   └── types/    # Types TypeScript
│   └── package.json
└── docker-compose.yml
```

## Stack Technique

| Composant | Technologie |
|-----------|------------|
| Backend | Java 21, Spring Boot 3.4, Spring Data JPA |
| Base de données | PostgreSQL 16 + pgcrypto |
| ISO 8583 | j8583 (Solab) |
| ISO 20022 | DOM Parser / JAXB |
| File de messages | Apache Kafka |
| Migrations | Flyway |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS 4 |
| Visualisation | Recharts |
| Conteneurisation | Docker Compose |

## Prérequis

- Java 21+
- Docker & Docker Compose
- Node.js 20+
- Maven 3.9+

## Installation et Démarrage

### 1. Base de données et Kafka

```bash
docker compose up -d
```

### 2. Backend

```bash
cd backend
mvn clean install -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Le backend démarre sur `http://localhost:8080`.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Le frontend démarre sur `http://localhost:3000`.

## API REST

### Endpoints Switch

| Méthode | Path | Description |
|---------|------|-------------|
| POST | `/api/v1/switch/iso8583` | Traiter un message ISO 8583 (binaire) |
| POST | `/api/v1/switch/iso8583/base64` | Traiter un message ISO 8583 (Base64) |
| POST | `/api/v1/switch/iso20022` | Traiter un message ISO 20022 (XML) |
| GET | `/api/v1/switch/transactions/{id}` | Détail d'une transaction |
| GET | `/api/v1/switch/transactions` | Liste des transactions |
| GET | `/api/v1/switch/health` | Health check |

### Endpoints Admin

| Méthode | Path | Description |
|---------|------|-------------|
| GET | `/api/v1/admin/dashboard` | Statistiques dashboard |
| GET | `/api/v1/admin/participants` | Liste des participants |
| POST | `/api/v1/admin/participants` | Créer un participant |
| GET | `/api/v1/admin/routing-rules` | Liste des règles de routage |
| POST | `/api/v1/admin/routing-rules` | Créer une règle de routage |
| GET | `/api/v1/admin/bin-tables` | Liste des tables BIN |

## Exemples d'utilisation

### Envoyer une transaction ISO 8583

```bash
# Transaction binaire
curl -X POST http://localhost:8080/api/v1/switch/iso8583 \
  -H "X-Source-Code: DEMO_ACQ" \
  -H "Content-Type: application/octet-stream" \
  --data-binary @message.iso

# Transaction en Base64
curl -X POST http://localhost:8080/api/v1/switch/iso8583/base64 \
  -H "X-Source-Code: DEMO_ACQ" \
  -H "Content-Type: application/json" \
  -d '{"message": "ADIwMDBCMDIyQzAwMDAwMDAwMDEyMzQ1Njc4OTAxMjM0..."}'
```

### Envoyer un virement ISO 20022

```bash
curl -X POST http://localhost:8080/api/v1/switch/iso20022 \
  -H "X-Source-Code: DEMO_ACQ" \
  -H "Content-Type: application/xml" \
  -d '<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.12">
  <FIToFICstmrCdtTrf>
    <GrpHdr>
      <MsgId>REF123456</MsgId>
      <CreDtTm>2026-05-26T10:00:00Z</CreDtTm>
      <NbOfTxs>1</NbOfTxs>
      <TtlIntrBkSttlmAmt Ccy="EUR">1000.00</TtlIntrBkSttlmAmt>
    </GrpHdr>
  </FIToFICstmrCdtTrf>
</Document>'
```

## Règles de Routage

Le moteur de routage supporte les conditions suivantes :

- `EQUALS` / `NOT_EQUALS` - Égalité
- `STARTS_WITH` / `CONTAINS` - Correspondance partielle
- `IN` - Appartenance à une liste
- `BIN_RANGE` - Plage de BIN
- `AMOUNT_RANGE` - Plage de montant

Les règles sont évaluées par ordre de priorité (croissant). La première règle qui correspond est utilisée.

## Schéma Base de Données

- **participants** - Participants au réseau (acquéreurs, émetteurs, switch)
- **bin_tables** - Tables d'identification bancaire (BIN/IIN)
- **routing_rules** - Règles de routage avec conditions JSON
- **transactions** - Ledger des transactions
- **transaction_audit** - Journal d'audit
- **message_templates** - Templates de messages
- **settlements** - Enregistrements de settlement

## Tests

```bash
cd backend
mvn test
```
