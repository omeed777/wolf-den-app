package com.wolfden.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wolfden.app.model.Booking
import com.wolfden.app.data.DemoRepository
import com.wolfden.app.data.remote.RemoteWolfDenAuthService
import com.wolfden.app.data.remote.RemoteWolfDenRepository
import com.wolfden.app.data.remote.SharedPreferencesAccessTokenStore
import com.wolfden.app.data.remote.WolfDenApiConfig
import com.wolfden.app.data.remote.WolfDenAuthService
import com.wolfden.app.data.remote.WolfDenHttpApi
import com.wolfden.app.data.remote.DemoWolfDenAdminRepository
import com.wolfden.app.data.remote.AdminMemberDto
import com.wolfden.app.data.remote.AdminCreateBookingRequest
import com.wolfden.app.data.remote.AdminSubscriptionDto
import com.wolfden.app.data.remote.TrainingClassDto
import com.wolfden.app.data.remote.CreateMemberRequest
import com.wolfden.app.data.remote.UpdateMemberRequest
import com.wolfden.app.data.remote.CreateClassRequest
import com.wolfden.app.data.remote.CoachDto
import com.wolfden.app.data.remote.CreateCoachRequest
import com.wolfden.app.data.remote.UpdateCoachRequest
import com.wolfden.app.data.remote.UpdateSubscriptionRequest
import com.wolfden.app.data.remote.RecordAttendanceRequest
import com.wolfden.app.data.remote.AdminBookingDto
import com.wolfden.app.data.remote.WolfDenAdminRepository
import com.wolfden.app.model.TrainingClass
import com.wolfden.app.viewmodel.WolfDenViewModel
import com.wolfden.app.viewmodel.WolfDenViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val WolfBlack = Color(0xFF080808)
private val WolfSurface = Color(0xFF111111)
private val WolfCard = Color(0xFF1A1A1A)
private val WolfGold = Color(0xFFD4AF37)
private val WolfGoldBright = Color(0xFFFFD700)
private val WolfText = Color(0xFFF5F5F5)
private val WolfMuted = Color(0xFFB8B8B8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WolfDenApp() }
    }
}

@Composable
private fun WolfDenApp() {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("wolf_den_session", android.content.Context.MODE_PRIVATE)
    }
    val tokenStore = remember { SharedPreferencesAccessTokenStore(context) }
    val apiBaseUrl = BuildConfig.WOLF_DEN_API_BASE_URL.trim()
    val productionMode = apiBaseUrl.isNotBlank()
    val api = remember(apiBaseUrl) {
        if (apiBaseUrl.isBlank()) null
        else WolfDenHttpApi(WolfDenApiConfig(apiBaseUrl))
    }
    val authService: WolfDenAuthService? = remember(api) {
        api?.let { RemoteWolfDenAuthService(it, tokenStore) }
    }

    var loggedIn by rememberSaveable {
        mutableStateOf(
            preferences.getBoolean("logged_in", false) &&
                (!productionMode || !tokenStore.get().isNullOrBlank())
        )
    }
    var phone by rememberSaveable {
        mutableStateOf(preferences.getString("phone", "") ?: "")
    }
    var memberId by rememberSaveable {
        mutableStateOf(preferences.getString("member_id", "") ?: "")
    }
    var accessToken by remember { mutableStateOf(tokenStore.get()) }
    var showAdmin by rememberSaveable { mutableStateOf(false) }

    fun loginDemo() {
        preferences.edit()
            .putBoolean("logged_in", true)
            .putString("phone", phone)
            .remove("member_id")
            .apply()
        loggedIn = true
        accessToken = null
    }

    fun loginProduction(auth: com.wolfden.app.data.remote.AuthResponse) {
        preferences.edit()
            .putBoolean("logged_in", true)
            .putString("phone", phone)
            .putString("member_id", auth.memberId)
            .apply()
        loggedIn = true
        memberId = auth.memberId
        accessToken = auth.accessToken
    }

    fun logout() {
        preferences.edit().clear().apply()
        tokenStore.clear()
        loggedIn = false
        phone = ""
        memberId = ""
        accessToken = null
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = WolfGoldBright,
            onPrimary = WolfBlack,
            secondary = WolfGold,
            background = WolfSurface,
            surface = WolfCard,
            onBackground = WolfText,
            onSurface = WolfText,
            outline = Color(0xFF4A4A4A)
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            if (showAdmin) {
                AdminDashboard(onBack = { showAdmin = false })
            } else if (!loggedIn) {
                LoginFlow(
                    phone = phone,
                    onPhoneChange = { phone = it },
                    authService = authService,
                    productionMode = productionMode,
                    onDemoLogin = ::loginDemo,
                    onProductionLogin = ::loginProduction,
                    onOpenAdmin = { showAdmin = true }
                )
            } else {
                val repository = remember(accessToken, memberId, productionMode) {
                    if (productionMode && !accessToken.isNullOrBlank() && memberId.isNotBlank()) {
                        RemoteWolfDenRepository(
                            api = api!!,
                            accessToken = accessToken!!,
                            memberId = memberId
                        )
                    } else {
                        DemoRepository(context)
                    }
                }
                val factory = remember(repository) {
                    WolfDenViewModelFactory(
                        application = context.applicationContext as Application,
                        repository = repository
                    )
                }
                val viewModel: WolfDenViewModel = viewModel(
                    key = if (productionMode) "wolf-den-production" else "wolf-den-demo",
                    factory = factory
                )
                MainShell(viewModel, onLogout = ::logout)
            }
        }
    }
}

@Composable
private fun LoginFlow(
    phone: String,
    onPhoneChange: (String) -> Unit,
    authService: WolfDenAuthService?,
    productionMode: Boolean,
    onDemoLogin: () -> Unit,
    onProductionLogin: (com.wolfden.app.data.remote.AuthResponse) -> Unit,
    onOpenAdmin: () -> Unit
) {
    var otpStep by rememberSaveable { mutableStateOf(false) }
    var otp by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun requestOtp() {
        error = null
        if (!productionMode || authService == null) {
            otpStep = true
            return
        }
        loading = true
        scope.launch {
            try {
                val success = withContext(Dispatchers.IO) { authService.requestOtp(phone) }
                if (success) otpStep = true
                else error = "ارسال کد تایید انجام نشد."
            } catch (e: Exception) {
                error = e.message?.take(140) ?: "ارتباط با سرور برقرار نشد."
            } finally {
                loading = false
            }
        }
    }

    fun verifyOtp() {
        error = null
        if (!productionMode || authService == null) {
            onDemoLogin()
            return
        }
        loading = true
        scope.launch {
            try {
                val auth = withContext(Dispatchers.IO) {
                    authService.verifyOtp(phone, otp)
                }
                onProductionLogin(auth)
            } catch (e: Exception) {
                error = e.message?.take(140) ?: "کد تایید معتبر نیست."
            } finally {
                loading = false
            }
        }
    }

    if (!otpStep) {
        LoginScreen(phone, onPhoneChange, ::requestOtp, loading, error, productionMode, onOpenAdmin)
    } else {
        OtpScreen(
            phone = phone,
            otp = otp,
            onOtpChange = { otp = it },
            onVerify = ::verifyOtp,
            onBack = {
                otpStep = false
                error = null
            },
            loading = loading,
            error = error,
            productionMode = productionMode
        )
    }
}

@Composable
private fun LoginScreen(
    phone: String,
    onPhoneChange: (String) -> Unit,
    onContinue: () -> Unit,
    loading: Boolean,
    error: String?,
    productionMode: Boolean,
    onOpenAdmin: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).background(WolfBlack),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WolfBrand(size = 74.dp)
        Spacer(Modifier.height(14.dp))
        Text("WOLF DEN", fontSize = 38.sp, fontWeight = FontWeight.Black, color = WolfGoldBright)
        Text("CROSSFIT", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WolfGold)
        Spacer(Modifier.height(36.dp))
        Text("ورود اعضا", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("برای ورود شماره موبایل خود را وارد کنید.", color = WolfMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { onPhoneChange(it.filter(Char::isDigit).take(11)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("شماره موبایل") },
            placeholder = { Text("09xxxxxxxxx") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            enabled = !loading
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onContinue,
            enabled = phone.length == 11 && phone.startsWith("09") && !loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text("دریافت کد تایید", fontSize = 16.sp)
        }
        error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = Color(0xFFFF6B6B), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(12.dp))
        if (productionMode) {
            Text("کد تایید از طریق Backend ارسال می‌شود.", color = WolfMuted, fontSize = 12.sp)
        } else {
            Text("Demo Mode: ارسال واقعی پیامک هنوز فعال نشده است.", color = WolfMuted, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onOpenAdmin, modifier = Modifier.fillMaxWidth()) {
                Text("پنل مدیریت (Demo)")
            }
        }
    }
}

@Composable
private fun OtpScreen(
    phone: String,
    otp: String,
    onOtpChange: (String) -> Unit,
    onVerify: () -> Unit,
    onBack: () -> Unit,
    loading: Boolean,
    error: String?,
    productionMode: Boolean
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).background(WolfBlack),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("تایید شماره", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("کد تایید ارسال‌شده به $phone را وارد کنید.", color = WolfMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = otp,
            onValueChange = { onOtpChange(it.filter(Char::isDigit).take(6)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("کد ۶ رقمی") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            enabled = !loading
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onVerify,
            enabled = otp.length == 6 && !loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text("ورود به Wolf Den", fontSize = 16.sp)
        }
        TextButton(onClick = onBack, enabled = !loading) { Text("ویرایش شماره موبایل") }
        if (!productionMode) {
            Text("کد تست: ۱۲۳۴۵۶", color = WolfGold, fontSize = 12.sp)
            Text("در Demo Mode هر کد ۶ رقمی پذیرفته می‌شود.", color = WolfMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
        } else {
            Text("اعتبارسنجی کد توسط Backend انجام می‌شود.", color = WolfMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = Color(0xFFFF6B6B), textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminDashboard(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository: WolfDenAdminRepository = remember { DemoWolfDenAdminRepository(context) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showAddMember by rememberSaveable { mutableStateOf(false) }
    var showAddClass by rememberSaveable { mutableStateOf(false) }
    var showAttendance by rememberSaveable { mutableStateOf(false) }
    var showAddBooking by rememberSaveable { mutableStateOf(false) }
    var showAddCoach by rememberSaveable { mutableStateOf(false) }
    var adminMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val bookings = remember { mutableStateListOf<AdminBookingDto>().apply { addAll(repository.getBookings()) } }
    val members = remember { mutableStateListOf<AdminMemberDto>().apply { addAll(repository.getMembers()) } }
    val subscriptions = remember { mutableStateListOf<AdminSubscriptionDto>().apply { addAll(repository.getSubscriptions()) } }
    val classes = remember { mutableStateListOf<TrainingClassDto>().apply { addAll(repository.getClasses()) } }
    val coaches = remember { mutableStateListOf<CoachDto>().apply { addAll(repository.getCoaches()) } }

    Scaffold(
        containerColor = WolfSurface,
        topBar = { TopAppBar(
            navigationIcon = { TextButton(onClick = onBack) { Text("بازگشت") } },
            title = { Text("مدیریت Wolf Den", fontWeight = FontWeight.Black) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = WolfBlack)
        ) },
        bottomBar = { NavigationBar {
            listOf("داشبورد", "اعضا", "اشتراک‌ها", "کلاس‌ها", "رزروها", "حضور", "مربی‌ها").forEachIndexed { i, label ->
                NavigationBarItem(selected = tab == i, onClick = { tab = i },
                    icon = { Text(if (i == 0) "●" else if (i == 1) "◆" else "▣") },
                    label = { Text(label) })
            }
        }}
    ) { padding ->
        when (tab) {
            0 -> AdminOverviewScreen(Modifier.padding(padding), members, subscriptions, classes, bookings)
            1 -> AdminMembersScreen(Modifier.padding(padding), members, subscriptions, onAdd = { showAddMember = true }, onEditSubscription = { tab = 2 }, onUpdateMember = { member ->
                try {
                    val updated = repository.updateMember(member.id, UpdateMemberRequest(member.name, member.phone, member.status))
                    val index = members.indexOfFirst { it.id == updated.id }
                    if (index >= 0) members[index] = updated
                } catch (e: Exception) {
                    adminMessage = e.message ?: "ویرایش عضو انجام نشد."
                }
            })
            2 -> AdminSubscriptionsScreen(Modifier.padding(padding), subscriptions, members, onRenew = { subscription ->
                try {
                    val updated = repository.updateSubscription(
                        subscription.memberId,
                        UpdateSubscriptionRequest(subscription.plan, subscription.totalSessions, subscription.totalSessions, "ACTIVE", subscription.expiresAt)
                    )
                    val index = subscriptions.indexOfFirst { it.id == updated.id }
                    if (index >= 0) subscriptions[index] = updated
                } catch (e: Exception) {
                    adminMessage = e.message ?: "تمدید اشتراک انجام نشد."
                }
            }, onUpdate = { subscription ->
                try {
                    val updated = repository.updateSubscription(
                        subscription.memberId,
                        UpdateSubscriptionRequest(subscription.plan, subscription.totalSessions, subscription.remainingSessions, subscription.status, subscription.expiresAt)
                    )
                    val index = subscriptions.indexOfFirst { it.id == updated.id }
                    if (index >= 0) subscriptions[index] = updated
                } catch (e: Exception) {
                    adminMessage = e.message ?: "ویرایش اشتراک انجام نشد."
                }
            })
            3 -> AdminClassesScreen(Modifier.padding(padding), classes, coaches, onAdd = { showAddClass = true }, onUpdate = { cls, request -> try { val updated = repository.updateClass(cls.id, request); val index = classes.indexOfFirst { it.id == updated.id }; if (index >= 0) classes[index] = updated } catch (e: Exception) { adminMessage = e.message ?: "ویرایش کلاس انجام نشد." } }, onDelete = { cls -> try { if (repository.deleteClass(cls.id)) classes.removeAll { it.id == cls.id } } catch (e: Exception) { adminMessage = e.message ?: "حذف کلاس انجام نشد." } })
            4 -> AdminBookingsScreen(Modifier.padding(padding), bookings, onAdd = { showAddBooking = true }, onCancel = { bookingId ->
                val booking = bookings.firstOrNull { it.id == bookingId }
                try {
                    if (booking != null && repository.cancelBooking(bookingId)) {
                        bookings.removeAll { it.id == bookingId }
                        val classIndex = classes.indexOfFirst { it.id == booking.classId }
                        if (classIndex >= 0) classes[classIndex] = classes[classIndex].copy(booked = (classes[classIndex].booked - 1).coerceAtLeast(0))
                        val subIndex = subscriptions.indexOfFirst { it.memberId == booking.memberId }
                        if (subIndex >= 0) subscriptions[subIndex] = subscriptions[subIndex].copy(remainingSessions = (subscriptions[subIndex].remainingSessions + 1).coerceAtMost(subscriptions[subIndex].totalSessions))
                    }
                } catch (e: Exception) { adminMessage = e.message ?: "لغو رزرو انجام نشد." }
            })
            5 -> AdminAttendanceScreen(Modifier.padding(padding), members, classes, onSave = { memberId, classId, date, present -> try { repository.recordAttendance(RecordAttendanceRequest(memberId, classId, date, present)); showAttendance = false } catch (e: Exception) { adminMessage = e.message ?: "ثبت حضور انجام نشد." } })
            else -> {
                AdminCoachesScreen(
                    Modifier.padding(padding), coaches,
                    onAdd = { showAddCoach = true },
                    onUpdate = { coach, request -> try { val updated = repository.updateCoach(coach.id, request); val i = coaches.indexOfFirst { it.id == updated.id }; if (i >= 0) coaches[i] = updated } catch (e: Exception) { adminMessage = e.message ?: "ویرایش مربی انجام نشد." } },
                    onDelete = { coach -> try { if (repository.deleteCoach(coach.id)) coaches.removeAll { it.id == coach.id } } catch (e: Exception) { adminMessage = e.message ?: "حذف مربی انجام نشد." } }
                )
            }
        }
    }

    if (showAddCoach) {
        AddCoachDialog(onDismiss = { showAddCoach = false }, onSave = { name, phone ->
            try { repository.createCoach(CreateCoachRequest(name, phone)); showAddCoach = false } catch (e: Exception) { adminMessage = e.message ?: "ثبت مربی انجام نشد." }
        })
    }

    if (showAddMember) {
        AddMemberDialog(
            onDismiss = { showAddMember = false },
            onSave = { name, phone ->
                try { members += repository.createMember(CreateMemberRequest(name, phone)); showAddMember = false } catch (e: Exception) { adminMessage = e.message ?: "ثبت عضو انجام نشد." }
            }
        )
    }
    if (showAddBooking) {
        AdminCreateBookingDialog(
            members = members,
            classes = classes,
            subscriptions = subscriptions,
            bookings = bookings,
            onDismiss = { showAddBooking = false },
            onSave = { memberId, classId ->
                try {
                    val booking = repository.createBooking(AdminCreateBookingRequest(memberId, classId))
                    bookings += booking
                val classIndex = classes.indexOfFirst { it.id == classId }
                if (classIndex >= 0) classes[classIndex] = classes[classIndex].copy(booked = classes[classIndex].booked + 1)
                val subIndex = subscriptions.indexOfFirst { it.memberId == memberId }
                if (subIndex >= 0) subscriptions[subIndex] = subscriptions[subIndex].copy(remainingSessions = (subscriptions[subIndex].remainingSessions - 1).coerceAtLeast(0))
                    showAddBooking = false
                } catch (e: Exception) { adminMessage = e.message ?: "ثبت رزرو انجام نشد." }
            }
        )
    }

    if (showAddClass) {
        AddClassDialog(
            coaches = coaches,
            onDismiss = { showAddClass = false },
            onSave = { title, day, time, capacity, coachId ->
                try { classes += repository.createClass(CreateClassRequest(title, day, time, capacity, coachId)); showAddClass = false } catch (e: Exception) { adminMessage = e.message ?: "ثبت کلاس انجام نشد." }
            }
        )
    }
    adminMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { adminMessage = null },
            title = { Text("پیام مدیریت") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { adminMessage = null }) { Text("باشه") } }
        )
    }

}

@Composable
private fun AdminMembersScreen(
    modifier: Modifier,
    members: List<AdminMemberDto>,
    subscriptions: List<AdminSubscriptionDto>,
    onAdd: () -> Unit,
    onEditSubscription: (AdminSubscriptionDto) -> Unit,
    onUpdateMember: (AdminMemberDto) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var editMember by remember { mutableStateOf<AdminMemberDto?>(null) }
    val filtered = members.filter { it.name.contains(query, ignoreCase = true) || it.phone.contains(query) }

    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("اعضای باشگاه", fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text("جستجو و مدیریت سریع اعضا", color = WolfMuted)
                }
                Button(onClick = onAdd) { Text("عضو جدید") }
            }
        }
        item {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("جستجو نام یا موبایل") }, singleLine = true)
        }
        items(filtered, key = { it.id }) { member ->
            val sub = subscriptions.firstOrNull { it.memberId == member.id }
            Card(Modifier.fillMaxWidth().clickable { selectedId = member.id }, RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(member.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(member.phone, color = WolfMuted)
                    Text("وضعیت: " + member.status, color = if (member.status == "ACTIVE") WolfGoldBright else Color(0xFFFF6B6B))
                    Text(if (sub != null) sub.plan + " • " + sub.remainingSessions + " جلسه باقی‌مانده" else "بدون اشتراک", color = WolfMuted)
                }
            }
        }
        if (filtered.isEmpty()) item { Text("عضوی پیدا نشد.", color = WolfMuted) }
    }

    members.firstOrNull { it.id == selectedId }?.let { selected ->
        val sub = subscriptions.firstOrNull { it.memberId == selected.id }
        AlertDialog(
            onDismissRequest = { selectedId = null },
            title = { Text(selected.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("موبایل: " + selected.phone)
                    Text("وضعیت عضو: " + selected.status)
                    HorizontalDivider()
                    if (sub != null) {
                        Text("اشتراک: " + sub.plan, fontWeight = FontWeight.Bold)
                        Text("جلسات: " + sub.remainingSessions + " از " + sub.totalSessions)
                        Text("انقضا: " + sub.expiresAt)
                    } else Text("این عضو اشتراک ندارد.", color = WolfMuted)
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { editMember = selected; selectedId = null }) { Text("ویرایش عضو") }
                    if (sub != null) Button(onClick = { onEditSubscription(sub); selectedId = null }) { Text("اشتراک") }
                }
            },
            dismissButton = { TextButton(onClick = { selectedId = null }) { Text("بستن") } }
        )
    }

    editMember?.let { member ->
        var name by remember(member.id) { mutableStateOf(member.name) }
        var phone by remember(member.id) { mutableStateOf(member.phone) }
        var status by remember(member.id) { mutableStateOf(member.status) }
        AlertDialog(
            onDismissRequest = { editMember = null },
            title = { Text("ویرایش عضو") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("نام و نام خانوادگی") }, singleLine = true)
                    OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(11) }, label = { Text("شماره موبایل") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(status == "ACTIVE", { status = "ACTIVE" }); Text("فعال")
                        Spacer(Modifier.width(12.dp))
                        RadioButton(status == "SUSPENDED", { status = "SUSPENDED" }); Text("تعلیق")
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = name.trim().isNotBlank() && phone.length == 11,
                    onClick = {
                        onUpdateMember(member.copy(name = name.trim(), phone = phone, status = status))
                        editMember = null
                    }
                ) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { editMember = null }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun AdminSubscriptionsScreen(
    modifier: Modifier,
    subscriptions: List<AdminSubscriptionDto>,
    members: List<AdminMemberDto>,
    onRenew: (AdminSubscriptionDto) -> Unit,
    onUpdate: (AdminSubscriptionDto) -> Unit
) {
    var selected by remember { mutableStateOf<AdminSubscriptionDto?>(null) }
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("اشتراک‌ها", fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("مدیریت پلن، جلسات و تاریخ انقضا", color = WolfMuted)
        }
        items(subscriptions, key = { it.id }) { subscription ->
            val member = members.firstOrNull { it.id == subscription.memberId }
            Card(Modifier.fillMaxWidth().clickable { selected = subscription }, RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(member?.name ?: subscription.memberId, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(subscription.plan)
                    Text("جلسات: ${subscription.remainingSessions} از ${subscription.totalSessions}")
                    Text("انقضا: ${subscription.expiresAt}", color = WolfMuted)
                    Text("وضعیت: ${subscription.status}", color = WolfGoldBright)
                }
            }
        }
    }
    selected?.let { sub ->
        var plan by remember(sub.id) { mutableStateOf(sub.plan) }
        var total by remember(sub.id) { mutableStateOf(sub.totalSessions.toString()) }
        var remaining by remember(sub.id) { mutableStateOf(sub.remainingSessions.toString()) }
        var expires by remember(sub.id) { mutableStateOf(sub.expiresAt) }
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("ویرایش اشتراک") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("عضو: " + (members.firstOrNull { it.id == sub.memberId }?.name ?: sub.memberId))
                    OutlinedTextField(plan, { plan = it }, label = { Text("پلن") }, singleLine = true)
                    OutlinedTextField(total, { total = it.filter(Char::isDigit) }, label = { Text("کل جلسات") }, singleLine = true)
                    OutlinedTextField(remaining, { remaining = it.filter(Char::isDigit) }, label = { Text("جلسات باقی‌مانده") }, singleLine = true)
                    OutlinedTextField(expires, { expires = it }, label = { Text("تاریخ انقضا") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdate(sub.copy(
                        plan = plan,
                        totalSessions = total.toIntOrNull()?.coerceAtLeast(1) ?: sub.totalSessions,
                        remainingSessions = remaining.toIntOrNull()?.coerceAtLeast(0) ?: sub.remainingSessions,
                        expiresAt = expires,
                        status = "ACTIVE"
                    ))
                    selected = null
                }) { Text("ذخیره") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { onRenew(sub); selected = null }) { Text("تمدید") }
                    TextButton(onClick = { selected = null }) { Text("انصراف") }
                }
            }
        )
    }
}

@Composable
private fun AdminClassesScreen(modifier: Modifier, classes: List<TrainingClassDto>, coaches: List<CoachDto>, onAdd: () -> Unit, onUpdate: (TrainingClassDto, UpdateClassRequest) -> Unit, onDelete: (TrainingClassDto) -> Unit) {
    var selectedClass by remember { mutableStateOf<TrainingClassDto?>(null) }
    var editing by remember { mutableStateOf<TrainingClassDto?>(null) }
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("کلاس‌ها", fontSize = 28.sp, fontWeight = FontWeight.Black); Text("برنامه کلاس‌ها و ظرفیت", color = WolfMuted) }; Button(onClick = onAdd) { Text("کلاس جدید") } } }
        items(classes, key = { it.id }) { cls -> Card(Modifier.fillMaxWidth().clickable { selectedClass = cls }, RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(cls.title, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(cls.day + " • " + cls.time, color = WolfMuted); Text("مربی: " + cls.coach); Text("ظرفیت: " + cls.booked + " / " + cls.capacity, color = if (cls.booked >= cls.capacity) Color.Red else WolfGoldBright) } } }
    }
    selectedClass?.let { cls -> AlertDialog(onDismissRequest = { selectedClass = null }, title = { Text(cls.title) }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("روز: " + cls.day); Text("ساعت: " + cls.time); Text("مربی: " + cls.coach); Text("ظرفیت: " + cls.booked + " / " + cls.capacity) } }, confirmButton = { TextButton(onClick = { editing = cls; selectedClass = null }) { Text("ویرایش") } }, dismissButton = { Row { if (cls.booked == 0) TextButton(onClick = { onDelete(cls); selectedClass = null }) { Text("حذف", color = Color(0xFFFF6B6B)) }; TextButton(onClick = { selectedClass = null }) { Text("بستن") } } }) }
    editing?.let { cls -> EditClassDialog(cls, coaches, { editing = null }) { request -> onUpdate(cls, request); editing = null } }
}

@Composable private fun EditClassDialog(
    cls: TrainingClassDto,
    coaches: List<CoachDto>,
    onDismiss: () -> Unit,
    onSave: (UpdateClassRequest) -> Unit
) {
    var title by remember { mutableStateOf(cls.title) }
    var day by remember { mutableStateOf(cls.day) }
    var time by remember { mutableStateOf(cls.time) }
    var capacity by remember { mutableStateOf(cls.capacity.toString()) }
    var coach by remember { mutableStateOf(coaches.firstOrNull { it.name == cls.coach } ?: coaches.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش کلاس") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("عنوان") }, singleLine = true)
                OutlinedTextField(day, { day = it }, label = { Text("روز") }, singleLine = true)
                OutlinedTextField(time, { time = it }, label = { Text("ساعت") }, singleLine = true)
                OutlinedTextField(capacity, { capacity = it.filter(Char::isDigit).take(2) }, label = { Text("ظرفیت") }, singleLine = true)
                Text("مربی", fontWeight = FontWeight.Bold)
                coaches.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().clickable { coach = item }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = coach?.id == item.id, onClick = { coach = item })
                        Text(item.name)
                    }
                }
            }
        },
        confirmButton = {
            val cap = capacity.toIntOrNull() ?: 0
            Button(
                enabled = title.isNotBlank() && day.isNotBlank() && time.isNotBlank() && cap >= cls.booked && cap > 0 && coach != null,
                onClick = { onSave(UpdateClassRequest(title, day, time, cap, coach!!.id)) }
            ) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun AdminOverviewScreen(modifier: Modifier, members: List<AdminMemberDto>, subscriptions: List<AdminSubscriptionDto>, classes: List<TrainingClassDto>, bookings: List<AdminBookingDto>) {
    val activeMembers = members.count { it.status == "ACTIVE" }
    val activeSubscriptions = subscriptions.count { it.status == "ACTIVE" }
    val lowSessions = subscriptions.count { it.remainingSessions <= 2 }
    val totalCapacity = classes.sumOf { it.capacity }
    val booked = classes.sumOf { it.booked }
    val confirmedBookings = bookings.count { it.status == "CONFIRMED" }
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("داشبورد مدیریت", fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("نمای کلی وضعیت Wolf Den", color = WolfMuted)
        AdminStatCard("کل اعضا", members.size.toString())
        AdminStatCard("اعضای فعال", activeMembers.toString())
        AdminStatCard("اشتراک فعال", activeSubscriptions.toString())
        AdminStatCard("اشتراک با جلسات کم", lowSessions.toString())
        AdminStatCard("رزرو کلاس‌ها", "$booked / $totalCapacity")
        AdminStatCard("رزروهای فعال", confirmedBookings.toString())
    }
}

@Composable
private fun AdminStatCard(title: String, value: String) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = WolfGoldBright)
        }
    }
}

@Composable
private fun AdminBookingsScreen(
    modifier: Modifier,
    bookings: List<AdminBookingDto>,
    onAdd: () -> Unit,
    onCancel: (String) -> Unit
) {
    var selected by remember { mutableStateOf<AdminBookingDto?>(null) }
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("رزروهای کلاس", fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text("مشاهده و مدیریت رزرو اعضا", color = WolfMuted)
                }
                Button(onClick = onAdd) { Text("رزرو دستی") }
            }
        }
        items(bookings, key = { it.id }) { booking ->
            Card(Modifier.fillMaxWidth().clickable { selected = booking }, RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(booking.memberName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(booking.classTitle + " • " + booking.day + " • " + booking.time)
                    Text("وضعیت: " + booking.status, color = WolfGoldBright)
                    Text("ثبت: " + booking.createdAt, color = WolfMuted, fontSize = 12.sp)
                }
            }
        }
        if (bookings.isEmpty()) item { Text("رزروی ثبت نشده است.", color = WolfMuted) }
    }
    selected?.let { booking ->
        AlertDialog(
            onDismissRequest = { selected = null },
            confirmButton = { TextButton(onClick = { onCancel(booking.id); selected = null }) { Text("لغو رزرو", color = Color(0xFFFF6B6B)) } },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("بستن") } },
            title = { Text("جزئیات رزرو") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("عضو: " + booking.memberName)
                Text("کلاس: " + booking.classTitle)
                Text("زمان: " + booking.day + " • " + booking.time)
                Text("وضعیت: " + booking.status)
            }}
        )
    }
}

@Composable
private fun AdminCoachesScreen(modifier: Modifier, coaches: List<CoachDto>, onAdd: () -> Unit, onUpdate: (CoachDto, UpdateCoachRequest) -> Unit, onDelete: (CoachDto) -> Unit) {
    var selected by remember { mutableStateOf<CoachDto?>(null) }
    var editing by remember { mutableStateOf<CoachDto?>(null) }
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("مربی‌ها", fontSize = 28.sp, fontWeight = FontWeight.Black); Text("مدیریت مربی‌های Wolf Den", color = WolfMuted) }
            Button(onClick = onAdd) { Text("مربی جدید") }
        }
        coaches.forEach { coach -> Card(Modifier.fillMaxWidth().clickable { selected = coach }) { Column(Modifier.padding(16.dp)) { Text(coach.name, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(coach.phone, color = WolfMuted) } } }
    }
    selected?.let { coach -> AlertDialog(onDismissRequest = { selected = null }, title = { Text(coach.name) }, text = { Text("تلفن: " + coach.phone) }, confirmButton = { TextButton(onClick = { editing = coach; selected = null }) { Text("ویرایش") } }, dismissButton = { Row { TextButton(onClick = { onDelete(coach); selected = null }) { Text("حذف", color = Color(0xFFFF6B6B)) }; TextButton(onClick = { selected = null }) { Text("بستن") } } }) }
    editing?.let { coach -> EditCoachDialog(coach, { editing = null }) { request -> onUpdate(coach, request); editing = null } }
}
@Composable
private fun EditCoachDialog(coach: CoachDto, onDismiss: () -> Unit, onSave: (UpdateCoachRequest) -> Unit) {
    var name by remember { mutableStateOf(coach.name) }; var phone by remember { mutableStateOf(coach.phone) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("ویرایش مربی") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("نام") }, singleLine = true)
        OutlinedTextField(phone, { phone = it }, label = { Text("موبایل") }, singleLine = true)
    }}, confirmButton = { Button(enabled = name.isNotBlank() && phone.isNotBlank(), onClick = { onSave(UpdateCoachRequest(name, phone)) }) { Text("ذخیره") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}
@Composable
private fun AddCoachDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("مربی جدید") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("نام") }, singleLine = true)
        OutlinedTextField(phone, { phone = it }, label = { Text("موبایل") }, singleLine = true)
    }}, confirmButton = { Button(enabled = name.isNotBlank() && phone.isNotBlank(), onClick = { onSave(name, phone) }) { Text("ثبت") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}

@Composable
private fun AdminAttendanceScreen(modifier: Modifier, members: List<AdminMemberDto>, classes: List<TrainingClassDto>, bookings: List<AdminBookingDto>, onSave: (String, Int, String, Boolean) -> Unit) {
    var memberId by remember { mutableStateOf(members.firstOrNull()?.id ?: "") }
    var classId by remember { mutableIntStateOf(classes.firstOrNull()?.id ?: 0) }
    var date by remember { mutableStateOf("امروز") }
    var present by remember { mutableStateOf(true) }
    val confirmed = bookings.any { it.memberId == memberId && it.classId == classId && it.status == "CONFIRMED" }
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("حضور و غیاب", fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("فقط اعضای دارای رزرو فعال می‌توانند حضورشان ثبت شود.", color = WolfMuted)
        OutlinedTextField(date, { date = it }, label = { Text("تاریخ") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("عضو", fontWeight = FontWeight.Bold)
        members.forEach { member ->
            Row(Modifier.fillMaxWidth().clickable { memberId = member.id }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = memberId == member.id, onClick = { memberId = member.id })
                Text(member.name)
            }
        }
        Text("کلاس", fontWeight = FontWeight.Bold)
        classes.forEach { cls ->
            Row(Modifier.fillMaxWidth().clickable { classId = cls.id }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = classId == cls.id, onClick = { classId = cls.id })
                Text(cls.title + " • " + cls.day + " • " + cls.time)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = present, onCheckedChange = { present = it })
            Text(if (present) "حاضر" else "غایب")
        }
        Button(enabled = memberId.isNotBlank() && classId > 0 && date.isNotBlank() && confirmed, onClick = { onSave(memberId, classId, date, present) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (confirmed) "ثبت وضعیت حضور" else "این عضو برای این کلاس رزرو فعال ندارد")
        }
    }
}

@Composable
private fun AdminCreateBookingDialog(
    members: List<AdminMemberDto>,
    classes: List<TrainingClassDto>,
    subscriptions: List<AdminSubscriptionDto>,
    bookings: List<AdminBookingDto>,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var memberId by remember { mutableStateOf(members.firstOrNull()?.id ?: "") }
    var classId by remember { mutableIntStateOf(classes.firstOrNull()?.id ?: 0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("رزرو دستی برای عضو") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("انتخاب عضو", fontWeight = FontWeight.Bold)
                members.forEach { member ->
                    val sub = subscriptions.firstOrNull { it.memberId == member.id }
                    Row(Modifier.fillMaxWidth().clickable { memberId = member.id }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = memberId == member.id, onClick = { memberId = member.id })
                        Column { Text(member.name); Text("${sub?.remainingSessions ?: 0} جلسه باقی‌مانده", color = WolfMuted, fontSize = 12.sp) }
                    }
                }
                Text("انتخاب کلاس", fontWeight = FontWeight.Bold)
                classes.forEach { cls ->
                    val duplicate = bookings.any { it.memberId == memberId && it.classId == cls.id && it.status == "CONFIRMED" }
                    val enabled = cls.booked < cls.capacity && !duplicate && ((subscriptions.firstOrNull { it.memberId == memberId }?.remainingSessions ?: 0) > 0)
                    Row(Modifier.fillMaxWidth().clickable(enabled = enabled) { classId = cls.id }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = classId == cls.id, onClick = { if (enabled) classId = cls.id }, enabled = enabled)
                        Column {
                            Text(cls.title + " • " + cls.day + " • " + cls.time, color = if (enabled) WolfText else WolfMuted)
                            Text("${cls.booked}/${cls.capacity}", color = WolfMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            val selectedClass = classes.firstOrNull { it.id == classId }
            val sub = subscriptions.firstOrNull { it.memberId == memberId }
            val duplicate = bookings.any { it.memberId == memberId && it.classId == classId && it.status == "CONFIRMED" }
            val enabled = memberId.isNotBlank() && selectedClass != null && selectedClass.booked < selectedClass.capacity && sub?.status == "ACTIVE" && sub.remainingSessions > 0 && !duplicate
            Button(enabled = enabled, onClick = { onSave(memberId, classId) }) { Text("ثبت رزرو") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun AddMemberDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("ثبت عضو جدید") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("نام و نام خانوادگی") }, singleLine = true)
            OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(11) }, label = { Text("شماره موبایل") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        }},
        confirmButton = { Button(enabled = name.isNotBlank() && phone.length == 11, onClick = { onSave(name, phone) }) { Text("ثبت") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}

@Composable
private fun AddClassDialog(coaches: List<CoachDto>, onDismiss: () -> Unit, onSave: (String, String, String, Int, String) -> Unit) {
    var title by remember { mutableStateOf("CrossFit") }
    var day by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("12") }
    var coachId by remember { mutableStateOf(coaches.firstOrNull()?.id ?: "") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("ثبت کلاس جدید") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("عنوان") }, singleLine = true)
            OutlinedTextField(day, { day = it }, label = { Text("روز") }, singleLine = true)
            OutlinedTextField(time, { time = it }, label = { Text("ساعت") }, singleLine = true)
            OutlinedTextField(capacity, { capacity = it.filter(Char::isDigit).take(2) }, label = { Text("ظرفیت") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Text("مربی", fontWeight = FontWeight.Bold)
            coaches.forEach { item -> Row(Modifier.fillMaxWidth().clickable { coachId = item.id }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = coachId == item.id, onClick = { coachId = item.id }); Text(item.name) } }
        }},
        confirmButton = { Button(enabled = title.isNotBlank() && day.isNotBlank() && time.isNotBlank() && (capacity.toIntOrNull() ?: 0) > 0 && coachId.isNotBlank(), onClick = { onSave(title, day, time, capacity.toInt(), coachId) }) { Text("ثبت") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}

@Composable
private fun MainShell(viewModel: WolfDenViewModel, onLogout: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = WolfSurface,
        bottomBar = {
            NavigationBar {
                listOf("خانه", "کلاس‌ها", "رزروهای من", "اشتراک").forEachIndexed { i, label ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(if (i == 0) "⌂" else if (i == 1) "▣" else if (i == 2) "✓" else "●") },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            when (tab) {
                0 -> HomeScreen(
                    Modifier.padding(padding),
                    uiState.member?.name ?: "عضو Wolf Den",
                    uiState.member?.subscription?.plan ?: "-",
                    uiState.member?.subscription?.remainingSessions ?: 0,
                    uiState.member?.subscription?.totalSessions ?: 0,
                    uiState.member?.subscription?.status ?: SubscriptionStatus.SUSPENDED,
                    uiState.member?.subscription?.expiresAt ?: "-",
                    uiState.bookings.size,
                    onLogout
                ) { tab = 1 }
                1 -> ClassesScreen(
                    Modifier.padding(padding),
                    uiState.classes,
                    uiState.bookings,
                    uiState.member?.subscription?.status == SubscriptionStatus.ACTIVE,
                    uiState.member?.subscription?.remainingSessions ?: 0,
                    viewModel::bookClass,
                    viewModel::cancelBooking
                )
                2 -> MyBookingsScreen(
                    Modifier.padding(padding),
                    uiState.bookings,
                    uiState.classes,
                    viewModel::cancelBooking
                )
                else -> SubscriptionScreen(
                    Modifier.padding(padding),
                    uiState.member?.name ?: "عضو Wolf Den",
                    uiState.member?.subscription?.plan ?: "-",
                    uiState.member?.subscription?.remainingSessions ?: 0,
                    uiState.member?.subscription?.totalSessions ?: 0,
                    uiState.member?.subscription?.expiresAt ?: "-",
                    uiState.member?.subscription?.status ?: SubscriptionStatus.SUSPENDED,
                    uiState.bookings.size
                )
            }
        }
    }

    uiState.message?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(1800)
            viewModel.clearMessage()
        }
        Box(
            Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = WolfBlack)) {
                Text(message, color = WolfText, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
            }
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier,
    memberName: String,
    plan: String,
    remainingSessions: Int,
    totalSessions: Int,
    subscriptionStatus: SubscriptionStatus,
    expiresAt: String,
    bookingCount: Int,
    onLogout: () -> Unit,
    onClasses: () -> Unit
) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WolfBrand(size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("WOLF DEN", fontSize = 24.sp, fontWeight = FontWeight.Black, color = WolfGoldBright)
                Text("سلام $memberName 👋", fontSize = 16.sp)
            }
        }
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = WolfBlack)) {
            Column(Modifier.padding(20.dp)) {
                val expired = expiresAt.matches(Regex("^\\\\d{4}-\\\\d{2}-\\\\d{2}$")) && runCatching { java.time.LocalDate.parse(expiresAt).isBefore(java.time.LocalDate.now()) }.getOrDefault(false)
                val statusText = when {
                    expired -> "منقضی شده"
                    subscriptionStatus == SubscriptionStatus.ACTIVE -> "اشتراک فعال"
                    else -> "اشتراک معلق"
                }
                Text(statusText, color = if (expired) Color(0xFFFF6B6B) else WolfText)
                Text(plan, color = WolfGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("$remainingSessions جلسه", color = WolfText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("از مجموع $totalSessions جلسه", color = WolfMuted)
                Text("تاریخ پایان: $expiresAt", color = WolfMuted, fontSize = 12.sp)
            }
        }
        if (bookingCount > 0) Text("رزروهای فعال: $bookingCount جلسه", color = WolfGoldBright, fontWeight = FontWeight.Bold)
        Text("دسترسی سریع", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Button(onClick = onClasses, Modifier.fillMaxWidth().height(52.dp)) { Text("مشاهده و رزرو کلاس‌ها") }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onLogout, Modifier.fillMaxWidth()) { Text("خروج از حساب") }
    }
}

@Composable
private fun ClassesScreen(
    modifier: Modifier,
    classes: List<TrainingClass>,
    bookings: List<Booking>,
    subscriptionActive: Boolean,
    remainingSessions: Int,
    onBook: (Int) -> Unit,
    onCancel: (Int) -> Unit
) {
    val bookedIds = bookings.map { it.classId }.toSet()
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("کلاس‌ها", fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("کلاس موردنظر را انتخاب و رزرو کن.", color = WolfMuted)
        Text("ظرفیت و وضعیت رزرو لحظه‌ای نمایش داده می‌شود.", color = WolfMuted, fontSize = 12.sp)
        if (!subscriptionActive) {
            Text("اشتراک شما فعال نیست؛ تا فعال شدن اشتراک امکان رزرو کلاس ندارید.", color = WolfGoldBright, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        } else if (remainingSessions <= 0) {
            Text("جلسه قابل استفاده ندارید؛ برای رزرو کلاس ابتدا اشتراک خود را تمدید کنید.", color = WolfGoldBright, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(16.dp))
        if (classes.isEmpty()) {
            Text("کلاسی برای نمایش وجود ندارد.", color = WolfMuted)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(classes, key = { it.id }) { trainingClass ->
                    val bookedByMe = trainingClass.id in bookedIds
                    ClassCard(
                        trainingClass = trainingClass,
                        bookedByMe = bookedByMe,
                        canBook = subscriptionActive && remainingSessions > 0,
                        onBooking = { if (bookedByMe) onCancel(trainingClass.id) else onBook(trainingClass.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MyBookingsScreen(
    modifier: Modifier,
    bookings: List<Booking>,
    classes: List<TrainingClass>,
    onCancel: (Int) -> Unit
) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("رزروهای من", fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("کلاس‌های رزروشده فعلی", color = WolfMuted)
        Spacer(Modifier.height(16.dp))
        if (bookings.isEmpty()) {
            Card(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = WolfCard)) {
                Column(Modifier.padding(20.dp)) {
                    Text("هنوز کلاسی رزرو نکرده‌ای.", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("از بخش کلاس‌ها می‌توانی اولین کلاس خودت را رزرو کنی.", color = WolfMuted)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(bookings, key = { it.id }) { booking ->
                    val trainingClass = classes.firstOrNull { it.id == booking.classId }
                    if (trainingClass != null) {
                    Card(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(trainingClass.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("${trainingClass.day} • ${trainingClass.time}", color = WolfMuted)
                            Text("مربی: ${trainingClass.coach}", color = WolfMuted)
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { onCancel(trainingClass.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("لغو رزرو") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassCard(
    trainingClass: TrainingClass,
    bookedByMe: Boolean,
    canBook: Boolean,
    onBooking: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(trainingClass.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("${trainingClass.day} • ${trainingClass.time}", color = WolfMuted)
                    Text("مربی: ${trainingClass.coach}", color = WolfMuted)
                }
                Text("${trainingClass.booked} / ${trainingClass.capacity}", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Text("ظرفیت باقی‌مانده: ${trainingClass.available} نفر", color = if (trainingClass.available > 0) WolfGoldBright else WolfMuted)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onBooking,
                enabled = bookedByMe || (canBook && trainingClass.available > 0),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (bookedByMe) "لغو رزرو" else "رزرو کلاس") }
        }
    }
}

@Composable
private fun SubscriptionScreen(
    modifier: Modifier,
    memberName: String,
    plan: String,
    remainingSessions: Int,
    totalSessions: Int,
    expiresAt: String,
    status: SubscriptionStatus,
    bookingCount: Int
) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("اشتراک من", fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(memberName, color = WolfMuted)
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = WolfCard)) {
            Column(Modifier.padding(20.dp)) {
                Text("پلن فعلی", color = WolfMuted)
                Text(plan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text("جلسات باقی‌مانده: $remainingSessions از $totalSessions")
                Text("تاریخ پایان: $expiresAt")
                val statusText = when (status) {
                    SubscriptionStatus.ACTIVE -> "فعال"
                    SubscriptionStatus.SUSPENDED -> "معلق"
                }
                Text("وضعیت: $statusText", color = if (status == SubscriptionStatus.ACTIVE) WolfGoldBright else WolfMuted, fontWeight = FontWeight.Bold)
                if (status != SubscriptionStatus.ACTIVE) {
                    Text("برای رزرو کلاس باید اشتراک فعال داشته باشید.", color = WolfGoldBright, fontSize = 13.sp)
                } else if (remainingSessions <= 0) {
                    Text("جلسات قابل استفاده شما تمام شده است.", color = WolfGoldBright, fontSize = 13.sp)
                }
            }
        }
        if (bookingCount > 0) Text("رزروهای فعال: $bookingCount جلسه")
        Text("پرداخت داخل اپ در نسخه اول فعال نیست.", color = WolfMuted)
    }
}

@Composable
private fun WolfBrand(size: androidx.compose.ui.unit.Dp) {
    Canvas(Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()
        val outer = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.50f, h * 0.05f)
            lineTo(w * 0.17f, h * 0.24f)
            lineTo(w * 0.08f, h * 0.76f)
            lineTo(w * 0.28f, h * 0.67f)
            lineTo(w * 0.50f, h * 0.95f)
            lineTo(w * 0.72f, h * 0.67f)
            lineTo(w * 0.92f, h * 0.76f)
            lineTo(w * 0.83f, h * 0.24f)
            close()
        }
        drawPath(outer, color = WolfGoldBright)

        val inner = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.50f, h * 0.20f)
            lineTo(w * 0.28f, h * 0.33f)
            lineTo(w * 0.25f, h * 0.61f)
            lineTo(w * 0.50f, h * 0.83f)
            lineTo(w * 0.75f, h * 0.61f)
            lineTo(w * 0.72f, h * 0.33f)
            close()
        }
        drawPath(inner, color = WolfBlack)

        drawCircle(WolfGoldBright, w * 0.045f, androidx.compose.ui.geometry.Offset(w * 0.39f, h * 0.47f))
        drawCircle(WolfGoldBright, w * 0.045f, androidx.compose.ui.geometry.Offset(w * 0.61f, h * 0.47f))

        val muzzle = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.42f, h * 0.64f)
            lineTo(w * 0.50f, h * 0.71f)
            lineTo(w * 0.58f, h * 0.64f)
            lineTo(w * 0.50f, h * 0.79f)
            close()
        }
        drawPath(muzzle, color = WolfGold)
    }
}