package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.BarcodeDetectionResult

interface ITemplateListener {
    fun onDetectionSuccess(matchedTemplate: BarcodeDetectionResult)
}