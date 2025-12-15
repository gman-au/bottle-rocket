package au.com.gman.bottlerocket.qrCode

import android.util.Log
import au.com.gman.bottlerocket.domain.BarcodeDetectionResult
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.calculateRotationAngle
import au.com.gman.bottlerocket.domain.round
import au.com.gman.bottlerocket.domain.scaleWithOffset
import au.com.gman.bottlerocket.imaging.BoundingBoxStabilizer
import au.com.gman.bottlerocket.imaging.PageTemplateRescaler
import au.com.gman.bottlerocket.interfaces.IBoundingBoxValidator
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import au.com.gman.bottlerocket.interfaces.IViewportRescaler
import com.google.mlkit.vision.barcode.common.Barcode
import javax.inject.Inject

class QrCodeHandler @Inject constructor(
    private val screenDimensions: IScreenDimensions,
    private val viewportRescaler: IViewportRescaler,
    private val pageTemplateRescaler: PageTemplateRescaler,
    private val qrCodeTemplateMatcher: IQrCodeTemplateMatcher,
    private val boundingBoxValidator: IBoundingBoxValidator
) : IQrCodeHandler {

    companion object {
        private const val TAG = "QrCodeHandler"
    }

    private val qrStabilizer = BoundingBoxStabilizer(0.15f, 3)
    private val pageStabilizer = BoundingBoxStabilizer(0.15f, 3)

    override fun handle(barcode: Barcode?): BarcodeDetectionResult {
        var matchFound = false
        var pageBoundingBox: RocketBoundingBox? = null
        var qrBoundingBoxUnscaled: RocketBoundingBox? = null
        var qrBoundingBoxScaled: RocketBoundingBox? = null
        var qrCode: String? = null
        var validationMessage: String? = null

        val pageTemplate =
            qrCodeTemplateMatcher
                .tryMatch(barcode?.rawValue ?: "")

        if (barcode != null) {
            qrCode = barcode.rawValue
            val rawQrBounds = RocketBoundingBox(barcode.cornerPoints)

            qrBoundingBoxUnscaled =
                qrStabilizer
                    .stabilize(rawQrBounds)

            if (!screenDimensions.isInitialised())
                throw IllegalStateException("Screen dimensions not initialised")

            val imageSize = screenDimensions.getImageSize()
            val previewSize = screenDimensions.getPreviewSize()
            val rotationDegrees = screenDimensions.getScreenRotation()

            Log.d(
                TAG,
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
                TAG,
                buildString {
                    appendLine("scalingFactorViewport: $scalingFactorViewport")
                }
            )

            val rotationAngle =
                qrBoundingBoxUnscaled
                    .calculateRotationAngle();

            Log.d(
                TAG,
                buildString {
                    appendLine("rotationAngle: $rotationAngle")
                }
            )

            if (pageTemplate != null) {

                qrBoundingBoxScaled =
                    qrBoundingBoxUnscaled
                        .scaleWithOffset(scalingFactorViewport)

                val rawPageBounds =
                    pageTemplateRescaler
                        .calculatePageBounds(
                            qrBoundingBoxUnscaled,
                            qrBoundingBoxScaled,
                            RocketBoundingBox(pageTemplate.pageDimensions)
                        )

                // Stabilize page bounds
                val stabilizedPageBounds =
                    pageStabilizer
                        .stabilize(rawPageBounds)

                matchFound = qrStabilizer.isStable() && pageStabilizer.isStable()

                // Validate the page bounds
                val isValid = boundingBoxValidator.isValid(stabilizedPageBounds)
                val isStable = qrStabilizer.isStable() && pageStabilizer.isStable()

                if (isValid && isStable) {
                    // Only show page overlay when valid
                    pageBoundingBox = stabilizedPageBounds
                    matchFound = true
                } else {
                    // Show validation feedback
                    val issues = boundingBoxValidator.getValidationIssues(stabilizedPageBounds)
                    validationMessage = issues.firstOrNull() ?: "Align camera with page"
                }

                Log.d(
                    TAG,
                    buildString {
                        appendLine("final qrBoundingBox:")
                        appendLine("$qrBoundingBoxUnscaled")
                    }
                )
            } else {
                pageStabilizer.reset()
            }
        } else {
            qrStabilizer.reset()
            pageStabilizer.reset()
        }

        return BarcodeDetectionResult(
            matchFound = matchFound,
            qrCode = qrCode,
            pageTemplate = pageTemplate,
            pageOverlayPath = pageBoundingBox?.round(),
            qrCodeOverlayPath = qrBoundingBoxScaled?.round(),
            validationMessage = validationMessage
        )
    }
}