# Bidscube MAX Adapter

AppLovin MAX mediation adapter for Bidscube.

## Features

- Image, Video (including skippable), and Native ad support
- Automatic/override ad positioning (header, footer, sidebar, above/below fold, full-screen)
- Built-in GDPR/CCPA consent via Google UMP (handled by Bidscube SDK)

## Requirements

- Android API 24+ (target/compile 35), Java 11+
- AppLovin MAX SDK 13.0.0+
- Bidscube SDK 1.0.2+

## Integration (High-level)

1. Add Bidscube SDK to your app module:
   ```kotlin
   dependencies {
       implementation("com.bidscube:bidscube-sdk:1.0.2")
   }
   repositories { google(); mavenCentral() }
   ```
2. In AppLovin MAX, configure Bidscube as a custom network and map your ad units.
3. Ensure consent flow runs before requesting ads (Bidscube SDK uses Google UMP).

## Adapter Parameters

- Server parameters:
  - `app_id` (required): Your Bidscube application ID
- Local parameters (optional):
  - `is_native`: Set to `true` for native ad requests

## Testing

- Enable MAX test mode for your device/app.
- Optionally enable Bidscube SDK consent debug mode during development.

