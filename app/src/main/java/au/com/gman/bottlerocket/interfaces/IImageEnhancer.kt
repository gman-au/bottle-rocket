package au.com.gman.bottlerocket.interfaces

import android.graphics.Bitmap
import au.com.gman.bottlerocket.domain.BarcodeDetectionResult

interface IImageEnhancer {
    fun enhanceImage(bitmap: Bitmap): Bitmap

    fun processImageWithMatchedTemplate(
        bitmap: Bitmap,
        detectionResult: BarcodeDetectionResult
    ): Bitmap?
}