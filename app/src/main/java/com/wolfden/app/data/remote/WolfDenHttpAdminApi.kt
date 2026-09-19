package com.wolfden.app.data.remote

import org.json.JSONObject
import java.io.IOException

/**
 * HTTP implementation of the admin API contract.
 *
 * The admin surface is kept separate from the member API so it can later be
 * protected by an admin role and, if desired, moved into a dedicated admin app.
 */
class WolfDenHttpAdminApi(
    private val config: WolfDenAdminApiConfig
) : WolfDenAdminApi {

    override fun getMembers(accessToken: String): List<AdminMemberDto> =
        request("GET", config.membersPath, token = accessToken).objects("members") { o ->
            AdminMemberDto(o.requiredString("id"), o.requiredString("name"), o.requiredString("phone"), o.requiredString("status"))
        }

    override fun getSubscriptions(accessToken: String): List<AdminSubscriptionDto> =
        request("GET", config.subscriptionsPath, token = accessToken).objects("subscriptions") { o ->
            AdminSubscriptionDto(o.requiredString("id"), o.requiredString("memberId"), o.requiredString("plan"), o.optInt("totalSessions"), o.optInt("remainingSessions"), o.requiredString("status"), o.requiredString("expiresAt"))
        }

    override fun getClasses(accessToken: String): List<TrainingClassDto> =
        request("GET", config.classesPath, token = accessToken).objects("classes") { o ->
            TrainingClassDto(o.getInt("id"), o.requiredString("title"), o.requiredString("day"), o.requiredString("time"), o.getInt("capacity"), o.getInt("booked"), o.requiredString("coach"))
        }

    override fun getCoaches(accessToken: String): List<CoachDto> =
        request("GET", config.coachesPath, token = accessToken).objects("coaches") { o ->
            CoachDto(o.requiredString("id"), o.requiredString("name"), o.requiredString("phone"))
        }

    override fun getBookings(accessToken: String): List<AdminBookingDto> =
        request("GET", config.bookingsPath, token = accessToken).objects("bookings") { o ->
            AdminBookingDto(o.requiredString("id"), o.requiredString("memberId"), o.requiredString("memberName"), o.getInt("classId"), o.requiredString("classTitle"), o.requiredString("day"), o.requiredString("time"), o.requiredString("status"), o.requiredString("createdAt"))
        }

    override fun cancelBooking(accessToken: String, bookingId: String): Boolean =
        request("DELETE", config.bookingsPath + "/" + bookingId, token = accessToken).optBoolean("success", true)

    override fun createBooking(accessToken: String, request: AdminCreateBookingRequest): AdminBookingDto =
        this.request(
            "POST",
            config.bookingsPath,
            JSONObject().put("memberId", request.memberId).put("classId", request.classId),
            accessToken
        ).let {
            AdminBookingDto(
                it.requiredString("id"),
                it.requiredString("memberId"),
                it.requiredString("memberName"),
                it.getInt("classId"),
                it.requiredString("classTitle"),
                it.requiredString("day"),
                it.requiredString("time"),
                it.requiredString("status"),
                it.requiredString("createdAt")
            )
        }

    override fun getAttendance(accessToken: String, date: String): List<AttendanceDto> =
        request("GET", config.attendancePath + "?date=" + java.net.URLEncoder.encode(date, "UTF-8"), token = accessToken).objects("attendance") { o ->
            AttendanceDto(o.requiredString("id"), o.requiredString("memberId"), o.getInt("classId"), o.requiredString("date"), o.optBoolean("present"))
        }

    override fun createMember(accessToken: String, request: CreateMemberRequest): AdminMemberDto =
        this.request("POST", config.membersPath, JSONObject().put("name", request.name).put("phone", request.phone), accessToken)
            .let { AdminMemberDto(it.requiredString("id"), it.requiredString("name"), it.requiredString("phone"), it.requiredString("status")) }

    override fun updateSubscription(accessToken: String, memberId: String, request: UpdateSubscriptionRequest): AdminSubscriptionDto =
        this.request("PUT", config.subscriptionsPath + "/" + memberId,
            JSONObject().put("plan", request.plan).put("totalSessions", request.totalSessions).put("remainingSessions", request.remainingSessions).put("status", request.status).put("expiresAt", request.expiresAt),
            accessToken).let {
                AdminSubscriptionDto(it.requiredString("id"), it.requiredString("memberId"), it.requiredString("plan"), it.optInt("totalSessions"), it.optInt("remainingSessions"), it.requiredString("status"), it.requiredString("expiresAt"))
            }

    override fun createClass(accessToken: String, request: CreateClassRequest): TrainingClassDto =
        this.request("POST", config.classesPath,
            JSONObject().put("title", request.title).put("day", request.day).put("time", request.time).put("capacity", request.capacity).put("coachId", request.coachId),
            accessToken).let {
                TrainingClassDto(it.getInt("id"), it.requiredString("title"), it.requiredString("day"), it.requiredString("time"), it.getInt("capacity"), it.getInt("booked"), it.requiredString("coach"))
            }

    override fun recordAttendance(accessToken: String, request: RecordAttendanceRequest): AttendanceDto =
        this.request("POST", config.attendancePath,
            JSONObject().put("memberId", request.memberId).put("classId", request.classId).put("date", request.date).put("present", request.present),
            accessToken).let {
                AttendanceDto(it.requiredString("id"), it.requiredString("memberId"), it.getInt("classId"), it.requiredString("date"), it.optBoolean("present"))
            }

    private fun request(method: String, path: String, body: JSONObject? = null, token: String): JSONObject {
        val connection = (java.net.URL(config.url(path)).openConnection() as java.net.HttpURLConnection)
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IOException("Wolf Den Admin API returned HTTP $status: $response")
            return if (response.isBlank()) JSONObject() else JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }
}

private inline fun <T> JSONObject.objects(key: String, mapper: (JSONObject) -> T): List<T> {
    val array = optJSONArray(key) ?: return emptyList()
    return buildList(array.length()) { for (i in 0 until array.length()) add(mapper(array.getJSONObject(i))) }
}

private fun JSONObject.requiredString(key: String): String =
    optString(key).takeIf { it.isNotBlank() } ?: throw IOException("Missing API field: $key")

data class WolfDenAdminApiConfig(
    val baseUrl: String,
    val membersPath: String = "/admin/members",
    val subscriptionsPath: String = "/admin/subscriptions",
    val classesPath: String = "/admin/classes",
    val coachesPath: String = "/admin/coaches",
    val attendancePath: String = "/admin/attendance",
    val bookingsPath: String = "/admin/bookings"
) {
    init { require(baseUrl.isNotBlank()) { "Admin API baseUrl must not be blank" } }
    fun url(path: String): String = baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}
