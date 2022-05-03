package com.example.testingpost.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface RetrofitApi {

    @Multipart
    @POST("UploadFile")
    suspend fun postFile(@Part type: MultipartBody.Part, @Part file: MultipartBody.Part) : Call<RequestBody>
}