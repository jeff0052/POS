package com.developer.pos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.developer.pos.data.local.dao.ProductDao
import com.developer.pos.data.local.entity.ProductEntity

@Database(
    entities = [ProductEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PosDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}
