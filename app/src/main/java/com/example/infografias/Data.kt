package com.example.infografias

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Si el modelo deja de funcionar, mira la lista actual en console.groq.com/docs/models
const val BASE = "https://api.groq.com/openai/v1"
const val LLM_MODEL = "openai/gpt-oss-120b"

data class Stat(val value: String, val label: String)
data class Side(val title: String, val items: List<String>)

sealed class Block
data class StatsB(val stats: List<Stat>) : Block()
data class ListB(val icon: String, val heading: String, val items: List<String>) : Block()
data class StepsB(val heading: String, val items: List<String>) : Block()
data class CompareB(val heading: String, val left: Side, val right: Side) : Block()

data class Info(val title: String, val subtitle: String, val blocks: List<Block>, val footer: String)

private fun strings(a: JSONArray?): List<String> =
    if (a == null) emptyList() else (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() }

private fun side(o: JSONObject?): Side =
    Side(o?.optString("title") ?: "", strings(o?.optJSONArray("items")))

fun parseInfo(text: String): Info {
    val o = JSONObject(text)
    val arr = o.optJSONArray("blocks") ?: JSONArray()
    val blocks = ArrayList<Block>()
    for (i in 0 until arr.length()) {
        val b = arr.optJSONObject(i) ?: continue
        when (b.optString("type")) {
            "stats" -> {
                val items = b.optJSONArray("items") ?: JSONArray()
                val stats = (0 until items.length()).mapNotNull { j ->
                    items.optJSONObject(j)?.let { Stat(it.optString("value"), it.optString("label")) }
                }.filter { it.value.isNotBlank() }.take(3)
                if (stats.isNotEmpty()) blocks.add(StatsB(stats))
            }
            "list" -> {
                val items = strings(b.optJSONArray("items"))
                if (items.isNotEmpty()) {
                    blocks.add(ListB(b.optString("icon", "•").ifBlank { "•" }, b.optString("heading"), items))
                }
            }
            "steps" -> {
                val items = strings(b.optJSONArray("items"))
                if (items.isNotEmpty()) blocks.add(StepsB(b.optString("heading"), items))
            }
            "compare" -> {
                val l = side(b.optJSONObject("left"))
                val r = side(b.optJSONObject("right"))
                if (l.items.isNotEmpty() || r.items.isNotEmpty()) {
                    blocks.add(CompareB(b.optString("heading"), l, r))
                }
            }
        }
    }
    if (blocks.isEmpty()) throw Exception("La IA no devolvió contenido. Intenta de nuevo.")
    return Info(
        o.optString("title", "Infografía").ifBlank { "Infografía" },
        o.optString("subtitle"), blocks, o.optString("footer")
    )
}

private fun readResponse(c: HttpURLConnection): String {
    val ok = c.responseCode in 200..299
    val txt = (if (ok) c.inputStream else c.errorStream).bufferedReader().readText()
    if (!ok) throw Exception("Error ${c.responseCode}: " + txt.take(300))
    return txt
}

fun makeInfo(key: String, topic: String): Info {
    val system = """
Eres un diseñador de infografías. Con el tema o texto del usuario, responde SOLO con un objeto JSON con estas claves:
"title": título corto (máximo 8 palabras).
"subtitle": una frase que resuma el tema.
"blocks": lista de 4 a 6 bloques, mezclando tipos. Cada bloque es uno de estos:
{"type":"stats","items":[{"value":"8 h","label":"texto corto"}]} con 1 a 3 cifras.
{"type":"list","icon":"un emoji","heading":"título","items":["...","..."]} con 3 a 5 ítems cortos.
{"type":"steps","heading":"título","items":["...","..."]} con 3 a 5 pasos.
{"type":"compare","heading":"título","left":{"title":"A","items":["..."]},"right":{"title":"B","items":["..."]}}.
"footer": una frase corta de cierre.
Reglas: cada ítem de máximo 12 palabras. No inventes cifras ni estadísticas: usa números solo si están en el texto del usuario o son datos muy conocidos y seguros; si no, usa datos cualitativos o evita el bloque stats. Escribe en el mismo idioma del usuario.
""".trimIndent()
    val body = JSONObject().put("model", LLM_MODEL).put("temperature", 0.4)
        .put("max_completion_tokens", 2500)
        .put("reasoning_effort", "low")
        .put("response_format", JSONObject().put("type", "json_object"))
        .put(
            "messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", system))
                .put(JSONObject().put("role", "user").put("content", topic))
        )
    val c = URL("$BASE/chat/completions").openConnection() as HttpURLConnection
    try {
        c.requestMethod = "POST"
        c.doOutput = true
        c.connectTimeout = 30000
        c.readTimeout = 90000
        c.setRequestProperty("Authorization", "Bearer $key")
        c.setRequestProperty("Content-Type", "application/json")
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        val content = JSONObject(readResponse(c)).getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content")
        return try {
            parseInfo(content.replace("```json", "").replace("```", "").trim())
        } catch (e: Exception) {
            throw Exception("No pude interpretar la respuesta de la IA. Intenta de nuevo. (" + e.message + ")")
        }
    } finally {
        c.disconnect()
    }
}

fun savePng(ctx: Context, bmp: Bitmap): Uri? {
    val v = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "infografia_" + System.currentTimeMillis() + ".png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Infografias")
    }
    val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v) ?: return null
    ctx.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return uri
}
