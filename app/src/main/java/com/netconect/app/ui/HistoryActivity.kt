package com.netconect.app.ui

import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.netconect.app.R
import com.netconect.app.util.ApiClient
import com.netconect.app.util.SessionManager
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.concurrent.thread

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val session = SessionManager(this)
        val progress = findViewById<ProgressBar>(R.id.progressHistory)
        val listView = findViewById<ListView>(R.id.listHistory)

        progress.visibility = View.VISIBLE

        thread {
            val result = ApiClient.get(
                session.getBaseUrl() + "/api/history.php",
                session.getToken()
            )

            runOnUiThread {
                progress.visibility = View.GONE
                val body = result.body

                if (result.success && body?.optString("status") == "success") {
                    val arr = body.optJSONArray("data") ?: JSONArray()
                    val items = mutableListOf<Map<String, String>>()

                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        val movementRaw = item.optString("movement")
                        val movementPt = when (movementRaw) {
                            "IN", "IN_STOCK" -> "Entrada"
                            "OUT" -> "Saída"
                            "DEFECT" -> "Defeito"
                            "RETURNED" -> "Retornado"
                            else -> movementRaw
                        }

                        val serial = cleanField(item.optString("serial_number"))
                        val mac = cleanField(item.optString("mac_address"))
                        val local = cleanField(item.optString("location"))
                        val productName = cleanField(item.optString("product_name", "Produto"))
                        val dateText = formatDate(item.optString("created_at"))
                        val badge = when (movementPt) {
                            "Entrada" -> "🟢 Entrada"
                            "Saída" -> "🔴 Saída"
                            "Defeito" -> "🟠 Defeito"
                            "Retornado" -> "🔵 Retornado"
                            else -> movementPt
                        }

                        items.add(
                            mapOf(
                                "date" to dateText,
                                "badge" to badge,
                                "product" to productName,
                                "serial" to "Serial: $serial",
                                "mac" to "MAC: $mac",
                                "local" to "Local: $local"
                            )
                        )
                    }

                    val adapter = SimpleAdapter(
                        this,
                        items,
                        R.layout.item_history_card,
                        arrayOf("date", "badge", "product", "serial", "mac", "local"),
                        intArrayOf(
                            R.id.tvHistoryDate,
                            R.id.tvHistoryBadge,
                            R.id.tvHistoryProduct,
                            R.id.tvHistorySerial,
                            R.id.tvHistoryMac,
                            R.id.tvHistoryLocal
                        )
                    )

                    listView.adapter = adapter
                } else {
                    Toast.makeText(
                        this,
                        body?.optString("message", result.message) ?: result.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun cleanField(value: String?): String {
        val text = value?.trim().orEmpty()
        return if (text.isBlank() || text.equals("null", ignoreCase = true)) "—" else text
    }

    private fun formatDate(raw: String): String {
        return try {
            if (raw.isBlank()) return raw
            val input = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val output = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val parsed = input.parse(raw)
            if (parsed != null) output.format(parsed) else raw
        } catch (_: Exception) {
            raw
        }
    }
}
