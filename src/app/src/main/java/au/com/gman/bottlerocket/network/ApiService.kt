package au.com.gman.bottlerocket.network

import android.net.Uri
import au.com.gman.bottlerocket.domain.UploadResponse
import au.com.gman.bottlerocket.interfaces.IApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class ApiService(private val baseUrl: String) {

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val apiService: IApiService by lazy {
        retrofit.create(IApiService::class.java)
    }

    fun uploadImage(imageFile: File, qrData: String?, callback: UploadCallback) {
        val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
        val qrDataBody = (qrData ?: "").toRequestBody("text/plain".toMediaTypeOrNull())

        apiService.uploadImage(body, qrDataBody).enqueue(
            object : retrofit2.Callback<UploadResponse> {
                override fun onResponse(
                    call: Call<UploadResponse>,
                    response: retrofit2.Response<UploadResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        callback.onSuccess(response.body()!!)
                    } else {
                        callback.onError("Upload failed: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                    callback.onError("Upload error: ${t.message}")
                }
            }
        )
    }

    interface UploadCallback {
        fun onSuccess(response: UploadResponse)
        fun onError(message: String)
    }
}