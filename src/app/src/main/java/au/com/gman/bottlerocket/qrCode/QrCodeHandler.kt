package au.com.gman.bottlerocket.qrCode

import android.util.Log
import au.com.gman.bottlerocket.domain.BarcodeDetectionResult
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.calculateRotationAngle
import au.com.gman.bottlerocket.domain.round
import au.com.gman.bottlerocket.domain.scaleWithOffset
import au.com.gman.bottlerocket.imaging.BoundingBoxStabilizer
import au.com.gman.bottlerocket.imaging.PageTemplateRescaler
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import com.google.mlkit.vision.barcode.common.Barcode
import javax.inject.Inject

class QrCodeHandler @Inject constructor(
    private val screenDimensions: IScreenDimensions,
    private val pageTemplateRescaler: PageTemplateRescaler,
    private val qrCodeTemplateMatcher: IQrCodeTemplateMatcher,
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

            val qrCornerPoints = RocketBoundingBox(barcode.cornerPoints)

            qrBoundingBoxUnscaled = qrCornerPoints

            if (!screenDimensions.isInitialised())
                throw IllegalStateException("Screen dimensions not initialised")

            screenDimensions
                .recalculateScalingFactorIfRequired()

            val scalingFactorViewport =
                screenDimensions
                    .getScalingFactor()

            val rotationAngle =
                qrBoundingBoxUnscaled
                    .calculateRotationAngle()

            if (pageTemplate != null && scalingFactorViewport != null) {

                qrBoundingBoxScaled =
                    qrBoundingBoxUnscaled
                        .scaleWithOffset(scalingFactorViewport)

                val rawPageBounds =
                    pageTemplateRescaler
                        .calculatePageBounds(
                            qrBoundingBoxUnscaled,
                            RocketBoundingBox(pageTemplate.pageDimensions),
                            rotationAngle
                        )

                val scaledPageBounds =
                    rawPageBounds
                        .scaleWithOffset(scalingFactorViewport)

                pageBoundingBox = scaledPageBounds
                matchFound = true

                Log.d(
                    TAG,
                    buildString {
                        appendLine("final qrBoundingBox:")
                        appendLine("$qrBoundingBoxUnscaled")
                    }
                )

                Log.d(
                    TAG,
                    buildString {
                        appendLine("final pageBoundingBox:")
                        appendLine("$pageBoundingBox")
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