package com.netconect.app.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.netconect.app.R
import com.netconect.app.util.ApiClient
import com.netconect.app.util.SessionManager
import org.json.JSONObject
import kotlin.concurrent.thread

class AddProductActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        session = SessionManager(this)
        progress = findViewById(R.id.progressAddProduct)

        val spinnerCategory = findViewById<Spinner>(R.id.spinnerAddCategory)
        spinnerCategory.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("ONU", "ROUTER", "OTHER")
        )

        findViewById<Button>(R.id.btnSaveProduct).setOnClickListener {
            saveProduct()
        }
    }

    private fun saveProduct() {
        val etRef = findViewById<EditText>(R.id.etAddRef)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerAddCategory)
        val etBrand = findViewById<EditText>(R.id.etAddBrand)
        val etModel = findViewById<EditText>(R.id.etAddModel)
        val etBarcode = findViewById<EditText>(R.id.etAddBarcode)
        val etMinQty = findViewById<EditText>(R.id.etAddMinQty)
        val etNotes = findViewById<EditText>(R.id.etAddNotes)

        val ref = etRef.text.toString().trim()
        val category = spinnerCategory.selectedItem?.toString()?.trim().orEmpty()
        val brand = etBrand.text.toString().trim()
        val model = etModel.text.toString().trim()
        val barcode = etBarcode.text.toString().trim()
        val minQty = etMinQty.text.toString().trim().toIntOrNull() ?: 0
        val notes = etNotes.text.toString().trim()

        if (model.isBlank()) {
            Toast.makeText(this, "Informe o modelo", Toast.LENGTH_SHORT).show()
            return
        }

        progress.visibility = View.VISIBLE

        thread {
            val payload = JSONObject().apply {
                put("ref_code", ref)
                put("category", category)
                put("brand", brand)
                put("model", model)
                put("barcode", barcode)
                put("min_qty", minQty)
                put("notes", notes)
            }

            val result = ApiClient.post(
                session.getBaseUrl() + "/api/add_product.php",
                payload,
                session.getToken()
            )

            runOnUiThread {
                progress.visibility = View.GONE
                val body = result.body

                if (result.success && body?.optString("status") == "success") {
                    Toast.makeText(
                        this,
                        body.optString("message", "Modelo cadastrado com sucesso"),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
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
