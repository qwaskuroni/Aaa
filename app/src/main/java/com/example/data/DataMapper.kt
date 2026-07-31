package com.example.data

import com.example.database.ScriptEntity
import com.example.database.ScriptTargetEntity
import com.example.model.ClickTarget
import com.example.model.ScriptModel
import com.example.model.TargetType

object DataMapper {

    fun mapToDomain(relation: ScriptWithTargets): ScriptModel {
        return ScriptModel(
            id = relation.script.id,
            name = relation.script.name,
            repeatCount = relation.script.repeatCount,
            repeatIntervalMs = relation.script.repeatIntervalMs,
            randomOffsetPx = relation.script.randomOffsetPx,
            createdAt = relation.script.createdAt,
            isFavorite = relation.script.isFavorite,
            targets = relation.targets.sortedBy { it.targetOrder }.map { mapTargetToDomain(it) }
        )
    }

    fun mapTargetToDomain(entity: ScriptTargetEntity): ClickTarget {
        return ClickTarget(
            id = entity.id,
            scriptId = entity.scriptId,
            order = entity.targetOrder,
            type = try { TargetType.valueOf(entity.type) } catch (e: Exception) { TargetType.SINGLE_TAP },
            xPx = entity.xPx,
            yPx = entity.yPx,
            swipeEndXPx = entity.swipeEndXPx,
            swipeEndYPx = entity.swipeEndYPx,
            delayMs = entity.delayMs,
            durationMs = entity.durationMs,
            label = entity.label,
            sizePx = entity.sizePx,
            isLocked = entity.isLocked,
            textContent = entity.textContent,
            mediaUri = entity.mediaUri,
            repeatCount = entity.repeatCount,
            minUnreadCount = entity.minUnreadCount,
            processOrder = entity.processOrder,
            maxChatsToOpen = entity.maxChatsToOpen,
            skipPinnedChats = entity.skipPinnedChats,
            skipMutedChats = entity.skipMutedChats,
            autoScroll = entity.autoScroll,
            stopAtEnd = entity.stopAtEnd,
            excelFilePath = entity.excelFilePath,
            excelRulesContent = entity.excelRulesContent,
            matchThreshold = entity.matchThreshold,
            fallbackReply = entity.fallbackReply
        )
    }

    fun mapToEntity(model: ScriptModel): ScriptEntity {
        return ScriptEntity(
            id = model.id,
            name = model.name,
            repeatCount = model.repeatCount,
            repeatIntervalMs = model.repeatIntervalMs,
            randomOffsetPx = model.randomOffsetPx,
            createdAt = model.createdAt,
            isFavorite = model.isFavorite
        )
    }

    fun mapTargetToEntity(model: ClickTarget, scriptId: Long): ScriptTargetEntity {
        return ScriptTargetEntity(
            id = model.id,
            scriptId = scriptId,
            targetOrder = model.order,
            type = model.type.name,
            xPx = model.xPx,
            yPx = model.yPx,
            swipeEndXPx = model.swipeEndXPx,
            swipeEndYPx = model.swipeEndYPx,
            delayMs = model.delayMs,
            durationMs = model.durationMs,
            label = model.label,
            sizePx = model.sizePx,
            isLocked = model.isLocked,
            textContent = model.textContent,
            mediaUri = model.mediaUri,
            repeatCount = model.repeatCount,
            minUnreadCount = model.minUnreadCount,
            processOrder = model.processOrder,
            maxChatsToOpen = model.maxChatsToOpen,
            skipPinnedChats = model.skipPinnedChats,
            skipMutedChats = model.skipMutedChats,
            autoScroll = model.autoScroll,
            stopAtEnd = model.stopAtEnd,
            excelFilePath = model.excelFilePath,
            excelRulesContent = model.excelRulesContent,
            matchThreshold = model.matchThreshold,
            fallbackReply = model.fallbackReply
        )
    }
}
