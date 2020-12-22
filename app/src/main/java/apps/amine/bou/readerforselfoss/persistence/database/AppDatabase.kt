package apps.amine.bou.readerforselfoss.persistence.database

import androidx.room.RoomDatabase
import androidx.room.Database
import apps.amine.bou.readerforselfoss.persistence.dao.ActionsDao
import apps.amine.bou.readerforselfoss.persistence.dao.DrawerDataDao
import apps.amine.bou.readerforselfoss.persistence.dao.ItemsDao
import apps.amine.bou.readerforselfoss.persistence.entities.ActionEntity
import apps.amine.bou.readerforselfoss.persistence.entities.ItemEntity
import apps.amine.bou.readerforselfoss.persistence.entities.SourceEntity
import apps.amine.bou.readerforselfoss.persistence.entities.TagEntity

@Database(entities = [TagEntity::class, SourceEntity::class, ItemEntity::class, ActionEntity::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun drawerDataDao(): DrawerDataDao

    abstract fun itemsDao(): ItemsDao

    abstract fun actionsDao(): ActionsDao
}