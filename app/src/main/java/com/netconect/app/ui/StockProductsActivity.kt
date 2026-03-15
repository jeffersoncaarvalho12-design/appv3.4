package com.netconect.app.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.core.content.ContextCompat
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
    private lateinit var adapter: ProductStockAdapter

    private val items = mutableListOf<JSONObject>()
    private var selectedProductId: Int = 0

    companion object {
        private const val REQUEST_TAKE_PHOTO = 3001
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_products)

        session = SessionManager(this)
        progress = findViewById(R.id.progressStock)
        listStock = findViewById(R.id.listStock)
        etSearchStock = findViewById(R.id.etSearchStock)

        adapter = ProductStockAdapter()
        listStock.adapter = adapter

        findViewById<Button>(R.id.btnSearchStock).setOnClickListener {
            loadStock(etSearchStock.text.toString().trim())
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

    private fun openCameraForProduct(productId: Int) {
        selectedProductId = productId

        val permissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
            return
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_TAKE_PHOTO)
        } else {
            Toast.makeText(
                this,
                "Câmera não disponível no aparelho",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun uploadProductPhotoBase64(base64DataUrl: String) {
        progress.visibility = View.VISIBLE

        thread {
            try {
                val payload = JSONObject().apply {
                    put("product_id", selectedProductId)
                    put("photo_base64", base64DataUrl)
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

    private fun bitmapToDataUrl(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        val bytes = baos.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
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
            val btnCardPhoto = view.findViewById<Button>(R.id.btnCardPhoto)

            val item = items[position]

            val productId = item.optInt("id", 0)
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

            btnCardPhoto.setOnClickListener {
                openCameraForProduct(productId)
            }

            return view
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (selectedProductId > 0) {
                    openCameraForProduct(selectedProductId)
                }
            } else {
                Toast.makeText(
                    this,
                    "Permissão da câmera negada",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            val bitmap = data?.extras?.get("data") as? Bitmap

            if (bitmap != null) {
                val dataUrl = bitmapToDataUrl(bitmap)
                uploadProductPhotoBase64(dataUrl)
            } else {
                Toast.makeText(this, "Erro ao capturar foto", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
