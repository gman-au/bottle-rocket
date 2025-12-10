package au.com.gman.bottlerocket.network

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import au.com.gman.bottlerocket.domain.QRTemplateInfo
import au.com.gman.bottlerocket.interfaces.IImageProcessor

class ImageProcessor : IImageProcessor {

    override fun parseQRCode(qrData: String): QRTemplateInfo {
        val parts = qrData.replace(" ", "")

        val position = if (parts.startsWith("P01")) "LEFT" else "RIGHT"
        val version = parts.substringAfter("P0").take(3)
        val typeMatch = Regex("[FT]+\\d+").find(parts)
        val type = typeMatch?.value ?: "UNKNOWN"
        val sequence = parts.substringAfter("S", "000")

        return QRTemplateInfo(position, version, type, sequence)
    }

    override fun enhanceImage(bitmap: Bitmap): Bitmap {
        val enhanced = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val contrastFactor = 1.2f
        val offset = 0f

        val cm = ColorMatrix(floatArrayOf(
            contrastFactor, 0f, 0f, 0f, offset,
            0f, contrastFactor, 0f, 0f, offset,
            0f, 0f, contrastFactor, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        ))

        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(cm)

        val canvas = Canvas(enhanced)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return enhanced
    }

    override fun processImage(bitmap: Bitmap, qrData: String): Bitmap {
        val templateInfo = parseQRCode(qrData)
        // Add more processing based on template
        return enhanceImage(bitmap)
    }
}