package com.wolfden.app.data.remote

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class DemoWolfDenAdminRepository(context: Context) : WolfDenAdminRepository {
    private val preferences = context.applicationContext.getSharedPreferences("wolf_den_demo_data", Context.MODE_PRIVATE)
    private val members = mutableListOf(
        AdminMemberDto("m-001", "علی رضایی", "09120000001", "ACTIVE"),
        AdminMemberDto("m-002", "سارا احمدی", "09120000002", "ACTIVE"),
        AdminMemberDto("m-003", "محمد کریمی", "09120000003", "SUSPENDED")
    )
    private val subscriptions = mutableListOf(
        AdminSubscriptionDto("s-001", "m-001", "۱۲ جلسه در ماه", 12, 8, "ACTIVE", "2026-10-01"),
        AdminSubscriptionDto("s-002", "m-002", "۱۶ جلسه در ماه", 16, 12, "ACTIVE", "2026-10-15"),
        AdminSubscriptionDto("s-003", "m-003", "۸ جلسه در ماه", 8, 0, "SUSPENDED", "2026-09-20")
    )
    private val classes = mutableListOf(
        TrainingClassDto(1, "CrossFit", "امروز", "18:00", 12, 8, "مربی Wolf"),
        TrainingClassDto(2, "CrossFit", "امروز", "20:00", 12, 10, "مربی Wolf"),
        TrainingClassDto(3, "Strength", "فردا", "18:00", 10, 5, "مربی Wolf")
    )
    private val bookings = mutableListOf(
        AdminBookingDto("b-001", "m-001", "علی رضایی", 1, "CrossFit", "امروز", "18:00", "CONFIRMED", "2026-09-19 10:00"),
        AdminBookingDto("b-002", "m-002", "سارا احمدی", 1, "CrossFit", "امروز", "18:00", "CONFIRMED", "2026-09-19 10:05"),
        AdminBookingDto("b-003", "m-003", "محمد کریمی", 2, "CrossFit", "امروز", "20:00", "CONFIRMED", "2026-09-19 10:10")
    )
    init {
        loadSharedState()
        loadPersistedMembers()
        loadPersistedCoaches()
        loadPersistedBookings()
    }

    private fun persistCoaches() {
        val encoded = coaches.joinToString("\n") { listOf(it.id, it.name.replace("|", " "), it.phone.replace("|", " ")).joinToString("|") }
        preferences.edit().putString("admin_coaches", encoded).apply()
    }

    private fun loadPersistedCoaches() {
        val encoded = preferences.getString("admin_coaches", null) ?: return
        val loaded = encoded.lines().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size == 3 && parts[0].isNotBlank()) CoachDto(parts[0], parts[1], parts[2]) else null
        }
        if (loaded.isNotEmpty()) {
            coaches.clear()
            coaches.addAll(loaded)
        }
    }

    private fun persistBookings() {
        val json = JSONArray()
        bookings.forEach {
            json.put(JSONObject()
                .put("id", it.id)
                .put("memberId", it.memberId)
                .put("memberName", it.memberName)
                .put("classId", it.classId)
                .put("classTitle", it.classTitle)
                .put("day", it.day)
                .put("time", it.time)
                .put("status", it.status)
                .put("createdAt", it.createdAt))
        }
        preferences.edit().putString("admin_bookings", json.toString()).apply()
    }

    private fun loadPersistedBookings() {
        val raw = preferences.getString("admin_bookings", null) ?: return
        try {
            val json = JSONArray(raw)
            bookings.clear()
            for (i in 0 until json.length()) {
                val o = json.getJSONObject(i)
                bookings += AdminBookingDto(
                    o.getString("id"), o.getString("memberId"), o.getString("memberName"),
                    o.getInt("classId"), o.getString("classTitle"), o.getString("day"),
                    o.getString("time"), o.getString("status"), o.getString("createdAt")
                )
            }
        } catch (_: Exception) {
            preferences.edit().remove("admin_bookings").apply()
        }
    }

    private fun persistSharedState() {
        val editor = preferences.edit()
        val classJson = JSONArray()
        classes.forEach { cls ->
            editor.putInt("admin_class_" + cls.id + "_booked", cls.booked)
            classJson.put(JSONObject()
                .put("id", cls.id)
                .put("title", cls.title)
                .put("day", cls.day)
                .put("time", cls.time)
                .put("capacity", cls.capacity)
                .put("booked", cls.booked)
                .put("coach", cls.coach))
        }
        editor.putString("admin_classes", classJson.toString())
        subscriptions.forEach { sub ->
            val prefix = "admin_sub_" + sub.memberId + "_"
            editor.putString(prefix + "plan", sub.plan)
            editor.putInt(prefix + "total", sub.totalSessions)
            editor.putInt(prefix + "remaining", sub.remainingSessions)
            editor.putString(prefix + "status", sub.status)
            editor.putString(prefix + "expires", sub.expiresAt)
        }
        editor.apply()
    }

    private fun loadSharedState() {
        val rawClasses = preferences.getString("admin_classes", null)
        if (!rawClasses.isNullOrBlank()) {
            try {
                val json = JSONArray(rawClasses)
                val loaded = mutableListOf<TrainingClassDto>()
                for (i in 0 until json.length()) {
                    val o = json.getJSONObject(i)
                    loaded += TrainingClassDto(
                        o.getInt("id"), o.getString("title"), o.getString("day"),
                        o.getString("time"), o.getInt("capacity"), o.getInt("booked"),
                        o.getString("coach")
                    )
                }
                if (loaded.isNotEmpty()) {
                    classes.clear()
                    classes.addAll(loaded)
                }
            } catch (_: Exception) {
                preferences.edit().remove("admin_classes").apply()
            }
        }
        classes.indices.forEach { i ->
            val cls = classes[i]
            val stored = preferences.getInt("admin_class_" + cls.id + "_booked", -1)
            if (stored >= 0) classes[i] = cls.copy(booked = stored)
        }
        subscriptions.indices.forEach { i ->
            val sub = subscriptions[i]
            val prefix = "admin_sub_" + sub.memberId + "_"
            val storedTotal = preferences.getInt(prefix + "total", -1)
            val storedRemaining = preferences.getInt(prefix + "remaining", -1)
            if (storedTotal >= 0 || storedRemaining >= 0 ||
                preferences.contains(prefix + "plan") ||
                preferences.contains(prefix + "status") ||
                preferences.contains(prefix + "expires")
            ) {
                subscriptions[i] = sub.copy(
                    plan = preferences.getString(prefix + "plan", sub.plan) ?: sub.plan,
                    totalSessions = if (storedTotal >= 0) storedTotal else sub.totalSessions,
                    remainingSessions = if (storedRemaining >= 0) storedRemaining else sub.remainingSessions,
                    status = preferences.getString(prefix + "status", sub.status) ?: sub.status,
                    expiresAt = preferences.getString(prefix + "expires", sub.expiresAt) ?: sub.expiresAt
                )
            }
        }
    }

    private val attendance = mutableListOf<AttendanceDto>()
    
    private fun persistAttendance(item: AttendanceDto) {
        preferences.edit()
            .putString("attendance_" + item.memberId + "_" + item.classId + "_" + item.date, if (item.present) "PRESENT" else "ABSENT")
            .apply()
    }
    private val coaches = mutableListOf(
        CoachDto("c-001", "مربی Wolf", "09121111111"),
        CoachDto("c-002", "مربی دوم", "09122222222")
    )
    override fun getMembers() = members.toList()
    override fun getSubscriptions() = subscriptions.toList()
    override fun getClasses() = classes.toList()
    override fun getCoaches() = coaches.toList()
    override fun getBookings() = bookings.filter { it.status == "CONFIRMED" }.toList()
    override fun createBooking(request: AdminCreateBookingRequest): AdminBookingDto {
        if (bookings.any { it.memberId == request.memberId && it.classId == request.classId && it.status == "CONFIRMED" }) {
            throw IllegalStateException("این عضو قبلاً این کلاس را رزرو کرده است.")
        }
        val classIndex = classes.indexOfFirst { it.id == request.classId }
        if (classIndex < 0) throw IllegalArgumentException("کلاس پیدا نشد.")
        val cls = classes[classIndex]
        if (cls.booked >= cls.capacity) throw IllegalStateException("ظرفیت کلاس تکمیل است.")

        val subIndex = subscriptions.indexOfFirst { it.memberId == request.memberId }
        if (subIndex < 0) throw IllegalStateException("عضو اشتراک فعال ندارد.")
        val sub = subscriptions[subIndex]
        val member = members.firstOrNull { it.id == request.memberId }
            ?: throw IllegalArgumentException("عضو پیدا نشد.")
        if (member.status != "ACTIVE") throw IllegalStateException("این عضو فعال نیست و امکان رزرو ندارد.")
        if (sub.status != "ACTIVE" || sub.remainingSessions <= 0) throw IllegalStateException("جلسه قابل استفاده ندارد.")
        if (isExpired(sub.expiresAt)) throw IllegalStateException("اشتراک این عضو منقضی شده است.")

        classes[classIndex] = cls.copy(booked = cls.booked + 1)
        subscriptions[subIndex] = sub.copy(remainingSessions = sub.remainingSessions - 1)
        val booking = AdminBookingDto(
            "b-" + System.currentTimeMillis(), member.id, member.name, cls.id, cls.title,
            cls.day, cls.time, "CONFIRMED", "2026-09-19"
        )
        bookings += booking
        persistBookings()
        persistSharedState()
        return booking
    }

    override fun cancelBooking(bookingId: String): Boolean {
        val i = bookings.indexOfFirst { it.id == bookingId && it.status == "CONFIRMED" }
        if (i < 0) return false
        val booking = bookings[i]
        bookings[i] = booking.copy(status = "CANCELLED")

        val classIndex = classes.indexOfFirst { it.id == booking.classId }
        if (classIndex >= 0) {
            val cls = classes[classIndex]
            classes[classIndex] = cls.copy(booked = (cls.booked - 1).coerceAtLeast(0))
        }

        val subIndex = subscriptions.indexOfFirst { it.memberId == booking.memberId }
        if (subIndex >= 0) {
            val sub = subscriptions[subIndex]
            subscriptions[subIndex] = sub.copy(
                remainingSessions = (sub.remainingSessions + 1).coerceAtMost(sub.totalSessions)
            )
        }
        persistBookings()
        persistSharedState()
        return true
    }
    override fun getAttendance(date: String): List<AttendanceDto> {
        val result = mutableListOf<AttendanceDto>()
        members.forEach { member ->
            classes.forEach { cls ->
                val key = "attendance_" + member.id + "_" + cls.id + "_" + date
                val value = preferences.getString(key, null) ?: return@forEach
                result += AttendanceDto("attendance-" + member.id + "-" + cls.id + "-" + date, member.id, cls.id, date, value == "PRESENT")
            }
        }
        return result
    }
    private fun persistMembers() {
        val json = JSONArray()
        members.forEach {
            json.put(JSONObject().put("id", it.id).put("name", it.name).put("phone", it.phone).put("status", it.status))
        }
        preferences.edit().putString("admin_members", json.toString()).apply()
    }

    private fun loadPersistedMembers() {
        val raw = preferences.getString("admin_members", null) ?: return
        try {
            val json = JSONArray(raw)
            val loaded = mutableListOf<AdminMemberDto>()
            for (i in 0 until json.length()) {
                val o = json.getJSONObject(i)
                loaded += AdminMemberDto(o.getString("id"), o.getString("name"), o.getString("phone"), o.getString("status"))
            }
            if (loaded.isNotEmpty()) {
                members.clear()
                members.addAll(loaded)
            }
        } catch (_: Exception) {
            preferences.edit().remove("admin_members").apply()
        }
    }

    override fun updateMember(memberId: String, request: UpdateMemberRequest): AdminMemberDto {
        val index = members.indexOfFirst { it.id == memberId }
        if (index < 0) throw IllegalArgumentException("عضو پیدا نشد.")
        validateMemberInput(request.name, request.phone, memberId)
        val current = members[index]
        val updated = current.copy(name = request.name.trim(), phone = normalizePhone(request.phone), status = request.status)
        members[index] = updated
        persistMembers()
        return updated
    }

    override fun createMember(request: CreateMemberRequest): AdminMemberDto {
        validateMemberInput(request.name, request.phone)
        val member = AdminMemberDto("m-" + nextNumericId(members.map { it.id }, "m-"), request.name.trim(), normalizePhone(request.phone), "ACTIVE")
        members += member
        persistMembers()
        return member
    }
    override fun updateSubscription(memberId: String, request: UpdateSubscriptionRequest): AdminSubscriptionDto {
        if (members.none { it.id == memberId }) throw IllegalArgumentException("عضو پیدا نشد.")
        validateSubscriptionInput(request)
        val index = subscriptions.indexOfFirst { it.memberId == memberId }
        val id = if (index >= 0) subscriptions[index].id else "s-" + nextNumericId(subscriptions.map { it.id }, "s-")
        val result = AdminSubscriptionDto(id, memberId, request.plan, request.totalSessions, request.remainingSessions, request.status, request.expiresAt)
        if (index >= 0) subscriptions[index] = result else subscriptions += result
        persistSharedState()
        return result
    }
    private fun validateMemberInput(name: String, phone: String, exceptMemberId: String? = null) {
        if (name.trim().length < 2) throw IllegalArgumentException("نام عضو باید حداقل ۲ کاراکتر باشد.")
        val normalized = normalizePhone(phone)
        if (!Regex("^09\\d{9}$").matches(normalized)) throw IllegalArgumentException("شماره موبایل باید ۱۱ رقم و با 09 شروع شود.")
        if (members.any { it.id != exceptMemberId && normalizePhone(it.phone) == normalized }) throw IllegalArgumentException("این شماره موبایل قبلاً ثبت شده است.")
    }

    private fun normalizePhone(phone: String): String = phone.trim().replace(" ", "")

    private fun validateSubscriptionInput(request: UpdateSubscriptionRequest) {
        if (request.plan.trim().isEmpty()) throw IllegalArgumentException("نوع اشتراک را وارد کنید.")
        if (request.totalSessions <= 0) throw IllegalArgumentException("تعداد کل جلسات باید بیشتر از صفر باشد.")
        if (request.remainingSessions < 0 || request.remainingSessions > request.totalSessions) throw IllegalArgumentException("جلسات باقی‌مانده باید بین صفر و تعداد کل جلسات باشد.")
        if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(request.expiresAt.trim())) throw IllegalArgumentException("تاریخ انقضا باید به شکل YYYY-MM-DD باشد.")
        try { java.time.LocalDate.parse(request.expiresAt.trim()) } catch (_: Exception) { throw IllegalArgumentException("تاریخ انقضا معتبر نیست.") }
    }

    private fun validateDate(value: String) {
        val date = value.trim()
        if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(date)) {
            throw IllegalArgumentException("تاریخ باید به شکل YYYY-MM-DD باشد.")
        }
        try {
            java.time.LocalDate.parse(date)
        } catch (_: Exception) {
            throw IllegalArgumentException("تاریخ معتبر نیست.")
        }
    }

    private fun isExpired(expiresAt: String): Boolean {
        return try {
            java.time.LocalDate.parse(expiresAt).isBefore(java.time.LocalDate.now())
        } catch (_: Exception) {
            true
        }
    }

    private fun validateClassInput(title: String, day: String, time: String, capacity: Int) {
        if (title.trim().isEmpty()) throw IllegalArgumentException("عنوان کلاس را وارد کنید.")
        if (day.trim().isEmpty()) throw IllegalArgumentException("روز کلاس را وارد کنید.")
        if (!Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(time.trim())) throw IllegalArgumentException("ساعت باید به شکل HH:MM باشد.")
        if (capacity <= 0) throw IllegalArgumentException("ظرفیت باید بیشتر از صفر باشد.")
    }

    private fun nextNumericId(ids: List<String>, prefix: String): Int =
        (ids.mapNotNull { it.removePrefix(prefix).toIntOrNull() }.maxOrNull() ?: 0) + 1

    private fun validateCoachInput(name: String, phone: String, exceptCoachId: String? = null) {
        if (name.trim().length < 2) throw IllegalArgumentException("نام مربی باید حداقل ۲ کاراکتر باشد.")
        val normalized = normalizePhone(phone)
        if (!Regex("^09\\d{9}$").matches(normalized)) throw IllegalArgumentException("شماره موبایل مربی باید ۱۱ رقم و با 09 شروع شود.")
        if (coaches.any { it.id != exceptCoachId && normalizePhone(it.phone) == normalized }) {
            throw IllegalArgumentException("این شماره موبایل قبلاً برای مربی دیگری ثبت شده است.")
        }
    }

    override fun createCoach(request: CreateCoachRequest): CoachDto {
        validateCoachInput(request.name, request.phone)
        if (coaches.any { it.name.trim().equals(request.name.trim(), ignoreCase = true) }) {
            throw IllegalArgumentException("این نام مربی قبلاً ثبت شده است.")
        }
        val coach = CoachDto(
            "c-" + nextNumericId(coaches.map { it.id }, "c-"),
            request.name.trim(),
            normalizePhone(request.phone)
        )
        coaches += coach
        persistCoaches()
        return coach
    }

    override fun updateCoach(coachId: String, request: UpdateCoachRequest): CoachDto {
        val index = coaches.indexOfFirst { it.id == coachId }
        if (index < 0) throw IllegalArgumentException("مربی پیدا نشد.")
        validateCoachInput(request.name, request.phone, coachId)
        if (coaches.any { it.id != coachId && it.name.trim().equals(request.name.trim(), ignoreCase = true) }) {
            throw IllegalArgumentException("این نام مربی قبلاً ثبت شده است.")
        }
        val current = coaches[index]
        val updated = current.copy(name = request.name.trim(), phone = normalizePhone(request.phone))
        coaches[index] = updated
        for (i in classes.indices) {
            if (classes[i].coach == current.name) {
                classes[i] = classes[i].copy(coach = updated.name)
            }
        }
        persistCoaches()
        persistSharedState()
        return updated
    }

    override fun deleteCoach(coachId: String): Boolean {
        val coach = coaches.firstOrNull { it.id == coachId } ?: return false
        if (classes.any { it.coach == coach.name }) {
            throw IllegalStateException("مربی به یک یا چند کلاس اختصاص داده شده و قابل حذف نیست.")
        }
        coaches.removeAll { it.id == coachId }
        persistCoaches()
        return true
    }

    override fun createClass(request: CreateClassRequest): TrainingClassDto {
        validateClassInput(request.title, request.day, request.time, request.capacity)
        val coachName = coaches.firstOrNull { it.id == request.coachId }?.name ?: throw IllegalArgumentException("مربی پیدا نشد.")
        val nextId = (classes.maxOfOrNull { it.id } ?: 0) + 1
        val result = TrainingClassDto(nextId, request.title.trim(), request.day.trim(), request.time.trim(), request.capacity, 0, coachName)
        classes += result
        persistSharedState()
        return result
    }
    override fun updateClass(classId: Int, request: UpdateClassRequest): TrainingClassDto {
        val index = classes.indexOfFirst { it.id == classId }
        if (index < 0) throw IllegalArgumentException("کلاس پیدا نشد.")
        val current = classes[index]
        validateClassInput(request.title, request.day, request.time, request.capacity)
        if (request.capacity < current.booked) throw IllegalStateException("ظرفیت جدید نمی‌تواند کمتر از تعداد رزروهای فعلی باشد.")
        val coachName = coaches.firstOrNull { it.id == request.coachId }?.name ?: throw IllegalArgumentException("مربی پیدا نشد.")
        val updated = current.copy(title = request.title.trim(), day = request.day.trim(), time = request.time.trim(), capacity = request.capacity, coach = coachName)
        classes[index] = updated
        persistSharedState()
        return updated
    }

    override fun deleteClass(classId: Int): Boolean {
        if (bookings.any { it.classId == classId && it.status == "CONFIRMED" }) {
            throw IllegalStateException("کلاسی که رزرو فعال دارد قابل حذف نیست.")
        }
        val removed = classes.removeAll { it.id == classId }
        if (removed) persistSharedState()
        return removed
    }

    override fun recordAttendance(request: RecordAttendanceRequest): AttendanceDto {
        validateDate(request.date)
        if (members.none { it.id == request.memberId }) throw IllegalArgumentException("عضو پیدا نشد.")
        if (classes.none { it.id == request.classId }) throw IllegalArgumentException("کلاس پیدا نشد.")
        if (!bookings.any { it.memberId == request.memberId && it.classId == request.classId && it.status == "CONFIRMED" }) {
            throw IllegalStateException("برای این عضو در این کلاس رزرو فعال وجود ندارد.")
        }
        val existing = attendance.indexOfFirst {
            it.memberId == request.memberId && it.classId == request.classId && it.date == request.date
        }
        val result = AttendanceDto(
            if (existing >= 0) attendance[existing].id else "a-" + System.currentTimeMillis(),
            request.memberId, request.classId, request.date, request.present
        )
        if (existing >= 0) attendance[existing] = result else attendance += result
        persistAttendance(result)
        return result
    }
}
