# Wolf Den App

Android-first member app for Wolf Den CrossFit.

## Current build

- Kotlin + Jetpack Compose
- Persian RTL interface
- Mobile-number login flow
- OTP verification screen (UI/test mode)
- Member home dashboard
- Class list with capacity
- Book/cancel class interaction (local test state)
- Subscription status screen
- Bottom navigation
- No in-app payments in V1

## Project structure

- `app/src/main/java/com/wolfden/app/MainActivity.kt` — application UI and navigation
- `app/src/main/java/com/wolfden/app/model/TrainingClass.kt` — class domain model
- `docs/BACKEND_CONTRACT.md` — planned backend API contract

## Roadmap

1. Finish member-facing UI and booking states.
2. Add persistent authentication/session handling.
3. Connect real OTP/SMS authentication.
4. Connect member, subscription, class and booking APIs.
5. Add real member booking history.
6. Add Wolf Den logo/assets.
7. Build the admin panel for members, subscriptions, classes, coaches and attendance.
8. Add production security, error handling, loading states and offline handling.
9. Prepare iOS client after the Android V1 is stable.

Payments remain outside V1.
