package com.wolfden.app.data.remote

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Small dependency-free HTTP implementation of WolfDenApi.
 *
 * The parser is intentionally defensive because production Supabase rows can
 * contain nullable fields (for example an unset subscription expiry date).
 */
class WolfDenHttpApi(
    private val config: WolfDenApiConfig
) : WolfDenApi {

    override fun requestOtp(request: LoginRequest): Boolean =
        requestJson("POST", config.requestOtpPath, JSONObject().put("phone", request.phone))
            .optBoolean("success", true)

    override fun verifyOtp(request: VerifyOtpRequest): AuthResponse {
        val json = requestJson(
            "POST",
            config.verifyOtpPath,
            JSONObject().put("phone", request.phone).put("code", request.code)
        )
        return AuthResponse(
            accessToken = json.requiredString("accessToken"),
            memberId = json.requiredString("memberId"),
            name = json.optString("name").ifBlank { "عضو Wolf Den" }
        )
    }

    override fun getMember(accessToken: String): MemberDto =
        requestJson("GET", config.memberPath, token = accessToken).toMemberDto()

    override fun getClasses(accessToken: String): List<TrainingClassDto> =
        requestJson("GET", config.classesPath, token = accessToken)
            .array("classes")
            .map { it.toTrainingClassDto() }

    override fun getMyBookings(accessToken: String): List<BookingDto> =
        requestJson("GET", config.bookingsPath, token = accessToken)
            .array("bookings")
            .map { it.toBookingDto() }

    override fun bookClass(accessToken: String, classId: Int): BookingDto =
        requestJson(
            "POST",
            config.bookingsPath,
            body = JSONObject().put("classId", classId),
            token = accessToken
        ).toBookingDto()

    override fun cancelBooking(accessToken: String, classId: Int): Boolean =
        requestJson("DELETE", config.bookingsPath + "/" + classId, token = accessToken)
            .optBoolean("success", true)

    private fun requestJson(
        method: String,
        path: String,
        body: JSONObject? = null,
        token: String? = null
    ): JSONObject {
        val connection = (URL(config.url(path)).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept-Charset", "utf-8")
            connection.setRequestProperty("Accept", "application/json")
            token?.takeIf { it.isNotBlank() }?.let {
                connection.setRequestProperty("Authorization", "Bearer $it")
            }

            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (status !in 200..299) throw IOException(apiError(status, response))
            return if (response.isBlank()) JSONObject() else JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }
}

private fun JSONObject.requiredString(key: String): String =
    optString(key).trim().takeIf { it.isNotBlank() }
        ?: throw IOException("Missing API field: $key")

private fun JSONObject.optionalString(key: String, fallback: String = "-"): String =
    optString(key).trim().takeIf { it.isNotBlank() && it != "null" } ?: fallback

private fun JSONObject.array(key: String): List<JSONObject> {
    val array = optJSONArray(key) ?: JSONArray()
    return buildList(array.length()) {
        for (index in 0 until array.length()) add(array.getJSONObject(index))
    }
}

private fun JSONObject.toMemberDto(): MemberDto = MemberDto(
    id = requiredString("id"),
    name = optionalString("name", "عضو Wolf Den"),
    phone = optionalString("phone"),
    plan = optionalString("plan", "بدون اشتراک"),
    totalSessions = optInt("totalSessions", 0),
    remainingSessions = optInt("remainingSessions", 0),
    subscriptionStatus = optionalString("subscriptionStatus", "expired"),
    expiresAt = optionalString("expiresAt", "-")
)

private fun JSONObject.toTrainingClassDto(): TrainingClassDto = TrainingClassDto(
    id = getInt("id"),
    title = optionalString("title", "CrossFit"),
    day = optionalString("day"),
    time = optionalString("time"),
    capacity = optInt("capacity", 0),
    booked = optInt("booked", 0),
    coach = optionalString("coach", "-")
)

private fun JSONObject.toBookingDto(): BookingDto = BookingDto(
    id = requiredString("id"),
    classId = getInt("classId"),
    status = optionalString("status", "CONFIRMED"),
    createdAt = optionalString("createdAt")
)

private fun apiError(status: Int, response: String): String {
    return try {
        val json = JSONObject(response)
        json.optString("message").takeIf { it.isNotBlank() }?.let {
            "Wolf Den API error ($status): $it"
        } ?: "Wolf Den API error ($status)"
    } catch (_: Exception) {
        "Wolf Den API error ($status)"
    }
}
