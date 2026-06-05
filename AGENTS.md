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
- **Migration**: V049–V051 / never modify an existing migration.
- **Security**: `AntPathRequestMatcher` for any new route (known bug with `MvcRequestMatcher`). Read = ANALYST+, write = ADMIN/OPERATOR.
- **Frontend**: `const data = await api.xxx()` without `{ data }` destructuring; handle paginated `{content:[…]}` envelope.
- **SMT figeage** : les champs SMT doivent être FIGÉS au moment du `processClearing()` (photo à l'instant T), pas lus à la génération — sinon une régénération ultérieure produirait des données incohérentes (marchand qui change d'enseigne, BIN qui change, etc.).

## Progress

### ✅ Done — P1–P6
Tout livré, commit `9715408`. Wiki pushé.

### ✅ Done — P7 (SMT interbank clearing)
**Objectif** : génération et ingestion des fichiers COMPCONF et CP50 pour la compensation interbancaire Tunisie (SMT/BPC).
**Commits** : `ff191ec` (positions + format), `a5ec109` (V050 figeage), `b05240c` (V051 représentation).

#### Format COMPCONF (168c)
- `SmtFieldFormatter` : `alphaLeft`, `numericRight`, `amount(int len)`, `dateJJMMAA`, `spaces`, `parseAmount`, `parseDateJJMMAA`.
- `CompconfFileService.generate()` : ligne construite champ par champ aux **positions exactes 1-indexées**.
- Trois zones REDEFINES : ZONE1 (présentation — enseigne commerçant), ZONE2 (chargeback/fees — motif, cycle, message), ZONE3 (représentation — code 'R' en 164).
- Inversion banques sur chargeback (15/17/18) et fee (10) : acquéreur ↔ émetteur.
- `parse()` : extraction par positions, pas de `trim()` sur les lignes.

#### Format CP50 (500c, pas 440c)
- Header 01 (code banque + code faconnier), type 80 (débit/crédit, montants 12c), trailer 99 (totaux).
- Type 40 : `UnsupportedOperationException("Type 40 layout pending SMT spec")`.
- `parse()` retourne les lignes type 40 brutes sans décodage.

#### Figeage V050
Les champs SMT sont figés UNE SEULE FOIS à `processClearing()`, pas lus à la génération :
- `mcc` : DE 18 du ISO8583 → fallback `Merchant.merchantCategoryCode`.
- `cardBrand` : BIN lookup (6 premiers chiffres du PAN) → `BinTable.cardBrand`.
- `tradingName` : lookup `MerchantRepository.findByMerchantId(merchantId)`.
- `merchantNumber` = `Transaction.merchantId`, `cardNumber` = PAN.
- `slipNumber` / `batchNumber` : capturés si disponibles, sinon `000001` (fallback).
- Anciens records (pre-V050) : fallback avec `warn` unique.

**Champs dérivés à la génération** (non stockés) : `sectorCode` depuis MCC, `systemCode` depuis cardBrand (1=CIB, 2=Visa, 3=MC), `surrenderDate` = clearingDate.

**Participant.codeFaconnier** (V050, défaut `222222`) : configurable par participant, utilisé dans le CP50 header pos 17-22.

#### Représentation V051
Quand un dispute passe en statut `REPRESENTMENT` :
- Un **nouveau `ClearingRecord`** est créé (clone du record original).
- `representationFlag=true`, `operationCode=05` (présentation), `operationNature=D`.
- `disputeId` lié au dispute (traçabilité complète).
- Idempotent : `findByDisputeId` vérifie avant création.
- Le nouveau record est inclus dans le **prochain** fichier COMPCONF (ZONE3).

#### Tests & Build
- 326 tests backend (`mvn clean compile && mvn test`) : SmtFieldFormatterTest (17), CompconfFileServiceTest (19), Cp50FileServiceTest (23), reste des P1–P6.
- Frontend : `npm run build` OK (2278 modules, dist 1.16 MB).

### ⏸️ Paused — waiting for external inputs
Ces 4 points sont fonctionnellement prêts côté code mais bloqués par des dépendances externes. Ne pas coder tant que les specs ne sont pas arrivées.

| Point | Dépendance | Statut |
|---|---|---|
| **Type 40 CP50** — layout des lignes transaction CP50 | Spec SMT/BPC (demandée) | `UnsupportedOperationException` en place |
| **3e format SMT** — non spécifié | Spec client | Aucun code |
| **Nommage fichier** — `CPMPAY23.NNNNN` vs `CP50bbbbb` | Contrat SMT de chaque banque (déjà configurable) | Préfixe par participant si besoin |
| **Source `slipNumber`** — facturette, actuellement NULL (fallback `000001`) | Source métier upstream | Aucun code ; colonne V050 déjà en base |

### ❌ Abandonné / hors scope
- **P6 BCT/SIBTEL** — formats non publics, template CSV actuel conservé tel quel.

## Key Decisions
- `AntPathRequestMatcher` pour tous les nouveaux endpoints (`MvcRequestMatcher` bug 401).
- COMPCONF : 168c, positions exactes 1-indexées, 3 REDEFINES, inversion banques sur chargeback/fee.
- CP50 : 500c (prouvé par fichiers de production), pas de type 40 sans spec.
- **Figeage V050** : champs SMT figés à `processClearing()` pas à la génération. Principe monétique validé : une régénération de fichier ne doit pas refléter des données modifiées après la transaction.
- **Représentation** = nouveau `ClearingRecord` (clone), pas un flag + régénération. Cohérent avec le modèle chargeback ISO8583 existant.
- `parse()` sans `trim()` — les espaces de fin font partie de la longueur fixe.
- Warning Kafka `UnknownHostException` = cosmétique (HTTP fonctionne).

## Next Steps (when external inputs arrive)
1. Implémenter type 40 CP50 dès que la spec SMT/BPC arrive.
2. Implémenter le 3e format SMT (spec client).
3. Configurer le préfixe de nommage par participant si le contrat l'exige.
4. Brancher la source du `slipNumber` (facturette) si une upstream devient disponible.
5. Push sur origin si demandé.

## Critical Context
- Dernière migration : `V051__add_dispute_id_to_clearing_records.sql`.
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.11` avant tout `mvn`.
- Backend : 326 tests. Frontend : `npm run build` OK.
- `SecurityConfig` utilise `AntPathRequestMatcher` pour `/api/v1/clearing/**` et `/api/v1/disputes/**`.
- Format parameter accepte : `CSV`, `ISO20022`, `COMPCONF`, `CP50`.
- `Participant.codeFaconnier` : défaut `"222222"`, modifiable par participant.
- Old records (pre-V050) : champs figés NULL → fallback + `warn` unique à la génération.

## Relevant Files
### SMT core
- `service/clearing/smt/SmtFieldFormatter.java`
- `service/clearing/smt/CompconfFileService.java`
- `service/clearing/smt/Cp50FileService.java`
- `service/clearing/SettlementFileService.java`
- `service/clearing/ClearingService.java` (ClearingData + figeage)
- `service/SwitchCore.java` (enrichissement ClearingData)

### Models
- `model/clearing/ClearingRecord.java` (V049–V051 fields)
- `model/Participant.java` (+ `bankCode`, `codeFaconnier`)
- `model/BinTable.java` (BIN → cardBrand lookup)
- `model/dispute/Dispute.java` (cycle + REPRESENTMENT status)

### Repositories
- `repository/clearing/ClearingRecordRepository.java`
- `repository/BinTableRepository.java`

### Dispute / représentation
- `service/dispute/DisputeService.java` (clone sur REPRESENTMENT)
- `controller/dispute/DisputeController.java`

### Migrations
- `resources/db/migration/V049__smt_clearing_fields.sql`
- `resources/db/migration/V050__freeze_smt_fields.sql`
- `resources/db/migration/V051__add_dispute_id_to_clearing_records.sql`

### Frontend
- `pages/Clearing.tsx` (format selector)
- `i18n/en.json`, `i18n/fr.json`
- `e2e/19-clearing-files.spec.ts`
