package apps.amine.bou.readerforselfoss.persistence.migrations

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `items` (`id` TEXT NOT NULL, `datetime` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `unread` INTEGER NOT NULL, `starred` INTEGER NOT NULL, `thumbnail` TEXT NOT NULL, `icon` TEXT NOT NULL, `link` TEXT NOT NULL, `sourcetitle` TEXT NOT NULL, `tags` TEXT NOT NULL, PRIMARY KEY(`id`))")
    }
}

val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `actions` (`id` INTEGER NOT NULL, `articleid` TEXT NOT NULL, `read` INTEGER NOT NULL, `unread` INTEGER NOT NULL, `starred` INTEGER NOT NULL, `unstarred` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    }
}

val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // @see https://stackoverflow.com/questions/57392015/how-to-migrate-not-null-table-column-into-null-in-android-room-database
        // Create the new table
        database.execSQL("CREATE TABLE IF NOT EXISTS `itemstmp` (`id` TEXT NOT NULL, `datetime` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `unread` INTEGER NOT NULL, `starred` INTEGER NOT NULL, `thumbnail` TEXT, `icon` TEXT, `link` TEXT NOT NULL, `sourcetitle` TEXT NOT NULL, `tags` TEXT NOT NULL, PRIMARY KEY(`id`))")

        // Copy the data
        database.execSQL(
                "INSERT INTO itemstmp (`id`, `datetime`, `title`, `content`, `unread`, `starred`, `thumbnail`, `icon`, `link`, `sourcetitle`, `tags`) SELECT `id`, `datetime`, `title`, `content`, `unread`, `starred`, `thumbnail`, `icon`, `link`, `sourcetitle`, `tags` FROM items")

        // Remove the old table
        database.execSQL("DROP TABLE items")

        // Change the table name to the correct one
        database.execSQL("ALTER TABLE itemstmp RENAME TO items")
    }
}
