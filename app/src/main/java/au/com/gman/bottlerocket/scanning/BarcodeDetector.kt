package au.com.gman.bottlerocket.scanning

import android.graphics.PointF
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import au.com.gman.bottlerocket.interfaces.IBarcodeDetector
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import au.com.gman.bottlerocket.interfaces.IBarcodeDetectionListener
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

class BarcodeDetector @Inject constructor(
    private val qrCodeHandler: IQrCodeHandler,
    private val screenDimensions: IScreenDimensions
) : IBarcodeDetector {

    companion object {
        private const val TAG = "BarcodeDetector"
    }

    private val scannerOptions:
        BarcodeScannerOptions =
            BarcodeScannerOptions
                .Builder()
                /*.setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_AZTEC
                )*/
                .build()

    private val scanner =
        BarcodeScanning
            .getClient(scannerOptions)

    private var listener: IBarcodeDetectionListener? = null

    override fun setListener(listener: IBarcodeDetectionListener) {
        this.listener = listener
    }

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage =
            imageProxy
                .image

        if (mediaImage != null) {
            val image =
                InputImage
                    .fromMediaImage(
                        mediaImage,
                        imageProxy
                            .imageInfo
                            .rotationDegrees
                    )

            Log.d(TAG, "InputImage dimensions: ${image.width}x${image.height}")  // This will likely be 1200x1600!

            val imageWidth =
                mediaImage
                    .width

            val imageHeight =
                mediaImage
                    .height

            val rotationDegrees =
                imageProxy
                    .imageInfo
                    .rotationDegrees

            Log.d(TAG, "ImageAnalysis actual resolution: ${imageProxy.width}x${imageProxy.height}")

            Log.d(TAG, "ImageProxy dimensions: ${imageProxy.width}x${imageProxy.height}")
            Log.d(TAG, "MediaImage dimensions: ${mediaImage.width}x${mediaImage.height}")
            Log.d(TAG, "Rotation degrees: $rotationDegrees")

            screenDimensions
                .setSourceSize(
                    PointF(
                        imageWidth.toFloat(),
                        imageHeight.toFloat()
                    )
                )

            screenDimensions
                .setScreenRotation(rotationDegrees)

            scanner
                .process(image)
                .addOnSuccessListener { barcodes ->
                    val barcode =
                        barcodes
                            .firstOrNull()

                    val barcodeDetectionResult =
                        qrCodeHandler
                            .handle(barcode)

                    listener?.onDetectionSuccess(barcodeDetectionResult)
                }
                .addOnCompleteListener {
                    imageProxy
                        .close()
                }
        } else {
            imageProxy
                .close()
        }
    }
}