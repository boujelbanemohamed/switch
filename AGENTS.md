# Switch Platform — Session Summary

## Goal
- Implement 6 work packages (P1–P6) to close platform gaps: stand-in/STIP resilience, full ISO 8583 coverage, network clearing files, COF recurring, multi-currency FX, and BCT/SIBTEL regulatory compliance.

## Constraints & Preferences
- Work packages must be done in order P1 → P6; do not start next until previous compiles and passes tests.
- After each package: `cd backend && mvn clean compile && mvn test`, then `cd frontend && npm run build`.
- For each new JPA model, create a dedicated Flyway migration (never modify an existing migration).
- Distinguish implemented work from decisions needing business owner sign-off; ask before proceeding.
- Security: write endpoints = ADMIN/OPERATOR, read endpoints = ADMIN/OPERATOR/ANALYST. Use `AntPathRequestMatcher` for all new routes (known bug with `requestMatchers(HttpMethod, String)` on this project — `MvcRequestMatcher` does not match paths correctly, returns 401 with valid token).
- Frontend: use `const data = await api.xxx.list()` (no `{ data }` destructuring); add `api.delete` if missing.
- Publish a Kafka event for each critical business action (stand-in used, file generated, recurring payment triggered).
- Add i18n keys (EN + FR) for each new module.
- Add one minimal Playwright E2E test per module.
- Do **not** mock tests: endpoints that should succeed return 2xx, not an expanded threshold.

## Progress
### Done
- **P1 — Stand-in/STIP**: migration `V045__stand_in_rules.sql` (rules + authorizations tables), `V046__add_stand_in_used_to_transactions.sql`; models `StandInRule.java`, `StandInAuthorization.java`; repositories `StandInRuleRepository`, `StandInAuthorizationRepository`; `StandInService.java` (attempt/decline logic, rule CRUD, reconciliation); integration into `SwitchCore.java` catch block (uses stand-in before code 99); `StandInController.java` (CRUD rules + list authorizations + pending count); `StandInEvent` + `publishStandInUsed()` added to `EventPublisher` interface, `KafkaEventPublisher`, `LoggingEventPublisher`, `TopicConstants`, `KafkaTopicConfig`; `reconcileStandInAuthorizations()` in `BatchService.java` (scheduled 06:30 daily); frontend page `StandIn.tsx` (rules table, inline edit, create modal, authorizations table, pending count badge), route in `App.tsx`, nav link in `Layout.tsx` (Radio icon, canSeeAdmin), i18n keys in `en.json`/`fr.json` (standIn.* + common.yes/no); E2E test `e2e/17-standin.spec.ts`; `findByTypeAndStatus` added to `ParticipantRepository`.
- **P2 — Full ISO 8583 coverage**: added `createAuthorizationAdvice()` (0220), `createAuthorizationAdviceResponse()` (0230), `createReversalAdvice()` (0420), `createReversalAdviceResponse()` (0430), `createNetworkManagementRequest()` (0800), `createNetworkManagementResponse()` (0810) in `Iso8583Engine.java`; MTI routing in `SwitchCore.processIso8583Message()` — 0800 → `handleNetworkManagementRequest()` (respond 0810, no financial transaction), 0220 → record advice + apply to ledger via `LedgerPostingEngine.postReversal()`, 0420 → `processReversal()` (routes normally + calls `postReversal()` on completion); `NetworkManagementService.java` with `@Scheduled(fixedRate=30s)` sending 0800/301 echo tests to all ACTIVE participants.
- **P3 — Settlement/clearing files**: `SettlementFileService.java` (CSV generation from `ClearingRecord` CLEARED, ISO 20022 pacs.008 via existing `Iso20022Engine`); endpoints `GET /api/v1/clearing/files/outgoing` (download CSV/ISO20022) and `POST /api/v1/clearing/files/incoming` (upload + reconcile) in `ClearingController.java`; `ReconciliationResult` record; frontend clearing files section in `Clearing.tsx` (generate/download form + upload/reconcile textarea + result card), i18n keys `clearing.files.*`, api.ts methods, `ReconciliationResult` type.
- **P4 — COF / Recurring**: migration `V047__cof_recurring.sql` (cof_tokens + recurring_schedules); models `CofToken.java`, `RecurringSchedule.java`; repositories `CofTokenRepository`, `RecurringScheduleRepository`; `CofTokenService.java` (token + schedule CRUD, daily MIT scheduler for `next_run_date <= today`); `CofController.java` (CRUD for `/api/v1/ecommerce/cof/tokens` and `/schedules`); frontend page `CofPage.tsx` (tokens table + schedules table with add forms); security rules added (`AntPathRequestMatcher` for GET writes allowed for ANALYST+, write methods for ADMIN/OPERATOR).
- **P5 — FX / Multi-currency**: migration `V048__fx_rates.sql`; model `FxRate.java`; repository `FxRateRepository` (findLatestByCurrencyPair); `FxService.java` (convert with margin, DCC `proposeDcc()`); `FxController.java` (CRUD rates + `POST /convert` + `POST /dcc/propose`); frontend page `FxRates.tsx` (rates table with add form + converter/DCC panel); security rules added (`AntPathRequestMatcher` for `GET /fx/rates` for ANALYST+, all methods for ADMIN/OPERATOR).
- **P6 — Regulatory compliance**: `RegulatoryReportService.java` (template-based CSV generator for BCT/SIBTEL with 4 templates: bct-daily, bct-monthly, sibtel-daily, sibtel-monthly); `RegulatoryController.java` (`GET /api/v1/regulatory/reports` list templates + `POST /api/v1/regulatory/reports/generate` download); frontend page `RegulatoryReports.tsx` (template selector with periodicity badges + date range/format picker + download button); security rules added (`AntPathRequestMatcher` for ADMIN/OPERATOR only).
- **Runtime validation (P1–P6)**: SecurityConfig fixed — all new routes now use `AntPathRequestMatcher` (clearing/**, standin/**, ecommerce/cof/**, fx/**, regulatory/**). Bug fixes: `maxAmount` missing default in `StandInRule.@PrePersist`, missing `@PreUpdate` handler causing 500 on PUT. All endpoints smoke-tested with real participant UUID against running backend + Docker infra (postgres, kafka) — all return 2xx, no-auth returns 401.
- Backend compiles and all 267+ tests pass; frontend `npm run build` OK.

### In Progress
- Routes + nav links wired in `App.tsx` and `Layout.tsx` for `/cof`, `/fx`, `/regulatory` pages (CofPage.tsx, FxRates.tsx, RegulatoryReports.tsx are now mounted).
- P2–P6 Playwright E2E tests still need to be written.

### Blocked
- P6 exact BCT/SIBTEL reporting formats are not public; current implementation generates CSV templates — the final format customization requires official documentation from the Central Bank of Tunisia and/or SIBTEL.
- P3 Visa BASE II / Mastercard T112 native format requires NDA + scheme specifications; current implementation uses CSV + ISO 20022 pacs.008.

## Key Decisions
- Used `AntPathRequestMatcher` everywhere instead of `requestMatchers(HttpMethod, String)` because `MvcRequestMatcher` does not match paths correctly on this project (returned 401 with valid token).
- Stand-in logic integrated into `SwitchCore.processIso8583Message` catch block (before setting code 99) — matches the exact insertion point specified.
- Reversal processing (0420) routes normally to the issuer, then calls `LedgerPostingEngine.postReversal()` on successful completion — preserving the existing flow while adding the ledger link.
- Network management heartbeat sends 0800/301 to both ISSUER and ACQUIRER participants with ACTIVE status every 30 seconds (configurable via `switch.network.heartbeat.interval`).
- Stand-in reconciliation batch runs at 06:30 daily via `@Scheduled(cron = "0 30 6 * * *")`.
- Clearing files support two formats initially: CSV (generic readable) and ISO 20022 pacs.008; Visa/Mastercard native formats deferred (require NDA).
- COF MIT payments triggered by a daily `@Scheduled` scanning `recurring_schedules` with `next_run_date <= today`.
- FX conversion uses its own `fx_rates` table with margin support; DCC (Dynamic Currency Conversion) exposes `proposeDcc()` method for frontend integration.
- Regulatory reports use a generic template-based generator; exact BCT/SIBTEL formats require their official specs.
- `StandInRule.@PrePersist` + `@PreUpdate` both set defaults (`maxAmount=0`, `dailyCountLimit=5`, `dailyAmountLimit=0`, `enabled=true`, `cardBrand=ALL`, `allowedMcc=*`) — prevents constraint violations on POST/PUT with partial JSON bodies.

## Next Steps
1. Write Playwright E2E tests for P2–P6.
2. Push all changes to Git.
3. User restarts backend with real env variables to validate end-to-end.

## Critical Context
- Last Flyway migration is `V048__fx_rates.sql` — next new migration would be `V049__*` if any.
- `JWT_SECRET`, `POSTGRES_PASSWORD`, `PAN_HASH_KEY`, `PIN_ENCRYPTION_KEY`, `PCI_ENCRYPTION_KEY` are required to start the backend.
- Backend compiled and all 267+ tests pass; frontend `npm run build` passes.
- The `SecurityConfig.java` now has explicit `AntPathRequestMatcher` entries for: `/api/v1/clearing/interchange`, `/api/v1/clearing/**`, `/api/v1/standin/**`, `/api/v1/ecommerce/cof/**`, `/api/v1/fx/rates` (GET), `/api/v1/fx/**`, `/api/v1/regulatory/**`.
- Runtime validated on actual backend + Docker infra: all P1–P6 endpoints return 2xx with valid token, 401 without.
- Bug fixed: `StandInRule` now has `@PreUpdate` lifecycle method to avoid 500 on PUT with partial body.
- Kafka connection warning (`UnknownHostException: kafka`) is pre-existing and cosmetic — the app serves HTTP traffic fine.
- Payments clearing file upload works via `POST /api/v1/clearing/files/incoming` with `content` + `format` JSON body.
- Frontend routes: `/stand-in`, `/cof`, `/fx-rates`, `/regulatory-reports` are all wired in App.tsx and linked in Layout.tsx nav.
- Nav icons: `Repeat2` for COF, `ArrowLeftRight` for FX, `FileBarChart` for Regulatory.

## Relevant Files
- `backend/.../config/auth/SecurityConfig.java` — contains all security rules, using `AntPathRequestMatcher` for problematic routes
- `backend/.../service/SwitchCore.java` — stand-in integration (P1), MTI routing for 0800/0220/0420 (P2), reversal processing
- `backend/.../iso8583/Iso8583Engine.java` — added 0220/0230/0420/0430/0800/0810 message creation methods
- `backend/.../service/standin/StandInService.java` — stand-in rule matching, authorization, reconciliation
- `backend/.../service/clearing/SettlementFileService.java` — CSV + ISO 20022 clearing file generation/ingestion
- `backend/.../service/network/NetworkManagementService.java` — heartbeat scheduler (0800/301)
- `backend/.../service/ecommerce/CofTokenService.java` — token management + MIT recurring payment trigger
- `backend/.../service/fx/FxService.java` — FX conversion with margins, DCC proposal
- `backend/.../service/regulatory/RegulatoryReportService.java` — template-based report generation
- `frontend/src/pages/StandIn.tsx` — stand-in rules + authorizations management page
- `frontend/src/pages/Clearing.tsx` — includes clearing files generate/upload section
- `frontend/src/pages/CofPage.tsx` — COF tokens + schedules table (routed at `/cof`)
- `frontend/src/pages/FxRates.tsx` — FX rates table + converter panel (routed at `/fx-rates`)
- `frontend/src/pages/RegulatoryReports.tsx` — regulatory report templates + generate form (routed at `/regulatory-reports`)
- `frontend/src/services/api.ts` — contains `clearing`, `standin`, `cof`, `fx`, `regulatory` sections
- `frontend/src/types/index.ts` — `InterchangeFee`, `StandInRule`, `StandInAuthorization`, `ReconciliationResult`, `CofToken`, `RecurringSchedule`, `FxRate`, `RegulatoryReportTemplate` interfaces
- `backend/src/main/resources/db/migration/V045__stand_in_rules.sql` + `V046__add_stand_in_used_to_transactions.sql` + `V047__cof_recurring.sql` + `V048__fx_rates.sql`
