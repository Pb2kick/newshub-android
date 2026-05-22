# NewsHub Android ↔ Web alignment

## What the zip confirms

The web repo (`NewsHub-main`) uses **two data sources**, same as production should:

| Feature | Web | Android (after fix) |
|--------|-----|---------------------|
| News feed | Spring Boot `GET /api/news` on Render | `BackendService` → Render URL |
| Elections, candidates, votes | **Supabase** (`elections`, `candidates`, `votes`, `verifications`) | `SupabaseService` (not Render) |
| Profile, notifications | Supabase | `SupabaseService` |

Render (`https://newshub-4kk0.onrender.com`) only implements **news** in Spring Boot.  
`GET /api/elections` **404 is expected** — those routes were never in the Java backend.

## Render deployment (keep as-is)

- **Root directory:** `backend`
- **Dockerfile:** `backend/Dockerfile` (Maven → JAR)
- **Env:** `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (Supabase Postgres or dedicated DB for news articles)
- **Do not** point Android elections at Render unless you add new Spring controllers (not required).

## Supabase checklist

1. Run `docs/supabase-profile-notifications.sql` if profile rows or RLS are missing.
2. Ensure **elections** and **candidates** have data (admin UI or SQL seed).
3. **Votes:** `votes.user_id` = numeric `public.users.id`; resolve via `auth_user_id` or `email` (Android matches web `Vote.jsx`).
4. **News empty feed:** Spring returns `[]` when `country` is blank — Android now sends `country` + `area` from geocoder (same contract as web `fetchNewsByLocation`).

## Profile 400 fix

`SupabaseService.fetchProfile` no longer requests non-existent columns (`avatar_url`) in a single `select=` list.
