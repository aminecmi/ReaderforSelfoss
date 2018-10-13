package apps.amine.bou.readerforselfoss.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import apps.amine.bou.readerforselfoss.persistence.entities.ItemEntity
import androidx.room.Update



@Dao
interface ItemsDao {
    @Query("SELECT * FROM items")
    fun items(): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllItems(vararg tags: ItemEntity)

    @Query("DELETE FROM items")
    fun deleteAllItems()

    @Update
    fun updateItem(item: ItemEntity)
}