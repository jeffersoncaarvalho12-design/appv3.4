package com.netconect.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
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

    private val movementLabels = listOf(
        "Entrada em estoque",
        "Saída",
        "Estoque com defeito",
        "Devolvido"
    )

    private val reasonLabels = listOf(
