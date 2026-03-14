package com.netconect.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.netconect.app.R
import com.netconect.app.util.ApiClient
import com.netconect.app.util.SessionManager
import org.json.JSONArray
import java.net.URLEncoder
import kotlin.concurrent.thread

class FinalizedBatchesActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var etSearchBatch: EditText
    private lateinit var listFinalizedBatches: ListView

    private val batchIds = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finalized_batches)

        session = SessionManager(this)
        etSearchBatch = findViewById(R.id.etSearchBatch)
        listFinalizedBatches = findViewById(R.id.listFinalizedBatches)

        findViewById<Button>(R.id.btnSearchBatches).setOnClickListener {
            loadBatches(etSearchBatch.text.toString().trim())
        }

        listFinalizedBatches.setOnItemClickListener { _, _, position, _ ->
            if (position < 0 || position >= batchIds.size) return@setOnItemClickListener
            val batchId = batchIds[position]

            val url = session.getBaseUrl() +
                "/receipt_batch_lookup.php?id=$batchId&mode=thermal&token=" +
                Uri.encode(session.getToken())

            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        listFinalizedBatches.setOnItemLongClickListener { _, _, position, _ ->
            if (position < 0 || position >= batchIds.size) return@setOnItemLongClickListener true
            val batchId = batchIds[position]

            val url = session.getBaseUrl() +
                "/receipt_batch_lookup.php?id=$batchId&mode=a4&token=" +
                Uri.encode(session.getToken())

            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            true
        }

        loadBatches("")
    }

    private fun loadBatches(query: String) {
        thread {
            val url = session.getBaseUrl() +
                "/api/batches_recent.php?q=" + URLEncoder.encode(query, "UTF-8")

            val result = ApiClient.get(url, session.getToken())

            runOnUiThread {
                val body = result.body

                if (result.success && body?.optString("status") == "success") {
                    val arr = body.optJSONArray("data") ?: JSONArray()
                    val lines = mutableListOf<String>()
                    batchIds.clear()

                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)

                        val batchId = item.optInt("id")
                        val tech = item.optString("technician", "-")
                        val osRaw = item.optString("os_number", "")
                        val os = if (osRaw.isBlank() || osRaw == "null") "-" else osRaw
                        val date = item.optString("created_at", "-")

                        batchIds.add(batchId)

                        val line = """
                            Lote #$batchId
                            Técnico: $tech
                            OS: $os
                            Data: $date
                        """.trimIndent()

                        lines.add(line)
                    }

                    if (lines.isEmpty()) {
                        lines.add("Nenhum lote finalizado encontrado.")
                    }

                    listFinalizedBatches.adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_list_item_1,
                        lines
                    )
                } else {
                    Toast.makeText(
                        this,
                        body?.optString("message", result.message) ?: result.message,
                        Toast.LENGTH_LONG
                    ).show()

                    listFinalizedBatches.adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_list_item_1,
                        listOf("Não foi possível carregar os lotes finalizados.")
                    )
                }
            }
        }
    }
}
