package com.wolfden.app

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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wolfden.app.model.Booking
import com.wolfden.app.model.TrainingClass
import com.wolfden.app.viewmodel.WolfDenViewModel

private val WolfBlack = Color(0xFF111111)
private val WolfRed = Color(0xFFE53935)
private val WolfSurface = Color(0xFFF4F4F2)
private val WolfGold = Color(0xFFFFC107)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WolfDenApp() }
    }
}

@Composable
private fun WolfDenApp() {
    var loggedIn by rememberSaveable { mutableStateOf(false) }
    var phone by rememberSaveable { mutableStateOf("") }
    val viewModel: WolfDenViewModel = viewModel()

    MaterialTheme(colorScheme = lightColorScheme(primary = WolfRed, background = WolfSurface)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            if (!loggedIn) LoginFlow(phone, { phone = it }) { loggedIn = true }
            else MainShell(viewModel, onLogout = { loggedIn = false })
        }
    }
}

@Composable
private fun LoginFlow(phone: String, onPhoneChange: (String) -> Unit, onLogin: () -> Unit) {
    var otpStep by rememberSaveable { mutableStateOf(false) }
    var otp by rememberSaveable { mutableStateOf("") }
    if (!otpStep) LoginScreen(phone, onPhoneChange) { otpStep = true }
    else OtpScreen(phone, otp, { otp = it }, onLogin) { otpStep = false }
}

@Composable
private fun LoginScreen(phone: String, onPhoneChange: (String) -> Unit, onContinue: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WolfBrand(size = 74.dp)
        Spacer(Modifier.height(14.dp))
        Text("WOLF DEN", fontSize = 38.sp, fontWeight = FontWeight.Black, color = WolfBlack)
        Text("CROSSFIT", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WolfRed)
        Spacer(Modifier.height(36.dp))
        Text("ورود اعضا", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("برای ورود شماره موبایل خود را وارد کنید.", color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { onPhoneChange(it.filter(Char::isDigit).take(11)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("شماره موبایل") },
            placeholder = { Text("09xxxxxxxxx") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onContinue,
            enabled = phone.length == 11 && phone.startsWith("09"),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("دریافت کد تایید", fontSize = 16.sp) }
        Spacer(Modifier.height(12.dp))
        Text("ارسال واقعی پیامک در مرحله اتصال Backend فعال می‌شود.", color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
private fun OtpScreen(phone: String, otp: String, onOtpChange: (String) -> Unit, onVerify: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("تایید شماره", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("کد تایید ارسال‌شده به $phone را وارد کنید.", color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = otp,
            onValueChange = { onOtpChange(it.filter(Char::isDigit).take(6)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("کد ۶ رقمی") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onVerify,
            enabled = otp.length == 6,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("ورود به Wolf Den", fontSize = 16.sp) }
        TextButton(onClick = onBack) { Text("ویرایش شماره موبایل") }
        Text("فعلاً کد فقط از نظر ۶ رقمی بودن بررسی می‌شود.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
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
                Text(message, color = Color.White, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
            }
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier,
    memberName: String,
    bookingCount: Int,
    onLogout: () -> Unit,
    onClasses: () -> Unit
) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WolfBrand(size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("WOLF DEN", fontSize = 24.sp, fontWeight = FontWeight.Black, color = WolfBlack)
                Text("سلام $memberName 👋", fontSize = 16.sp)
            }
        }
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = WolfBlack)) {
            Column(Modifier.padding(20.dp)) {
                Text("اشتراک فعال", color = Color.White)
                Text("۱۲ جلسه", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("برای مشاهده تعداد جلسات باقی‌مانده وارد بخش اشتراک شوید.", color = Color.LightGray)
            }
        }
        if (bookingCount > 0) Text("رزروهای فعال: $bookingCount جلسه", color = WolfRed, fontWeight = FontWeight.Bold)
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
        Text("کلاس موردنظر را انتخاب و رزرو کن.", color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        if (classes.isEmpty()) {
            Text("کلاسی برای نمایش وجود ندارد.", color = Color.Gray)
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
        Text("کلاس‌های رزروشده فعلی", color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        if (bookings.isEmpty()) {
            Card(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("هنوز کلاسی رزرو نکرده‌ای.", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("از بخش کلاس‌ها می‌توانی اولین کلاس خودت را رزرو کنی.", color = Color.Gray)
                }
            }
        } else {
            bookings.forEach { booking ->
                val trainingClass = classes.firstOrNull { it.id == booking.classId }
                if (trainingClass != null) {
                    Card(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(trainingClass.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("${trainingClass.day} • ${trainingClass.time}", color = Color.Gray)
                            Text("مربی: ${trainingClass.coach}", color = Color.Gray)
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
                    Text("${trainingClass.day} • ${trainingClass.time}", color = Color.Gray)
                    Text("مربی: ${trainingClass.coach}", color = Color.Gray)
                }
                Text("${trainingClass.booked} / ${trainingClass.capacity}", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Text("ظرفیت باقی‌مانده: ${trainingClass.available} نفر", color = if (trainingClass.available > 0) WolfRed else Color.Gray)
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
        Text(memberName, color = Color.Gray)
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("پلن فعلی", color = Color.Gray)
                Text(plan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text("جلسات باقی‌مانده: $remainingSessions از $totalSessions")
                Text("تاریخ پایان: $expiresAt")
                Text("وضعیت: فعال", color = WolfRed, fontWeight = FontWeight.Bold)
            }
        }
        if (bookingCount > 0) Text("رزروهای فعال: $bookingCount جلسه")
        Text("پرداخت داخل اپ در نسخه اول فعال نیست.", color = Color.Gray)
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
        drawCircle(WolfRed, radius = w * 0.055f, center = androidx.compose.ui.geometry.Offset(w * 0.39f, h * 0.48f))
        drawCircle(WolfRed, radius = w * 0.055f, center = androidx.compose.ui.geometry.Offset(w * 0.61f, h * 0.48f))
        drawLine(WolfGold, androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.68f), androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.74f), strokeWidth = w * 0.04f)
        drawLine(WolfGold, androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.68f), androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.74f), strokeWidth = w * 0.04f)
    }
}
