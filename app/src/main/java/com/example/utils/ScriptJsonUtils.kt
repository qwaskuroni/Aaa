package com.example.utils

import com.example.model.ClickTarget
import com.example.model.ScriptModel
import com.example.model.TargetType
import org.json.JSONArray
import org.json.JSONObject

object ScriptJsonUtils {

    fun exportToJson(script: ScriptModel): String {
        val root = JSONObject()
        root.put("name", script.name)
        root.put("repeatCount", script.repeatCount)
        root.put("repeatIntervalMs", script.repeatIntervalMs)
        root.put("randomOffsetPx", script.randomOffsetPx)

        val targetsArray = JSONArray()
        script.targets.forEach { target ->
            val targetObj = JSONObject()
            targetObj.put("order", target.order)
            targetObj.put("type", target.type.name)
            targetObj.put("xPx", target.xPx.toDouble())
            targetObj.put("yPx", target.yPx.toDouble())
            targetObj.put("swipeEndXPx", target.swipeEndXPx.toDouble())
            targetObj.put("swipeEndYPx", target.swipeEndYPx.toDouble())
            targetObj.put("delayMs", target.delayMs)
            targetObj.put("durationMs", target.durationMs)
            targetObj.put("label", target.label)
            targetObj.put("sizePx", target.sizePx.toDouble())
            targetObj.put("isLocked", target.isLocked)
            targetObj.put("textContent", target.textContent)
            targetObj.put("mediaUri", target.mediaUri)
            targetObj.put("repeatCount", target.repeatCount)
            targetObj.put("minUnreadCount", target.minUnreadCount)
            targetObj.put("processOrder", target.processOrder)
            targetObj.put("maxChatsToOpen", target.maxChatsToOpen)
            targetObj.put("skipPinnedChats", target.skipPinnedChats)
            targetObj.put("skipMutedChats", target.skipMutedChats)
            targetObj.put("autoScroll", target.autoScroll)
            targetObj.put("stopAtEnd", target.stopAtEnd)
            targetObj.put("excelFilePath", target.excelFilePath)
            targetObj.put("excelRulesContent", target.excelRulesContent)
            targetObj.put("matchThreshold", target.matchThreshold.toDouble())
            targetObj.put("fallbackReply", target.fallbackReply)
            targetObj.put("voiceToTextDelayBeforeMs", target.voiceToTextDelayBeforeMs)
            targetObj.put("voiceToTextWaitAfterMs", target.voiceToTextWaitAfterMs)
            targetObj.put("aiIntentApiKey", target.aiIntentApiKey)
            targetsArray.put(targetObj)
        }

        root.put("targets", targetsArray)
        return root.toString(2)
    }

    fun importFromJson(jsonString: String): ScriptModel? {
        return try {
            val root = JSONObject(jsonString)
            val name = root.optString("name", "Imported Profile")
            val repeatCount = root.optInt("repeatCount", -1)
            val repeatIntervalMs = root.optLong("repeatIntervalMs", 500L)
            val randomOffsetPx = root.optInt("randomOffsetPx", 0)

            val targets = mutableListOf<ClickTarget>()
            val targetsArray = root.optJSONArray("targets")
            if (targetsArray != null) {
                for (i in 0 until targetsArray.length()) {
                    val obj = targetsArray.getJSONObject(i)
                    val typeStr = obj.optString("type", TargetType.SINGLE_TAP.name)
                    val targetType = try {
                        TargetType.valueOf(typeStr)
                    } catch (e: Exception) {
                        TargetType.SINGLE_TAP
                    }

                    targets.add(
                        ClickTarget(
                            id = 0,
                            order = obj.optInt("order", i + 1),
                            type = targetType,
                            xPx = obj.optDouble("xPx", 200.0).toFloat(),
                            yPx = obj.optDouble("yPx", 500.0).toFloat(),
                            swipeEndXPx = obj.optDouble("swipeEndXPx", 200.0).toFloat(),
                            swipeEndYPx = obj.optDouble("swipeEndYPx", 200.0).toFloat(),
                            delayMs = obj.optLong("delayMs", 500L),
                            durationMs = obj.optLong("durationMs", 100L),
                            label = obj.optString("label", "Target ${i + 1}"),
                            sizePx = obj.optDouble("sizePx", 96.0).toFloat(),
                            isLocked = obj.optBoolean("isLocked", false),
                            textContent = obj.optString("textContent", ""),
                            mediaUri = obj.optString("mediaUri", ""),
                            repeatCount = obj.optInt("repeatCount", 1),
                            minUnreadCount = obj.optInt("minUnreadCount", 1),
                            processOrder = obj.optString("processOrder", "TOP_TO_BOTTOM"),
                            maxChatsToOpen = obj.optInt("maxChatsToOpen", 0),
                            skipPinnedChats = obj.optBoolean("skipPinnedChats", false),
                            skipMutedChats = obj.optBoolean("skipMutedChats", false),
                            autoScroll = obj.optBoolean("autoScroll", true),
                            stopAtEnd = obj.optBoolean("stopAtEnd", true),
                            excelFilePath = obj.optString("excelFilePath", ""),
                            excelRulesContent = obj.optString("excelRulesContent", ""),
                            matchThreshold = obj.optDouble("matchThreshold", 0.3).toFloat(),
                            fallbackReply = obj.optString("fallbackReply", ""),
                            voiceToTextDelayBeforeMs = obj.optLong("voiceToTextDelayBeforeMs", 2000L),
                            voiceToTextWaitAfterMs = obj.optLong("voiceToTextWaitAfterMs", 2000L),
                            aiIntentApiKey = obj.optString("aiIntentApiKey", "")
                        )
                    )
                }
            }

            ScriptModel(
                id = 0,
                name = name,
                repeatCount = repeatCount,
                repeatIntervalMs = repeatIntervalMs,
                randomOffsetPx = randomOffsetPx,
                createdAt = System.currentTimeMillis(),
                isFavorite = false,
                targets = targets
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
