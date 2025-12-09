package au.com.gman.bottlerocket

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("/upload") // Change to your backend endpoint
    fun uploadImage(
        @Part image: MultipartBody.Part,
        @Part("qr_data") qrData: RequestBody,
        @Part("timestamp") timestamp: RequestBody
    ): Call<UploadResponse>
}

data class UploadResponse(
    val success: Boolean,
    val message: String,
    val imageId: String?
)