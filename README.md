# Notification Vault (Android)

A private, local-first Android notification archive with optional cross-device cloud sync.

## Features implemented
- Capture new notifications after the user grants Android Notification Access.
- Store notifications locally with Room.
- Create an account or sign in using Supabase email/password authentication.
- Store the Supabase session and publishable key using Android encrypted preferences.
- Upload new local notifications and download the signed-in user's archive across devices.
- Refresh expired access tokens automatically.
- Sync on notification capture, app launch, and a 15-minute network-connected background schedule.
- Delete one notification or clear the archive in both local and cloud storage.
- Per-user row-level security in Supabase.

## Supabase setup
The dedicated project is in Mumbai (`ap-south-1`).

- Project ref: `qijinaxofwfxwcbvsvjl`
- Project URL: `https://qijinaxofwfxwcbvsvjl.supabase.co`
- Table and RLS policies: `supabase/schema.sql`

In the app, paste the project's **publishable key** from Supabase Project Settings → API Keys. Do not use a secret/service-role key. Use the same email account on every phone. If email confirmation is enabled, confirm the signup email before signing in.

## Build
GitHub Actions builds a debug APK on pushes to `main` that touch app/build files, and uploads it as `notification-vault-debug-apk`.

Open the Actions tab:
https://github.com/theurbanfitfashion-hash/NotificationVault/actions

## Android setup
1. Install the debug APK.
2. Open Notification Vault and grant Notification Access in Android Settings.
3. Enter the Supabase publishable key and sign in or create an account.
4. Sign in with that same account on other Android devices to sync the archive.

Capture begins only after permission is granted. Notifications received before installation/permission cannot be recovered. Some source apps redact sensitive notification content.

## Privacy and limitations
Notification text can contain sensitive information. Keep device access protected. The app does not use a Supabase service-role key. Cloud access is protected by authenticated-user RLS.

Background sync requires internet access and Android may defer WorkManager jobs due to battery restrictions. The app currently fetches the full archive in pages of 1,000 rows; very large histories will take longer to sync. Debug APK and real-device behavior still need CI/device verification before calling this production-ready.
