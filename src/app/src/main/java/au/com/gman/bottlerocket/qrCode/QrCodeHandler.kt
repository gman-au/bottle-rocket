package au.com.gman.bottlerocket.qrCode

import android.graphics.PointF
import android.util.Log
import au.com.gman.bottlerocket.BottleRocketApplication
import au.com.gman.bottlerocket.domain.BarcodeDetectionResult
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.applyRotation
import au.com.gman.bottlerocket.domain.calculateRotationAngle
import au.com.gman.bottlerocket.domain.normalize
import au.com.gman.bottlerocket.domain.scale
import au.com.gman.bottlerocket.interfaces.IBoundingBoxRescaler
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import com.google.mlkit.vision.barcode.common.Barcode
import javax.inject.Inject

class QrCodeHandler @Inject constructor(
    private val screenDimensions: IScreenDimensions,
    private val pageTemplateRescaler: IBoundingBoxRescaler,
    private val qrCodeTemplateMatcher: IQrCodeTemplateMatcher
): IQrCodeHandler {
    override fun handle(barcode: Barcode?): BarcodeDetectionResult {
        var matchFound = false
        var pageBoundingBox: RocketBoundingBox? = null
        var qrBoundingBox: RocketBoundingBox? = null
        var qrCode: String? = null

        val pageTemplate = qrCodeTemplateMatcher.tryMatch(barcode?.rawValue ?: "")

        if (barcode != null) {
            qrCode = barcode.rawValue
            qrBoundingBox = RocketBoundingBox(barcode.cornerPoints)

            if (!screenDimensions.isInitialised())
                throw IllegalStateException("Screen dimensions not initialised")

            val imageSize = screenDimensions.getImageSize()
            val previewSize = screenDimensions.getPreviewSize()
            val rotationDegrees = screenDimensions.getScreenRotation()

            Log.d(
                BottleRocketApplication.AppConstants.APPLICATION_LOG_TAG,
                buildString {
                    appendLine("imageSize: ${imageSize?.x} x ${imageSize?.y}")
                    appendLine("previewSize: ${previewSize?.x} x ${previewSize?.y}")
                    appendLine("rotationDegrees: $rotationDegrees")
                }
            )

            // the first scale factor is the viewport vs the preview
            val scalingFactorViewport =
                pageTemplateRescaler
                    .calculateScalingFactor(
                        firstWidth = imageSize!!.x,
                        firstHeight = imageSize.y,
                        secondWidth = previewSize!!.x,
                        secondHeight = previewSize.y,
                        rotationAngle = rotationDegrees!!
                    )

            Log.d(
                BottleRocketApplication.AppConstants.APPLICATION_LOG_TAG,
                buildString {
                    appendLine("scalingFactorViewport: $scalingFactorViewport")
                }
            )

            // the second scale factor is the comparative QR code vs the actual
            // we need to 'straighten up' the QR code box
            val rotationAngle = qrBoundingBox.calculateRotationAngle();

            Log.d(
                BottleRocketApplication.AppConstants.APPLICATION_LOG_TAG,
                buildString {
                    appendLine("rotationAngle: $rotationAngle")
                }
            )

            val straightQrBox =
                qrBoundingBox
                    .applyRotation(
                        -rotationAngle,
                        qrBoundingBox.bottomLeft
                    )
                    .normalize()

            val scalingFactorQrCode =
                pageTemplateRescaler
                    .calculateScalingFactor(
                        firstWidth = 20.0F,
                        firstHeight = 20.0F,
                        secondWidth = straightQrBox.topRight.x - straightQrBox.topLeft.x,
                        secondHeight = straightQrBox.bottomRight.y - straightQrBox.topRight.y,
                        rotationAngle = 0
                    )

            Log.d(
                BottleRocketApplication.AppConstants.APPLICATION_LOG_TAG,
                buildString {
                    appendLine("scalingFactorQrCode: $scalingFactorQrCode")
                }
            )

            val scalingFactor = PointF(
                scalingFactorViewport.x * 1,//,scalingFactorQrCode.x,
                scalingFactorViewport.y * 1//scalingFactorQrCode.y
            )

            Log.d(
                BottleRocketApplication.AppConstants.APPLICATION_LOG_TAG,
                buildString {
                    appendLine("final scalingFactor: $scalingFactor")
                }
            )

            if (pageTemplate != null) {
                matchFound = true

                val qrTopLeft = qrBoundingBox.topLeft
                val template = pageTemplate.pageDimensions

                // the page template is offset from the location of the QR code
                val pageTemplateBoundingBox = RocketBoundingBox(
                    topLeft = PointF(template.topLeft.x + qrTopLeft.x,template.topLeft.y + qrTopLeft.y),
                    topRight = PointF(template.topRight.x + qrTopLeft.x, template.topRight.y + qrTopLeft.y),
                    bottomRight = PointF(template.bottomRight.x + qrTopLeft.x, template.bottomRight.y + qrTopLeft.y),
                    bottomLeft = PointF(template.bottomLeft.x + qrTopLeft.x, template.bottomLeft.y + qrTopLeft.y)
                )

                val scaledBoundingBox = pageTemplateBoundingBox.scale(scalingFactor.x, scalingFactor.y)

                // scaling factor must be inaccurate
                qrBoundingBox =
                    qrBoundingBox
                        .scale(
                            scalingFactor.x,
                            scalingFactor.y
                        )

                Log.d(
                    BottleRocketApplication.AppConstants.APPLICATION_LOG_TAG,
                    buildString {
                        appendLine("final qrBoundingBox:")
                        appendLine("$qrBoundingBox")
                    }
                )

                pageBoundingBox =
                    scaledBoundingBox
                        .applyRotation(
                            rotationAngle,
                            scaledBoundingBox.bottomLeft
                        )
            }
        }

        return BarcodeDetectionResult(
            matchFound = matchFound,
            qrCode = qrCode,
            pageTemplate = pageTemplate,
            pageOverlayPath = null, // pageBoundingBox,
            qrCodeOverlayPath = qrBoundingBox
        )
    }
}