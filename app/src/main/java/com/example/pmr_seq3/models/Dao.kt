package com.example.pmr_seq3.models

import androidx.room.*

@Dao
interface ListDao {
    @Query("SELECT * FROM lists")
    suspend fun getAllLists(): List<ListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLists(lists: List<ListEntity>)

    @Query("DELETE FROM lists")
    suspend fun clearAll()
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE list_id = :listId")
    suspend fun getItemsForList(listId: Int): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemEntity>)

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("SELECT * FROM items WHERE needsSync = 1")
    suspend fun getItemsToSync(): List<ItemEntity>

    @Query("DELETE FROM items WHERE list_id = :listId")
    suspend fun clearItemsForList(listId: Int)

    @Query("DELETE FROM items")
    suspend fun clearAll()
} 