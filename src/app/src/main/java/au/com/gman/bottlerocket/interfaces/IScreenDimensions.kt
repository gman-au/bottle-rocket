package au.com.gman.bottlerocket.interfaces

import android.graphics.PointF

interface IScreenDimensions {
    fun setImageSize(size: PointF?)
    fun setPreviewSize(size: PointF?)
    fun setScreenRotation(angle: Int?)

    fun getImageSize(): PointF?
    fun getPreviewSize(): PointF?
    fun getScreenRotation(): Int?

    fun isInitialised(): Boolean
}