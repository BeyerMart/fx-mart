# Core Converter Feature Spec

## Objective

Let user complete reliable currency conversion from one screen.

## User story

As a user
I want to enter an amount and convert it between two currencies
So that I can see an immediate and trustworthy result

## Scope

Included:
- Amount entry
- Base currency selection
- Target currency selection
- Swap action
- Converted result display
- Freshness state display
- Retry or manual refresh
- Open chart with same pair and amount

Excluded:
- Fees and spread simulation
- Multi-leg conversion
- Saved conversion history

## Rules

- Conversion uses latest available rate for selected pair.
- If base currency equals target currency, result equals entered amount.
- Invalid input blocks result display.
- Cached fallback may be used only with visible stale warning.
- Result display must identify visible pair.

## Edge cases

- Empty amount
- Negative amount
- Non-numeric amount
- Same-currency conversion
- Unsupported pair
- Fresh request fails with cache present
- Fresh request fails without cache

## Acceptance criteria

### AC1: Successful conversion
Given user selects supported base currency, target currency, and valid amount
When latest or cached usable rate is available
Then app shows converted amount for visible pair

### AC2: Swap behavior
Given user already selected base currency, target currency, and valid amount
When user taps swap
Then app exchanges currencies and recalculates result for same amount

### AC3: Same-currency result
Given base currency and target currency are identical
When user enters valid amount
Then app shows result equal to entered amount

### AC4: Invalid amount handling
Given amount field is empty, negative, or non-numeric
When conversion would otherwise run
Then app shows input guidance and does not show misleading converted result

### AC5: Cached fallback
Given fresh latest-rate request fails and cached rate exists for selected pair
When converter resolves data load
Then app shows converted result from cached data with stale warning and retry action

### AC6: No-data failure
Given fresh latest-rate request fails and no cached rate exists for selected pair
When converter resolves data load
Then app shows explicit error state and no converted result value

### AC7: Chart handoff
Given converter shows valid pair and valid amount
When user opens historical chart
Then chart receives same base currency, target currency, and amount

## Assumptions

- Assumption (needs validation): conversion recalculates automatically after each valid change.
- Assumption (needs validation): amount input accepts locale-aware decimal separator.
- Assumption (needs validation): display precision follows target currency convention when available; otherwise 2 decimals.
