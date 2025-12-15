package au.com.gman.bottlerocket.qrCode

import android.util.Log
import au.com.gman.bottlerocket.BottleRocketApplication.AppConstants
import au.com.gman.bottlerocket.domain.BarcodeDetectionResult
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.applyRotation
import au.com.gman.bottlerocket.domain.calculateRotationAngle
import au.com.gman.bottlerocket.domain.normalize
import au.com.gman.bottlerocket.domain.scaleWithOffset
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import au.com.gman.bottlerocket.interfaces.IViewportRescaler
import com.google.mlkit.vision.barcode.common.Barcode
import javax.inject.Inject

class QrCodeHandler @Inject constructor(
    private val screenDimensions: IScreenDimensions,
    private val viewportRescaler: IViewportRescaler,
    private val qrCodeTemplateMatcher: IQrCodeTemplateMatcher
): IQrCodeHandler {
    override fun handle(barcode: Barcode?): BarcodeDetectionResult {
        var matchFound = false
        var pageBoundingBox: RocketBoundingBox? = null
        var qrBoundingBoxUnscaled: RocketBoundingBox? = null
        var qrBoundingBoxScaled: RocketBoundingBox? = null
        var qrCode: String? = null

        val pageTemplate = qrCodeTemplateMatcher.tryMatch(barcode?.rawValue ?: "")

        if (barcode != null) {
            qrCode = barcode.rawValue
            qrBoundingBoxUnscaled = RocketBoundingBox(barcode.cornerPoints)

            if (!screenDimensions.isInitialised())
                throw IllegalStateException("Screen dimensions not initialised")

            val imageSize = screenDimensions.getImageSize()
            val previewSize = screenDimensions.getPreviewSize()
            val rotationDegrees = screenDimensions.getScreenRotation()

            Log.d(
                AppConstants.APPLICATION_LOG_TAG,
                buildString {
                    appendLine("imageSize: ${imageSize?.x} x ${imageSize?.y}")
                    appendLine("previewSize: ${previewSize?.x} x ${previewSize?.y}")
                    appendLine("rotationDegrees: $rotationDegrees")
                }
            )

            // the first scale factor is the viewport vs the preview
            val scalingFactorViewport =
                viewportRescaler
                    .calculateScalingFactorWithOffset(
                        firstWidth = imageSize!!.x,
                        firstHeight = imageSize.y,
                        secondWidth = previewSize!!.x,
                        secondHeight = previewSize.y,
                        rotationAngle = rotationDegrees!!
                    )

            Log.d(
                AppConstants.APPLICATION_LOG_TAG,
                buildString {
                    appendLine("scalingFactorViewport: $scalingFactorViewport")
                }
            )

            // the second scale factor is the comparative QR code vs the actual
            // we need to 'straighten up' the QR code box
            val rotationAngle = qrBoundingBoxUnscaled.calculateRotationAngle();

            Log.d(
                AppConstants.APPLICATION_LOG_TAG,
                buildString {
                    appendLine("rotationAngle: $rotationAngle")
                }
            )

            val straightQrBox =
                qrBoundingBoxUnscaled
                    .applyRotation(
                        -rotationAngle,
                        qrBoundingBoxUnscaled.bottomLeft
                    )
                    .normalize()

            val scalingFactorQrCode =
                viewportRescaler
                    .calculateScalingFactorWithOffset(
                        firstWidth = 20.0F,
                        firstHeight = 20.0F,
                        secondWidth = straightQrBox.topRight.x - straightQrBox.topLeft.x,
                        secondHeight = straightQrBox.bottomRight.y - straightQrBox.topRight.y,
                        rotationAngle = 0
                    )

            /*Log.d(
                BottleRocketApplication.AppConstants.APPLICATION_LOG_TAG,
                buildString {
                    appendLine("scalingFactorQrCode: $scalingFactorQrCode")
                }
            )

            val scalingFactor = PointF(
                scalingFactorViewport.scale.x * 1,//,scalingFactorQrCode.x,
                scalingFactorViewport.scale.y * 1//scalingFactorQrCode.y
            )

            Log.d(
                BottleRocketApplication.AppConstants.APPLICATION_LOG_TAG,
                buildString {
                    appendLine("final scalingFactor: $scalingFactor")
                }
            )*/

            if (pageTemplate != null) {
                matchFound = true

                val qrTopLeft = qrBoundingBoxUnscaled.topLeft
                val template = pageTemplate.pageDimensions

                qrBoundingBoxScaled =
                    qrBoundingBoxUnscaled
                        .scaleWithOffset(scalingFactorViewport)

                pageBoundingBox =
                    viewportRescaler
                        .calculatePageBounds(
                            qrBoundingBoxUnscaled,
                            qrBoundingBoxScaled,
                    RocketBoundingBox(pageTemplate.pageDimensions)
                        )

                Log.d(
                    AppConstants.APPLICATION_LOG_TAG,
                    buildString {
                        appendLine("final qrBoundingBox:")
                        appendLine("$qrBoundingBoxUnscaled")
                    }
                )

                /*
                pageBoundingBox =
                    scaledBoundingBox
                        .applyRotation(
                            rotationAngle,
                            scaledBoundingBox.bottomLeft
                        )
                 */
            }
        }

        return BarcodeDetectionResult(
            matchFound = matchFound,
            qrCode = qrCode,
            pageTemplate = pageTemplate,
            pageOverlayPath = pageBoundingBox,
            qrCodeOverlayPath = qrBoundingBoxScaled
        )
    }
}