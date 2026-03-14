package com.netconect.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.netconect.app.R
import com.netconect.app.util.SessionManager

class SecondReceiptActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var etBatchId: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second_receipt)

        session = SessionManager(this)
        etBatchId = findViewById(R.id.etSecondReceiptBatchId)

        findViewById<Button>(R.id.btnOpenThermal).setOnClickListener {
            openReceipt("thermal")
        }

        findViewById<Button>(R.id.btnOpenA4).setOnClickListener {
            openReceipt("a4")
        }
    }

    private fun openReceipt(mode: String) {
        val batchId = etBatchId.text.toString().trim()

        if (batchId.isBlank()) {
            Toast.makeText(this, "Informe o ID do lote", Toast.LENGTH_SHORT).show()
            return
        }

        val url = session.getBaseUrl() +
            "/receipt_batch_lookup.php?id=$batchId&mode=$mode&token=" +
            Uri.encode(session.getToken())

        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
