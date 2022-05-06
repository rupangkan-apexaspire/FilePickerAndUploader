package com.example.testingpost.api

import com.example.testingpost.models.FilePostResponse
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface RetrofitApi {

    @Multipart
    @POST("UploadFile/?type=&file=")
    fun postFile(@Part("type") type: RequestBody, @Part file: MultipartBody.Part) : Call<FilePostResponse>

    @Multipart
    @POST("UploadFile/?type=&file=")
    fun postFile2(@Part("type") type: RequestBody, @Part file: MultipartBody.Part) : Call<ResponseBody>

    companion object {

        private val interceptor = run {
            val httpLoggingInterceptor = HttpLoggingInterceptor()
            httpLoggingInterceptor.apply {
                httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor) // same for .addInterceptor(...)
            .build()


        operator fun invoke(): RetrofitApi {
            return Retrofit.Builder()
                .baseUrl("http://api.dev2.infotute.com/infotute/")
                .client(OkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RetrofitApi::class.java)
        }
    }
}