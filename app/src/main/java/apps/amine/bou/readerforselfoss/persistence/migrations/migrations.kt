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
