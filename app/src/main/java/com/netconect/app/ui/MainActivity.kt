package com.netconect.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.netconect.app.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Scanner
        findViewById<Button>(R.id.btnGoScanner).setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
        }

        // Entrada
        findViewById<Button>(R.id.btnGoEntry).setOnClickListener {
            startActivity(Intent(this, EntryActivity::class.java))
        }

        // Saída
        findViewById<Button>(R.id.btnGoExit).setOnClickListener {
            startActivity(Intent(this, ExitActivity::class.java))
        }

        // Lote
        findViewById<Button>(R.id.btnGoBatch).setOnClickListener {
            startActivity(Intent(this, BatchMoveActivity::class.java))
        }

        // Ver estoque
        findViewById<Button>(R.id.btnGoStock).setOnClickListener {
            startActivity(Intent(this, StockActivity::class.java))
        }

        // Histórico
        findViewById<Button>(R.id.btnGoHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // Configurações
        findViewById<Button>(R.id.btnGoSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // ⭐ NOVO BOTÃO - SEGUNDA VIA DE RECIBO
        findViewById<Button>(R.id.btnGoSecondReceipt).setOnClickListener {
            startActivity(Intent(this, SecondReceiptActivity::class.java))
        }
    }
}
