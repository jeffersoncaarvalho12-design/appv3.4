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
import kotlin.concurrent.thread
import java.net.URLEncoder

class FinalizedBatchesActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var etSearchBatch: EditText
    private lateinit var listBatches: ListView

    private val batchIds = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finalized_batches)

        session = SessionManager(this)
        etSearchBatch = findViewById(R.id.etSearchBatch)
        listBatches = findViewById(R.id.listFinalizedBatches)

        findViewById<Button>(R.id.btnSearchBatches).setOnClickListener {
            loadBatches(etSearchBatch.text.toString().trim())
        }

        listBatches.setOnItemClickListener { _, _, position, _ ->
            if (position < 0 || position >= batchIds.size) return@setOnItemClickListener
            val batchId = batchIds[position]

            val url = session.getBaseUrl() +
                "/receipt_batch_lookup.php?id=$batchId&mode=thermal&token=" +
                Uri.encode(session.getToken())

            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        listBatches.setOnItemLongClickListener { _, _, position, _ ->
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
                "/api/finalized_batches.php?q=" + URLEncoder.encode(query, "UTF-8") +
                "&limit=30"

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
                        val tech = item.optString("technician_name", "-")
                        val os = item.optString("os_number", "-")
                        val date = item.optString("created_at_br", "-")
                        val total = item.optInt("total_items", 0)

                        batchIds.add(batchId)

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
                        lines.add("Nenhum lote finalizado encontrado.")
                    }

                    listBatches.adapter = ArrayAdapter(
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
                }
            }
        }
    }
}
