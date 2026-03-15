package com.netconect.app.ui

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.netconect.app.R
import com.netconect.app.util.ApiClient
import com.netconect.app.util.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class StockProductsActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var progress: ProgressBar
    private lateinit var listStock: ListView
    private lateinit var etSearchStock: EditText
    private lateinit var btnAddPhotoProduct: Button
    private lateinit var adapter: ProductStockAdapter

    private val items = mutableListOf<JSONObject>()
    private var selectedProductId: Int = 0
    private var selectedPosition: Int = -1

    companion object {
        private const val REQUEST_PICK_IMAGE = 3001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_products)

        session = SessionManager(this)
        progress = findViewById(R.id.progressStock)
        listStock = findViewById(R.id.listStock)
        etSearchStock = findViewById(R.id.etSearchStock)
        btnAddPhotoProduct = findViewById(R.id.btnAddPhotoProduct)

        adapter = ProductStockAdapter()
        listStock.adapter = adapter

        findViewById<Button>(R.id.btnSearchStock).setOnClickListener {
            loadStock(etSearchStock.text.toString().trim())
        }

        listStock.setOnItemClickListener { _, _, position, _ ->
            val item = items[position]
            selectedProductId = item.optInt("id", 0)
            selectedPosition = position
            adapter.notifyDataSetChanged()

            val label = cleanText(item.optString("label", ""), "Produto")
            Toast.makeText(
                this,
                "Selecionado: $label",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnAddPhotoProduct.setOnClickListener {
            if (selectedProductId <= 0) {
                Toast.makeText(
                    this,
                    "Toque primeiro em um produto da lista",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            pickImage()
        }

        loadStock("")
    }

    private fun loadStock(query: String) {
        progress.visibility = View.VISIBLE

        thread {
            val url = if (query.isBlank()) {
                session.getBaseUrl() + "/api/stock_products.php"
            } else {
                session.getBaseUrl() + "/api/stock_products.php?q=" + URLEncoder.encode(query, "UTF-8")
            }

            val result = ApiClient.get(url, session.getToken())

            runOnUiThread {
                progress.visibility = View.GONE
                val body = result.body

                if (result.success && body?.optString("status") == "success") {
                    val arr = body.optJSONArray("data") ?: JSONArray()
                    items.clear()

                    for (i in 0 until arr.length()) {
                        items.add(arr.getJSONObject(i))
                    }

                    selectedProductId = 0
                    selectedPosition = -1
                    adapter.notifyDataSetChanged()
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

    private fun cleanText(value: String?, fallback: String = "—"): String {
        val text = value?.trim().orEmpty()
        return if (text.isBlank() || text.equals("null", ignoreCase = true)) fallback else text
    }

    private fun translateCategory(raw: String): String {
        return when (raw.uppercase()) {
            "ONU" -> "ONU"
            "ROUTER" -> "Roteador"
            "OTHER" -> "Outros"
            else -> raw
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    private fun uploadProductPhoto(imageUri: Uri) {
        progress.visibility = View.VISIBLE

        thread {
            try {
                val inputStream = contentResolver.openInputStream(imageUri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes == null || bytes.isEmpty()) {
                    runOnUiThread {
                        progress.visibility = View.GONE
                        Toast.makeText(this, "Não foi possível ler a imagem", Toast.LENGTH_LONG).show()
                    }
                    return@thread
                }

                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap == null) {
                    runOnUiThread {
                        progress.visibility = View.GONE
                        Toast.makeText(this, "Imagem inválida", Toast.LENGTH_LONG).show()
                    }
                    return@thread
                }

                val baos = ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, baos)
                val compressed = baos.toByteArray()

                val base64 = Base64.encodeToString(compressed, Base64.NO_WRAP)
                val dataUrl = "data:image/jpeg;base64,$base64"

                val payload = JSONObject().apply {
                    put("product_id", selectedProductId)
                    put("photo_base64", dataUrl)
                }

                val result = ApiClient.post(
                    session.getBaseUrl() + "/api/upload_product_photo.php",
                    payload,
                    session.getToken()
                )

                runOnUiThread {
                    progress.visibility = View.GONE
                    val body = result.body

                    if (result.success && body?.optString("status") == "success") {
                        Toast.makeText(
                            this,
                            body.optString("message", "Foto enviada com sucesso"),
                            Toast.LENGTH_LONG
                        ).show()
                        loadStock(etSearchStock.text.toString().trim())
                    } else {
                        Toast.makeText(
                            this,
                            body?.optString("message", result.message) ?: result.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    progress.visibility = View.GONE
                    Toast.makeText(
                        this,
                        "Erro ao enviar foto: ${e.message ?: "falha desconhecida"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    inner class ProductStockAdapter : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@StockProductsActivity)
                .inflate(R.layout.item_stock_card, parent, false)

            val img = view.findViewById<ImageView>(R.id.imgCardProduct)
            val tvName = view.findViewById<TextView>(R.id.tvCardProductName)
            val tvQty = view.findViewById<TextView>(R.id.tvCardQty)
            val tvCategory = view.findViewById<TextView>(R.id.tvCardCategory)
            val tvRef = view.findViewById<TextView>(R.id.tvCardRef)

            val item = items[position]

            val label = cleanText(item.optString("label", ""), "Produto")
            val qtyInStock = item.optInt("qty_in_stock", 0)
            var category = cleanText(item.optString("category", ""), "Sem categoria")
            val refCode = cleanText(item.optString("ref_code", ""), "")
            val imagePath = cleanText(item.optString("image_path", ""), "")

            category = translateCategory(category)

            tvName.text = label
            tvQty.text = "Disponível: $qtyInStock"
            tvCategory.text = category
            tvRef.text = if (refCode.isBlank()) "" else "Ref: $refCode"

            if (qtyInStock == 0) {
                tvQty.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            } else {
                tvQty.setTextColor(resources.getColor(android.R.color.black))
            }

            if (position == selectedPosition) {
                view.alpha = 0.82f
            } else {
                view.alpha = 1.0f
            }

            img.setImageResource(android.R.drawable.sym_def_app_icon)

            if (imagePath.isNotBlank()) {
                val fullUrl = if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                    imagePath
                } else {
                    session.getBaseUrl() + if (imagePath.startsWith('/')) imagePath else "/$imagePath"
                }

                thread {
                    try {
                        val conn = URL(fullUrl).openConnection() as HttpURLConnection
                        conn.connectTimeout = 10000
                        conn.readTimeout = 10000
                        conn.doInput = true
                        conn.connect()

                        val bmp = BitmapFactory.decodeStream(conn.inputStream)
                        if (bmp != null) {
                            runOnUiThread {
                                img.setImageBitmap(bmp)
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            return view
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data
            if (imageUri != null) {
                uploadProductPhoto(imageUri)
            } else {
                Toast.makeText(this, "Nenhuma imagem selecionada", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
