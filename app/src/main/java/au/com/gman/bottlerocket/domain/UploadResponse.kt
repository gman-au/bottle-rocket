package au.com.gman.bottlerocket.domain

data class UploadResponse(
    val success: Boolean,
    val message: String,
    val imageId: String?
)