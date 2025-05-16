package com.example.entregador.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.withRotation
import com.example.entregador.R

class CircularProgressView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paintCircle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 20f
        strokeCap = Paint.Cap.ROUND
    }

    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 64f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private var progress = 0

    fun setProgress(value: Int) {
        progress = value.coerceIn(0, 100)
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (width.coerceAtMost(height) / 2f) - 40f
        val rect = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

        // Degradê verde do início ao fim do círculo
        val gradient = SweepGradient(
            centerX, centerY,
            intArrayOf(
                ContextCompat.getColor(context, R.color.greenDark),
                ContextCompat.getColor(context, R.color.greenPrimary)
            ),
            floatArrayOf(0f, 1f)
        )
        paintCircle.shader = gradient

        // Desenha o arco do progresso
        canvas.withRotation(-90f, centerX, centerY) {
            drawArc(rect, 0f, (progress * 360 / 100f), false, paintCircle)
        }

        // Texto no centro
        canvas.drawText("$progress%", centerX, centerY + (paintText.textSize / 3), paintText)
    }
}