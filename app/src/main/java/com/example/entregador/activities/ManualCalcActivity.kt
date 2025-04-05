package com.example.entregador.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.entregador.R
import java.util.Locale
import androidx.core.net.toUri  // Adicione este import
import kotlin.math.max

class ManualCalcActivity : AppCompatActivity() {
    private var etKm: EditText? = null
    private var etManualOrigin: EditText? = null
    private var etManualDest: EditText? = null
    private var tvResult: TextView? = null
    private var calculatedValue = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_calc)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        etKm = findViewById(R.id.etKm)
        etManualOrigin = findViewById(R.id.etManualOrigin)
        etManualDest = findViewById(R.id.etManualDest)
        tvResult = findViewById(R.id.tvResult)

        // Remova as declarações não utilizadas ou utilize-as
        findViewById<Button>(R.id.btnCalculateManual).setOnClickListener {
            calculateManual()
        }

        findViewById<Button>(R.id.btnWhatsAppManual).setOnClickListener {
            sendManualWhatsApp()
        }
    }

    private fun setupClickListeners() {
        findViewById<View?>(R.id.btnCalculateManual).setOnClickListener(View.OnClickListener { v: View? -> calculateManual() })
        findViewById<View?>(R.id.btnWhatsAppManual).setOnClickListener(View.OnClickListener { v: View? -> sendManualWhatsApp() })
    }

    private fun calculateManual() {
        try {
            val km = etKm!!.text.toString().toDouble()
            val calculator = DeliveryCalculator()
            val result = calculator.calculateManualDelivery(km)

            updateResults(result)
            calculatedValue = result.total
        } catch (_: NumberFormatException) {  // Usamos _ para indicar parâmetro não usado
            showToast(R.string.error_invalid_values)
        }
    }

    private fun updateResults(result: CalculationResult) {
        val formattedResult = String.format(
            Locale.getDefault(),
            "Ida: R$ %.2f\nRetorno: R$ %.2f\nTotal: R$ %.2f",
            result.oneWay,
            result.returnTrip,
            result.total
        )

        tvResult?.text = formattedResult  // Usando property access syntax
    }

    private fun sendManualWhatsApp() {
        try {
            val message = createWhatsAppMessage()
            openWhatsApp(message)
        } catch (_: ActivityNotFoundException) {
            showToast(R.string.error_whatsapp_not_found)
        } catch (_: Exception) {
            showToast(R.string.error_generic)
        }
    }

    private fun createWhatsAppMessage(): String {
        val message = StringBuilder("*Cálculo Manual*\n\n")

        appendIfNotEmpty(message, "Origem: ", etManualOrigin?.text?.toString() ?: "")
        appendIfNotEmpty(message, "Destino: ", etManualDest?.text?.toString() ?: "")

        message.append("Distância: ").append(etKm?.text ?: "").append(" km\n")
        message.append("Resultado:\n").append(tvResult?.text ?: "")

        return message.toString()
    }

    private fun appendIfNotEmpty(sb: StringBuilder, prefix: String?, value: String?) {
        if (!TextUtils.isEmpty(value)) {
            sb.append(prefix).append(value).append("\n")
        }
    }

    @Throws(ActivityNotFoundException::class)
    private fun openWhatsApp(message: String?) {
        Intent(Intent.ACTION_VIEW).apply {
            data = "https://api.whatsapp.com/send?phone=5513991337370&text=${Uri.encode(message)}".toUri()
        }.takeIf { it.resolveActivity(packageManager) != null }?.let {
            startActivity(it)
        } ?: throw ActivityNotFoundException()
    }

    private fun showToast(messageResId: Int) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show()
    }

    // Classe de cálculo separada para melhor organização
    private class DeliveryCalculator {
        fun calculateManualDelivery(km: Double): CalculationResult {
            val oneWay = max(MINIMUM_VALUE, BASE_RATE + (km * RATE_PER_KM))
            val returnTrip = oneWay
            val total = oneWay + returnTrip

            return CalculationResult(oneWay, returnTrip, total)
        }

        companion object {
            private const val BASE_RATE = 2.0
            private const val RATE_PER_KM = 2.0
            private const val MINIMUM_VALUE = 7.0
        }
    }

    // Classe para encapsular os resultados
    private class CalculationResult(val oneWay: Double, val returnTrip: Double, val total: Double)
}