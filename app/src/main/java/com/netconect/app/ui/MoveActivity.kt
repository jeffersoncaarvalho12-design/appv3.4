package com.netconect.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.netconect.app.R
import com.netconect.app.util.ApiClient
import com.netconect.app.util.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.concurrent.thread

class MoveActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private var currentItemId: Int = 0
    private val technicianIds = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_move)

        session = SessionManager(this)

        val etBarcode = findViewById<EditText>(R.id.etBarcode)
        val etLocation = findViewById<EditText>(R.id.etLocation)
        val etNote = findViewById<EditText>(R.id.etNote)
        val tvItemName = findViewById<TextView>(R.id.tvItemName)
        val tvItemStock = findViewById<TextView>(R.id.tvItemStock)
        val progress = findViewById<ProgressBar>(R.id.progressMove)

        val spinnerMovement = findViewById<Spinner>(R.id.spinnerMovement)
        val spinnerTechnician = findViewById<Spinner>(R.id.spinnerTechnicianMove)
        val spinnerReason = findViewById<Spinner>(R.id.spinnerReasonMove)

        val movementOptions = listOf(
            "Saída",
            "Entrada em estoque",
            "Estoque com defeito",
            "Devolvido"
        )

        val reasonOptions = listOf(
            "Sem motivo",
            "Técnico não usou",
            "Troca de aparelho",
            "Contrato cancelado",
            "Recolhimento por débitos",
            "Com defeito"
        )

        spinnerMovement.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            movementOptions
        )

        spinnerReason.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            reasonOptions
        )

        loadTechnicians(spinnerTechnician, progress)

        val barcode = intent.getStringExtra("barcode").orEmpty()
        if (barcode.isNotBlank()) {
            etBarcode.setText(barcode)
            searchItem(barcode, tvItemName, tvItemStock, progress)
        }

        findViewById<Button>(R.id.btnSearchItem).setOnClickListener {
            val term = etBarcode.text.toString().trim()
            if (term.isBlank()) {
                Toast.makeText(this, "Informe serial, MAC ou modelo", Toast.LENGTH_SHORT).show()
            } else {
                searchItem(term, tvItemName, tvItemStock, progress)
            }
        }

        findViewById<Button>(R.id.btnScanMove).setOnClickListener {
            val integrator = IntentIntegrator(this)
            integrator.setPrompt("Escaneie serial ou MAC")
            integrator.setBeepEnabled(true)
            integrator.setOrientationLocked(false)
            integrator.initiateScan()
        }

        findViewById<Button>(R.id.btnSendMove).setOnClickListener {
            if (currentItemId <= 0) {
                Toast.makeText(this, "Busque um item primeiro", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val movementLabel = spinnerMovement.selectedItem?.toString().orEmpty()
            val reasonLabel = spinnerReason.selectedItem?.toString().orEmpty()
            val location = etLocation.text.toString().trim()
            val noteText = etNote.text.toString().trim()

            val movement = resolveMovement(movementLabel, reasonLabel)

            val technicianId =
                if (spinnerTechnician.selectedItemPosition in technicianIds.indices) {
                    technicianIds[spinnerTechnician.selectedItemPosition]
                } else {
                    0
                }

            if ((movement == "OUT" || movement == "DEFECT") && technicianId <= 0) {
                Toast.makeText(this, "Selecione o técnico", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val finalNote = buildFinalNote(reasonLabel, noteText)

            sendMovement(
                itemId = currentItemId,
                movement = movement,
                technicianId = technicianId,
                note = finalNote,
                location = location,
                progress = progress,
                tvItemStock = tvItemStock
            )
        }
    }

    private fun loadTechnicians(spinner: Spinner, progress: ProgressBar) {
        progress.visibility = android.view.View.VISIBLE

        thread {
            val result = ApiClient.get(
                session.getBaseUrl() + "/api/technicians.php",
                session.getToken()
            )

            runOnUiThread {
                progress.visibility = android.view.View.GONE
                val body = result.body

                if (result.success && body?.optString("status") == "success") {
                    val arr = body.optJSONArray("data") ?: JSONArray()
                    val names = mutableListOf<String>()

                    technicianIds.clear()
                    technicianIds.add(0)
                    names.add("Selecione o técnico")

                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        technicianIds.add(obj.optInt("id"))
                        names.add(obj.optString("name"))
                    }

                    spinner.adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        names
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

    private fun resolveMovement(movementLabel: String, reasonLabel: String): String {
        if (reasonLabel == "Com defeito") {
            return "DEFECT"
        }

        return when (movementLabel) {
            "Entrada em estoque" -> "IN_STOCK"
            "Estoque com defeito" -> "DEFECT"
            "Devolvido" -> "RETURNED"
            else -> "OUT"
        }
    }

    private fun buildFinalNote(reasonLabel: String, noteText: String): String {
        val reason = if (reasonLabel == "Sem motivo") "" else reasonLabel

        return when {
            reason.isNotBlank() && noteText.isNotBlank() -> "$reason - $noteText"
            reason.isNotBlank() -> reason
            else -> noteText
        }
    }

    private fun searchItem(
        term: String,
        tvItemName: TextView,
        tvItemStock: TextView,
        progress: ProgressBar
    ) {
        progress.visibility = android.view.View.VISIBLE

        thread {
            val result = ApiClient.get(
                session.getBaseUrl() + "/api/search.php?q=" + URLEncoder.encode(term, "UTF-8"),
                session.getToken()
            )

            runOnUiThread {
                progress.visibility = android.view.View.GONE
                val body = result.body

                if (result.success && body?.optString("status") == "success") {
                    val data = body.opt("data")
                    val item = when (data) {
                        is JSONObject -> data
                        is JSONArray -> if (data.length() > 0) data.optJSONObject(0) else null
                        else -> null
                    }

                    currentItemId = item?.optInt("id", 0) ?: 0

                    val productName = item?.optString("product_name", "Item") ?: "Item"
                    val serial = item?.optString("serial_number", "-") ?: "-"
                    val macRaw = item?.optString("mac_address", "") ?: ""
                    val mac = if (macRaw.isBlank() || macRaw == "null") "—" else macRaw
                    val statusRaw = item?.optString("status", "-") ?: "-"
                    val locationRaw = item?.optString("location", "") ?: ""
                    val location = if (locationRaw.isBlank() || locationRaw == "null") "—" else locationRaw

                    tvItemName.text = """
                        $productName
                        Serial: $serial
                        MAC: $mac
                    """.trimIndent()

                    tvItemStock.text = """
                        Status atual: ${translateStatus(statusRaw)}
                        Local: $location
                    """.trimIndent()
                } else {
                    currentItemId = 0
                    tvItemName.text = "Item não encontrado"
                    tvItemStock.text = "Status atual: —"

                    Toast.makeText(
                        this,
                        body?.optString("message", result.message) ?: result.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun translateStatus(status: String): String {
        return when (status.uppercase()) {
            "IN_STOCK" -> "Em estoque"
            "OUT" -> "Saída"
            "DEFECT" -> "Com defeito"
            "RETURNED" -> "Devolvido"
            else -> status
        }
    }

    private fun sendMovement(
        itemId: Int,
        movement: String,
        technicianId: Int,
        note: String,
        location: String,
        progress: ProgressBar,
        tvItemStock: TextView
    ) {
        progress.visibility = android.view.View.VISIBLE

        thread {
            val payload = JSONObject().apply {
                put("item_id", itemId)
                put("movement", movement)
                put("technician_id", technicianId)
                put("note", note)
                put("location", location)
                put("photo_base64", "")
            }

            val result = ApiClient.post(
                session.getBaseUrl() + "/api/move.php",
                payload,
                session.getToken()
            )

            runOnUiThread {
                progress.visibility = android.view.View.GONE
                val body = result.body

                if (result.success && body?.optString("status") == "success") {
                    val data = body.optJSONObject("data")
                    val newStatus = data?.optString("new_status", movement) ?: movement
                    val locationRaw = data?.optString("location", "") ?: ""
                    val finalLocation =
                        if (locationRaw.isBlank() || locationRaw == "null") "—" else locationRaw

                    tvItemStock.text = """
                        Status atual: ${translateStatus(newStatus)}
                        Local: $finalLocation
                    """.trimIndent()

                    Toast.makeText(
                        this,
                        body.optString("message", "Movimentação registrada"),
                        Toast.LENGTH_LONG
                    ).show()
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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result != null && result.contents != null) {
            val etBarcode = findViewById<EditText>(R.id.etBarcode)
            val tvItemName = findViewById<TextView>(R.id.tvItemName)
            val tvItemStock = findViewById<TextView>(R.id.tvItemStock)
            val progress = findViewById<ProgressBar>(R.id.progressMove)

            etBarcode.setText(result.contents)
            searchItem(result.contents, tvItemName, tvItemStock, progress)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
