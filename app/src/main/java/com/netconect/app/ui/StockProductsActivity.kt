package com.netconect.app.ui

import android.graphics.BitmapFactory
import android.os.Bundle
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
            val category = cleanText(item.optString("category", ""), "Sem categoria")
            val refCode = cleanText(item.optString("ref_code", ""), "")
            val imagePath = cleanText(item.optString("image_path", ""), "")

            tvName.text = label
            tvQty.text = "Disponível: $qtyInStock"
            tvCategory.text = category
            tvRef.text = if (refCode.isBlank()) "" else "Ref: $refCode"

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
}
