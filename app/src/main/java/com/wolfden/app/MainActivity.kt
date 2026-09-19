package com.wolfden.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    var tab by remember { mutableIntStateOf(0) }

    MaterialTheme(colorScheme = lightColorScheme(primary = WolfRed, background = WolfSurface)) {
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
                0 -> HomeScreen(Modifier.padding(padding))
                1 -> ClassesScreen(Modifier.padding(padding))
                else -> SubscriptionScreen(Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun HomeScreen(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("WOLF DEN", fontSize = 30.sp, fontWeight = FontWeight.Black, color = WolfBlack)
        Text("خوش آمدی 👋", fontSize = 18.sp)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = WolfBlack)
        ) {
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
