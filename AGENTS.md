# Switch Platform — Session Summary

## Goal
Compléter POS / Acquiring (mode d'entrée, cycle transactionnel, vues backoffice) et Clearing / Settlement (abstraction réseau, fichier BCT, réconciliation, rapports vers le FSD SMT).

## Constraints & Preferences
- Bloc A puis B ; chaque bloc compile, `mvn test` passe, `npm run build` OK avant de passer au suivant.
- Nouvelle migration Flyway par changement de schéma ; ne jamais modifier une migration existante — dernières : V051, V052, V053.
- `AntPathRequestMatcher` explicite pour toute nouvelle route (bug connu `requestMatchers(HttpMethod, String)` → 401).
- Lecture = ADMIN/OPERATOR/ANALYST ; écriture = ADMIN/OPERATOR.
- Frontend : `const data = await api.xxx()` sans `{ data }` ; réponse paginée `{content:[…]}`.
- Ne pas `trim()` les lignes fixed‑width SMT (les espaces de fin font partie de la longueur).
- Figer les données de compensation au moment du traitement (photo instant T), pas à la génération.
- Les formats réseau propriétaires (Visa BASE II, Mastercard IPM) ne sont PAS disponibles — ne jamais inventer leurs positions. Infrastructure avec stubs `UnsupportedOperationException`.
- Validation après chaque bloc : `curl` direct port 8085, codes 2xx attendus, 401 sans token.

## Progress

### ✅ Done — P1–P6
Livré, commit `9715408`. Wiki pushé.

### ⏸️ Paused — P7 SMT interbank clearing (commits ff191ec, a5ec109, b05240c, 8b15a5f, pushé origin/main)
COMPCONF 168c + CP50 500c + V050 figeage + V051 représentation. 4 points en attente externe : type 40, 3e format, nommage fichier, source slipNumber.

### ✅ Done — BLOC A : POS / Acquiring (V052)
- **A.1 — Mode d'entrée et contexte POS** : migration V052 ajoute `pos_entry_mode`, `pos_condition_code`, `channel`, `transaction_type` sur `transactions`. SwitchCore extrait DE 22, DE 25, dérive `channel` (POS/ATM/ECOM) et `transactionType` (PURC/PRAU/REFD/VOID/COMP/REVS/OTHR). Stockés sur la Transaction dès la création.
- **A.2 — Opérations POS cycle transactionnel** : `deriveTransactionType()` route par DE 3 + MTI. `processAdvice()` (0220 → completion/reversal), `processPreAuth()` (0100 → pas de clearing). Refund (20xxxx) montant négatif. Void (21xxxx) pas de clearing. Reversal (0400/0420) existant conservé.
- **A.3 — Vue POS backoffice** : page Transactions enrichie : filtres canal + type d'opération, colonnes Channel + Operation.
- **A.4 — Tests** : 326 pass, `npm run build` OK.

### ✅ Done — BLOC B : Clearing / Settlement (V053)
- **B.1 — Infrastructure réseau** : `NetworkClearingGenerator` (interface + ReconciliationResult), `Iso20022ClearingGenerator` (réutilise Iso20022Engine), `VisaBaseIIGenerator` + `MastercardIpmGenerator` (stubs `UnsupportedOperationException`).
- **B.2 — Fichier de règlement net BCT** : `BctSettlementService.generateBctSettlementFile()` agrège les positions nettes par institution domestique (exclut étrangères via `Participant.isDomestic`, migration V053). CSV provisoire en attendant FCOMPSMT.BCT officiel.
- **B.3 — Endpoints** : `GET /clearing/files/bct?date=`, `GET /clearing/reconciliation`, `GET /clearing/reports/quarterly`. `POST /clearing/files/incoming` accepte `participantId`. Formats génération : CSV, ISO20022, COMPCONF, CP50, VISA, MASTERCARD.
- **B.4 — Frontend** : BCT download card, Quarterly report card, Reconciliation history table, VISA/MC dans le sélecteur de format.
- **B.5 — Réconciliation + Rapports** : `SettlementFileService.ingestIncomingClearingFile()` persiste `ReconciliationRecord` (source=SCHEME, statut MATCHED/PARTIALLY_MATCHED). `SchemeReportService.generateQuarterlyReport()` (volumes par type, somme, moyenne).
- **Tests** : 326 backend pass, `npm run build` frontend OK.

### ⏸️ Paused / blocked
- **Type 40 CP50** : layout inconnu, attend spec SMT/BPC.
- **3e format SMT** : non spécifié.
- **Nommage fichier** (CPMPAY23.NNNNN vs CP50bbbbb) : contrat banque, déjà configurable.
- **Source slipNumber** : upstream non disponible, fallback 000001.
- **Layout FCOMPSMT.BCT** : spec BCT non publique, CSV provisoire.
- **Formats Visa BASE II / MC IPM** : propriétaires, stubs en place.
- **Gabarits rapports trimestriels Visa/MC** : structure générique en attendant.

## Key Decisions
- **Figage (V050)** : champs SMT figés à processClearing(), pas à la génération.
- **Représentation = clone ClearingRecord** (pas flag), idempotent via findByDisputeId.
- **COMPCONF inversion banques** : chargeback/fee swap acquéreur ↔ émetteur.
- **CP50 = 500c** (prouvé par fichiers de production).
- **Processing code routing** : DE 3 (2 premiers digits) + MTI dérive transactionType. 0100→PRAU, 0200/00xxxx→PURC, 0200/20xxxx→REFD, 0220/02xxxx→COMP, 0220/autre→REVS, 0400/0420→REVS.
- **Channel** : DE 22: 01-05→POS, 06/10-19→ATM, 80-99→ECOM, défaut→POS.
- **Réseau : stubs > format inventé** : UnsupportedOperationException pour Visa/MC.
- **BCT CSV** : format provisoire, en attendant FCOMPSMT.
- **`Participant.isDomestic`** (V053) : distingue banques domestiques/étrangères pour le fichier BCT.

## Next Steps
1. Écrire tests BLOC B (B.6) : BctSettlementService (équilibre comptable, exclusion étrangères), ingestion ISO 20022, stubs Visa/MC, E2E sélecteur schéma.
2. Smoke test curl après BLOC B (port 8085).
3. Commit BLOC A + BLOC B.
4. À l'arrivée des specs : type 40, 3e format, nommage fichier, slipNumber, Visa BASE II, Mastercard IPM, layout BCT.

## Critical Context
- **Dernière migration** : V053__add_is_domestic_to_participants.sql.
- **JAVA_HOME** : /opt/homebrew/Cellar/openjdk@21/21.0.11.
- **Backend** : 326 tests passent. Frontend : npm run build OK.
- Kafka UnknownHostException cosmétique — HTTP fonctionne.
- Serveur port 8085 : conteneur Docker, redémarrage nécessaire après déploiement.
- Format parameter (outgoing) : CSV, ISO20022, COMPCONF, CP50, VISA, MASTERCARD.
- Participant.isDomestic : requis pour BCT, ajouté V053.

## Relevant Files
### Migrations
- `resources/db/migration/V052__add_pos_context_to_transactions.sql`
- `resources/db/migration/V053__add_is_domestic_to_participants.sql`

### BLOC A — POS
- `model/Transaction.java` (+ posEntryMode, posConditionCode, channel, transactionType)
- `repository/TransactionRepository.java` (+ findByChannel, findByTransactionType, etc.)
- `service/SwitchCore.java` (+ deriveChannel, deriveTransactionType, processAdvice, processPreAuth)
- `service/MonitoringService.java` (+ filtres channel/transactionType)
- `controller/SwitchController.java` (+ query params)
- `frontend/src/pages/Transactions.tsx` (+ filtres canal + type)
- `frontend/src/services/api.ts` (+ params channel/transactionType/posEntryMode)
- `frontend/src/types/index.ts` (+ posEntryMode, posConditionCode, channel, transactionType)

### BLOC B — Clearing / Settlement
- `service/clearing/network/NetworkClearingGenerator.java` (interface)
- `service/clearing/network/Iso20022ClearingGenerator.java`
- `service/clearing/network/VisaBaseIIGenerator.java` (stub)
- `service/clearing/network/MastercardIpmGenerator.java` (stub)
- `service/clearing/SettlementFileService.java` (+ VISA/MASTERCARD, reconciliation fix)
- `service/clearing/bct/BctSettlementService.java`
- `service/clearing/reporting/SchemeReportService.java`
- `controller/clearing/ClearingController.java` (+ bct, reconciliation, quarterly)
- `model/participant/Participant.java` (+ isDomestic)
- `repository/ParticipantRepository.java` (+ findByIsDomesticFalse)
- `frontend/src/pages/Clearing.tsx` (+ BCT, reports, reconciliation history, VISA/MC)
- `frontend/src/services/api.ts` (+ files.downloadBct, reconciliation.list, reports.quarterly)
- `frontend/src/types/index.ts` (+ ReconciliationRecord)
- `frontend/src/i18n/en.json` (+ BCT, report, VISA/MC i18n keys)
