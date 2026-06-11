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
- Visa BASE II (TC 05 TCR 0) et Mastercard IPM (Type 1000) sont simulés avec des hypothèses centralisées (VisaBaseIISimConfig, MastercardIpmSimConfig). Les autres TC/TCR/Types (TC06/07/25, TCR1/2/3, Type 1100/1200/1300) sont des stubs vides — nécessitent la vraie spec réseau pour implémenter.
- Validation après chaque bloc : `curl` direct port 8085, codes 2xx attendus, 401 sans token.
- Crédit : intérêts = solde d'ouverture × (APR/12). Pas d'intérêts si relevé précédent payé intégralement (grâce). Min payment = max(closing × pct%, floor). Ledger via CREDIT_RECEIVABLE (ASSET) + CREDIT_FUNDING (LIABILITY) en partie double.
- Crédit frontend : `const data = await api.xxx()` sans destructuring `{ data }`. Les routes GET = ADMIN/OPERATOR/ANALYST, écritures = ADMIN/OPERATOR. `AntPathRequestMatcher` pour `/api/v1/credit/**`.
- Crédit : valeurs par défaut proposées (APR 18%, min 5%/10 TND, grace period) — pas de décision métier sans validation client. Conformité réglementaire (taux d'usure, disclosures) hors scope technique.

## Bugs corrigés
- **POST /interchange/configure 500** : `@Valid` sur `Map<String, Object>` dans `ClearingController.configureInterchange()` — Spring Validation tente de valider un `Map` non typé, ce qui échoue après l'exécution du handler (l'entité est bien persistée mais la réponse est 500 → l'utilisateur croit que la création a échoué). Fix : retrait de l'annotation `@Valid`. Vérifié runtime : POST retourne désormais 200.
- **Loyalty loadMemberships** : 3 causes cumulées — (1) frontend appelait `listByCardholder('all')` mais l'API exige un UUID, (2) aucun endpoint backend ne listait toutes les adhésions, (3) `loadMemberships()` n'était déclenché par aucun `useEffect`. Fix : ajout de `GET /api/v1/loyalty/memberships` + `api.loyalty.memberships.list()` + `useEffect([activeTab])` pour charger les adhésions au clic sur l'onglet.
- **Stand-in 3 bugs** : (1) `SwitchCore` passait `"VISA"` en dur à `attemptStandIn()` → Mastercard/CB/Amex jamais matché ; (2) DE 18 jamais extrait → `mcc=null` → toute règle avec restriction MCC retournait `false` ; (3) `findRule(null,cardBrand)` plantait `IncorrectResultSizeDataAccessException` avec plusieurs règles ALL sans issuer, ET ignorait les règles globales de marque spécifique avant de tomber sur ALL. Fix : `resolveCardBrand(pan)` via BIN 8→6, extraction DE 18 en début de `processIso8583Message()`, `findRule` avec garde `issuerId!=null` + recherche marque globale avant fallback ALL. Prouvé runtime : Mastercard 550000 → log `MASTERCARD`, VISA-only rule → Mastercard DECLINED (NO_RULE), VISA → APPROVED. 393 tests verts.
- **POST /admin/participants** : String `metadata` mappée sur `JSONB` via `@Column(columnDefinition = "JSONB")` → PostgreSQL rejette l'insert car Hibernate bind le paramètre `String` comme `Types.VARCHAR` et PG ne caste pas implicitement `text` → `jsonb`. Fix : ajout de `@JdbcTypeCode(SqlTypes.JSON)` sur le champ — Hibernate 6 utilise alors le bon type JDBC (`Types.OTHER` avec les métadonnées JSON). 393 tests verts, runtime POST 200.
- **Bug sécurité device fingerprint** : `DeviceFingerprintService.evaluate()` appelait `scoreDevice()` APRÈS `registerFingerprint()` → `registerFingerprint` mettait à jour l'IP avant la comparaison → `scoreDevice` voyait toujours IP identique → score 0.0, rendant la détection de changement d'IP/device inactive. Fix : `scoreDevice()` déplacé AVANT `registerFingerprint()`, avec `isKnownDevice()` pour l'état pré-enregistrement. Prouvé runtime : IP connue → score 0, IP différente → +30 pts, device inconnu → +90 pts. (commit [inséré])
- **UUID `String` vs `UUID` résiduels** : `HoldRecord.cardId`/`cardAccountId` et `DeviceFingerprintRecord.cardId` déclarés `String` avec `@Column(length=64)` mais les appels passaient `Request.cardId` qui est `UUID` → conversions `.toString()`/`UUID.fromString()` dans toute la chaîne + perte de typage. Fix : migration `String→UUID` sur les 2 entités, repos, services, et contrôleurs — retire les conversions manuelles. Suppression de `.id(UUID.randomUUID())` dans `HoldService.placeHold()` et `DeviceFingerprintService.registerFingerprint()` (pattern `@GeneratedValue` incompatible avec setId manuel causant `StaleObjectStateException`).
- **Règle "Block High Amount" inactive** : `FraudEngine.matchesCondition()` appelait `evaluateExpression()` qui retournait `false` pour toute expression non reconnue, et retournait aussi `false` pour `amount>{expr}` simple (confusion avec clause `false != true` au niveau supérieur). Fix : `evaluateExpression()` retourne `null` pour les formats non supportés, et `matchesCondition()` n'utilise le résultat que si non-null, laissant la règle tomber dans le `switch/case` par catégorie. Prouvé runtime : transaction > 10000 → score augmenté de 30pts.
- **VelocityCheck enregistrement** : `FraudEngine.recordForVelocity()` créait `VelocityCheck` sans `windowStart`/`windowEnd` → requêtes de vélocité par fenêtre cassées. Fix : ajout `windowStart=now`, `windowEnd=now+1hour`.

## Bugs connus non corrigés
- **BinTable.resolveCardBrand ignore bin_length** : `findByBinAndIsActiveTrue(bin)` retourne
  `Optional<BinTable>` mais n'a aucun filtre sur `binLength`. Si deux entrees ont le meme `bin`
  avec des `binLength` differents (permis par la contrainte `(bin, bin_length, participant_id)`),
  l'`Optional` lance `IncorrectResultSizeDataAccessException` — exactement le meme pattern
  que le bug stand-in. Exemple : entree `bin=550000, binLength=6, brand=VISA` et entree
  `bin=550000, binLength=8, brand=VISA` → un PAN commencant par 550000 trouve 2 lignes → crash.
  **Fix attendu** : remplacer `Optional` par `List<BinTable>` dans la signature et le retour
  de `findByBinAndIsActiveTrue` (ou creer une methode dedice filtrant par `binLength`),
  puis trier la liste du BIN le plus long au plus court et prendre le premier resultat.
  Alternativement, ne pas filtrer par `binLength` et laisser le tri par longueur de `bin`
  (colonne) suffit car 8-chiffre != 6-chiffre dans la colonne `bin` — mais le cas
  meme-valeur-de-bin-avec-lengths-differents doit etre protege par `List`.

## Patterns interdits (anti-régression)

- **`requestMatchers(HttpMethod, String)` dans SecurityConfig** → utiliser `requestMatchers(new AntPathRequestMatcher(path, method))` impérativement. La version `String` crée un `MvcRequestMatcher` qui **ne match que les routes avec un handler Spring MVC**. Toute route sans controller (forward vers `/error`, 404, etc.) reçoit un 401/403 fantôme.
- **`.id(UUID.randomUUID())` / `setId(UUID.randomUUID())` sur une entité `@GeneratedValue`** → laisser Hibernate générer l'ID. Le set manuel fait croire à Hibernate que l'entité est *détachée* (existe déjà) plutôt que *nouvelle* → `merge()` au lieu de `persist()` → `StaleObjectStateException` au `save()`.
- **`String` sur `@Column(columnDefinition = "JSONB")` sans `@JdbcTypeCode(SqlTypes.JSON)`** → Hibernate bind le String comme `Types.VARCHAR` mais PostgreSQL attend un `jsonb` via `Types.OTHER`. L'insert échoue avec `ERROR: column "x" is of type jsonb but expression is of type character varying`. Toujours ajouter `@JdbcTypeCode(SqlTypes.JSON)` sur les champs String mappés à une colonne JSON/JSONB.

## Bugs connus non corrigés

### ✅ Done — Lot 3 : RegulatoryReports FR guide + label maps
- **RegulatoryReportsHelp.tsx** : guide 6 sections + PERIODICITY_LABELS (DAILY, MONTHLY), FORMAT_LABELS (CSV). Explique les 4 modèles (bct-daily, bct-monthly, sibtel-daily, sibtel-monthly) et l'état stub.
- **Intégration** : help button + PERIODICITY_LABELS sur les cartes templates dans RegulatoryReports.tsx.
- **Runtime validé** : GET 4 templates, POST generate retourne CSV stub.

### ✅ Done — Lot 3 : ConfigLive FR guide + label maps
- **ConfigLiveHelp.tsx** : guide 6 sections + CATEGORY_LABELS (9 catégories), DATA_TYPE_LABELS (5 types). Fausses affirmations corrigées (validation type, revalidation routage/cache, audit table → log seulement). Tableau d'impact par catégorie ajouté (section 3, SWITCH=Critique). FAQ valeur invalide détaille getInt() silencieux → défaut 0.
- **Intégration** : help button + CATEGORY_LABELS (badges) + DATA_TYPE_LABELS (colonne type) dans ConfigLive.tsx.
- **Corrections appliquées** : section 2 validation retirée, section 3 impact table, section 5 étapes corrigées (2 au lieu de 4), section 6 FAQ (valeur invalide + journalisation logs vs audit).
- **Runtime validé** : GET 23 paramètres, PUT update temps réel (value → 999999).

### ✅ Done — Volet Online : RBA 3DS (V060)
- **RBA core (AcsService.evaluateRba)** : pont entre l'authentification 3DS et le FraudEngine — évalue le score de risque via 4 catégories (static rules, behavioral profile, velocity, device fingerprint), prend la décision (AUTHENTICATED < 30, CHALLENGE_REQUIRED 30-69, DECLINED ≥ 70). Seuils configurables dans `application.yml` (`score-low-threshold=30`, `score-high-threshold=70`).
- **V060 migration** : ajoute `risk_score` (INTEGER) + `risk_decision` (VARCHAR 20) à `acs_authentications`.
- **Phase 1 — Frictionless 3DS** : `POST /api/v1/simulator/ecommerce/frictionless` → EPG → 3DS → AReq → RBA réel → score < 30 → AUTHENTICATED (CAVV/ECI générés) → autorisation. Vérifié runtime.
- **Phase 2 — Défi navigateur OTP** : `POST /api/v1/simulator/ecommerce/challenge` initie (score 30-69 → CHALLENGE_REQUIRED + OTP créé), `POST …/challenge/verify` valide OTP → AUTHENTICATED. Vérifié runtime + DB.
- **Phase 3 — Défi app mobile** : `POST /api/v1/simulator/ecommerce/app-challenge` initie (challengeType=APP_NOTIFICATION), `POST …/app-challenge/respond` (APPROVE/REJECT) → AUTHENTICATED/DECLINED. Vérifié runtime + DB.
- **Phase 4 — Batch transactionnel (vrai RBA)** : `POST /api/v1/simulator/ecommerce/batch` distribue N transactions entre 4 profils de risque calibrés (LOW_RISK, MEDIUM_DOMESTIC, MEDIUM_FOREIGN, HIGH_RISK) — chaque transaction passe par le vrai `AcsService.evaluateRba()`. Distribution approximative (documentée). Répartition par profils définie par `frictionlessPercent`, `challengePercent`, `appChallengePercent`, `declinedPercent`.
- **Multi-cartes batch** : `cardIds` (List<UUID>) remplace `cardId` (UUID) — chaque transaction pioche une carte aléatoire dans le pool, répartissant la vélocité. `clearVelocity` au début du batch (ardoise vierge). Pré-enregistrement device fingerprints pour les profils utilisateurs connus.
- **Device fingerprint fix (bug sécurité)** : `scoreDevice()` déplacé AVANT `registerFingerprint()` — la comparaison d'IP fonctionne désormais. Prouvé runtime : IP connue → score 0, IP nouvelle → +30, device inconnu → +90. Détection IP active prouvée : +30 entre même IP et IP différente sur un device connu.
- **Bugs résiduels corrigés** : UUID `String→UUID` sur HoldRecord/DeviceFingerprintRecord/AuthorizationEngine, `evaluateExpression()` retourne `null` au lieu de `false` pour les expressions non supportées, `VelocityCheck.windowStart/windowEnd` ajoutés, `Block High Amount` rule active, `.id(UUID.randomUUID())` supprimé de HoldService/DeviceFingerprintService.
- **Tests** : 393 passent (AcsServiceTest injecte le mock FraudEngine).

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
- **B.1 — Infrastructure réseau** : `NetworkClearingGenerator` (interface + ReconciliationResult), `Iso20022ClearingGenerator` (réutilise Iso20022Engine), `VisaBaseIIGenerator` (TC 05 TCR 0, 168c) + `MastercardIpmGenerator` (Type 1000, 200c) + `VisaBaseIIFormatter`/`MastercardIpmFormatter`.
- **B.2 — Fichier de règlement net BCT** : `BctSettlementService.generateBctSettlementFile()` agrège les positions nettes par institution domestique (exclut étrangères via `Participant.isDomestic`, migration V053). CSV provisoire en attendant FCOMPSMT.BCT officiel.
- **B.3 — Endpoints** : `GET /clearing/files/bct?date=`, `GET /clearing/reconciliation`, `GET /clearing/reports/quarterly`. `POST /clearing/files/incoming` accepte `participantId`. Formats génération : CSV, ISO20022, COMPCONF, CP50, VISA, MASTERCARD.
- **B.4 — Frontend** : BCT download card, Quarterly report card, Reconciliation history table, VISA/MC dans le sélecteur de format.
- **B.5 — Réconciliation + Rapports** : `SettlementFileService.ingestIncomingClearingFile()` persiste `ReconciliationRecord` (source=SCHEME, statut MATCHED/PARTIALLY_MATCHED). `SchemeReportService.generateQuarterlyReport()` (volumes par type, somme, moyenne).
- **Tests** : 326 backend pass, `npm run build` frontend OK.

### ✅ Done — Visa BASE II (TC 05 TCR 0, format réel)
- `VisaBaseIISimConfig` centralise 24 constantes d'hypothèses simulateur (marquées "Hypothèse simulateur — à remplacer par la spec Visa du client"). Comprend ARN_BIN, ARN_ZONE, BID, pays, etc.
- `VisaBaseIIFormatter` : `an()` (left-justify, space-padded), `un()` (zero-padded), `amount()` avec map de décimales (TND/KWD/BHD=3).
- `VisaBaseIIGenerator.generate()` produit des lignes TC 05 TCR 0 de 168 chars exactement : TC (1-4), PAN (5-20), ARN structuré SZ+BIN+MMDD+txnKey+checksum (27-49), BID acquéreur (50-57), montant en millimes (62-73), devise, ville, pays 788, code auth (152-157), etc.
- `VisaBaseIIGenerator.ingest()` parse les 168c par ARN → `ClearingRecordRepository.findAll()` → match par normalized transactionId.
- `generateTc06()`, `generateTc07()`, `generateTc25()`, `generateTc05Tcr1/2/3()` : stubs `""` avec log warn (nécessitent vraie spec Visa, pas appelés par le cycle nominal).
- Cycle REST : `POST /api/v1/simulator/clearing/cycle/visa-baseii` — 10 matchés, 1 écart simulé.
- **Honest status** : ce simulateur couvre le cas nominal (transaction financière de base) mais PAS l'intégralité du format BASE II (TC06 chargeback, TC07 representment, TC25 fee collection, TCR1/2/3 country-specific manquent).

### ✅ Done — Module 1/3 : Credit / Revolving (V055)
- **Bloc 1.1 — Migration V055 + JPA** : `V055__credit_accounts.sql` (4 tables + 2 ledgers). Entités `CreditLine`, `CreditStatement`, `InstallmentPlan`, `InstallmentEntry`. Repos Spring Data JPA. `mvn clean compile` OK.
- **Bloc 1.2 — CreditLineService** : `openCreditLine()` (création + mise à jour CardAccount type → CREDIT), `authorize()` (vérifie plafond, pose hold), `postPurchase()` (ledger CREDIT_RECEIVABLE + CREDIT_FUNDING), `postPayment()` (inverse, marque OPEN paid si full), `releaseHold()`. Toute écriture ledger vérifiée débit=crédit.
- **Bloc 1.3 — StatementService** : `@Scheduled` quotidien relève si `statementDay == today`. Intérêts = `openingBalance × (APR/12/100)` si précédent NON payé intégralement. Min payment = `max(closing × pct%, floor)`. Marquage OVERDUE après due_date.
- **Bloc 1.4 — InstallmentService** : `convertToInstallments()` (N échéances mensuelles + frais répartis), `markEntryPaid()` (décrémente remaining, COMPLETED si 0).
- **Bloc 1.5 — Controller + Frontend** : 14 endpoints `/api/v1/credit/**`. Page CreditLines.tsx avec cards + tabs (details/statements/installments/simulate) + modal d'ouverture. Routes sécurisées AntPathRequestMatcher. i18n EN/FR.
- **Bloc 1.6 — Tests** : 13 tests (CreditLineServiceTest, StatementServiceTest, InstallmentServiceTest) — authorization > limit refuse, purchase+payment→zero balance, ledger équilibré, intérêts 15 TND exacts, min payment floor/pct, 12 échéances de 100.
- **Total** : 379 tests backend pass, `npm run build` frontend OK. Smoke test 401 (sans token) confirme route enregistrée.

### ✅ Done — Mastercard IPM (Type 1000, format simulé)
- `MastercardIpmSimConfig` centralise 18 constantes d'hypothèses simulateur (marquées "Hypothèse simulateur — à remplacer par la spec Mastercard"). Comprend RECORD_TYPE_1000, TX_CODE_PURCHASE, ICA acquéreur/émetteur, codes pays/devise, etc.
- `MastercardIpmFormatter`: `an()` (left-justify), `nn()` (right-justify numeric), `amount()` avec map de décimales.
- `MastercardIpmGenerator.generate()` produit des lignes Type 1000 de 200 chars exactement : record "1000" (1-4), trans "00" (5-6), clearing "00" (7-8), activity "20" (9-10), sender ref MC+MMDD+txnKey+checksum (34-57), acquiring ICA (58-65), issuing ICA (66-73), settlement sign "C" (74-75), amount 12c en millimes (76-87), currency 788 (88-90), country 788 (106-108), MCC 4c (157-160), auth code 6c (169-174), level indicator "1" (191), etc.
- `generateType1100()`, `generateType1200()`, `generateType1300()` : stubs `""` avec log warn (nécessitent vraie spec Mastercard, pas appelés par le cycle nominal).
- Cycle REST : `POST /api/v1/simulator/clearing/cycle/mastercard-ipm` — 10 matchés, 1 écart simulé.
- **Honest status** : ce simulateur couvre le Type 1000 (présentement financier) mais PAS Type 1100 (frais), 1200 (chargeback), 1300 (representment).

### ⏸️ Paused / blocked
- **Type 40 CP50** : layout inconnu, attend spec SMT/BPC.
- **3e format SMT** : non spécifié.
- **Nommage fichier** (CPMPAY23.NNNNN vs CP50bbbbb) : contrat banque, déjà configurable.
- **Source slipNumber** : upstream non disponible, fallback 000001.
- **Layout FCOMPSMT.BCT** : spec BCT non publique, CSV provisoire.
- **Gabarits rapports trimestriels Visa/MC** : structure générique en attendant.

### ✅ Done — Module 2/3 : Loyalty / Fidélité (V056)
- **V056 Migration** : 6 tables (loyalty_programs, loyalty_tiers, loyalty_memberships, loyalty_transactions, loyalty_rewards, loyalty_redemptions) + seed program Standard avec tiers Silver/Gold/Platinum.
- **JPA models** : 6 entités (LoyaltyProgram, LoyaltyTier, LoyaltyMembership, LoyaltyTransaction, LoyaltyReward, LoyaltyRedemption) + 6 repositories.
- **LoyaltyService** : program CRUD + toggle, tier creation, member enrollment, earn/burn points (rate × multiplier), auto tier upgrade, reward management, reward + balance credit redemption. Montage points = `amount × earningRate × tierMultiplier`.
- **REST controller** : 18 endpoints `/api/v1/loyalty/**` — programs, tiers, enrollment, earn/burn, rewards, redemptions. Sécurité via `requestMatchers` (GET = ADMIN/OPERATOR/ANALYST, writes = ADMIN/OPERATOR).
- **Frontend** : page `Loyalty.tsx` avec 3 tabs (Programs, Memberships, Rewards). Programs → tiers inline, Memberships → enrollment + earn/burn + transaction/redemption history, Rewards → CRUD. Route `/loyalty`, nav item.
- **i18n** : EN/FR for loyalty section.
- **Tests** : 379 backend pass, `npm run build` frontend OK.

### ✅ Done — Module 3/3 : Transfers A2A / P2P (V059)
- **V059 Migration** : `transfers` + `transfer_limits` + `transfer_beneficiaries` + seed `TRANSFER_FEE_INCOME` ledger (INCOME) et `SETTLEMENT_MAIN` lien ledger.
- **JPA models** : Transfer, TransferLimit, TransferBeneficiary + 3 repos.
- **TransferConfig** : `FeeConfig` fixed+percent pour A2A (`a2a.fixed=5`, `a2a.percent=0`) et P2P (`p2p.fixed=2`, `p2p.percent=0`).
- **TransferService** : `executeA2A()` / `executeP2P()` / `reverseTransfer()` + `resolvePanToAccount()` (suffix → card → cardholder → accounts.get(0)) + `resolveDestinationRef()` (UUID ou account_number) + `validateAccounts()` + `computeFee()` + `checkLimits()` + `postFeeLedger()`.
- **Atomicité transactionnelle** : `@Transactional` sur executeA2A + executeP2P — tout `RuntimeException` déclenche rollback complet (vérifié runtime PostgreSQL).
- **Contrôle balance** : `source.getAvailableBalance().compareTo(totalDebit) < 0` avant débit.
- **Ledger fees** : partie double `TRANSFER_FEE_INCOME` (INCOME) ↔ `SETTLEMENT_MAIN` (LIABILITY).
- **Controller** : 5 endpoints `POST /api/v1/transfers/{a2a,p2p,{id}/reverse,...}`.
- **Security** : `AntPathRequestMatcher` POST = ADMIN/OPERATOR, GET = +ANALYST.
- **Frontend** : page `Transfers.tsx` avec forms A2A (comptes) + P2P (suffixes cartes), transfer history, limits. Route `/transfers`, nav ícone.
- **Tests** : 12 tests TransferServiceTest (A2A nominal, solde insuffisant, currency mismatch, limits, destination inactive, reverse, P2P nominal, source=destination, fees).
- **Runtime A2A validé** : nominal (A=50000→49695, B=15000→15300, fee 5 posté TRANSFER_FEE_INCOME), atomicité dest absente (A=49695 inchangé), atomicité dest INACTIVE (A=49695 inchangé — preuve rollback runtime).
- **Runtime P2P validé** : cross‑account (source suffix 1234 → dst UUID → 50 TND + fee 2 TND → COMPLETED), source=destination rejet (400), atomicité dest INACTIVE (rollback prouvé).
- **Total** : 393 tests backend pass, `npm run build` frontend OK.

### ✅ Done — Lot 2 : Issuing FR guide + label maps
- **IssuingHelp.tsx** : panneau latéral Aide avec 6 sections en français (concepts, pas à pas, statuts, FAQ).
- **Label maps exportées** : CARD_STATUS_LABELS, ACCOUNT_STATUS_LABELS, CARDHOLDER_STATUS_LABELS, TOKEN_STATUS_LABELS, CARD_PRODUCT_LABELS, WALLET_PROVIDER_LABELS, CARD_ACTION_LABELS, NOTIFICATION_TYPE_LABELS + getNotificationLabel().
- **Issuing.tsx** : tous les statuts/selects/actions traduits via labels ; StatusBadge utilise label ; notification type via getNotificationLabel().
- **Runtime test** : blocage carte → BLOCKED, déblocage → ACTIVE, crédit compte +1000 → solde 16300. Hold 30 min confirmé dans HoldService.java (Duration.ofMinutes(30) + @Scheduled expireHolds).
- **Guide vérifié** : PIN et tokenisation correspondent au code. FAQ corrigée : capture via flux d'autorisation, pas depuis Issuing.

### ✅ Done — Lot 2 : Ecommerce FR guide + label maps
- **EcommerceHelp.tsx** : panneau latéral Aide avec 6 sections (concepts, pas à pas, statuts 3DS, FAQ).
- **Label maps** : CARDHOLDER_LANGUAGE_LABELS, CHALLENGE_LABELS, METHOD_PREFERENCE_LABELS, EPG_INTEGRATION_LABELS (4 types : HOSTED, API, IFRAME, WEBHOOK).
- **Ecommerce.tsx** : EPG merchantId dropdowns avec create + status toggle. Guide vérifié contre EpgService, ThreeDsService, AcsService.
- **Runtime test** : création PG, listing PGs, lien merchant, porteur 3DS, transaction 3DS. 400/401 codes validés.
- **Bug trouvé** : `EpgService.setMerchant()` recherche `merchantId` mais le MERCHANT n'a pas ce champ — seule la table `merchant_terminal` a un `merchant_id`. Non bloquant (création PG sans merchant).

### ✅ Done — Lot 2 : StandIn FR guide + label maps
- **StandInHelp.tsx** : guide + STANDIN_DECISION_LABELS (3 valeurs), STANDIN_REASON_LABELS (7 raisons).
- **StandIn.tsx** : auth.reason traduit via STANDIN_REASON_LABELS ; auth.decision via STANDIN_DECISION_LABELS.
- **Runtime test** : create/update/delete rule, listing all rules OK.
- **Bugs 1-3 (corrigés commit a9ce614)** : `SwitchCore` passait `"VISA"` hardcodé + `mcc=null` + `findRule` ignorait les règles globales de marque. Voir section Bugs corrigés.

### ✅ Done — Lot 2 : FxRates FR guide + label maps
- **FxRatesHelp.tsx** : guide 6 sections (concepts, pas à pas, DCC, FAQ).
- **Bug backend** : `FxService.convert()` incluait la marge (rate × (1+margin%)), et `proposeDcc()` appelait `convert()` puis ajoutait encore la marge → marge². Fix : `convert()` ne fait plus que `amount × rate` ; `proposeDcc()` ajoute la marge une seule fois.
- **Frontend** : ajout du champ `marginPercentage` dans le formulaire de création.
- **Runtime test** : create rate + convert + DCC proposé OK.

### ✅ Done — Lot 2 : CofPage FR guide + label maps
- **CofHelp.tsx** : guide 6 sections + TOKEN_STATUS_LABELS, SCHEDULE_STATUS_LABELS, FREQUENCY_LABELS, TOKEN_TYPE_LABELS.
- **Bug backend** : `processDueSchedules()` n'avait pas d'annotation `@Scheduled` — ajout de `@Scheduled(cron = 0 0 5 * * *)`.
- **Guide corrigé** : PAN display max 8 caractères (colonne DB `length=8`), retrait mentions "plafond" et "transaction au switch" (seulement mise à jour du schedule).
- **Runtime test** : create token (8-char PAN), create schedule, list tokens/schedules OK.

### ✅ Done — Lot 2 : Transactions FR guide + label maps
- **TransactionsHelp.tsx** : guide 6 sections + 5 label maps + 2 color maps (TRANSACTION_STATUS_COLORS, CHANNEL_COLORS).
- **Toutes les maps déjà utilisées dans Transactions.tsx** : statuts, types, canaux, couleurs — aucune modification nécessaire.
- **Guide vérifié** : sections techniques (SwitchCore, RoutingEngine, stand-in, clearing) exactes par rapport au code.
- **Runtime test** : liste, filtre POS, filtre PURC, combinaison POS+PURC OK (11 transactions, 8 POS, 8 PURC).

### ✅ Done — Lot 3 : BinTables FR guide + label maps
- **BinTablesHelp.tsx** : guide 6 sections + CARD_BRAND_LABELS, CARD_TYPE_LABELS.
- **Intégration** : help button + label maps dans BinTables.tsx.
- **Bug connu** : `resolveCardBrand` utilise `Optional<BinTable>` mais pas de filtre `binLength` → doublon de `bin` possible → `IncorrectResultSizeDataAccessException`.

### ✅ Done — Lot 3 : RoutingRules FR guide + label maps
- **RoutingRulesHelp.tsx** : guide (priority ASC, first-match-short-circuit, 7 operators, JSON format `{"operator":"AND","rules":[...]}`), PROTOCOL_LABELS, RULE_STATUS_LABELS.
- **Runtime validé** : CRUD + toggle rule. Guide corrigé (exemples JSON en format réel, pas simplifié).

### ✅ Done — Lot 3 : FeeSchedules FR guide + label maps
- **FeeSchedulesHelp.tsx** : guide (priority DESC vs RoutingRules ASC, INTERCHANGE_LOOKUP intact description "code mort", 5 calc methods). SCHEDULE_TYPE_LABELS, SCHEDULE_STATUS_LABELS, APPLIES_TO_LABELS, CALC_METHOD_LABELS.
- **Runtime validé** : CRUD + 4 calculs (flat, percentage, clamp-min, clamp-max).
- **INTERCHANGE_LOOKUP** : décrit dans la FAQ comme non connecté à la table d'interchange (retourne toujours 0).

### ✅ Done — Lot 3 : InterchangeFees FR guide + label maps
- **InterchangeFeesHelp.tsx** : guide (formule, cascade wildcard, 2 systèmes indépendants). REGION_LABELS + réutilisation CARD_BRAND_LABELS/CARD_TYPE_LABELS depuis BinTablesHelp.
- **Runtime validé** : GET list, PUT update, DELETE, 3 calculs (1,30/2,75/7,00 TND), POST retourne 200 après fix `@Valid`.
- **INTERCHANGE_LOOKUP documenté comme code mort** : guide explique que cette table n'est pas liée aux barmes de frais.

### ✅ Done — Lot 3 : CardPrograms FR guide + label maps
- **CardProgramsHelp.tsx** : guide 6 sections + PROGRAM_TYPE_LABELS (8 types), PROGRAM_STATUS_LABELS (4 statuts), PRODUCT_CARD_TYPE_LABELS (DEBIT/CREDIT/PREPAID/CHARGE/VIRTUAL), CARD_NETWORK_LABELS (VISA_NET/MASTERCARD_NET/CB_NET/AMEX_NET/VERVE_NET).
- **Intégration** : help button + label maps dans CardPrograms.tsx. Cartes types/produits/nouveaux labels.
- **Extension BinTablesHelp** : ajout VERVE à CARD_BRAND_LABELS, ajout CHARGE + VIRTUAL à CARD_TYPE_LABELS.
- **Runtime validé** : CRUD program, activate/deactivate, create product activable, cascading delete program→product (204). 3 seeds confirmés (Classic TND, Premium Business, Platinum Rewards).

### ✅ Done — Lot 3 : Participants FR guide + label maps
- **ParticipantsHelp.tsx** : guide 6 sections (types, statuts, endpoint, protocoles, flag domestique) + PARTICIPANT_TYPE_LABELS (4), PARTICIPANT_STATUS_LABELS (3), ENDPOINT_TYPE_LABELS (4). Tableau des 12 seeds.
- **Intégration** : help button + label maps dans Participants.tsx (type badges, status, selects).
- **Bug backend JSONB** : String `metadata` → JSONB nécessitait `@JdbcTypeCode(SqlTypes.JSON)`. Fixé.
- **Runtime validé** : CRUD participant (POST 200, PUT SUSPENDED, GET by ID, DELETE 204). 393 tests verts.


## Key Decisions
- **Figage (V050)** : champs SMT figés à processClearing(), pas à la génération.
- **Représentation = clone ClearingRecord** (pas flag), idempotent via findByDisputeId.
- **COMPCONF inversion banques** : chargeback/fee swap acquéreur ↔ émetteur.
- **CP50 = 500c** (prouvé par fichiers de production).
- **Processing code routing** : DE 3 (2 premiers digits) + MTI dérive transactionType. 0100→PRAU, 0200/00xxxx→PURC, 0200/20xxxx→REFD, 0220/02xxxx→COMP, 0220/autre→REVS, 0400/0420→REVS.
- **Channel** : DE 22: 01-05→POS, 06/10-19→ATM, 80-99→ECOM, défaut→POS.
- **Visa BASE II / MC IPM : implémentés avec hypothèses simulateur** : les formats sont remplis (TC 05 TCR 0 pour Visa, Type 1000 pour Mastercard) mais avec des valeurs d'hypothèse centralisées dans VisaBaseIISimConfig / MastercardIpmSimConfig — chaque hypothèse est marquée "Hypothèse simulateur — à remplacer par la spec du client". Les autres types (TC06/07/25, TCR1/2/3, Type 1100/1200/1300) sont des stubs vides.
- **BCT CSV** : format provisoire, en attendant FCOMPSMT.
- **`Participant.isDomestic`** (V053) : distingue banques domestiques/étrangères pour le fichier BCT.
- **Crédit calcul intérêts** : implémenté sur `openingBalance × (APR/12/100)`. Le modèle initial proposait le **solde moyen journalier** (qui donne un montant différent selon la date des achats). Ce choix (**openingBalance** vs **average daily balance**) est une décision métier non tranchée — à confirmer par le client avec l'APR, le floor, et la période de grâce avant toute mise en production.
- **Crédit min payment** : `max(closingBalance × minPaymentPct%, minPaymentFloor)`.
- **Crédit grace period** : intérêts facturés seulement si précédent relevé NON payé intégralement.
- **Crédit ledger** : nouveaux comptes `CREDIT_RECEIVABLE` (ASSET) + `CREDIT_FUNDING` (LIABILITY) via partie double existante.
- **Crédit holds** : `CreditLine.holdAmount` (pas CardAccount.holdAmount) pour éviter interférence avec le flux débit.
- **Migration crédit** : V055_\_credit_accounts.sql (après V054__set_foreign_participants_domestic_false.sql).
- **AccountType.CREDIT** : existe déjà dans CardAccount — pas de migration d'enum nécessaire.
- **Fees A2A/P2P** : configurées dans `application.yml` via `feeConfig.a2a.fixed=5, percent=0` / `feeConfig.p2p.fixed=2, percent=0`.
- **Ledger fees** : `TRANSFER_FEE_INCOME` (INCOME) ↔ `SETTLEMENT_MAIN` (LIABILITY) en partie double.
- **Atomicité transfers** : `@Transactional` rollback complet sur `RuntimeException` — vérifié runtime avec destination UUID inexistante et destination INACTIVE.
- **P2P résolution PAN** : suffix → `CardRepository.findByCardNumberSuffix()` → `CardAccountRepository.findByCardholderId()` → `accounts.get(0)`. Simplifié en attendant HMAC hash complet.
- **Card creation bug connu** : `@GeneratedValue(GenerationType.UUID)` sur `Card.id` + `card.setId(UUID.randomUUID())` manuel dans `createCard()` → `isNew()` retourne false → `merge()` appelé → `StaleObjectStateException`. Fix : supprimer `@GeneratedValue` ou retirer le setId manuel.
- ~~**Settlement creation bug** : MÊME PATTERN que Card — `MerchantSettlement` a `@GeneratedValue(GenerationType.UUID)` sur `id` (MerchantSettlement.java:19) mais `MerchantSettlementService.createSettlement()` (ligne 27) appelle `.id(UUID.randomUUID())` → `StaleObjectStateException` au `save()`. **CORRIGÉ** : retrait de `.id(UUID.randomUUID())` ligne 27 (same pattern as Card fix).~~
- ~~**Merchant status mismatch** : frontend select `Acquiring.tsx:413` liste `PENDING_APPROVAL` mais le backend `MerchantStatus` enum contient `PENDING_ONBOARDING` (pas PENDING_APPROVAL). **CORRIGÉ** : select aligné sur les 5 valeurs backend (ACTIVE, PENDING_ONBOARDING, SUSPENDED, TERMINATED, UNDER_REVIEW).~~

## Next Steps
1. **UI tableau de bord batch** : afficher les résultats du batch dans une page frontend simple (Phase 6).
2. **Persistance d'historique des simulations** : enregistrer chaque run batch pour rejeu et comparaison.
3. **Variations montant/devise** dans le batch.
4. **Corriger `CardService.createCard()`** : `@GeneratedValue` + setId manuel → `StaleObjectStateException`.
5. **À l'arrivée des specs** : type 40, 3e format, nommage fichier, slipNumber, Visa BASE II TC06/07/25 et TCR1/2/3, Mastercard IPM Type 1100/1200/1300, layout BCT FCOMPSMT.

## Critical Context
- **Dernière migration** : V060__add_risk_score_to_acs.sql (ajoute risk_score + risk_decision à acs_authentications).
- **JAVA_HOME** : /opt/homebrew/Cellar/openjdk@21/21.0.11.
- **Backend** : 393 tests passent (dont VisaBaseIITcr0Test 24 tests, NetworkClearingGeneratorTest 6 tests avec vérification format Mastercard 200c). Frontend : npm run build OK.
- **Seuils RBA** : `score-low-threshold=30`, `score-high-threshold=70` (application.yml) — score < 30 → AUTHENTICATED, 30-69 → CHALLENGE_REQUIRED, ≥ 70 → DECLINED.
- **8 endpoints simulator actifs** : `POST /api/v1/simulator/ecommerce/{frictionless,challenge,challenge/verify,app-challenge,app-challenge/respond,batch}` + `POST /api/v1/simulator/clearing/cycle/visa-baseii` + `POST /api/v1/simulator/clearing/cycle/mastercard-ipm` — tous permitAll via AntPathRequestMatcher.
- **Device fingerprint fix prouvé** : `DeviceFingerprintService.evaluate()` appelle `scoreDevice()` AVANT `registerFingerprint()`. IP connue → 0 pts, IP nouvelle → 30 pts, device inconnu → 90 pts. Prouvé runtime par différence de score entre IP identique et IP différente sur device connu.
- **Batch distribution approximative** : les % définissent la répartition des profils d'entrée, PAS les statuts de sortie. Le RBA décide réellement.
- **OTP** stocké en clair dans `acs_challenges.challengeData` (pas de SMS réel).
- **BehavioralProfileService** nécessite ≥ 5 échantillons avant de pouvoir détecter des anomalies.
- **Comptes ledger crédit** : CREDIT_RECEIVABLE (ASSET), CREDIT_FUNDING (LIABILITY) — seedés dans V055.
- **Comptes ledger fees** : TRANSFER_FEE_INCOME (INCOME), SETTLEMENT_MAIN (LIABILITY) — seedés dans V059.
- **AccountType.CREDIT** : existe déjà dans `CardAccount`, pas de migration enum.
- **Visa BASE II (TC 05 TCR 0) et Mastercard IPM (Type 1000)** : implémentés, validés runtime avec vérification substring position par position. Les hypothèses simulateur sont dans VisaBaseIISimConfig / MastercardIpmSimConfig. TC06/07/25/TCR1/2/3 et Type 1100/1200/1300 sont des stubs vides.
- **Card creation bug** : `@GeneratedValue` + manual `setId()` → `StaleObjectStateException`. Ne pas créer de cartes via API tant que non corrigé.
- **Convention signe netting vs BCT** : `NettingRecord.netAmount = totalSent - totalReceived`. Négatif = participant reçoit (créancier net). BCT `net_position = totalReceived - totalSent`, signe inverse. Les deux sont cohérents, juste conventions opposées (comptable vs flux).
- Kafka UnknownHostException cosmétique — HTTP fonctionne.
- Serveur port 8085 : `mvn spring-boot:run -Dspring-boot.run.profiles=dev` avec env vars `PCI_ENCRYPTION_KEY`, `PAN_HASH_KEY`, `JWT_SECRET`, `PIN_ENCRYPTION_KEY`, `CORS_ALLOWED_ORIGINS`.

## UX à améliorer

- **Issuing — Set PIN : le champ pinBlock est dans la section Verify PIN au lieu de Set PIN (Issuing.tsx:335 vs 344).** L'utilisateur qui suit le guide ("saisissez le PIN en clair et un pin block") ne voit qu'un champ rawPin dans Set PIN ; le second champ pinBlock est physiquement sous l'intitulé "Verify PIN". À corriger : déplacer le champ pinBlock dans la section Set PIN pour que les deux champs soient côte à côte.
- **CardPrograms — imports morts (CardPrograms.tsx:12)** : `Pencil`, `Trash2`, `RefreshCw` importés de `lucide-react` mais jamais utilisés dans le JSX. Aucun bouton Modifier ni Supprimer n'existe dans l'interface. Nettoyage cosmétique : supprimer les 3 imports.
- **CardPrograms — isReissuable:true en dur sans UI (CardPrograms.tsx:281)** : `isReissuable: true` dans le state par défaut du ProductForm mais aucun checkbox correspondant dans le formulaire (lignes 327-364). La valeur est envoyée `true` à l'API sans que l'utilisateur puisse la modifier. Défaut UI mineur : ajouter un checkbox ou retirer le champ du form state.

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

### MODULE 2/3 — Loyalty / Fidélité
- `resources/db/migration/V056__loyalty_programs.sql` : 6 tables + seed data
- `model/loyalty/` : LoyaltyProgram, LoyaltyTier, LoyaltyMembership, LoyaltyTransaction, LoyaltyReward, LoyaltyRedemption (JPA)
- `repository/loyalty/` : 6 Spring Data repos
- `service/loyalty/LoyaltyService.java` : program/tier/membership/points/rewards/redemptions
- `controller/loyalty/LoyaltyController.java` : 18 endpoints `/api/v1/loyalty`
- `frontend/src/pages/Loyalty.tsx` : 3 tabs (programs, memberships, rewards) with inline tiers + modals
- `frontend/src/types/index.ts` : 6 loyalty interfaces
- `frontend/src/services/api.ts` : loyalty.* endpoints
- `frontend/src/App.tsx` : route `/loyalty`
- `frontend/src/components/Layout.tsx` : nav Loyalty
- `frontend/src/i18n/en.json` : loyalty section
- `frontend/src/i18n/fr.json` : loyalty section

### MODULE 3/3 — Transfers A2A / P2P
- `resources/db/migration/V059__transfers.sql` : 3 tables + 2 ledgers seeds
- `model/transfer/Transfer.java` : JPA entity with type/amount/fee/status/source/destination
- `model/transfer/TransferLimit.java` : per-transfer-type daily limits
- `model/transfer/TransferBeneficiary.java` : saved beneficiaries
- `repository/transfer/TransferRepository.java`
- `repository/transfer/TransferLimitRepository.java`
- `repository/transfer/TransferBeneficiaryRepository.java`
- `config/TransferConfig.java` : FeeConfig (fixed + percent)
- `service/transfer/TransferService.java` : executeA2A/executeP2P/reverseTransfer + fee/ledger/limits
- `controller/transfer/TransferController.java` : 5 endpoints `/api/v1/transfers`
- `frontend/src/pages/Transfers.tsx` : A2A + P2P forms + history + limits
- `frontend/src/types/transfers.ts` : transfer interfaces
- `frontend/src/types/index.ts` : + transfer types
- `frontend/src/services/api.ts` : transfers.* API calls
- `frontend/src/App.tsx` : route `/transfers`
- `frontend/src/components/Layout.tsx` : nav IconArrowsLeftRight
- `frontend/src/i18n/en.json` : transfer section
- `frontend/src/i18n/fr.json` : transfer section
- `test/.../transfer/TransferServiceTest.java` : 12 tests (A2A/P2P, limits, fees, atomicity, reverse)
- `frontend/src/components/CardProgramsHelp.tsx` : guide + PROGRAM_TYPE_LABELS, PROGRAM_STATUS_LABELS, PRODUCT_CARD_TYPE_LABELS, CARD_NETWORK_LABELS
- `frontend/src/pages/CardPrograms.tsx` : help button + label maps (types, statuts, brand, network)
- `frontend/src/components/ParticipantsHelp.tsx` : guide + PARTICIPANT_TYPE_LABELS, PARTICIPANT_STATUS_LABELS, ENDPOINT_TYPE_LABELS
- `frontend/src/pages/Participants.tsx` : help button + label maps (type, status, selects)
- `model/Participant.java` : fix `@JdbcTypeCode(SqlTypes.JSON)` pour le champ metadata (String→JSONB)
- `service/ParticipantService.java` : CRUD participant
- `frontend/src/components/RegulatoryReportsHelp.tsx` : guide + PERIODICITY_LABELS, FORMAT_LABELS
- `frontend/src/pages/RegulatoryReports.tsx` : help button + periodicity label maps
- `frontend/src/components/ConfigLiveHelp.tsx` : guide + CATEGORY_LABELS, DATA_TYPE_LABELS
- `frontend/src/pages/ConfigLive.tsx` : help button + category/data type label maps
- `service/clearing/network/visa/VisaBaseIISimConfig.java` : centralisation hypothèses Visa BASE II (24 constantes)
- `service/clearing/network/visa/VisaBaseIIFormatter.java` : formatteurs champs (an/un/amount)
- `service/clearing/network/mastercard/MastercardIpmSimConfig.java` : centralisation hypothèses Mastercard IPM (18 constantes)
- `service/clearing/network/mastercard/MastercardIpmFormatter.java` : formatteurs champs (an/nn/amount)
- `service/simulator/ClearingCycleService.java` : +executeMastercardIpmCycle()
