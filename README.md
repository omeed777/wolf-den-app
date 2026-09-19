# Wolf Den App

Android-first member app for Wolf Den CrossFit.

## Current build

- Kotlin + Jetpack Compose
- Persian RTL interface
- Black + gold Wolf Den theme
- Mobile-number login flow
- OTP verification screen (UI/test mode)
- Persistent local login/session
- Member home dashboard
- Live demo subscription values on the dashboard
- Class list with capacity
- Book/cancel class interaction
- Persistent local demo bookings and remaining sessions
- Subscription status screen
- Bottom navigation
- Repository abstraction between UI and data
- Production backend API contract prepared
- No in-app payments in V1

## Project structure

- `app/src/main/java/com/wolfden/app/MainActivity.kt` — application UI and navigation
- `app/src/main/java/com/wolfden/app/data/WolfDenRepository.kt` — data boundary used by the ViewModel
- `app/src/main/java/com/wolfden/app/data/DemoRepository.kt` — persistent local demo implementation
- `app/src/main/java/com/wolfden/app/data/remote/WolfDenApi.kt` — production backend contract
- `app/src/main/java/com/wolfden/app/data/remote/WolfDenApiModels.kt` — backend request/response models

## Backend contract

The production API is intentionally separated from the UI. The planned server flow is:

1. Request OTP by mobile number.
2. Verify OTP and receive an access token.
3. Load member and subscription information.
4. Load available classes.
5. Create a booking.
6. Cancel a booking.
7. Load the member's active bookings.

The Android app currently uses the local demo repository so it remains testable without server credentials.

## Roadmap

1. Replace the demo OTP flow with a real SMS/OTP provider.
2. Implement the backend API client and authentication token storage.
3. Connect member, subscription, class and booking endpoints.
4. Add booking history and server-side validation.
5. Replace placeholder branding with the official Wolf Den logo/assets.
6. Build the admin panel for members, subscriptions, classes, coaches and attendance.
7. Add production security, error handling, loading states and offline handling.
8. Run release QA and publish the Android V1.
9. Prepare iOS client after Android V1 is stable.

Payments remain outside V1.
