package au.com.gman.bottlerocket.interfaces

import android.graphics.PointF
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.ScaleAndOffset

interface IViewportRescaler {
    fun rescaleUsingQrCorners(
        qrCorners: RocketBoundingBox,
        sourceBoundingBox: RocketBoundingBox,
        scalingFactor: PointF
    ): RocketBoundingBox

     fun calculateScalingFactorWithOffset(
        firstWidth: Float,
        firstHeight: Float,
        secondWidth: Float,
        secondHeight: Float,
        rotationAngle: Int
    ): ScaleAndOffset
}