# Historical Chart Feature Spec

## Objective

Let user inspect how one entered amount would have converted across time.

## User story

As a user
I want to scrub a historical chart for the same amount and pair
So that I can inspect past converted values by date

## Scope

Included:
- Chart for selected base currency, target currency, and amount
- Presets: `30D`, `MTD`, `YTD`, `1Y`, `5Y`, `MAX`
- Amount-based plotting
- Swipe or drag scrub interaction
- Detail tooltip or floating value card during scrub
- Header summary for selected point and latest point
- Loading, stale, empty, and error states

Excluded:
- Multi-pair comparison
- Candlestick view
- Export or sharing
- Technical indicators

## Rules

- Each plotted point represents converted value for selected amount on that date.
- Example: if amount is `50`, base is `CHF`, and target is `EUR`, each plotted point represents `50 CHF -> X EUR` on that date.
- Chart opens with same pair and amount as source flow.
- Changing preset reloads historical data for that preset.
- Changing amount with same pair and same loaded range recalculates plotted values from loaded rate series without requiring a new historical fetch.
- Scrubbing updates selected date and selected converted value.
- Scrubbing snaps to nearest available provider-supported date.
- Sparse dates remain valid; chart must not invent unsupported dates.
- Cached fallback may be shown only with visible stale warning.

## Edge cases

- Selected range has sparse data
- Selected amount changes after chart open
- Selected pair has no historical coverage
- Fresh historical request fails with cache present
- Fresh historical request fails without cache
- User stops scrubbing outside plotted area

## Acceptance criteria

### AC1: Chart entry context
Given user opens chart from valid conversion context
When chart screen loads
Then screen shows same base currency, target currency, and amount as source context

### AC2: Amount-based plotting
Given chart has historical data and selected amount
When points are rendered
Then each point reflects converted amount for that amount on that date

### AC3: Preset switching
Given chart is visible with one active preset
When user selects another preset
Then app reloads data for that preset and updates chart, labels, and summary

### AC4: Scrub inspection
Given chart contains visible data points
When user swipes or drags across chart
Then app updates highlighted point, date label, and converted value to nearest visible data point

### AC5: Scrub detail content
Given chart is open for amount `50`, base `CHF`, and target `EUR`
When user scrubs to a visible historical point
Then app shows the selected date, the unit rate for that date, and the converted amount for `50 CHF` on that date

### AC6: Amount recalculation without refetch
Given historical rate series is already loaded for selected pair and preset
When user changes only the amount
Then app recalculates chart values from loaded series without requiring a new historical request

### AC7: Cached historical fallback
Given fresh historical request fails and cached history exists for selected pair and preset
When chart resolves data load
Then app shows cached chart data with stale warning and retry action

### AC8: No-data historical state
Given no usable historical data exists from network or cache
When chart resolves data load
Then app shows explicit no-data or error state instead of empty chart

### AC9: Return consistency
Given user leaves chart and returns to converter
When converter becomes visible again
Then previously selected base currency, target currency, and amount remain unchanged unless user changed them

## Assumptions

- Assumption (needs validation): chart header shows both latest visible converted value and scrub-selected converted value.
- Assumption (needs validation): `MAX` requests earliest provider-supported start date for selected pair.
- Assumption (needs validation): amount changes on chart screen trigger recalculation from loaded rate series without requiring new historical fetch when pair and range stay unchanged.

