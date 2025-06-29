package com.example.pmr_seq3.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lists")
data class ListEntity(
    @PrimaryKey val id: Int,
    val label: String,
    val user_id: Int
)

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: Int,
    val label: String,
    val url: String?,
    val check: Int,
    val list_id: Int,
    val needsSync: Boolean = false // Pour la synchro offline
) 