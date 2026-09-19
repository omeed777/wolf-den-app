package com.wolfden.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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

private val WolfBlack = Color(0xFF111111)
private val WolfRed = Color(0xFFE53935)
private val WolfSurface = Color(0xFFF5F5F5)

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

    MaterialTheme(colorScheme = lightColorScheme(primary = WolfRed, background = WolfSurface)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            if (!loggedIn) {
                LoginFlow(
                    phone = phone,
                    onPhoneChange = { phone = it },
                    onLogin = { loggedIn = true }
                )
            } else {
                MainShell(onLogout = { loggedIn = false })
            }
        }
    }
}

@Composable
private fun LoginFlow(phone: String, onPhoneChange: (String) -> Unit, onLogin: () -> Unit) {
    var otpStep by rememberSaveable { mutableStateOf(false) }
    var otp by rememberSaveable { mutableStateOf("") }

    if (!otpStep) {
        LoginScreen(phone, onPhoneChange) { otpStep = true }
    } else {
        OtpScreen(
            phone = phone,
            otp = otp,
            onOtpChange = { otp = it },
            onVerify = onLogin,
            onBack = { otpStep = false }
        )
    }
}

@Composable
private fun LoginScreen(phone: String, onPhoneChange: (String) -> Unit, onContinue: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("WOLF DEN", fontSize = 38.sp, fontWeight = FontWeight.Black, color = WolfBlack)
        Text("CROSSFIT", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WolfRed)
        Spacer(Modifier.height(36.dp))
        Text("ورود اعضا", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("برای ورود شماره موبایل خود را وارد کنید.", color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { value -> onPhoneChange(value.filter { it.isDigit() }.take(11)) },
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
        Text("در این مرحله ارسال واقعی پیامک هنوز متصل نشده است.", color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
private fun OtpScreen(
    phone: String,
    otp: String,
    onOtpChange: (String) -> Unit,
    onVerify: () -> Unit,
    onBack: () -> Unit
) {
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
            onValueChange = { onOtpChange(it.filter { c -> c.isDigit() }.take(6)) },
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
        Text("فعلاً برای تست، کد واردشده فقط از نظر ۶ رقمی بودن بررسی می‌شود.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MainShell(onLogout: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = WolfSurface,
        bottomBar = {
            NavigationBar {
                listOf("خانه", "کلاس‌ها", "اشتراک").forEachIndexed { i, label ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(if (i == 0) "⌂" else if (i == 1) "▣" else "●") },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        when (tab) {
            0 -> HomeScreen(Modifier.padding(padding), onLogout)
            1 -> ClassesScreen(Modifier.padding(padding))
            else -> SubscriptionScreen(Modifier.padding(padding))
        }
    }
}

@Composable
private fun HomeScreen(modifier: Modifier = Modifier, onLogout: () -> Unit) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("WOLF DEN", fontSize = 30.sp, fontWeight = FontWeight.Black, color = WolfBlack)
        Text("خوش آمدی 👋", fontSize = 18.sp)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = WolfBlack)) {
            Column(Modifier.padding(20.dp)) {
                Text("اشتراک فعال", color = Color.White)
                Text("۱۲ جلسه", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("وضعیت اشتراک شما", color = Color.LightGray)
            }
        }
        Text("دسترسی سریع", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("رزرو کلاس") }
            Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("اشتراک") }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("خروج از حساب") }
    }
}

@Composable
private fun ClassesScreen(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("کلاس‌ها", fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        ClassCard("CrossFit", "امروز • 18:00", "8 / 12 نفر")
        Spacer(Modifier.height(12.dp))
        ClassCard("CrossFit", "امروز • 20:00", "10 / 12 نفر")
        Spacer(Modifier.height(12.dp))
        ClassCard("Strength", "فردا • 18:00", "5 / 10 نفر")
    }
}

@Composable
private fun ClassCard(title: String, time: String, capacity: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(time, color = Color.Gray)
                Text(capacity, color = Color.Gray)
            }
            Button(onClick = {}) { Text("رزرو") }
        }
    }
}

@Composable
private fun SubscriptionScreen(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("اشتراک من", fontSize = 28.sp, fontWeight = FontWeight.Black)
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("پلن فعلی", color = Color.Gray)
                Text("۱۲ جلسه در ماه", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text("جلسات باقی‌مانده: ۸")
                Text("وضعیت: فعال", color = WolfRed, fontWeight = FontWeight.Bold)
            }
        }
        Text("پرداخت داخل اپ در نسخه اول فعال نیست.", color = Color.Gray)
    }
}
