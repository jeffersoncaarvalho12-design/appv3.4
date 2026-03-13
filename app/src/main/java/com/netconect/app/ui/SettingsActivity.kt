package com.netconect.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.netconect.app.R
import com.netconect.app.util.SessionManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var etBaseUrl: EditText
    private lateinit var tvCredits: TextView
    private lateinit var btnSaveServer: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        session = SessionManager(this)

        etBaseUrl = findViewById(R.id.etBaseUrl)
        tvCredits = findViewById(R.id.tvCredits)
        btnSaveServer = findViewById(R.id.btnSaveServer)
        btnLogout = findViewById(R.id.btnLogout)

        etBaseUrl.setText(session.getBaseUrl())

        tvCredits.text = """
            Desenvolvido por Jefferson Carvalho

            Provérbios 15:33
            "O temor do Senhor ensina a sabedoria,
            e a humildade antecede a honra."
        """.trimIndent()

        btnSaveServer.setOnClickListener {
            val baseUrl = etBaseUrl.text.toString().trim()

            if (baseUrl.isBlank()) {
                Toast.makeText(this, "Informe a URL do servidor", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            session.saveBaseUrl(baseUrl)
            Toast.makeText(this, "Servidor salvo com sucesso", Toast.LENGTH_SHORT).show()
        }

        btnLogout.setOnClickListener {
            session.clearSession()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
