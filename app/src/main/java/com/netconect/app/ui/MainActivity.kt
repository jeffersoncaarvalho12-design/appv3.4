package com.netconect.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.netconect.app.R
import com.netconect.app.util.ApiClient
import com.netconect.app.util.SessionManager
import org.json.JSONArray
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var tvWelcome: TextView
    private lateinit var tvTopTechnicians: TextView
    private lateinit var etQuickSearch: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        session = SessionManager(this)

        if (session.getToken().isNullOrBlank()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        tvWelcome = findViewById(R.id.tvWelcome)
        tvTopTechnicians = findViewById(R.id.tvTopTechnicians)
        etQuickSearch = findViewById(R.id.etQuickSearch)

        tvWelcome.text = "Olá, ${session.getUsername() ?: "usuário"}"

        findViewById<Button>(R.id.btnQuickSearch).setOnClickListener {
            val query = etQuickSearch.text.toString().trim()

            if (query.isBlank()) {
                Toast.makeText(this, "Digite serial, MAC ou modelo", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, MoveActivity::class.java)
                intent.putExtra("barcode", query)
                startActivity(intent)
            }
        }

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

        findViewById<Button>(R.id.btnGoHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        loadDashboard()
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    private fun loadDashboard() {

        thread {

            val result = ApiClient.get(
                session.getBaseUrl() + "/api/dashboard.php",
                session.getToken()
            )

            runOnUiThread {

                val body = result.body

                if (result.success && body?.optString("status") == "success") {

                    val data = body.optJSONObject("data")
                    val top = data?.optJSONArray("top_technicians")

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
            return "Top técnicos em retiradas\nNenhuma retirada registrada."
        }

        val sb = StringBuilder()

        sb.append("Top técnicos em retiradas\n\n")

        for (i in 0 until arr.length()) {

            val item = arr.getJSONObject(i)

            val name = item.optString("technician_name", "Técnico")
            val total = item.optString("total_out", "0")
            val last = item.optString("last_out_date", "-")

            sb.append("${i + 1}. $name — $total retirada(s)\n")
            sb.append("Última retirada: $last")

            if (i < arr.length() - 1) {
                sb.append("\n\n")
            }
        }

        return sb.toString()
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
