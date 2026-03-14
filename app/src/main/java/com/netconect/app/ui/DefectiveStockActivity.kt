package com.netconect.app.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
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
import kotlin.concurrent.thread

class DefectiveStockActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var progress: ProgressBar
    private lateinit var listDefects: ListView
    private lateinit var adapter: DefectAdapter
    private val items = mutableListOf<JSONObject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_defective_stock)

        session = SessionManager(this)
        progress = findViewById(R.id.progressDefects)
        listDefects = findViewById(R.id.listDefects)

        adapter = DefectAdapter()
        listDefects.adapter = adapter

        loadDefects()
    }

    private fun loadDefects() {
        progress.visibility = View.VISIBLE

        thread {
            val result = ApiClient.get(
                session.getBaseUrl() + "/api/defective_stock.php",
                session.getToken()
            )

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

    inner class DefectAdapter : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@DefectiveStockActivity)
                .inflate(R.layout.item_defect, parent, false)

            val img = view.findViewById<ImageView>(R.id.imgDefectProduct)
            val tvName = view.findViewById<TextView>(R.id.tvDefectName)
            val tvSerial = view.findViewById<TextView>(R.id.tvDefectSerial)
            val btnReturn = view.findViewById<Button>(R.id.btnReturnStock)

            val item = items[position]

            val itemId = item.optInt("id")
            val name = item.optString("product_name", "Produto")
            val serial = item.optString("serial_number", "—")
            val photoPath = item.optString("photo_path", "")

            tvName.text = name
            tvSerial.text = "Serial: $serial"

            img.setImageResource(android.R.drawable.ic_menu_report_image)

            if (photoPath.isNotBlank()) {
                val fullUrl = if (photoPath.startsWith("http://") || photoPath.startsWith("https://")) {
                    photoPath
                } else {
                    session.getBaseUrl() + if (photoPath.startsWith('/')) photoPath else "/$photoPath"
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

            btnReturn.setOnClickListener {
                returnToStock(itemId)
            }

            return view
        }
    }

    private fun returnToStock(itemId: Int) {
        thread {
            val payload = JSONObject().apply {
                put("item_id", itemId)
            }

            val result = ApiClient.post(
                session.getBaseUrl() + "/api/return_defect.php",
                payload,
                session.getToken()
            )

            runOnUiThread {
                val body = result.body

                if (result.success && body?.optString("status") == "success") {
                    Toast.makeText(
                        this,
                        body.optString("message", "Item devolvido ao estoque"),
                        Toast.LENGTH_SHORT
                    ).show()
                    loadDefects()
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
