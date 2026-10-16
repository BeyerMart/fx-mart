# fx-mart Roadmap

## MVP

### Goal

Ship a reliable currency converter with practical historical exploration.

### Deliverables

- Core converter with amount, pair selection, swap, and result state.
- Historical chart with amount-based plotting.
- Presets: `30D`, `MTD`, `YTD`, `1Y`, `5Y`, `MAX`.
- Swipe or drag scrub interaction for chart inspection.
- Frankfurter-backed currency, latest-rate, and historical-rate data.
- Cached fallback with stale warning and retry action.
- Favorites-based currency discovery.
- Search by code, currency name, and country name.

### MVP exit criteria

- User can complete conversion without hidden setup.
- User can open chart from conversion context and inspect date-specific values.
- App distinguishes fresh data, stale cached data, and no-data failure.
- User can quickly find common currencies through favorites or search.

## Later phase 1

- Stronger empty states and onboarding hints.
- Better tablet and large-screen chart layout.
- Faster selector UX for long lists.
- Recent pairs and pinned default pair.

## Later phase 2

- Compare current value versus selected historical point.
- Mini sparklines in favorites or watchlist.
- Dual display mode for converted amount and raw rate.
- More explicit accessibility refinements for chart inspection.

## Later phase 3

- Rate alerts for favorite pairs.
- Home-screen widget.
- Shareable chart snapshot.
- Localization expansion.

## Risks

- Free API availability may reduce perceived reliability.
- Country-name search requires supplemental metadata.
- Very long-range data may be sparse or uneven.

## Assumptions

- Assumption (needs validation): alerts and widgets remain post-MVP scope.
- Assumption (needs validation): MVP success depends more on clarity and resilience than on feature breadth.
