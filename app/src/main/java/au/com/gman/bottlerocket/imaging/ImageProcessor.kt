package au.com.gman.bottlerocket.imaging

import android.graphics.BitmapFactory
import android.util.Log
import au.com.gman.bottlerocket.domain.BarcodeDetectionResult
import au.com.gman.bottlerocket.interfaces.IImageEnhancer
import au.com.gman.bottlerocket.interfaces.IImageProcessingListener
import au.com.gman.bottlerocket.interfaces.IImageProcessor
import java.io.File
import javax.inject.Inject

class ImageProcessor @Inject constructor(
    private val imageEnhancer: IImageEnhancer
) : IImageProcessor {

    companion object {
        private const val TAG = "ImageProcessor"
    }

    private var listener: IImageProcessingListener? = null

    override fun setListener(listener: IImageProcessingListener) {
        this.listener = listener
    }

    override fun processImage(imageFile: File, lastBarcodeDetectionResult: BarcodeDetectionResult) {
        try {

            val originalBitmap =
                BitmapFactory
                    .decodeFile(imageFile.absolutePath)

            if (originalBitmap == null || originalBitmap.height == 0 || originalBitmap.width == 0)
                throw Exception("Bitmap is empty")

            Log.d(TAG, "Processing image: ${originalBitmap.width}x${originalBitmap.height}")

            val processedBitmap =
                imageEnhancer
                    .processImageWithMatchedTemplate(
                        originalBitmap,
                        lastBarcodeDetectionResult
                    )

            if (processedBitmap != null) {
                Log.d(TAG, "Processed: ${processedBitmap.width}x${processedBitmap.height}")

                listener?.onProcessingSuccess(processedBitmap)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Processing failed", e)
            listener?.onProcessingFailure()
        } finally {
            imageFile.delete()
        }
    }
}