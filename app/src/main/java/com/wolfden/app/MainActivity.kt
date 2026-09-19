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
import com.wolfden.app.data.remote.AdminSubscriptionDto
import com.wolfden.app.data.remote.TrainingClassDto
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
            if (!loggedIn) {
                LoginFlow(
                    phone = phone,
                    onPhoneChange = { phone = it },
                    authService = authService,
                    productionMode = productionMode,
                    onDemoLogin = ::loginDemo,
                    onProductionLogin = ::loginProduction
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
    onProductionLogin: (com.wolfden.app.data.remote.AuthResponse) -> Unit
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
        LoginScreen(phone, onPhoneChange, ::requestOtp, loading, error, productionMode)
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
    productionMode: Boolean
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
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
        Modifier.fillMaxSize().padding(24.dp),
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

@Composable
private fun AdminDashboard() {
    val context = LocalContext.current
    val repository = remember { DemoWolfDenAdminRepository(context) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val members = remember { mutableStateListOf<AdminMemberDto>().apply { addAll(repository.getMembers()) } }
    val subscriptions = remember { mutableStateListOf<AdminSubscriptionDto>().apply { addAll(repository.getSubscriptions()) } }
    val classes = remember { mutableStateListOf<TrainingClassDto>().apply { addAll(repository.getClasses()) } }
    Scaffold(containerColor = WolfSurface, topBar = { TopAppBar(title = { Text("مدیریت Wolf Den", fontWeight = FontWeight.Black) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = WolfBlack)) }, bottomBar = { NavigationBar { listOf("اعضا", "اشتراک‌ها", "کلاس‌ها").forEachIndexed { i, label -> NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Text(if (i == 0) "●" else if (i == 1) "◆" else "▣") }, label = { Text(label) }) } } }) { padding ->
        when (tab) {
            0 -> AdminMembersScreen(Modifier.padding(padding), members)
            1 -> AdminSubscriptionsScreen(Modifier.padding(padding), subscriptions)
            else -> AdminClassesScreen(Modifier.padding(padding), classes)
        }
    }
}

@Composable
private fun AdminMembersScreen(modifier: Modifier, members: List<AdminMemberDto>) {
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("اعضای باشگاه", fontSize = 28.sp, fontWeight = FontWeight.Black); Text("مدیریت اعضای Wolf Den", color = WolfMuted) }
        items(members, key = { it.id }) { member ->
            Card(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp)) { Text(member.name, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(member.phone, color = WolfMuted); Text("وضعیت: " + member.status, color = WolfGoldBright) } }
        }
    }
}

@Composable
private fun AdminSubscriptionsScreen(modifier: Modifier, subscriptions: List<AdminSubscriptionDto>) {
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("اشتراک‌ها", fontSize = 28.sp, fontWeight = FontWeight.Black); Text("وضعیت و جلسات باقی‌مانده اعضا", color = WolfMuted) }
        items(subscriptions, key = { it.id }) { subscription ->
            Card(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp)) { Text(subscription.plan, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("عضو: " + subscription.memberId, color = WolfMuted); Text("جلسات: " + subscription.remainingSessions + " از " + subscription.totalSessions); Text("انقضا: " + subscription.expiresAt, color = WolfMuted); Text("وضعیت: " + subscription.status, color = WolfGoldBright) } }
        }
    }
}

@Composable
private fun AdminClassesScreen(modifier: Modifier, classes: List<TrainingClassDto>) {
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("کلاس‌ها", fontSize = 28.sp, fontWeight = FontWeight.Black); Text("برنامه کلاس‌ها و ظرفیت", color = WolfMuted) }
        items(classes, key = { it.id }) { trainingClass ->
            Card(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp)) { Text(trainingClass.title, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(trainingClass.day + " • " + trainingClass.time, color = WolfMuted); Text("مربی: " + trainingClass.coach); Text("ظرفیت: " + trainingClass.booked + " / " + trainingClass.capacity, color = WolfGoldBright) } }
        }
    }
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
                    uiState.bookings.size,
                    onLogout
                ) { tab = 1 }
                1 -> ClassesScreen(
                    Modifier.padding(padding),
                    uiState.classes,
                    uiState.bookings,
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
                Text("اشتراک فعال", color = WolfText)
                Text(plan, color = WolfGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("$remainingSessions جلسه", color = WolfText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("از مجموع $totalSessions جلسه", color = WolfMuted)
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
    onBook: (Int) -> Unit,
    onCancel: (Int) -> Unit
) {
    val bookedIds = bookings.map { it.classId }.toSet()
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("کلاس‌ها", fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("کلاس موردنظر را انتخاب و رزرو کن.", color = WolfMuted)
        Spacer(Modifier.height(16.dp))
        if (classes.isEmpty()) {
            Text("کلاسی برای نمایش وجود ندارد.", color = WolfMuted)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(classes, key = { it.id }) { trainingClass ->
                    val bookedByMe = trainingClass.id in bookedIds
                    ClassCard(trainingClass, bookedByMe) {
                        if (bookedByMe) onCancel(trainingClass.id) else onBook(trainingClass.id)
                    }
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
            bookings.forEach { booking ->
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
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ClassCard(trainingClass: TrainingClass, bookedByMe: Boolean, onBooking: () -> Unit) {
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
                enabled = bookedByMe || trainingClass.available > 0,
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
                Text("وضعیت: فعال", color = WolfGoldBright, fontWeight = FontWeight.Bold)
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
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.50f, h * 0.08f)
            lineTo(w * 0.20f, h * 0.28f)
            lineTo(w * 0.14f, h * 0.78f)
            lineTo(w * 0.50f, h * 0.94f)
            lineTo(w * 0.86f, h * 0.78f)
            lineTo(w * 0.80f, h * 0.28f)
            close()
        }
        drawPath(path, color = WolfBlack)
        drawCircle(WolfGoldBright, radius = w * 0.055f, center = androidx.compose.ui.geometry.Offset(w * 0.39f, h * 0.48f))
        drawCircle(WolfGoldBright, radius = w * 0.055f, center = androidx.compose.ui.geometry.Offset(w * 0.61f, h * 0.48f))
        drawLine(WolfGold, androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.68f), androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.74f), strokeWidth = w * 0.04f)
        drawLine(WolfGold, androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.68f), androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.74f), strokeWidth = w * 0.04f)
    }
}
