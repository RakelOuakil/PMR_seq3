package com.example.pmr_seq3.api

import com.example.pmr_seq3.models.ApiModels
import retrofit2.Response
import retrofit2.http.*

interface TodoApiService {
    
    // Authentication - using query parameters as per Postman docs
    @POST("authenticate")
    suspend fun authenticate(
        @Query("user") username: String,
        @Query("password") password: String
    ): Response<ApiModels.AuthResponse>
    
    // Regenerate token
    @PUT("authenticate")
    suspend fun regenerateToken(
        @Header("hash") token: String
    ): Response<ApiModels.AuthResponse>
    
    // Lists - try both formats
    @GET("lists")
    suspend fun getLists(
        @Header("hash") token: String
    ): Response<ApiModels.ListsResponse>
    
    @POST("lists")
    suspend fun createList(
        @Header("hash") token: String,
        @Query("label") label: String
    ): Response<ApiModels.ListResponse>
    
    @GET("lists/{listId}")
    suspend fun getList(
        @Header("hash") token: String,
        @Path("listId") listId: Int
    ): Response<ApiModels.ListResponse>
    
    @PUT("lists/{listId}")
    suspend fun updateList(
        @Header("hash") token: String,
        @Path("listId") listId: Int,
        @Query("label") label: String
    ): Response<ApiModels.ListResponse>
    
    @DELETE("lists/{listId}")
    suspend fun deleteList(
        @Header("hash") token: String,
        @Path("listId") listId: Int
    ): Response<Unit>
    
    // Items - try both formats
    @GET("lists/{listId}/items")
    suspend fun getItems(
        @Header("hash") token: String,
        @Path("listId") listId: Int
    ): Response<ApiModels.ItemsResponse>
    
    @POST("lists/{listId}/items")
    suspend fun createItem(
        @Header("hash") token: String,
        @Path("listId") listId: Int,
        @Query("label") label: String,
        @Query("url") url: String? = null
    ): Response<ApiModels.ItemResponse>
    
    @PUT("lists/{listId}/items/{itemId}")
    suspend fun updateItem(
        @Header("hash") token: String,
        @Path("listId") listId: Int,
        @Path("itemId") itemId: Int,
        @Query("label") label: String? = null,
        @Query("url") url: String? = null,
        @Query("check") check: Int? = null
    ): Response<ApiModels.ItemResponse>
    
    @DELETE("lists/{listId}/items/{itemId}")
    suspend fun deleteItem(
        @Header("hash") token: String,
        @Path("listId") listId: Int,
        @Path("itemId") itemId: Int
    ): Response<Unit>
    
    // Users (for testing)
    @GET("users")
    suspend fun getUsers(
        @Header("hash") token: String
    ): Response<ApiModels.UsersResponse>
} 