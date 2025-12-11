package au.com.gman.bottlerocket.interfaces

import android.graphics.Bitmap
import android.graphics.Rect
import au.com.gman.bottlerocket.domain.QRTemplateInfo

interface IImageProcessor {
    fun parseQRCode(qrData: String): QRTemplateInfo
    fun enhanceImage(bitmap: Bitmap): Bitmap
    fun processImage(bitmap: Bitmap, qrData: String): Bitmap

    fun processImageWithQR(
        bitmap: Bitmap,
        qrData: String,
        qrBoundingBox: Rect?
    ): Bitmap
}