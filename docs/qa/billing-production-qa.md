# dBcheck Billing production QA

Date: 2026-06-29

Scope: Osa 96 release-readiness QA for Google Play Billing, `dbcheck_pro`, purchase-state handling, restore behavior, and the release entitlement guard. This document is a QA checklist and risk record, not a product-scope change.

Official docs checked before writing:
- Google Play Billing integration: https://developer.android.com/google/play/billing/integrate
- One-time product lifecycle: https://developer.android.com/google/play/billing/lifecycle/one-time
- Billing test purchases: https://developer.android.com/google/play/billing/test
- Play Console in-app products: https://support.google.com/googleplay/android-developer/answer/1153481

## Current local state

- Billing Library 8.3.0 is declared in `gradle/libs.versions.toml`.
- Manifest declares `com.android.vending.BILLING`.
- App product ID is `dbcheck_pro` via `BillingManager.PRO_PRODUCT_ID`.
- Play Console: NOT VERIFIED in this turn. No authenticated Play Console access or Play release track evidence was available from the local workspace.
- Manual purchase QA: NOT RUN in this turn. No license-tester device/account, Play-distributed build, or Play Console product evidence was available.
- License tester purchase: NOT RUN.

## Play Console checklist

| Item | Required state | Current evidence |
|---|---|---|
| One-time product | `dbcheck_pro` exists as an in-app product / one-time product in Play Console. | Play Console: NOT VERIFIED. |
| Product type | Product is a non-consumable entitlement, not a consumable credit or subscription. | Code uses `BillingClient.ProductType.INAPP`; console state NOT VERIFIED. |
| Activation | Product is active for the release track used for QA. | NOT VERIFIED. |
| Price/locales | Price and localized Play purchase copy are configured for launch countries. | NOT VERIFIED. |
| License testers | At least one tester account can install the Play-distributed build and use Play test instruments. | License tester purchase: NOT RUN. |
| Package/signing match | Purchase QA uses the release package/signing lineage expected by Play. | Deferred to Osa 97 - Release signing QA. |

## Automated billing contract

| Flow | Code contract | Automated evidence |
|---|---|---|
| Product details | `launchPurchaseFlow(...)` calls `queryProductDetails` for `dbcheck_pro` as `INAPP` immediately before launching Play Billing. | `BillingManagerTest.launchPurchaseFlowStartsBillingWithQueriedProductDetails`; `BillingProductionQaTest`. |
| Purchase complete | `PurchaseState.PURCHASED` for `dbcheck_pro` sets `isPurchased = true`, acknowledges unacknowledged purchases with `acknowledgePurchase`, and emits `PurchaseEvent.Completed` only for user-completed purchase callbacks. | `BillingManagerTest.queriedPurchasedProductIsAcknowledgedWithoutUserCompletionEvent`; `BillingManagerTest.acknowledgePurchaseExceptionEmitsFailureAndKeepsBillingEventsAlive`. |
| Pending | `PurchaseState.PENDING` emits `PurchaseEvent.Pending`, does not acknowledge, and does not unlock Pro before Play returns purchased state. | `BillingManagerTest.pendingPurchaseUpdateDoesNotUnlockProAndEmitsPendingEvent`. |
| Cancelled | `USER_CANCELED` emits `PurchaseEvent.Cancelled`; Settings clears launch loading without a persistent error. | `BillingManagerTest.acknowledgePurchaseExceptionEmitsFailureAndKeepsBillingEventsAlive`; `SettingsViewModelPurchaseTest.cancelledPurchaseClearsLoadingWithoutPersistentError`. |
| Already-owned | `ITEM_ALREADY_OWNED` does not blindly unlock. It runs `queryPurchasesAsync`, processes the verified `dbcheck_pro` snapshot, acknowledges if needed, then emits `PurchaseEvent.AlreadyOwned` only when a real purchase is found. | `BillingManagerTest.alreadyOwnedResponseDoesNotUnlockBeforeVerifiedProPurchase`; `BillingManagerTest.alreadyOwnedResponseRefreshesAndAcknowledgesExistingPurchase`. |
| Restore | Startup/resume restore uses `refreshPurchases()` -> `queryPurchasesAsync` for `INAPP`; `MainActivity.onResume()` calls refresh so Play-side changes are picked up. | `BillingManagerTest.refreshPurchasesProcessesCurrentPurchaseSnapshot`; source review. |
| User messages | Billing debug messages are not exposed to users on launch failures. | `BillingFailureMessagesTest`. |

## Release entitlement

Release entitlement must come from the verified purchase state only:

- `ProEntitlementPolicy.isProUser(isPurchased = false, isDebugBuild = false, debugForceFreeEnabled = false)` is `false`.
- `ProEntitlementPolicy.isProUser(isPurchased = false, isDebugBuild = false, debugForceFreeEnabled = true)` is `false`.
- `ProEntitlementPolicy.isProUser(isPurchased = true, isDebugBuild = false, debugForceFreeEnabled = false)` is `true`.

The debug Pro default is intentionally limited to debug builds. Release build must not open debug Pro, and `debugForceFreeEnabled` must not create or remove a release entitlement by itself.

## Manual purchase QA script

Run these only with a Play-distributed build installed by a license tester account.

1. Verify Play Console:
   - `dbcheck_pro` exists.
   - Product is active and available for the QA release track.
   - Tester account is configured as a license tester and can install the Play-distributed build.
2. Fresh purchase, approved:
   - Use `Test card, always approves`.
   - Start purchase from Settings Pro card.
   - Verify Play Billing sheet shows the `dbcheck_pro` product.
   - Complete purchase.
   - Verify Settings shows success, Pro gates open, and a cold restart keeps Pro after `queryPurchasesAsync`.
3. Pending purchase:
   - Use `Slow test card, approves after a few minutes`.
   - Verify Settings shows pending copy through `PurchaseEvent.Pending`.
   - Verify Pro gates stay locked until Play returns purchased state.
   - After approval, foreground/resume the app and verify restore refresh opens Pro.
4. Pending decline:
   - Use `Slow test card, declines after a few minutes`.
   - Verify pending state does not unlock Pro and later decline does not leave a stale success state.
5. Declined purchase:
   - Use `Test card, always declines`.
   - Verify user-facing failure copy appears and debug billing message is not shown.
6. Cancelled purchase:
   - Open Play Billing sheet and cancel.
   - Verify `PurchaseEvent.Cancelled` clears loading and leaves no persistent error.
7. Already-owned / restore:
   - On an account that already owns `dbcheck_pro`, press Upgrade again.
   - Verify `PurchaseLaunchResult.AlreadyOwned` / `PurchaseEvent.AlreadyOwned` path is reached only after `queryPurchasesAsync` returns the product.
   - Clear app data, reinstall from Play, and verify restore on startup/resume.
8. Release entitlement:
   - Install release build from Play, not a debug build.
   - Use an account without the purchase and verify Pro is locked.
   - Use an account with the purchase and verify Pro opens only after Play purchase refresh.

## Release risks and follow-up ownership

- Release risk: Play Console `dbcheck_pro` was not verified in this turn. Treat the product setup as unproven until an authenticated Play Console check confirms product type, activation, track availability, country/price, and tester access.
- Release risk: Manual purchase QA: NOT RUN. No test-card purchase, pending, decline, cancel, already-owned, or restore evidence was captured.
- Release risk: Pending transactions require real Play Billing test instruments; unit tests prove app policy but cannot prove Play-side state transitions.
- Release risk: Restore requires a Play-distributed build and account state; local debug install is insufficient.
- Release risk: release signing and Play package/signing lineage are owned by Osa 97 - Release signing QA.
