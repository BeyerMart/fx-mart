# fx-mart Feature Set Overview

## Product goal

Deliver a reliable Android currency converter with clear visual history for a selected amount.

## Primary user

User who wants quick conversion, visual trend context, and low-friction currency selection.

## Core value

- Convert amount between currencies.
- Inspect historical converted values for same amount.
- Scrub chart to inspect value on selected dates.
- Recover gracefully from API failure through cached data.
- Reach common currencies quickly through favorites and tolerant search.

## In scope for MVP

- Latest conversion for selected base currency, target currency, and amount.
- Historical chart for selected pair and amount.
- Range presets: `30D`, `MTD`, `YTD`, `1Y`, `5Y`, `MAX`.
- Touch scrub interaction on chart.
- Free API integration for currencies, latest rates, and historical rates.
- Cached fallback with stale warning.
- Favorites-based currency discovery.
- Search by code, currency name, and country name.

## Out of scope for MVP

- Real-time streaming rates.
- Multi-pair compare chart.
- User accounts or sync.
- Buy/sell spreads, fees, or trading execution.
- Chart export or social sharing.
- Background alerts as release blocker.

## Key UX rules

- Converter remains usable in one screen.
- Chart opens with same pair and amount as converter context.
- Chart values reflect selected amount, not only unit rate.
- Stale data must be visible, not implied.
- Favorites reduce selection friction but do not limit full currency access.

## Dependencies

- Architecture baseline in `fx-mart-architecture.md`.
- Public free rates provider: Frankfurter API.
- Local cache for latest successful data.

## Risks

- Free API availability can affect first-load experience.
- Country-name search needs supplemental metadata.
- Long historical ranges may contain sparse dates.

## Cross-feature acceptance criteria

### AC1: End-to-end conversion context
Given app has loaded supported currencies
When user selects base currency, target currency, and amount
Then app shows a converted result and allows opening a historical chart for same pair and amount

### AC2: Historical amount continuity
Given user opens chart from a valid conversion context
When chart screen loads
Then chart header and chart points use same base currency, target currency, and amount from converter

### AC3: Failure transparency
Given fresh network data cannot be loaded and cached data exists
When converter or chart displays cached data
Then screen shows stale warning and retry action

### AC4: No-data recovery
Given fresh network data cannot be loaded and no cached data exists
When converter or chart finishes loading
Then screen shows explicit error state and no misleading result value

### AC5: Currency access
Given API-supported currencies are available
When user searches or browses currency selection
Then user can find currencies through favorites, code, name, or country label matching

## Assumptions

- Assumption (needs validation): default launch pair remains `USD -> EUR`.
- Assumption (needs validation): default amount remains `1.00`.
- Assumption (needs validation): `MTD` and `YTD` use device local date at load time.
