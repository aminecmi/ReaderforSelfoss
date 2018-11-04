package apps.amine.bou.readerforselfoss.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "actions")
data class ActionEntity(
    @ColumnInfo(name = "articleid")
    val articleId: String,
    @ColumnInfo(name = "read")
    val read: Boolean,
    @ColumnInfo(name = "unread")
    val unread: Boolean,
    @ColumnInfo(name = "starred")
    var starred: Boolean,
    @ColumnInfo(name = "unstarred")
    var unstarred: Boolean
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}