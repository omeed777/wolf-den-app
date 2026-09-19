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
- Persistent local demo bookings, capacity and remaining sessions
- Persistent demo members and subscription edits
- Persistent demo classes (create/edit/delete + coach + capacity)
- Persistent demo coaches and attendance
- Subscription status screen with booking eligibility
- Bottom navigation
- Repository abstraction between UI and data
- Production member backend API contract
- Production repository adapter with DTO-to-domain mapping
- Admin backend contract for members, subscriptions, classes, coaches and attendance
- No in-app payments in V1

## Project structure

- `app/src/main/java/com/wolfden/app/MainActivity.kt` — application UI and navigation
- `app/src/main/java/com/wolfden/app/data/WolfDenRepository.kt` — data boundary used by the ViewModel
- `app/src/main/java/com/wolfden/app/data/DemoRepository.kt` — persistent local demo implementation
- `app/src/main/java/com/wolfden/app/data/remote/WolfDenApi.kt` — production member backend contract
- `app/src/main/java/com/wolfden/app/data/remote/WolfDenApiModels.kt` — member backend DTOs
- `app/src/main/java/com/wolfden/app/data/remote/WolfDenMappers.kt` — API DTO to domain mapping
- `app/src/main/java/com/wolfden/app/data/remote/RemoteWolfDenRepository.kt` — production repository adapter
- `app/src/main/java/com/wolfden/app/data/remote/WolfDenAdminApi.kt` — admin backend contract

## Backend contract

The production API is intentionally separated from the UI. The planned member flow is:

1. Request OTP by mobile number.
2. Verify OTP and receive an access token.
3. Load member and subscription information.
4. Load available classes.
5. Create a booking.
6. Cancel a booking.
7. Load the member's active bookings.

The Android app currently uses the local demo repository so it remains testable without server credentials.

The admin contract covers:

- Members
- Subscriptions
- Classes
- Coaches
- Attendance

No real backend URL or credentials are embedded in the client until the production backend is selected and provisioned.

## Current status

The Android demo is now functionally wired end-to-end for local testing:

- Member login/demo OTP
- Member dashboard
- Subscription status and booking eligibility
- Class capacity and booking/cancellation
- Admin members
- Admin subscriptions
- Admin classes and coaches
- Admin bookings
- Attendance
- Shared demo persistence between member and admin flows

## Remaining production work

1. Provision the production backend and real SMS/OTP provider.
2. Set WOLF_DEN_API_BASE_URL for production builds.
3. Connect and verify the production member/admin endpoints.
4. Replace the geometric placeholder wolf mark with the official Wolf Den logo asset.
5. Run Android release QA on real devices.
6. Configure signing and generate the release APK/AAB.
7. Prepare the iOS client after Android V1 is stable.

Payments remain outside V1.
