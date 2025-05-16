package com.example.entregador.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.entregador.R
import kotlin.math.max

class ManualCalcActivity : AppCompatActivity() {

    private lateinit var etKm: EditText
    private lateinit var etManualOrigin: EditText
    private lateinit var etManualDest: EditText
    private lateinit var tvResult: TextView
    private var calculatedValue = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_calc)

        bindViews()
        setupClickListeners()
    }

    private fun bindViews() {
        etKm = findViewById(R.id.etKm)
        etManualOrigin = findViewById(R.id.etManualOrigin)
        etManualDest = findViewById(R.id.etManualDest)
        tvResult = findViewById(R.id.tvResult)
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.btnCalculateManual).setOnClickListener { calculateManual() }
        findViewById<Button>(R.id.btnWhatsAppManual).setOnClickListener { sendManualWhatsApp() }
    }

    private fun calculateManual() {
        val km = etKm.text.toString().toDoubleOrNull()
        if (km == null || km < 0) {
            showToast(R.string.error_invalid_values)
            return
        }

        val calculator = DeliveryCalculator()
        val result = calculator.calculateManualDelivery(km)

        updateResults(result)
        calculatedValue = result.total
    }

    private fun updateResults(result: CalculationResult) {
        // Usando strings.xml com placeholders para internacionalização e clareza
        val formattedResult = getString(
            R.string.manual_calc_result,
            result.oneWay,
            result.returnTrip,
            result.total
        )
        tvResult.text = formattedResult
    }

    private fun sendManualWhatsApp() {
        val message = createWhatsAppMessage()
        try {
            openWhatsApp(message)
        } catch (_: ActivityNotFoundException) {
            showToast(R.string.error_whatsapp_not_found)
        } catch (_: Exception) {
            showToast(R.string.error_generic)
        }
    }

    private fun createWhatsAppMessage(): String {
        val origem = etManualOrigin.text.toString().trim()
        val destino = etManualDest.text.toString().trim()
        val km = etKm.text.toString().trim()
        val resultado = tvResult.text.toString().trim()

        return """
            Detalhes da Entrega Manual

            Origem: $origem
            Destino: $destino
            Distância: $km km

            Valores:
            $resultado
        """.trimIndent()
    }

    private fun openWhatsApp(message: String) {
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=5513991337370&text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            throw ActivityNotFoundException("WhatsApp não instalado")
        }
    }

    private fun showToast(messageResId: Int) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show()
    }

    // Classe de cálculo com constantes organizadas e documentação
    private class DeliveryCalculator {
        companion object {
            private const val BASE_RATE = 2.0
            private const val RATE_PER_KM = 2.0
            private const val MINIMUM_VALUE = 7.0
        }

        fun calculateManualDelivery(km: Double): CalculationResult {
            val oneWay = max(MINIMUM_VALUE, BASE_RATE + (km * RATE_PER_KM))
            val returnTrip = oneWay
            val total = oneWay + returnTrip
            return CalculationResult(oneWay, returnTrip, total)
        }
    }

    // Data class para resultados de cálculo
    private data class CalculationResult(val oneWay: Double, val returnTrip: Double, val total: Double)
}