# Submission Checklist

Use this checklist before sharing your repository link.

## 1) Functional Requirements

- [ ] Register works with JSON request/response.
- [ ] Login works with JSON request/response.
- [ ] Dashboard opens only with valid token/session.
- [ ] Profile page loads user info from Supabase.
- [ ] Update Profile (first name + last name + full name derived) works.
- [ ] Change Password works through Supabase Auth.
- [ ] Avatar upload works to Supabase Storage and persists URL in profile.

## 2) API Integration Requirements

- [ ] Retrofit interface(s) implemented.
- [ ] Request/response data models implemented.
- [ ] Centralized API client/service implemented.
- [ ] Bearer token used for protected endpoints.
- [ ] HTTP status handling includes 200, 400, 401, 500.
- [ ] Network + server errors are handled with user-friendly messages.

## 3) UI/UX Requirements

- [ ] Loading indicators are shown during API calls.
- [ ] Action buttons are disabled while requests are running.
- [ ] Success and error feedback is visible to users.
- [ ] Layout is clean and consistent across screens.

## 4) Documentation Requirements

- [ ] Public GitHub repository link added to submission.
- [ ] `README.md` includes API integration notes.
- [ ] `README.md` includes screenshot section with paths.

## 5) Required Screenshots

Store screenshots in `docs/screenshots/` and keep these names:

- [ ] `docs/screenshots/register.png`
- [ ] `docs/screenshots/login.png`
- [ ] `docs/screenshots/dashboard.png`
- [ ] `docs/screenshots/profile.png`
- [ ] `docs/screenshots/update-profile.png`
- [ ] `docs/screenshots/change-password.png`

## 6) Final Verification

- [ ] `:app:assembleDebug` builds successfully.
- [ ] App installs on emulator/device.
- [ ] Re-login confirms persisted profile changes.

