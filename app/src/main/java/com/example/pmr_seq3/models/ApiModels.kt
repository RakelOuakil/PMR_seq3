package com.example.pmr_seq3.models

object ApiModels {
    
    // Authentication response
    data class AuthResponse(
        val token: String? = null,
        val hash: String? = null,
        val user: UserInfo? = null
    )
    
    data class UserInfo(
        val id: Int? = null,
        val login: String? = null,
        val pseudo: String? = null
    )
    
    // Wrapper responses for API endpoints
    data class ListsResponse(
        val lists: List<ListResponse>? = null,
        val count: Int? = null
    )
    
    data class ItemsResponse(
        val items: List<ItemResponse>? = null,
        val count: Int? = null
    )
    
    data class UsersResponse(
        val users: List<UserResponse>? = null,
        val count: Int? = null
    )
    
    // User response
    data class UserResponse(
        val id: Int,
        val login: String,
        val pass: String
    )
    
    // List response
    data class ListResponse(
        val id: Int,
        val label: String,
        val user_id: Int
    )
    
    // Item response
    data class ItemResponse(
        val id: Int,
        val label: String,
        val url: String?,
        val check: Int,
        val list_id: Int
    )
    
    // Error response
    data class ErrorResponse(
        val error: String
    )
} 