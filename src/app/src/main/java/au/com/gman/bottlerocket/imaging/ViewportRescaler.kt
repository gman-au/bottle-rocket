package au.com.gman.bottlerocket.imaging

import android.graphics.Matrix
import android.graphics.PointF
import android.util.Log
import au.com.gman.bottlerocket.BottleRocketApplication
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.ScaleAndOffset
import au.com.gman.bottlerocket.interfaces.IViewportRescaler
import au.com.gman.bottlerocket.BottleRocketApplication.AppConstants
import javax.inject.Inject

class ViewportRescaler @Inject constructor(): IViewportRescaler {

    override fun calculateScalingFactorWithOffset(
        firstWidth: Float,
        firstHeight: Float,
        secondWidth: Float,
        secondHeight: Float,
        rotationAngle: Int
    ): ScaleAndOffset {

        // Account for rotation - swap dimensions if rotated 90° or 270°
        val (actualFirstW, actualFirstH) = if (rotationAngle % 180 != 0) {
            Pair(firstHeight, firstWidth)
        } else {
            Pair(firstWidth, firstHeight)
        }

        // Calculate aspect ratios
        val firstAspect = actualFirstW / actualFirstH
        val secondAspect = secondWidth / secondHeight

        val scale: PointF
        val offset: PointF

        if (firstAspect > secondAspect) {
            // First is wider - horizontal crop (left/right sides cut off)
            // Scale based on height
            val uniformScale = secondHeight / actualFirstH
            scale = PointF(uniformScale, uniformScale)

            // Calculate how much width is cropped
            val scaledWidth = actualFirstW * uniformScale
            val cropAmount = (scaledWidth - secondWidth) / 2f

            // The crop happens in the FIRST coordinate space, then scaled
            val cropInFirstSpace = cropAmount / uniformScale
            offset = PointF(-cropInFirstSpace * uniformScale, 0f)

        } else {
            // First is taller - vertical crop (top/bottom cut off)
            // Scale based on width
            val uniformScale = secondWidth / actualFirstW
            scale = PointF(uniformScale, uniformScale)

            // Calculate how much height is cropped
            val scaledHeight = actualFirstH * uniformScale
            val cropAmount = (scaledHeight - secondHeight) / 2f

            val cropInFirstSpace = cropAmount / uniformScale
            offset = PointF(0f, -cropInFirstSpace * uniformScale)
        }

        Log.d(AppConstants.APPLICATION_LOG_TAG, "First: ${actualFirstW}x${actualFirstH} (aspect: ${firstAspect})")
        Log.d(AppConstants.APPLICATION_LOG_TAG, "Second: ${secondWidth}x${secondHeight} (aspect: ${secondAspect})")
        Log.d(AppConstants.APPLICATION_LOG_TAG, "Scale: ${scale.x}, ${scale.y}")
        Log.d(AppConstants.APPLICATION_LOG_TAG, "Offset: ${offset.x}, ${offset.y}")

        return ScaleAndOffset(scale, offset)
    }

    override fun rescaleUsingQrCorners(
        qrCorners: RocketBoundingBox,
        sourceBoundingBox: RocketBoundingBox,
        scalingFactor: PointF
    ): RocketBoundingBox {

        // Step 1: Ideal QR square
        val idealQrSquare = floatArrayOf(
            0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f
        )

        // Step 2: Actual QR in image space
        val actualQrInImageSpace = floatArrayOf(
            qrCorners.topLeft.x, qrCorners.topLeft.y,
            qrCorners.topRight.x, qrCorners.topRight.y,
            qrCorners.bottomRight.x, qrCorners.bottomRight.y,
            qrCorners.bottomLeft.x, qrCorners.bottomLeft.y
        )

        // Step 3: QR dimensions
        val qrWidth = (qrCorners.topRight.x - qrCorners.topLeft.x +
                qrCorners.bottomRight.x - qrCorners.bottomLeft.x) / 2f
        val qrHeight = (qrCorners.bottomLeft.y - qrCorners.topLeft.y +
                qrCorners.bottomRight.y - qrCorners.topRight.y) / 2f

        Log.d("IBoundingBoxRescaler", "QR size: ${qrWidth}x${qrHeight}")

        // Step 4: Page template in QR units
        val pageInQrUnits = floatArrayOf(
            sourceBoundingBox.topLeft.x / qrWidth,
            sourceBoundingBox.topLeft.y / qrHeight,
            sourceBoundingBox.topRight.x / qrWidth,
            sourceBoundingBox.topRight.y / qrHeight,
            sourceBoundingBox.bottomRight.x / qrWidth,
            sourceBoundingBox.bottomRight.y / qrHeight,
            sourceBoundingBox.bottomLeft.x / qrWidth,
            sourceBoundingBox.bottomLeft.y / qrHeight
        )

        // Step 5: Apply perspective transform
        val matrix = Matrix()
        matrix.setPolyToPoly(idealQrSquare, 0, actualQrInImageSpace, 0, 4)

        val pageInImageSpace = FloatArray(8)
        matrix.mapPoints(pageInImageSpace, pageInQrUnits)

        Log.d("IBoundingBoxRescaler", "Page (image space):")
        for (i in pageInImageSpace.indices step 2) {
            Log.d("IBoundingBoxRescaler", "  [${i/2+1}]: (${pageInImageSpace[i]}, ${pageInImageSpace[i+1]})")
        }

        // Step 6: Scale AND rotate to preview space (90° rotation)
        val pageInPreviewSpace = FloatArray(8)
        for (i in pageInImageSpace.indices step 2) {
            val x = pageInImageSpace[i]
            val y = pageInImageSpace[i+1]

            // Rotate 90° clockwise: (x,y) → (imageHeight - y, x)
            // Then scale
            pageInPreviewSpace[i] = x * scalingFactor.x
            pageInPreviewSpace[i+1] = y * scalingFactor.y
        }

        Log.d("IBoundingBoxRescaler", "Page (preview space - FINAL):")
        for (i in pageInPreviewSpace.indices step 2) {
            Log.d("IBoundingBoxRescaler", "  [${i/2+1}]: (${pageInPreviewSpace[i]}, ${pageInPreviewSpace[i+1]})")
        }

        return RocketBoundingBox(pageInPreviewSpace)
    }
}