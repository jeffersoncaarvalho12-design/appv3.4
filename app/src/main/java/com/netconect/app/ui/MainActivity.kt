package com.netconect.app.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.integration.android.IntentIntegrator
import com.netconect.app.R
import com.netconect.app.util.ApiClient
import com.netconect.app.util.SessionManager
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var session: SessionManager
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var tvWelcome: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvTopTechnicians: TextView
    private lateinit var etQuickSearch: EditText
    private lateinit var imgUserPhoto: ImageView
    private lateinit var tvUserInitials: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        session = SessionManager(this)
        if (session.getToken().isNullOrBlank()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        swipe = findViewById(R.id.swipeDashboard)
        tvWelcome = findViewById(R.id.tvWelcome)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tvTopTechnicians = findViewById(R.id.tvTopTechnicians)
        etQuickSearch = findViewById(R.id.etQuickSearch)
        imgUserPhoto = findViewById(R.id.imgUserPhoto)
        tvUserInitials = findViewById(R.id.tvUserInitials)

        val username = session.getUsername() ?: "usuário"
        tvWelcome.text = "Olá, $username"
        tvSubtitle.text = "Controle de estoque Net Conect"
        setupUserHeader(username)

        findViewById<Button>(R.id.btnQuickSearch).setOnClickListener { openQuickSearch() }
        findViewById<Button>(R.id.btnGoScanner).setOnClickListener { startScanner() }
        findViewById<Button>(R.id.btnGoEntry).setOnClickListener {
            startActivity(Intent(this, EntryActivity::class.java))
        }
        findViewById<Button>(R.id.btnGoExit).setOnClickListener {
            startActivity(Intent(this, MoveActivity::class.java))
        }
        findViewById<Button>(R.id.btnGoBatch).setOnClickListener {
            startActivity(Intent(this, BatchMoveActivity::class.java))
        }
        findViewById<Button>(R.id.btnGoStock).setOnClickListener {
            startActivity(Intent(this, StockProductsActivity::class.java))
        }
        findViewById<Button>(R.id.btnGoHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        findViewById<Button>(R.id.btnGoSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<FloatingActionButton>(R.id.fabScanner).setOnClickListener { startScanner() }

        swipe.setOnRefreshListener { loadDashboard() }
        loadDashboard()
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    private fun setupUserHeader(username: String) {
        val photoPath = session.getUserPhotoPath().orEmpty()
        if (photoPath.isBlank()) {
            showInitials(username)
            return
        }

        val fullUrl = if (photoPath.startsWith("http://") || photoPath.startsWith("https://")) {
            photoPath
        } else {
            session.getBaseUrl() + if (photoPath.startsWith('/')) photoPath else "/$photoPath"
        }

        thread {
            try {
                val conn = URL(fullUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.doInput = true
                conn.connect()
                val bmp = BitmapFactory.decodeStream(conn.inputStream)

                runOnUiThread {
                    if (bmp != null) {
                        imgUserPhoto.setImageBitmap(bmp)
                        imgUserPhoto.visibility = android.view.View.VISIBLE
                        tvUserInitials.visibility = android.view.View.GONE
                    } else {
                        showInitials(username)
                    }
                }
            } catch (_: Exception) {
                runOnUiThread { showInitials(username) }
            }
        }
    }

    private fun showInitials(username: String) {
        val initials = username
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { part -> part.first().uppercaseChar().toString() }
            .ifBlank { "NC" }

        tvUserInitials.text = initials
        tvUserInitials.visibility = android.view.View.VISIBLE
        imgUserPhoto.visibility = android.view.View.GONE
    }

    private fun openQuickSearch() {
        val term = etQuickSearch.text.toString().trim()
        if (term.isBlank()) {
            Toast.makeText(this, "Digite serial, MAC ou modelo", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, MoveActivity::class.java)
        intent.putExtra("barcode", term)
        startActivity(intent)
    }

    private fun loadDashboard() {
        swipe.isRefreshing = true

        thread {
            val result = ApiClient.get(
                session.getBaseUrl() + "/api/dashboard.php",
                session.getToken()
            )

            runOnUiThread {
                swipe.isRefreshing = false
                val body = result.body

                if (result.success && body?.optString("status") == "success") {
                    val d = body.optJSONObject("data")
                    val top = d?.optJSONArray("top_technicians")
                    tvTopTechnicians.text = buildTopTechText(top)
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

    private fun buildTopTechText(arr: JSONArray?): String {
        if (arr == null || arr.length() == 0) {
            return "🏆 Top técnicos em retiradas
-"
        }

        val sb = StringBuilder()
        sb.append("🏆 Top técnicos em retiradas

")

        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val name = item.optString("technician_name", "Técnico")
            val total = item.optString("total_out", "0")
            val date = formatDate(item.optString("last_out_date", "-"))

            sb.append(i + 1)
                .append(". ")
                .append(name)
                .append(" — ")
                .append(total)
                .append(" retirada(s)
")
                .append("Última retirada: ")
                .append(date)

            if (i < arr.length() - 1) {
                sb.append("

")
            }
        }

        return sb.toString()
    }

    private fun formatDate(raw: String): String {
        return try {
            if (raw == "-" || raw.isBlank()) return raw
            val input = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val output = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val parsed = input.parse(raw)
            if (parsed != null) output.format(parsed) else raw
        } catch (_: Exception) {
            raw
        }
    }

    private fun startScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setPrompt("Ler serial ou MAC")
        integrator.setBeepEnabled(true)
        integrator.setOrientationLocked(false)
        integrator.initiateScan()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            val intent = Intent(this, MoveActivity::class.java)
            intent.putExtra("barcode", result.contents)
            startActivity(intent)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
