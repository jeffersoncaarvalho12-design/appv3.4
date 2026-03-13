package com.netconect.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.netconect.app.R
import com.netconect.app.util.ApiClient
import com.netconect.app.util.SessionManager
import org.json.JSONObject
import kotlin.concurrent.thread

class LoginActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var etUser: EditText
    private lateinit var etPass: EditText
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        session = SessionManager(this)

        if (!session.getToken().isNullOrBlank()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        etUser = findViewById(R.id.etUsername)
        etPass = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val username = etUser.text.toString().trim()
            val password = etPass.text.toString().trim()

            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Informe usuário e senha", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            doLogin(username, password)
        }
    }

    private fun doLogin(username: String, password: String) {
        thread {
            val payload = JSONObject().apply {
                put("username", username)
                put("password", password)
            }

            val result = ApiClient.post(
                session.getBaseUrl() + "/api/login.php",
                payload,
                null
            )

            runOnUiThread {
                val body = result.body

                if (result.success && body?.optString("status") == "success") {
                    val token = body.optString("token", "")
                    val user = body.optJSONObject("user")
                    val userName = user?.optString("username", username) ?: username

                    session.saveLogin(token, userName)

                    Toast.makeText(this, "Login realizado com sucesso", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
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
