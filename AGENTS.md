# Switch Platform — Session Summary

## Goal
Implement SMT interbank clearing formats (COMPCONF 168c, CP50 500c) for the Tunisian switch platform — generation and ingestion, as new values of the existing `format` parameter on `/clearing/files/*` endpoints.

## Constraints & Preferences
- Work packages must be done in order; do not start next until previous compiles and passes tests.
- After each package: `cd backend && JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.11 mvn clean compile && mvn test`, then `cd frontend && npm run build`.
- For each new JPA model, create a dedicated Flyway migration (never modify an existing migration).
- Security: write endpoints = ADMIN/OPERATOR, read endpoints = ADMIN/OPERATOR/ANALYST. Use `AntPathRequestMatcher` for all new routes (known bug with `requestMatchers(HttpMethod, String)` — `MvcRequestMatcher` does not match paths correctly, returns 401 with valid token).
- Frontend: use `const data = await api.xxx.list()` (no `{ data }` destructuring); add `api.delete` if missing.
- Publish a Kafka event for each critical business action (stand-in used, file generated, recurring payment triggered).
- Add i18n keys (EN + FR) for each new module.
- Add one minimal Playwright E2E test per module.
- Fixed‑width SMT formats: every character counts, one misaligned field rejects the entire file.
- **Do not `trim()`** fixed‑width SMT lines — trailing spaces are part of the format length. Use `line.isEmpty()` check only.
- **Migration**: V049 / V050 / never modify an existing migration.
- **Security**: `AntPathRequestMatcher` for any new route (known bug with `MvcRequestMatcher`). Read = ANALYST+, write = ADMIN/OPERATOR.
- **Frontend**: `const data = await api.xxx()` without `{ data }` destructuring; handle paginated `{content:[…]}` envelope.
- **Validation after each step**: `mvn clean compile && mvn test`, then `npm run build`, then curl smoke‑test new endpoints (port 8085, expect 2xx).
- **Tests**: unit tests for SMT file services, E2E test confirming selector includes COMPCONF and CP50.
- **Kafka event**: publish an event for each generated file.
- **I18n**: EN + FR keys for new format labels in the frontend.
- **SMT figeage**: les champs doivent être FIGÉS au moment du `processClearing()` (photo à l'instant T), pas lus à la génération — sinon une régénération ultérieure produirait des données incohérentes (marchand qui change d'enseigne, BIN qui change, etc.).

## Progress
### Done
- **P1 – P6 all complete** – backend + frontend + E2E committed and pushed (`9715408`).
- Wiki updated with P1–P6 API reference, database schema, and home page (pushed to `switch.wiki`).
- **E2E suite**: 92 tests pass, 0 failures.
- `common.add` i18n key added (EN + FR).
- **P7 — SMT formats (COMPCONF + CP50)**:
  - **Round 1 (initial, now superseded)**:
    - V049 migration: `bank_code` on `participants`, SMT fields on `clearing_records`.
    - SmtFieldFormatter, CompconfFileService (168c), Cp50FileService (440c — **corrigé à 500c** dans Round 2).
    - Tests: 30 SMT round‑trip tests. 297 total.
  - **Round 2 (positions corrigées)**:
    - CP50 corrigé à 500c (pas 440c). COMPCONF hardcodé aux positions exactes du tableau 1-indexé.
    - `SmtFieldFormatter` simplifié : `alphaLeft`, `numericRight`, `amount(int len)`, `dateJJMMAA`, `spaces`.
    - Tests repassés en assertions de position absolues (plus round‑trip).
    - COMPCONF : 3 REDEFINES (ZONE1 présentation, ZONE2 chargeback/fees, ZONE3 représentation), inversion banques sur chargeback (15/17/18) et fee (10).
    - Type 40 : `UnsupportedOperationException`.
    - 325 tests pass.
  - **Round 3 (figeage V050, en cours)**:
    - **V050** migration : `card_brand`, `trading_name`, `slip_number`, `representation_flag` sur `clearing_records` ; `code_faconnier` (défaut `222222`) sur `participants`.
    - **Figeage à processClearing** : les champs SMT sont figés une seule fois au moment du `processClearing()` via `ClearingData` enrichi, plus jamais recalculés.
    - **SwitchCore.postProcessTransaction** enrichit `ClearingData` avec :
      - `mcc` : DE 18 du ISO8583 (`parsedMessage.field_18`) → fallback `Merchant.merchantCategoryCode`.
      - `cardBrand` : BIN lookup (6 premiers chiffres du PAN) → `BinTable.cardBrand`.
      - `tradingName` : lookup `MerchantRepository.findByMerchantId(merchantId)`.
      - `merchantNumber` = `Transaction.merchantId`, `cardNumber` = PAN.
    - **Champs dérivés à la génération** (non stockés) : `sectorCode` (N/E/C depuis MCC), `systemCode` (1/2/3 depuis cardBrand), `surrenderDate` (= clearingDate).
    - **CompconfFileService** utilise les champs figés : `tradingName` pour ENSEIGNE (ZONE1), `cardBrand` pour SYSTEME (pos 95), `slipNumber` pour NUMERO FACTURETTE. Fallback avec `warn` unique pour les anciens records (pre-V050).
    - **Cp50FileService** utilise `Participant.codeFaconnier` au lieu du hardcodé `222222`.
    - **SettlementFileService** passe `Participant` directement à `Cp50FileService.generate()`.
    - 326 tests pass (1 nouveau : `headerCodeFaconnierCustom`).

### In Progress
- SMT format customisation per bank's actual subscription parameters (file‑naming convention CPMPAY23 vs CP50bbbbb).

### Blocked
- Type 40 transaction lines for CP50 — layout unknown, pending official SMT/BPC specification.
- Third SMT file format not yet specified by customer.
- P6 exact BCT/SIBTEL reporting formats not public; current implementation generates CSV templates.

## Key Decisions
- Used `AntPathRequestMatcher` everywhere instead of `requestMatchers(HttpMethod, String)` because `MvcRequestMatcher` does not match paths correctly on this project (returned 401 with valid token).
- SMT field formatter uses `alphaLeft` for text fields, `numericRight` for numeric codes, `amount(int len)` for monetary amounts with 3 implicit decimals.
- COMPCONF: 168 characters, exact 1-indexed positions, 3 REDEFINES zones, bank inversion for chargeback (15/17/18) and fee requests (10).
- CP50: 500 characters (not 440), multi‑type (01 header, 80 movement, 99 trailer). No type 40 until SMT spec arrives.
- **Figeage (V050)** : SMT fields are frozen at `processClearing()` time, not read at generation time. Protects against merchant/Tx data changes when old files are regenerated.
- `parse` methods must **not** `trim()` lines — fixed‑width format relies on trailing spaces for correct length. Only check `line.isEmpty()` to skip empty lines from trailing newlines.
- Kafka connection warning (`UnknownHostException: kafka`) is cosmetic — HTTP traffic works.

## Next Steps
1. Commit and push V050 + figeage changes.
2. Obtain official type 40 layout for CP50 transaction lines.
3. Capture upstream source for `slipNumber` (facturette) — currently null, uses fallback `000001`.
4. Implement `representationFlag` population in the dispute/chargeback lifecycle.
5. Resolve file‑naming convention (CPMPAY23 vs CP50bbbbb) per bank contract.

## Critical Context
- Last migration is `V050__freeze_smt_fields.sql`.
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.11` must be set before running Maven (project requires Java 21, only Java 17 is the default on this machine).
- Backend currently has **326 tests passing**; frontend `npm run build` passes.
- `SecurityConfig.java` uses `AntPathRequestMatcher` for `/api/v1/clearing/**` — no new route entries needed.
- Format parameter currently accepts `CSV`, `ISO20022`, `COMPCONF`, `CP50`.
- SMT bank codes are 5 digits (e.g. `12345`). The `bankCode` field on `Participant` is `String(5)`.
- `Participant.codeFaconnier` defaults to `"222222"` but is individually configurable per participant.
- Clearing file upload works with JSON body `{ "content": "...", "format": "..." }`.
- Old records (pre-V050) have NULL frozen fields — fallback values used at generation (logged once per `warn`).

## Relevant Files
- `backend/.../service/clearing/smt/SmtFieldFormatter.java`
- `backend/.../service/clearing/smt/CompconfFileService.java`
- `backend/.../service/clearing/smt/Cp50FileService.java`
- `backend/.../service/clearing/SettlementFileService.java`
- `backend/.../service/clearing/ClearingService.java` (ClearingData inner class + figeage)
- `backend/.../controller/clearing/ClearingController.java`
- `backend/.../model/Participant.java` (+ `codeFaconnier`)
- `backend/.../model/clearing/ClearingRecord.java` (+ `cardBrand`, `tradingName`, `slipNumber`, `representationFlag`)
- `backend/.../repository/clearing/ClearingRecordRepository.java`
- `backend/.../service/SwitchCore.java` (enrichissement ClearingData)
- `backend/.../repository/BinTableRepository.java` (BIN lookup pour cardBrand)
- `backend/.../event/*`
- `backend/src/main/resources/db/migration/V049__smt_clearing_fields.sql`
- `backend/src/main/resources/db/migration/V050__freeze_smt_fields.sql`
- `frontend/src/pages/Clearing.tsx`
- `frontend/src/i18n/en.json`
- `frontend/src/i18n/fr.json`
- `frontend/e2e/19-clearing-files.spec.ts`
