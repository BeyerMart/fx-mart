# Currency Discovery and Favorites Feature Spec

## Objective

Let user find currencies quickly and manage a small preferred set.

## User story

As a user
I want to search currencies and maintain favorites
So that I can reach common pairs with less effort

## Scope

Included:
- Full list of API-supported currencies
- Search by 3-letter code
- Search by currency name
- Search by country name
- Add currency to favorites
- Remove currency from favorites
- Favorites surfaced before or above full list

Excluded:
- Server-synced favorites
- Multi-folder lists
- Personal tags

## Rules

- Full supported list remains available even when favorites exist.
- Favorites change only selection priority, not currency support.
- Search updates visible results without requiring extra navigation.
- Search returns partial matches across supported search fields.
- Empty search restores default browse state.

## Edge cases

- No matching search results
- Duplicate favorite toggle input
- Large currency list
- Country-name metadata unavailable for one currency

## Acceptance criteria

### AC1: Full-list access
Given supported currencies are loaded
When user opens currency selector
Then user can browse the full provider-supported currency list

### AC2: Search by code
Given currency selector is open
When user enters part or all of a 3-letter currency code
Then selector returns matching currencies by code

### AC3: Search by name
Given currency selector is open
When user enters part of a currency name
Then selector returns matching currencies by currency name

### AC4: Search by country
Given currency selector is open and country labels exist for indexed currencies
When user enters part of a country name
Then selector returns matching currencies by country label

### AC5: Add favorite
Given currency selector shows a non-favorite currency
When user marks that currency as favorite
Then currency appears in favorites state and remains selectable from full list

### AC6: Remove favorite
Given currency is already marked as favorite
When user removes favorite state
Then currency no longer appears in favorites section and remains selectable from full list

### AC7: No-match feedback
Given currency selector is open
When search returns no matches
Then selector shows explicit no-results feedback and preserves ability to clear search

## Assumptions

- Assumption (needs validation): country-name search uses local metadata enrichment because provider does not expose country names directly.
- Assumption (needs validation): favorites are stored locally on device for MVP.
- Assumption (needs validation): favorites are shown in a dedicated section above all currencies.

