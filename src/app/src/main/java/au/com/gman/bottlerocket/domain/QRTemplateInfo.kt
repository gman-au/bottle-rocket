package au.com.gman.bottlerocket.domain

import android.graphics.Rect

data class QRTemplateInfo(
    val position: String,  // "LEFT" or "RIGHT"
    val version: String,   // "V1", "V17", etc.
    val type: String,      // "FT02", "T01", etc.
    val sequence: String,   // "S000"
    val boundingBox: Rect
)