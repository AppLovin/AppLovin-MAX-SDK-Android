# Bidscube + AppLovin MAX Integration (Android)

## Requirements

- **Android** minSdk 23+
- **AppLovin MAX SDK** 13.6.0+
- **Bidscube SDK** 1.0.2+
- **Google UMP SDK** 4.0.0+
- AppLovin **SDK Key** and **Ad Units**
- Bidscube **Application ID (`app_id`)**

---

## Add the Adapter

Copy the `Bidscube` adapter module from this repository into your project, then include it in **`settings.gradle`**:

```gradle
include ':app'
include ':Bidscube'
```

If the `Bidscube` folder is not at the project root, set the path:

```gradle
include ':Bidscube'
project(':Bidscube').projectDir = new File('path/to/Bidscube')
```

Add dependencies in **`app/build.gradle`**:

```gradle
dependencies {
    implementation 'com.applovin:applovin-sdk:13.6.0@aar'
    implementation 'androidx.browser:browser:1.6.0'

    implementation project(':Bidscube')
}
```

Sync the project and build (e.g. `./gradlew assembleDebug`).

---

## AppLovin MAX Dashboard Setup

1. Open the [AppLovin MAX Dashboard](https://dash.applovin.com).
2. Select your application (package name must match your app's `applicationId`).
3. Go to **Mediation → Manage Mediation**.
4. Add a **Custom network** named **Bidscube**.
5. Configure the required server parameter:
   - **`app_id`** = your Bidscube Application ID
6. (Optional) For native ad units, set local parameter **`is_native`** = `true`.
7. Save the network. Enable Bidscube for the MAX ad units where you want Bidscube demand.

---

## Consent (GDPR/CCPA)

Run the Google UMP consent flow **before** initializing the AppLovin SDK and loading ads. The Bidscube adapter relies on consent; without it, Bidscube ads may not serve correctly.

---

## Supported Ad Formats

Banner, Interstitial, Rewarded, Native, MREC.

---

## Тестування реклами через Demo App (Bidscube SDK з цього репо)

Щоб переглядати рекламу через Bidscube SDK у AppLovin MAX Demo App:

1. **Структура проєкту**  
   Папка `bidscube-sdk-android` має бути поруч з `AppLovin-MAX-SDK-Android` (наприклад, обидва в `android/`). Demo App підхопить модулі `:sdk` та `:applovin-adapter` з `../../bidscube-sdk-android`.

2. **Відкрити проєкт**  
   Відкрийте в Android Studio папку **`AppLovin MAX Demo App - Kotlin`**. Після sync мають з’явитися модулі `app`, `sdk`, `applovin-adapter`.

3. **Ad Unit ID для тесту**  
   У **AppLovin MAX Dashboard** створіть ad units і додайте Bidscube як custom network з параметром **`app_id`**. У корені Demo App створіть або відредагуйте **`gradle.properties`**:
   ```properties
   maxAdUnitId=ВАШ_MAX_AD_UNIT_ID
   ```
   Якщо не вказати — у коді використовується `YOUR_AD_UNIT_ID` і реклама не завантажиться.

4. **Запуск**  
   Зберіть і запустіть Demo App на пристрої/емуляторі. У меню оберіть потрібний формат (Banner, Interstitial, Rewarded, Native, MREC) — реклама підвантажиться через MAX з Bidscube (якщо мережа налаштована для обраного ad unit).

---

## References

- [AppLovin MAX SDK Android](https://github.com/AppLovin/AppLovin-MAX-SDK-Android)
- Adapter source: `Bidscube/` in this repo, see `Bidscube/README.md`
- [AppLovin Android integration](https://developers.applovin.com/en/android/overview/integration/)
