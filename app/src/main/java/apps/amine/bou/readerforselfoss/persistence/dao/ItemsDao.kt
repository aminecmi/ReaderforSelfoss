package apps.amine.bou.readerforselfoss.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import apps.amine.bou.readerforselfoss.persistence.entities.ItemEntity
import androidx.room.Update



@Dao
interface ItemsDao {
    @Query("SELECT * FROM items order by id desc")
    fun items(): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllItems(vararg items: ItemEntity)

    @Query("DELETE FROM items")
    fun deleteAllItems()

    @Delete
    fun delete(item: ItemEntity)

    @Update
    fun updateItem(item: ItemEntity)
}