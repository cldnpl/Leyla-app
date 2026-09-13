# Leyla — Android

The Android build of Leyla, ported screen by screen from `../ios`. Same backend,
same copy, same visual language; only the platform idioms differ.

Currently implemented: the **Home** screen and the tab bar. The other three tabs
are placeholders (`ComingSoonScreen`), the way iOS shipped them before their
features landed.

## Requirements

- JDK 17 or 21 (Gradle is happy with either; JDK 25 is not supported by AGP yet).
  Android Studio's bundled JBR works: `/Applications/Android Studio.app/Contents/jbr/Contents/Home`.
- Android SDK 36.

## Google Maps key

The Home map needs a Google Maps key. It is read from `local.properties`, which
is gitignored, so it never lands in the repo:

```properties
sdk.dir=/Users/you/Library/Android/sdk
MAPS_API_KEY=AIza...
```

Get one at <https://console.cloud.google.com/google/maps-apis/credentials> with
the **Maps SDK for Android** enabled, and restrict it to this app's package
(`com.claudianapolitano.leyla`) plus the debug/release signing certificates.

Without a key the app still runs: the map card falls back to a warm placeholder
panel instead of Google's "for development purposes only" grey grid.

## Build and run

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.claudianapolitano.leyla/.MainActivity
```

## Notes on the port

- `SharedConfig.DEMO_MODE` mirrors the iOS flag: it puts Claudia in Naples and
  Alex in Tashkent so Home is populated before pairing and location sharing
  exist. **Set it to `false` before any public launch.**
- Where iOS says "Apple Health", the Android build says "Health Connect" — the
  same data, the platform's own name for it. That is the only copy that
  deliberately differs.
- Cards are filled with an opaque colour rather than a translucent one. iOS uses
  `.regularMaterial`; a translucent fill on Android lets each card's own drop
  shadow show through as a darker ring inside its edges, so the fill is the
  flattened equivalent instead.
- Strings live in `res/values/strings.xml` for now. iOS pulls translations from
  the backend at runtime (`TranslationStore`); that lands with the language
  picker.
