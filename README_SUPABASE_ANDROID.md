# Supabase Setup for NewsHub Android

This project now includes a Supabase-backed profile sync flow for:

- first name / last name / full name
- password update
- profile picture upload to Storage bucket

## 1) Configure Supabase project values

Set these in `gradle.properties` (already added with defaults):

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_PROFILE_BUCKET` (default: `profile-pictures`)
- `SUPABASE_PROFILE_TABLE` (default: `users`)
- `SUPABASE_PROFILE_USER_ID_COLUMN` (default: `auth_user_id`)
- `SUPABASE_PROFILE_PHOTO_COLUMN` (default: `profile_photo_url`)

Example:

```ini
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_ANON_KEY=your-anon-key
SUPABASE_PROFILE_BUCKET=profile-pictures
SUPABASE_PROFILE_TABLE=users
SUPABASE_PROFILE_USER_ID_COLUMN=auth_user_id
SUPABASE_PROFILE_PHOTO_COLUMN=profile_photo_url
```

## 2) Expected table shape

The app expects your profile table (default `users`) to have these columns:

- `auth_user_id` (UUID, same as auth user id)
- `first_name` (text)
- `last_name` (text)
- `full_name` (text)
- `profile_photo_url` (text, nullable)

## 3) Expected auth/session keys in app prefs

After your real login flow completes, store these keys in app `SharedPreferences` file `newshub_prefs`:

- `sb_user_id`
- `sb_access_token`

Without them, the screen still works locally but Supabase sync is skipped.

## 4) Storage bucket notes

The avatar uploader writes to:

- `storage/v1/object/{bucket}/{user_id}/avatar.jpg` or `.png`

For this to work in production, your bucket policies must allow authenticated users to upload/update their own path.

## 5) Run

```powershell
Set-Location "C:\Users\Pb2kick\AndroidStudioProjects\NewsHub"
.\gradlew.bat :app:assembleDebug --console=plain
```

