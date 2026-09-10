# CocktailCraft 🍸

CocktailCraft is a modern Android application built for home bartenders and enthusiasts. It goes beyond simple recipe storage by integrating your actual bar inventory with your recipe library to show you exactly what you can create.

## Key Features

### 🏡 My Bar (Smart Dashboard)
- **Instant Availability**: Automatically identifies which cocktails in your library are "Ready to Make" based on your current inventory.
- **Unrated Reminders**: Keeps track of recipes you've added but haven't rated yet so you never forget to try something new.
- **Top Picks**: Shows your highest-rated drinks for quick access.

### 🍾 Intelligent Inventory
- **Barcode & Brand Search**: Easily add items to your cabinet.
- **Visual Reference**: High-quality, uncropped bottle photos with a full-screen preview.
- **Stock Management**: Track expiry dates for perishable items (syrups, juices, vermouths).
- **Ingredient Integration**: Link specific bottles (e.g., *Tanqueray*) to general ingredients (*Gin*) for seamless recipe matching.

### 📚 Personalized Library
- **Split-Base Support**: Create complex recipes using multiple specific spirits (e.g., two types of Rum in a Mai Tai).
- **Preferred Brands**: Specify a "wishlist" brand for a recipe even if you don't own it yet; the app will automatically link it when you add it to your inventory.
- **Smart Imports**: Fetch classic specs from the web with intelligent unit conversion and ingredient auto-matching.
- **Rating History**: Track every "tweak" you make to a drink and see your average ratings.

### 🥂 Glassware Guide
- **Visual Dictionary**: Integrated guide for 10+ standard and specialty glass types.
- **Tap-to-Identify**: Click on any glass name in a recipe to see a line-art illustration and description of that vessel.

## Technology Stack
- **UI**: Jetpack Compose with Material 3.
- **Architecture**: MVVM with Clean Architecture principles.
- **Database**: Room (SQL) with complex relational queries for inventory matching.
- **DI**: Hilt (Dagger).
- **Networking**: Retrofit & OkHttp.
- **Image Loading**: Coil.

## Getting Started
1. Clone the repository.
2. Open in **Android Studio Ladybug** (or newer).
3. Build and Run. No API keys are required for core functionality.

## License
This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
