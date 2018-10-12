package apps.amine.bou.readerforselfoss.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey
    @ColumnInfo(name = "tag")
    val tag: String,
    @ColumnInfo(name = "color")
    val color: String,
    @ColumnInfo(name = "unread")
    val unread: Int
)

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "tags")
    val tags: String,
    @ColumnInfo(name = "spout")
    val spout: String,
    @ColumnInfo(name = "error")
    val error: String,
    @ColumnInfo(name = "icon")
    val icon: String
)