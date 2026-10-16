# fx-mart Product Specs Index

## Recommended `docs/` structure

```text
docs/
├── fx-mart-product-specs-index.md
├── fx-mart-architecture.md
├── fx-mart-feature-set.md
├── fx-mart-feature-core-converter.md
├── fx-mart-feature-historical-chart.md
├── fx-mart-feature-currency-discovery.md
├── fx-mart-feature-free-api-integration.md
└── fx-mart-roadmap.md
```

## Scope

Product feature documents for fx-mart Android app.

## Source context

- Existing app baseline: Android app with converter, chart, watchlist, cache, free API integration.
- Existing architecture doc: `fx-mart-architecture.md`.
- Primary data source: Frankfurter API.
- Historical presets requested: `30D`, `MTD`, `YTD`, `1Y`, `5Y`, `MAX`.
- Offline behavior requested: show last successful cached data with warning.
- Currency scope requested: all API-supported currencies; frequent currencies managed through favorites.
- Search tolerance requested: code, currency name, country name.

## Documents

1. [Feature Set Overview](./fx-mart-feature-set.md)
2. [Core Converter](./fx-mart-feature-core-converter.md)
3. [Historical Chart](./fx-mart-feature-historical-chart.md)
4. [Currency Discovery and Favorites](./fx-mart-feature-currency-discovery.md)
5. [Free API Integration](./fx-mart-feature-free-api-integration.md)
6. [Roadmap](./fx-mart-roadmap.md)

## Shared product rules

- Currency labels use 3-letter code plus readable name when available.
- Conversion and chart flows carry same base currency, target currency, and amount unless user changes them.
- Network failure never silently overwrites visible data.
- Cached fallback must display stale warning plus retry action.
- Amount-based chart values represent converted amount for selected amount on each date.
- Assumption (needs validation): country-name search uses local metadata because Frankfurter does not provide country names directly.
- Assumption (needs validation): `MAX` range maps to earliest date supported by selected API for selected pair.
