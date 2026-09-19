package com.wolfden.app.data.remote

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Small dependency-free HTTP implementation of WolfDenApi.
 *
 * Endpoint paths and JSON field names follow the client contract in this
 * project. The base URL is injected at runtime; no server address is hardcoded.
 */
class WolfDenHttpApi(
    private val config: WolfDenApiConfig
) : WolfDenApi {

    override fun requestOtp(request: LoginRequest): Boolean =
        requestJson("POST", config.requestOtpPath, JSONObject()
            .put("phone", request.phone)
        ).optBoolean("success", true)

    override fun verifyOtp(request: VerifyOtpRequest): AuthResponse {
        val json = requestJson("POST", config.verifyOtpPath, JSONObject()
            .put("phone", request.phone)
            .put("code", request.code)
        )
        return AuthResponse(
            accessToken = json.requiredString("accessToken"),
            memberId = json.requiredString("memberId"),
            name = json.requiredString("name")
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
        requestJson(
            "DELETE",
            config.bookingsPath + "/" + classId,
            token = accessToken
        ).optBoolean("success", true)

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
            connection.setRequestProperty("Accept", "application/json")
            token?.takeIf { it.isNotBlank() }?.let {
                connection.setRequestProperty("Authorization", "Bearer $it")
            }

            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { output ->
                    output.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (status !in 200..299) {
                throw IOException("Wolf Den API returned HTTP $status: $response")
            }

            return if (response.isBlank()) JSONObject() else JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }
}

private fun JSONObject.requiredString(key: String): String =
    optString(key).takeIf { it.isNotBlank() }
        ?: throw IOException("Missing API field: $key")

private fun JSONObject.array(key: String): List<JSONObject> {
    val array = optJSONArray(key) ?: JSONArray()
    return buildList(array.length()) {
        for (index in 0 until array.length()) {
            add(array.getJSONObject(index))
        }
    }
}

private fun JSONObject.toMemberDto(): MemberDto = MemberDto(
    id = requiredString("id"),
    name = requiredString("name"),
    phone = requiredString("phone"),
    plan = requiredString("plan"),
    totalSessions = optInt("totalSessions"),
    remainingSessions = optInt("remainingSessions"),
    subscriptionStatus = requiredString("subscriptionStatus"),
    expiresAt = requiredString("expiresAt")
)

private fun JSONObject.toTrainingClassDto(): TrainingClassDto = TrainingClassDto(
    id = getInt("id"),
    title = requiredString("title"),
    day = requiredString("day"),
    time = requiredString("time"),
    capacity = getInt("capacity"),
    booked = getInt("booked"),
    coach = requiredString("coach")
)

private fun JSONObject.toBookingDto(): BookingDto = BookingDto(
    id = requiredString("id"),
    classId = getInt("classId"),
    status = requiredString("status"),
    createdAt = requiredString("createdAt")
)
