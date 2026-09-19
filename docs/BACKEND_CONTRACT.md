# Wolf Den Backend Contract

This document defines the V1 API shape before connecting the Android client to a real backend.

## Authentication

### POST /auth/request-otp
Request:
{
  "phone": "09123456789"
}

Response:
{
  "requestId": "otp-request-id",
  "expiresIn": 120
}

### POST /auth/verify-otp
Request:
{
  "phone": "09123456789",
  "code": "123456",
  "requestId": "otp-request-id"
}

Response:
{
  "accessToken": "jwt-or-session-token",
  "member": {
    "id": "member-id",
    "name": "Member Name",
    "phone": "09123456789"
  }
}

## Member

### GET /me

Returns the authenticated member profile and current subscription.

Example:
{
  "id": "member-id",
  "name": "Member Name",
  "phone": "09123456789",
  "subscription": {
    "plan": "12 sessions",
    "totalSessions": 12,
    "remainingSessions": 8,
    "status": "ACTIVE",
    "expiresAt": "2026-10-01"
  }
}

## Classes

### GET /classes?date=2026-09-19

Returns available classes.

Each class should include:
- id
- title
- date
- startTime
- endTime
- coach
- capacity
- bookedCount
- availableCount
- bookingOpen

## Booking

### POST /classes/{classId}/book

Creates a booking for the authenticated member.

Rules:
- member must have an active subscription
- remaining sessions must be greater than zero
- class must have capacity
- duplicate booking must be rejected

### DELETE /classes/{classId}/book

Cancels the member booking.

The backend should define the cancellation cutoff, for example 60 minutes before class start.

## My bookings

### GET /me/bookings?from=2026-09-19&to=2026-09-26

Returns the member's upcoming and recent bookings.

## Admin requirements for later

The admin panel will eventually need:
- member management
- subscription management
- class schedule management
- coach management
- booking/cancellation management
- attendance
- dashboard/reporting

No payment endpoint is included in V1.
