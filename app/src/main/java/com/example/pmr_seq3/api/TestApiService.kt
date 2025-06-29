package com.example.pmr_seq3.api

import retrofit2.Response
import retrofit2.http.GET

interface TestApiService {
    @GET("posts/1")
    suspend fun testConnection(): Response<Any>
} 