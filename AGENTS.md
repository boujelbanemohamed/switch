# Switch Platform — Session Summary

## Goal
Compléter POS / Acquiring (mode d'entrée, cycle transactionnel, vues backoffice), Clearing / Settlement (abstraction réseau, fichier BCT, réconciliation, rapports vers le FSD SMT), et Credit / Revolving (Module 1/3 — ligne crédit, relevés, échéanciers, comptabilisation).

## Constraints & Preferences
- Bloc A puis B ; chaque bloc compile, `mvn test` passe, `npm run build` OK avant de passer au suivant.
- Nouvelle migration Flyway par changement de schéma ; ne jamais modifier une migration existante — dernières : V051, V052, V053, V054, V055.
- `AntPathRequestMatcher` explicite pour toute nouvelle route (bug connu `requestMatchers(HttpMethod, String)` → 401).
- Lecture = ADMIN/OPERATOR/ANALYST ; écriture = ADMIN/OPERATOR.
- Frontend : `const data = await api.xxx()` sans `{ data }` ; réponse paginée `{content:[…]}`.
- Ne pas `trim()` les lignes fixed‑width SMT (les espaces de fin font partie de la longueur).
- Figer les données de compensation au moment du traitement (photo instant T), pas à la génération.
- Les formats réseau propriétaires (Visa BASE II, Mastercard IPM) ne sont PAS disponibles — ne jamais inventer leurs positions. Infrastructure avec stubs `UnsupportedOperationException`.
- Validation après chaque bloc : `curl` direct port 8085, codes 2xx attendus, 401 sans token.
- Crédit : intérêts = solde d'ouverture × (APR/12). Pas d'intérêts si relevé précédent payé intégralement (grâce). Min payment = max(closing × pct%, floor). Ledger via CREDIT_RECEIVABLE (ASSET) + CREDIT_FUNDING (LIABILITY) en partie double.
- Crédit frontend : `const data = await api.xxx()` sans destructuring `{ data }`. Les routes GET = ADMIN/OPERATOR/ANALYST, écritures = ADMIN/OPERATOR. `AntPathRequestMatcher` pour `/api/v1/credit/**`.
- Crédit : valeurs par défaut proposées (APR 18%, min 5%/10 TND, grace period) — pas de décision métier sans validation client. Conformité réglementaire (taux d'usure, disclosures) hors scope technique.

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

### ✅ Done — Visa BASE II DRAFT phase 1 (commit 21338dc)
- TC 05 TCR 0 (168 chars) with position-by-position breakdown.
- ARN (pos 27-49) et Acquirer BID (pos 50-57) marqués TODO : format Visa structuré requis, pas de texte libre.
- 5 champs placeholder (145, 147, 150, 158, 159-168) en attente Clearing Data Codes manual.
- **Pause** : attend les 3 specs externes (Clearing Data Codes, ARN spec, fichier référence Visa).

### ✅ Done — Module 1/3 : Credit / Revolving (V055)
- **Bloc 1.1 — Migration V055 + JPA** : `V055__credit_accounts.sql` (4 tables + 2 ledgers). Entités `CreditLine`, `CreditStatement`, `InstallmentPlan`, `InstallmentEntry`. Repos Spring Data JPA. `mvn clean compile` OK.
- **Bloc 1.2 — CreditLineService** : `openCreditLine()` (création + mise à jour CardAccount type → CREDIT), `authorize()` (vérifie plafond, pose hold), `postPurchase()` (ledger CREDIT_RECEIVABLE + CREDIT_FUNDING), `postPayment()` (inverse, marque OPEN paid si full), `releaseHold()`. Toute écriture ledger vérifiée débit=crédit.
- **Bloc 1.3 — StatementService** : `@Scheduled` quotidien relève si `statementDay == today`. Intérêts = `openingBalance × (APR/12/100)` si précédent NON payé intégralement. Min payment = `max(closing × pct%, floor)`. Marquage OVERDUE après due_date.
- **Bloc 1.4 — InstallmentService** : `convertToInstallments()` (N échéances mensuelles + frais répartis), `markEntryPaid()` (décrémente remaining, COMPLETED si 0).
- **Bloc 1.5 — Controller + Frontend** : 14 endpoints `/api/v1/credit/**`. Page CreditLines.tsx avec cards + tabs (details/statements/installments/simulate) + modal d'ouverture. Routes sécurisées AntPathRequestMatcher. i18n EN/FR.
- **Bloc 1.6 — Tests** : 13 tests (CreditLineServiceTest, StatementServiceTest, InstallmentServiceTest) — authorization > limit refuse, purchase+payment→zero balance, ledger équilibré, intérêts 15 TND exacts, min payment floor/pct, 12 échéances de 100.
- **Total** : 379 tests backend pass, `npm run build` frontend OK. Smoke test 401 (sans token) confirme route enregistrée.

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
- **Crédit calcul intérêts** : `openingBalance × (APR/12/100)` — standard international, en attente validation client (contexte SMT tunisien peut différer).
- **Crédit min payment** : `max(closingBalance × minPaymentPct%, minPaymentFloor)`.
- **Crédit grace period** : intérêts facturés seulement si précédent relevé NON payé intégralement.
- **Crédit ledger** : nouveaux comptes `CREDIT_RECEIVABLE` (ASSET) + `CREDIT_FUNDING` (LIABILITY) via partie double existante.
- **Crédit holds** : `CreditLine.holdAmount` (pas CardAccount.holdAmount) pour éviter interférence avec le flux débit.
- **Migration crédit** : V055_\_credit_accounts.sql (après V054__set_foreign_participants_domestic_false.sql).
- **AccountType.CREDIT** : existe déjà dans CardAccount — pas de migration d'enum nécessaire.

## Next Steps
1. **Démarrer backend** : `mvn spring-boot:run -Dspring-boot.run.profiles=dev` avec variables d'env, smoke test crédit (2xx attendu avec token).
2. **Démonstration concrète** : ouvrir ligne crédit → autoriser → achat → générer relevé → montrer intérêts + min payment.
3. **Commit Module 1/3 Crédit** : après validation utilisateur.
4. **Module 2/3** (à définir) : probablement prélèvements automatiques / SEPA / SCT.
5. **À l'arrivée des specs** : type 40, 3e format, nommage fichier, slipNumber, Visa BASE II TC 05 TCR 0 (ARN + BID a compléter), Mastercard IPM, layout BCT FCOMPSMT.

## Critical Context
- **Dernière migration** : V055__credit_accounts.sql.
- **JAVA_HOME** : /opt/homebrew/Cellar/openjdk@21/21.0.11.
- **Backend** : 379 tests passent (366 orig. + 13 crédit). Frontend : npm run build OK.
- **Comptes ledger crédit** : CREDIT_RECEIVABLE (ASSET), CREDIT_FUNDING (LIABILITY) — seedés dans V055.
- **AccountType.CREDIT** : existe déjà dans `CardAccount`, pas de migration enum.
- **Visa BASE II** : DRAFT phase 1 commit `21338dc` sur `origin/main`, en pause.
- Kafka UnknownHostException cosmétique — HTTP fonctionne.
- Serveur port 8085 : `mvn spring-boot:run -Dspring-boot.run.profiles=dev` avec env vars `PCI_ENCRYPTION_KEY`, `PAN_HASH_KEY`, `JWT_SECRET`, `PIN_ENCRYPTION_KEY`, `CORS_ALLOWED_ORIGINS`.

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

### MODULE 1/3 — Crédit / Revolving
- `resources/db/migration/V055__credit_accounts.sql` : 4 tables + 2 ledgers
- `model/credit/CreditLine.java` : JPA entity credit_lines
- `model/credit/CreditStatement.java` : JPA entity credit_statements
- `model/credit/InstallmentPlan.java` : JPA entity installment_plans
- `model/credit/InstallmentEntry.java` : JPA entity installment_entries
- `repository/credit/CreditLineRepository.java`
- `repository/credit/CreditStatementRepository.java`
- `repository/credit/InstallmentPlanRepository.java`
- `repository/credit/InstallmentEntryRepository.java`
- `service/credit/CreditLineService.java` : open/authorize/purchase/payment/release-hold + ledger
- `service/credit/StatementService.java` : @Scheduled monthly statements + overdue
- `service/credit/InstallmentService.java` : convert to installments, mark paid
- `controller/credit/CreditController.java` : 14 endpoints `/api/v1/credit`
- `frontend/src/pages/CreditLines.tsx` : cards + tabs + modal + simulator
- `frontend/src/types/index.ts` : CreditLine, CreditStatement, InstallmentPlan, InstallmentEntry
- `frontend/src/services/api.ts` : credit.lines.*/credit.statements.*/credit.installmentPlans.*/credit.installmentEntries.*
- `frontend/src/App.tsx` : route `/credit`
- `frontend/src/components/Layout.tsx` : nav HandCoins
- `frontend/src/i18n/en.json` : credit section (40+ keys)
- `frontend/src/i18n/fr.json` : credit section
- `test/.../credit/CreditLineServiceTest.java` : 4 tests (authorization limits, full cycle, ledger balanced)
- `test/.../credit/StatementServiceTest.java` : 4 tests (interest 15 TND, grace period, floor, pct)
- `test/.../credit/InstallmentServiceTest.java` : 5 tests (12×100, fees, validation, completion)
