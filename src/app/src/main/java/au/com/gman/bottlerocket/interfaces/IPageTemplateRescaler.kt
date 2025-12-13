package au.com.gman.bottlerocket.interfaces

import android.graphics.Path
import android.graphics.Point
import android.graphics.RectF

interface IPageTemplateRescaler {
    fun rescalePageOverlay(
        qrCorners: Array<Point>,
        pageTemplateBoundingBox: RectF,
        imageWidth: Float,
        imageHeight: Float,
        previewWidth: Float,
        previewHeight: Float
    ): Path
}