package apps.amine.bou.readerforselfoss.utils.persistence

import apps.amine.bou.readerforselfoss.api.selfoss.Item
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossTagType
import apps.amine.bou.readerforselfoss.api.selfoss.Source
import apps.amine.bou.readerforselfoss.api.selfoss.Tag
import apps.amine.bou.readerforselfoss.persistence.entities.ItemEntity
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
            SelfossTagType(this.tags),
            this.spout,
            this.error,
            this.icon
        )

fun Source.toEntity(): SourceEntity =
        SourceEntity(
            this.id,
            this.getTitleDecoded(),
            this.tags.tags,
            this.spout,
            this.error,
            this.icon.orEmpty()
        )

fun Tag.toEntity(): TagEntity =
        TagEntity(
            this.tag,
            this.color,
            this.unread
        )

fun ItemEntity.toView(): Item =
        Item(
            this.id,
            this.datetime,
            this.title,
            this.content,
            this.unread,
            this.starred,
            this.thumbnail,
            this.icon,
            this.link,
            this.sourcetitle,
            SelfossTagType(this.tags)
        )

fun Item.toEntity(): ItemEntity =
    ItemEntity(
        this.id,
        this.datetime,
        this.getTitleDecoded(),
        this.content,
        this.unread,
        this.starred,
        this.thumbnail,
        this.icon,
        this.link,
        this.getSourceTitle(),
        this.tags.tags
    )