package au.com.gman.bottlerocket.imaging

import android.graphics.Path
import android.graphics.Point
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import au.com.gman.bottlerocket.domain.TemplateMatchResponse
import au.com.gman.bottlerocket.interfaces.IPageTemplateRescaler
import au.com.gman.bottlerocket.interfaces.IQrCodeDetector
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.ITemplateListener
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject
import kotlin.math.sqrt

class QrCodeDetector @Inject constructor(
    private val qrCodeTemplateMatcher: IQrCodeTemplateMatcher,
    private val pageTemplateRescaler: IPageTemplateRescaler
): IQrCodeDetector {

    private val scanner = BarcodeScanning.getClient()
    private var previewWidth: Int = 0
    private var previewHeight: Int = 0

    override fun setPreviewSize(width: Int, height: Int) {
        previewWidth = width
        previewHeight = height
    }

    private var listener: ITemplateListener? = null

    override fun setListener(listener: ITemplateListener) {
        this.listener = listener
    }

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            val imageWidth = mediaImage.width
            val imageHeight = mediaImage.height
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val barcode = barcodes.firstOrNull()
                    val qrCode = barcode?.rawValue
                    var matchFound = false
                    var pageOverlayPath: Path? = null
                    var qrOverlayPath: Path? = null

                    val pageTemplate = qrCodeTemplateMatcher.tryMatch(barcode?.rawValue ?: "")

                    if (barcode != null && barcode.boundingBox != null) {
                        val cornerPoints = toPointArray(barcode.cornerPoints)
                        qrOverlayPath = pointsToPath(cornerPoints)

                        if (pageTemplate != null) {
                            matchFound = true
                            if (qrOverlayPath != null) {
                                // Calculate page overlay path based on QR position and template
                                val rescaledPageOverlayPath = pageTemplateRescaler.rescalePageOverlay(
                                    qrCorners = cornerPoints,
                                    pageTemplateBoundingBox = pageTemplate.pageDimensions,
                                    imageWidth = imageWidth.toFloat(),
                                    imageHeight = imageHeight.toFloat(),
                                    previewWidth = previewWidth.toFloat(),
                                    previewHeight = previewHeight.toFloat()
                                )
                                pageOverlayPath = rescaledPageOverlayPath
                            }
                        }
                    }

                    val result = TemplateMatchResponse(
                        matchFound = matchFound,
                        qrCode = qrCode,
                        pageTemplate = pageTemplate,
                        pageOverlayPath = pageOverlayPath,
                        qrCodeOverlayPath = qrOverlayPath
                    )

                    listener?.onDetectionSuccess(result)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun toPointArray(points: Array<out Point>?): Array<Point> {
        return points?.toList()?.toTypedArray() ?: arrayOf()
    }

    private fun pointsToPath(qrCorners: Array<out Point?>?) : Path? {
        val path = Path()

        qrCorners?.size?.let { if (it < 4) return null }

        path.moveTo(qrCorners?.get(0)?.x?.toFloat() ?: 0F, qrCorners?.get(0)?.y?.toFloat() ?: 0F)
        path.lineTo(qrCorners?.get(1)?.x?.toFloat() ?: 0F, qrCorners?.get(1)?.y?.toFloat() ?: 0F)
        path.lineTo(qrCorners?.get(2)?.x?.toFloat() ?: 0F, qrCorners?.get(2)?.y?.toFloat() ?: 0F)
        path.lineTo(qrCorners?.get(3)?.x?.toFloat() ?: 0F, qrCorners?.get(3)?.y?.toFloat() ?: 0F)
        path.close()

        return path
    }

}