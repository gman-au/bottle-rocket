package au.com.gman.bottlerocket.interfaces
import au.com.gman.bottlerocket.domain.UploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface IApiService {
    @Multipart
    @POST("/upload")
    fun uploadImage(
        @Part image: MultipartBody.Part,
        @Part("qr_data") qrData: RequestBody
    ): Call<UploadResponse>
}