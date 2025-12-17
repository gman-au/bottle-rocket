package au.com.gman.bottlerocket.imaging

import android.graphics.BitmapFactory
import android.util.Log
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

    override fun processImage(imageFile: File) {
        try {

            val originalBitmap =
                BitmapFactory
                    .decodeFile(imageFile.absolutePath)

            Log.d(TAG, "Processing image: ${originalBitmap.width}x${originalBitmap.height}")

            // Process with QR bounding box for smart cropping
            val processedBitmap =
                imageEnhancer
                    .enhanceImage(originalBitmap)
            /*imageProcessor.processImageWithQR(
            originalBitmap,
            qrData,
            qrBoundingBox
        )*/

            Log.d(TAG, "Processed: ${processedBitmap.width}x${processedBitmap.height}")

            listener?.onProcessingSuccess(processedBitmap)

        } catch (e: Exception) {
            Log.e(TAG, "Processing failed", e)
            listener?.onProcessingFailure()
        } finally {
            imageFile.delete()
        }
    }



}