package au.com.gman.bottlerocket.interfaces

import androidx.camera.core.ImageAnalysis

interface IBarcodeDetector : ImageAnalysis.Analyzer {
    fun setListener(listener: ITemplateListener)
}