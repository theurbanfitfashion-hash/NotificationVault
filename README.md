# Notification Vault (Android)

Private, local-first Android notification archive.

## Current scaffold
- Kotlin + Jetpack Compose app shell
- Android NotificationListenerService capture
- Room local database
- WorkManager sync scheduling hook
- Supabase schema with per-user Row Level Security
- GitHub Actions debug-APK workflow

**Status:** starter source only. Cloud authentication/upload is not yet implemented, and an APK has not yet been built or device-tested. Do not treat this scaffold as a finished sync app.

## Supabase
Project ref: `qijinaxofwfxwcbvsvjl`
Project URL: `https://qijinaxofwfxwcbvsvjl.supabase.co`
Apply `supabase/schema.sql` in this dedicated project. Never put a Supabase service-role/secret key in the Android app; use a publishable key and authenticated user sessions.

## Build
Open in Android Studio, allow Gradle sync, then build the `app` debug variant. GitHub Actions is configured to build and upload a debug APK artifact.

## Android permissions
The user must explicitly grant Notification Access in Android Settings. Capture begins only after permission is granted. Previously received notifications cannot be recovered; source apps may redact sensitive notification content.

## Privacy
Notification content can be highly sensitive. Keep the device secure, use per-user RLS, and provide deletion controls.
