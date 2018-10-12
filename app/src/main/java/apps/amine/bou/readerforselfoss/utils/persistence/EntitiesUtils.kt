package apps.amine.bou.readerforselfoss.utils.persistence

import android.content.Context
import apps.amine.bou.readerforselfoss.api.selfoss.Source
import apps.amine.bou.readerforselfoss.api.selfoss.Tag
import apps.amine.bou.readerforselfoss.persistence.entities.SourceEntity
import apps.amine.bou.readerforselfoss.persistence.entities.TagEntity

fun TagEntity.toView(): Tag =
        Tag(
            this.tag,
            this.color,
            this.unread
        )

fun SourceEntity.toView(): Source =
        Source(
            this.id,
            this.title,
            this.tags,
            this.spout,
            this.error,
            this.icon
        )

fun Source.toEntity(context: Context): SourceEntity =
        SourceEntity(
            this.id,
            this.title,
            this.tags,
            this.spout,
            this.error,
            this.getIcon(context)
        )

fun Tag.toEntity(): TagEntity =
        TagEntity(
            this.tag,
            this.color,
            this.unread
        )