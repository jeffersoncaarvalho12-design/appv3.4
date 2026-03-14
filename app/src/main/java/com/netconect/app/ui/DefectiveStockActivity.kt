package com.netconect.app.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.netconect.app.R
import com.netconect.app.util.ApiClient
import com.netconect.app.util.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

class DefectiveStockActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var listDefects: ListView
    private lateinit var progress: ProgressBar

    private val items = mutableListOf<JSONObject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_defective_stock)

        session = SessionManager(this)

        listDefects = findViewById(R.id.listDefects)
        progress = findViewById(R.id.progressDefects)

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

                    listDefects.adapter = object : BaseAdapter() {

                        override fun getCount() = items.size
                        override fun getItem(position: Int) = items[position]
                        override fun getItemId(position: Int) = position.toLong()

                        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

                            val view = layoutInflater.inflate(R.layout.item_defect, parent, false)

                            val tvName = view.findViewById<TextView>(R.id.tvDefectName)
                            val tvSerial = view.findViewById<TextView>(R.id.tvDefectSerial)
                            val btnReturn = view.findViewById<Button>(R.id.btnReturnStock)

                            val item = items[position]

                            tvName.text = item.optString("product_name")
                            tvSerial.text = "Serial: " + item.optString("serial_number")

                            btnReturn.setOnClickListener {
                                returnToStock(item.optInt("id"))
                            }

                            return view
                        }
                    }

                } else {

                    Toast.makeText(
                        this,
                        "Erro ao carregar defeitos",
                        Toast.LENGTH_LONG
                    ).show()
                }

            }

        }
    }

    private fun returnToStock(itemId: Int) {

        thread {

            val payload = JSONObject()
            payload.put("item_id", itemId)

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
                        "Item devolvido ao estoque",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadDefects()

                } else {

                    Toast.makeText(
                        this,
                        "Erro ao devolver item",
                        Toast.LENGTH_LONG
                    ).show()

                }

            }

        }

    }

}
