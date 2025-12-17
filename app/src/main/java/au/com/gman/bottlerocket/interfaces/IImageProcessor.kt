package au.com.gman.bottlerocket.interfaces

import androidx.camera.core.ImageCapture
import java.io.File

interface IImageProcessor {
    fun setListener(listener: IImageProcessingListener)

    fun processImage(imageFile: File)
}