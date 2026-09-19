package com.wolfden.app.data.remote

/**
 * Runtime configuration for the production API.
 *
 * Keep the URL outside source control for production builds. The demo app
 * does not instantiate this client, so no live server is required yet.
 */
data class WolfDenApiConfig(
    val baseUrl: String,
    val requestOtpPath: String = "/auth/request-otp",
    val verifyOtpPath: String = "/auth/verify-otp",
    val memberPath: String = "/member",
    val classesPath: String = "/classes",
    val bookingsPath: String = "/bookings",
    val adminMembersPath: String = "/admin/members",
    val adminSubscriptionsPath: String = "/admin/subscriptions",
    val adminClassesPath: String = "/admin/classes",
    val adminCoachesPath: String = "/admin/coaches",
    val adminAttendancePath: String = "/admin/attendance"
) {
    init {
        require(baseUrl.isNotBlank()) { "API baseUrl must not be blank" }
    }

    fun url(path: String): String =
        baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}
