package au.com.gman.bottlerocket.qrCode

import android.util.Log
import au.com.gman.bottlerocket.domain.BarcodeDetectionResult
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.extensions.aggressiveSmooth
import au.com.gman.bottlerocket.extensions.calculateRotationAngle
import au.com.gman.bottlerocket.extensions.round
import au.com.gman.bottlerocket.extensions.scaleWithOffset
import au.com.gman.bottlerocket.interfaces.IPageTemplateRescaler
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.IRocketBoundingBoxMedianFilter
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import com.google.mlkit.vision.barcode.common.Barcode
import javax.inject.Inject

class QrCodeHandler @Inject constructor(
    private val screenDimensions: IScreenDimensions,
    private val pageTemplateRescaler: IPageTemplateRescaler,
    private val qrCodeTemplateMatcher: IQrCodeTemplateMatcher,
    private val rocketBoundingBoxMedianFilter: IRocketBoundingBoxMedianFilter
) : IQrCodeHandler {

    companion object {
        private const val TAG = "QrCodeHandler"
    }

    private var previousPageBounds: RocketBoundingBox? = null

    override fun handle(barcode: Barcode?): BarcodeDetectionResult {
        var matchFound = false
        var pageBoundingBox: RocketBoundingBox? = null
        var qrCornerPointsBoxUnscaled: RocketBoundingBox? = null
        var qrCornerPointsBoxScaled: RocketBoundingBox? = null
        var qrCodeValue: String? = null
        var validationMessage: String? = null

        val pageTemplate =
            qrCodeTemplateMatcher
                .tryMatch(barcode?.rawValue ?: "")

        if (barcode != null) {
            qrCodeValue = barcode.rawValue

            val qrCornerPoints = RocketBoundingBox(barcode.cornerPoints)

            // Use raw corner points - don't stabilize the input
            // The homography amplifies tiny movements, so we stabilize AFTER
            qrCornerPointsBoxUnscaled = qrCornerPoints

            if (!screenDimensions.isInitialised())
                throw IllegalStateException("Screen dimensions not initialised")

            screenDimensions
                .recalculateScalingFactorIfRequired()

            val scalingFactorViewport =
                screenDimensions
                    .getScalingFactor()

            val rotationAngle =
                qrCornerPointsBoxUnscaled
                    .calculateRotationAngle()

            if (pageTemplate != null && scalingFactorViewport != null) {

                qrCornerPointsBoxScaled =
                    qrCornerPointsBoxUnscaled
                        .scaleWithOffset(scalingFactorViewport)

                Log.d(
                    TAG,
                    buildString {
                        appendLine("qrBoundingBoxUnscaled:")
                        appendLine("$qrCornerPointsBoxUnscaled")
                    }
                )

                Log.d(
                    TAG,
                    buildString {
                        appendLine("qrBoundingBoxScaled:")
                        appendLine("$qrCornerPointsBoxScaled")
                    }
                )

                val rawPageBounds =
                    pageTemplateRescaler
                        .calculatePageBoundsFromTemplate(
                            qrCornerPointsBoxUnscaled,
                            RocketBoundingBox(pageTemplate.pageDimensions)
                        )


                val scaledPageBounds =
                    rawPageBounds
                        .scaleWithOffset(scalingFactorViewport)

                pageBoundingBox =
                    scaledPageBounds
                        .aggressiveSmooth(
                            previous = previousPageBounds,
                            smoothFactor = 0.3f,        // 90% previous, 10% current
                            maxJumpThreshold = 50f      // Completely reject frames with ANY corner jumping >50px
                        )

                pageBoundingBox =
                    rocketBoundingBoxMedianFilter
                        .add(pageBoundingBox)

                previousPageBounds = pageBoundingBox

                matchFound = true

                Log.d(
                    TAG,
                    buildString {
                        appendLine("final pageBoundingBox:")
                        appendLine("$pageBoundingBox")
                    }
                )
            } else {
                // Reset when we lose tracking
                previousPageBounds = null
                rocketBoundingBoxMedianFilter.reset()
            }
        } else {
            // Reset when no barcode detected
            previousPageBounds = null
            rocketBoundingBoxMedianFilter.reset()
        }

        return BarcodeDetectionResult(
            matchFound = matchFound,
            qrCode = qrCodeValue,
            pageTemplate = pageTemplate,
            pageOverlayPath = pageBoundingBox?.round(),
            qrCodeOverlayPath = qrCornerPointsBoxScaled?.round(),
            validationMessage = validationMessage
        )
    }
}