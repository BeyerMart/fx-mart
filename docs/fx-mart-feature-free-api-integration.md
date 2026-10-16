# Free API Integration Spec

## Objective

Define product requirements for exchange-rate data used by fx-mart.

## Primary provider

Frankfurter API.

## Scope

Included:
- Supported currency list retrieval
- Latest rate retrieval
- Historical range retrieval
- Cached fallback after failed refresh
- User-visible freshness and failure states

Excluded:
- Paid provider failover
- Authenticated API access
- Real-time streaming quotes

## Product rules

- App loads supported currencies from provider-backed source.
- App loads latest rate data for converter.
- App loads historical range data for chart presets.
- Visible data state must distinguish fresh data, cached stale data, and no data.
- Pair shown in UI must match pair used for latest conversion or chart data.
- Unsupported pairs must not produce misleading result values.

## Failure rules

- Failed refresh with cached data available shows cached data plus stale warning.
- Failed refresh without cached data shows explicit recoverable error.
- Retry action remains available after recoverable failure.

## Acceptance criteria

### AC1: Currency population
Given provider request for supported currencies succeeds
When currency selector opens
Then selector contains provider-supported currencies for user selection

### AC2: Latest-rate retrieval
Given user requests conversion for supported pair
When latest-rate request succeeds
Then converter uses provider-backed latest data for that pair

### AC3: Historical retrieval
Given user opens chart for supported pair and supported preset
When historical request succeeds
Then chart renders date-ordered historical values for that pair and preset

### AC4: Cached transparency
Given provider request fails and cached data exists for requested screen context
When app displays fallback data
Then app labels data as stale and offers retry action

### AC5: No-data transparency
Given provider request fails and no cached data exists for requested screen context
When app completes load
Then app shows explicit error state and no misleading conversion or chart value

### AC6: Unsupported pair handling
Given provider does not support requested pair or range
When user attempts conversion or chart load
Then app blocks misleading output and shows unsupported-state messaging

## Assumptions

- Assumption (needs validation): provider availability and rate cadence are sufficient for MVP positioning as "latest available".
- Assumption (needs validation): app stores last successful fetch timestamp per latest-rate context and historical-range context.
- Assumption (needs validation): request volume stays within practical limits for public API usage.
