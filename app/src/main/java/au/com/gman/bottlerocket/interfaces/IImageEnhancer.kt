package au.com.gman.bottlerocket.interfaces

import android.graphics.Bitmap

interface IImageEnhancer {
    fun enhanceImage(bitmap: Bitmap): Bitmap
}