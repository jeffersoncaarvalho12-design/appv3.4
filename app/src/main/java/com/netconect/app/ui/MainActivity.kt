package com.netconect.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.zxing.integration.android.IntentIntegrator
import com.netconect.app.R
import com.netconect.app.util.ApiClient
import com.netconect.app.util.SessionManager
import org.json.JSONArray
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var swipeDashboard: SwipeRefreshLayout
    private lateinit var tvWelcome: TextView
    private lateinit var tvUserInitials: TextView
    private lateinit var imgUserPhoto: ImageView
    private lateinit var etQuickSearch: EditText
    private lateinit var tvTopTechnicians: TextView
    private lateinit var listLatestBatches: ListView

    private val latestBatchIds = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        session = SessionManager(this)

        swipeDashboard = findViewById(R.id.swipeDashboard)
        tvWelcome = findViewById(R.id.tvWelcome)
        tvUserInitials = findViewById(R.id.tvUserInitials)
        imgUserPhoto = findViewById(R.id.imgUserPhoto)
        etQuickSearch = findViewById(R.id.etQuickSearch)
        tvTopTechnicians = findViewById(R.id.tvTopTechnicians)
        listLatestBatches = findViewById(R.id.listLatestBatches)

        val username = session.getUsername().orEmpty().ifBlank { "usuário" }
        tvWelcome.text = "Olá, $username"
        tvUserInitials.text = username.take(2).uppercase()

        findViewById<Button>(R.id.btnQuickSearch).setOnClickListener {
            openQuickSearch()
        }

        findViewById<Button>(R.id.btnQuickScan).setOnClickListener {
            startScanner()
        }

        findViewById<Button>(R.id.btnGoScanner).setOnClickListener {
            startScanner()
        }

        findViewById<View>(R.id.fabScanner).setOnClickListener {
            startScanner()
        }

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

        findViewById<Button>(R.id.btnGoSecondReceipt).setOnClickListener {
            startActivity(Intent(this, SecondReceiptActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoFinalizedBatches).setOnClickListener {
            startActivity(Intent(this, FinalizedBatchesActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        listLatestBatches.setOnItemClickListener { _, _, position, _ ->
            if (position < 0 || position >= latestBatchIds.size) return@setOnItemClickListener
            val batchId = latestBatchIds[position]

            val url = session.getBaseUrl() +
                "/receipt_batch_lookup.php?id=$batchId&mode=thermal&token=" +
                Uri.encode(session.getToken())

            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        listLatestBatches.setOnItemLongClickListener { _, _, position, _ ->
            if (position < 0 || position >= latestBatchIds.size) return@setOnItemLongClickListener true
            val batchId = latestBatchIds[position]

            val url = session.getBaseUrl() +
                "/receipt_batch_lookup.php?id=$batchId&mode=a4&token=" +
                Uri.encode(session.getToken())

            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            true
        }

        swipeDashboard.setOnRefreshListener {
            loadDashboard()
            loadLatestBatches()
        }

        loadDashboard()
        loadLatestBatches()
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
        loadLatestBatches()
    }

    private fun openQuickSearch() {
        val query = etQuickSearch.text.toString().trim()

        if (query.isBlank()) {
            Toast.makeText(this, "Digite modelo, nome, serial ou MAC", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, MoveActivity::class.java)
        intent.putExtra("barcode", query)
        startActivity(intent)
    }

    private fun loadDashboard() {
        thread {
            val result = ApiClient.get(
                session.getBaseUrl() + "/api/dashboard.php",
                session.getToken()
            )

            runOnUiThread {
                swipeDashboard.isRefreshing = false
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

    private fun loadLatestBatches() {
        thread {
            val result = ApiClient.get(
                session.getBaseUrl() + "/api/latest_batches.php?limit=5",
                session.getToken()
            )

            runOnUiThread {
                swipeDashboard.isRefreshing = false
                val body = result.body

                if (result.success && body?.optString("status") == "success") {
                    val arr = body.optJSONArray("data") ?: JSONArray()
                    val lines = mutableListOf<String>()
                    latestBatchIds.clear()

                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)

                        val batchId = item.optInt("id")
                        val tech = item.optString("technician_name", "-")
                        val os = item.optString("os_number", "-")
                        val date = item.optString("created_at_br", "-")
                        val total = item.optInt("total_items", 0)

                        latestBatchIds.add(batchId)

                        val line = """
                            Lote #$batchId
                            Técnico: $tech
                            OS: $os
                            Data: $date
                            Itens: $total
                        """.trimIndent()

                        lines.add(line)
                    }

                    if (lines.isEmpty()) {
                        lines.add("Nenhum lote entregue ainda.")
                    }

                    listLatestBatches.adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_list_item_1,
                        lines
                    )
                } else {
                    listLatestBatches.adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_list_item_1,
                        listOf("Não foi possível carregar os últimos lotes.")
                    )
                }
            }
        }
    }

    private fun buildTopTechText(arr: JSONArray?): String {
        if (arr == null || arr.length() == 0) {
            return "🏆 Top técnicos em retiradas\n\nNenhuma retirada registrada."
        }

        val sb = StringBuilder()
        sb.append("🏆 Top técnicos em retiradas\n\n")

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
            etQuickSearch.setText(result.contents)
            etQuickSearch.requestFocus()
            Toast.makeText(this, "Código lido e preenchido na busca rápida", Toast.LENGTH_SHORT).show()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
