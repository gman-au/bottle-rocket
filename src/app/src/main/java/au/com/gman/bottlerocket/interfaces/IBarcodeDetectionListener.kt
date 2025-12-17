package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.BarcodeDetectionResult

interface IBarcodeDetectionListener {
    fun onDetectionSuccess(matchedTemplate: BarcodeDetectionResult)
}