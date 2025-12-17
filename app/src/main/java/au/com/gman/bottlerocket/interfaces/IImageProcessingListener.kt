package au.com.gman.bottlerocket.interfaces

import android.graphics.Bitmap

interface IImageProcessingListener {
    fun onProcessingSuccess(processedBitmap: Bitmap)

    fun onProcessingFailure()
}