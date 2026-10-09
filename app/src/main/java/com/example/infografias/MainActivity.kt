package com.example.infografias

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Palette(
    val name: String, val bg: Color, val card: Color, val accent: Color,
    val onAccent: Color, val text: Color
)

val PALETTES = listOf(
    Palette("Azul", Color(0xFFEAF2FF), Color.White, Color(0xFF1E66F5), Color.White, Color(0xFF1B2A41)),
    Palette("Verde", Color(0xFFE8F6EC), Color.White, Color(0xFF2E9E5B), Color.White, Color(0xFF1B3A28)),
    Palette("Naranja", Color(0xFFFFF1E6), Color.White, Color(0xFFEF6C00), Color.White, Color(0xFF4A2A10)),
    Palette("Morado", Color(0xFFF1EBFA), Color.White, Color(0xFF6A3FC0), Color.White, Color(0xFF2B1B4A)),
    Palette("Oscuro", Color(0xFF14161C), Color(0xFF1F2430), Color(0xFF4DD0E1), Color(0xFF0B1A1D), Color(0xFFEDEFF4))
)

val AppBg = Color(0xFFF6F7FB)
val Brand = Color(0xFF6A3FC0)
val Muted = Color(0xFF777777)
val RedErr = Color(0xFFC62828)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("p", MODE_PRIVATE)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Brand)) { App(prefs) }
        }
    }
}

@Composable
fun Bullet(text: String, pal: Palette) {
    Row(verticalAlignment = Alignment.Top) {
        Text("●", color = pal.accent, fontSize = 8.sp, modifier = Modifier.padding(top = 7.dp, end = 8.dp))
        Text(text, color = pal.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun BlockView(b: Block, pal: Palette) {
    val shape = RoundedCornerShape(18.dp)
    val base = Modifier.fillMaxWidth().clip(shape).background(pal.card)
    when (b) {
        is StatsB -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            b.stats.forEach { st ->
                Column(
                    Modifier.weight(1f).clip(shape).background(pal.card).padding(vertical = 16.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        st.value, color = pal.accent, fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
                    )
                    Text(st.label, color = pal.text, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }
        is ListB -> Column(base.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(b.icon, fontSize = 26.sp)
                Spacer(Modifier.width(10.dp))
                Text(b.heading, color = pal.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            b.items.forEach { Bullet(it, pal) }
        }
        is StepsB -> Column(base.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(b.heading, color = pal.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            b.items.forEachIndexed { i, t ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(pal.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("" + (i + 1), color = pal.onAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(t, color = pal.text, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(top = 3.dp))
                }
            }
        }
        is CompareB -> Column(base.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (b.heading.isNotBlank()) {
                Text(b.heading, color = pal.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(b.left, b.right).forEach { sd ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(sd.title, color = pal.accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        sd.items.forEach { Bullet(it, pal) }
                    }
                }
            }
        }
    }
}

@Composable
fun Infographic(info: Info, pal: Palette) {
    Column(
        Modifier.fillMaxWidth().background(pal.bg).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(pal.accent).padding(20.dp)
        ) {
            Text(
                info.title, color = pal.onAccent, fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp
            )
            if (info.subtitle.isNotBlank()) {
                Text(
                    info.subtitle, color = pal.onAccent.copy(alpha = 0.9f), fontSize = 14.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        info.blocks.forEach { BlockView(it, pal) }
        if (info.footer.isNotBlank()) {
            Text(
                info.footer, color = pal.text.copy(alpha = 0.6f), fontSize = 12.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(
        text, color = if (selected) Color.White else Brand, fontSize = 14.sp,
        modifier = Modifier.clip(shape)
            .background(if (selected) Brand else Color.White)
            .border(1.dp, Brand, shape)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
fun App(prefs: SharedPreferences) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    var key by remember { mutableStateOf(prefs.getString("groq", "") ?: "") }
    var keyInput by remember { mutableStateOf(key) }
    var showKey by remember { mutableStateOf(key.isEmpty()) }
    var topic by remember { mutableStateOf("") }
    var palIdx by remember { mutableStateOf(0) }
    var info by remember { mutableStateOf<Info?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val pal = PALETTES[palIdx]

    fun generate() {
        if (key.isEmpty()) { showKey = true; return }
        loading = true
        error = ""
        scope.launch {
            try {
                info = withContext(Dispatchers.IO) { makeInfo(key, topic.trim()) }
            } catch (e: Exception) {
                error = e.message ?: "Error"
            }
            loading = false
        }
    }

    fun export(share: Boolean) {
        scope.launch {
            try {
                val bmp = layer.toImageBitmap().asAndroidBitmap()
                val uri = withContext(Dispatchers.IO) { savePng(ctx, bmp) }
                if (uri == null) {
                    error = "No pude guardar la imagen."
                    return@launch
                }
                if (share) {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    ctx.startActivity(Intent.createChooser(send, "Compartir infografía"))
                } else {
                    Toast.makeText(ctx, "Guardada en Imágenes/Infografias", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                error = "No pude crear la imagen: " + e.message
            }
        }
    }

    Box(Modifier.fillMaxSize().background(AppBg).safeDrawingPadding()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Infografías IA", color = Brand, fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f)
                )
                Text(
                    "🔑", fontSize = 22.sp,
                    modifier = Modifier.clickable { keyInput = key; showKey = true }.padding(8.dp)
                )
            }
            Text("Escribe un tema o pega un texto y la IA arma la infografía.", color = Muted)
            OutlinedTextField(
                value = topic, onValueChange = { topic = it }, minLines = 4,
                placeholder = { Text("Ej: los beneficios de dormir bien") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Beneficios de dormir bien", "Cómo ahorrar dinero", "El ciclo del agua").forEach { ex ->
                    Chip(ex) { topic = ex }
                }
            }
            Text("Colores", fontWeight = FontWeight.Bold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PALETTES.forEachIndexed { i, p -> Chip(p.name, i == palIdx) { palIdx = i } }
            }
            Button(
                onClick = { generate() }, enabled = topic.isNotBlank() && !loading,
                modifier = Modifier.fillMaxWidth()
            ) { Text("✨ Crear infografía") }
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Diseñando tu infografía…", color = Muted)
                }
            }
            if (error.isNotEmpty()) Text(error, color = RedErr)
            info?.let { inf ->
                Box(
                    Modifier.fillMaxWidth().drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    }
                ) { Infographic(inf, pal) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { export(false) }) { Text("💾 Guardar imagen") }
                    OutlinedButton(onClick = { export(true) }) { Text("📤 Compartir") }
                }
                Text(
                    "Revisa los datos y cifras antes de compartir: la IA puede equivocarse.",
                    color = Muted, fontSize = 12.sp
                )
            }
        }
    }

    if (showKey) {
        AlertDialog(
            onDismissRequest = { if (key.isNotEmpty()) showKey = false },
            title = { Text("Groq API key") },
            text = {
                OutlinedTextField(
                    value = keyInput, onValueChange = { keyInput = it }, singleLine = true,
                    label = { Text("gsk_...") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    key = keyInput.trim()
                    prefs.edit().putString("groq", key).apply()
                    if (key.isNotEmpty()) showKey = false
                }) { Text("Guardar") }
            }
        )
    }
}
