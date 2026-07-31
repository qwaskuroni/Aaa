package com.example.automation

import android.content.Context
import android.net.Uri
import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

data class ExcelRuleRow(
    val keywords: List<String>,
    val reply: String
)

object XlsSmartReplyEngine {

    private const val TAG = "XlsSmartReplyEngine"

    // Built-in Bengali & English Synonym Dictionary
    private val synonymGroups = listOf(
        setOf("নাম্বার", "নম্বর", "নাম্বারটা", "নম্বরটা", "number", "num", "no", "মোবাইল"),
        setOf("বিকাশ", "bkash", "bk"),
        setOf("নগদ", "nagad", "ngd"),
        setOf("রকেট", "rocket", "rkt"),
        setOf("টাকা", "পেমেন্ট", "payment", "pay", "টাকা পাঠাবো", "পে", "মূল্য", "দাম", "price", "cost", "টাকা কত"),
        setOf("কত", "কতো", "দাম কত", "দাম কতো", "price", "how much"),
        setOf("দাও", "দেন", "দিন", "দেবেন", "দিবেন", "পাঠাও", "পাঠান", "give", "send"),
        setOf("হাই", "হ্যালো", "হেলো", "hi", "hello", "hey", "assalamu alaikum", "সালাম", "আসসালামু আলাইকুম")
    )

    suspend fun processAutoReply(
        context: Context,
        excelFilePath: String,
        excelRulesContent: String,
        scannedMessage: String,
        matchThreshold: Float = 0.3f,
        fallbackReply: String = ""
    ): String {
        val inputMessage = scannedMessage.ifEmpty { MacroContext.scannedMessage }
        if (inputMessage.isBlank()) {
            Log.d(TAG, "Scanned message is empty. No auto reply generated.")
            MacroContext.autoReply = fallbackReply
            return fallbackReply
        }

        val rules = loadRules(context, excelFilePath, excelRulesContent)
        if (rules.isEmpty()) {
            Log.w(TAG, "No Excel rules loaded. Returning fallback reply.")
            MacroContext.autoReply = fallbackReply
            return fallbackReply
        }

        val bestMatch = findBestMatchingReply(inputMessage, rules, matchThreshold)
        val selectedReply = bestMatch ?: fallbackReply

        Log.d(TAG, "Scanned: '$inputMessage' -> Selected Reply: '$selectedReply'")
        MacroContext.autoReply = selectedReply
        return selectedReply
    }

    private fun findBestMatchingReply(
        scannedMessage: String,
        rules: List<ExcelRuleRow>,
        threshold: Float
    ): String? {
        val normalizedInput = normalizeText(scannedMessage)
        val inputTokens = tokenize(normalizedInput)

        var highestScore = 0f
        var bestReply: String? = null

        for (rule in rules) {
            for (rawKeyword in rule.keywords) {
                val score = calculateMatchScore(normalizedInput, inputTokens, rawKeyword)
                if (score > highestScore) {
                    highestScore = score
                    bestReply = rule.reply
                }
            }
        }

        Log.d(TAG, "Highest matching confidence score: $highestScore (Threshold: $threshold)")

        return if (highestScore >= threshold) {
            bestReply
        } else {
            null
        }
    }

    private fun calculateMatchScore(
        normalizedInput: String,
        inputTokens: Set<String>,
        rawKeyword: String
    ): Float {
        val normalizedKeyword = normalizeText(rawKeyword)
        if (normalizedKeyword.isEmpty()) return 0f

        // 1. Exact string match
        if (normalizedInput == normalizedKeyword) {
            return 1.0f
        }

        // 2. Exact keyword / phrase substring match
        if (normalizedInput.contains(normalizedKeyword) || normalizedKeyword.contains(normalizedInput)) {
            return 0.9f
        }

        val keywordTokens = tokenize(normalizedKeyword)
        if (keywordTokens.isEmpty()) return 0f

        // 3. Token overlap matching with synonym expansion
        var matchedTokensCount = 0f

        for (kToken in keywordTokens) {
            if (inputTokens.contains(kToken)) {
                matchedTokensCount += 1.0f
            } else {
                // Check synonym match
                var synonymFound = false
                for (group in synonymGroups) {
                    if (group.contains(kToken)) {
                        for (syn in group) {
                            if (inputTokens.contains(syn)) {
                                matchedTokensCount += 0.85f
                                synonymFound = true
                                break
                            }
                        }
                    }
                    if (synonymFound) break
                }
            }
        }

        val tokenScore = matchedTokensCount / keywordTokens.size.toFloat()
        return tokenScore.coerceIn(0f, 1f)
    }

    private fun normalizeText(text: String): String {
        if (text.isBlank()) return ""
        var result = text.lowercase()
        // Replace emojis and special punctuation with spaces
        result = result.replace(Regex("[^\\a-zA-Z0-9\\s\\u0980-\\u09FF]"), " ")
        // Normalize multiple spaces
        return result.replace(Regex("\\s+"), " ").trim()
    }

    private fun tokenize(normalizedText: String): Set<String> {
        return normalizedText.split(" ").filter { it.length > 1 }.toSet()
    }

    private fun loadRules(
        context: Context,
        filePathOrUri: String,
        inlineRulesContent: String
    ): List<ExcelRuleRow> {
        val rulesList = mutableListOf<ExcelRuleRow>()

        // 1. Attempt parsing from Excel / File URI if provided
        if (filePathOrUri.isNotEmpty()) {
            try {
                val uri = Uri.parse(filePathOrUri)
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    if (filePathOrUri.endsWith(".xlsx", ignoreCase = true) || filePathOrUri.contains("spreadsheet")) {
                        rulesList.addAll(parseXlsxInputStream(inputStream))
                    } else {
                        rulesList.addAll(parseCsvInputStream(inputStream))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error opening file URI: $filePathOrUri", e)
            }
        }

        // 2. Parse inline rules if rulesList is empty or inline content is provided
        if (inlineRulesContent.isNotBlank()) {
            rulesList.addAll(parseInlineRulesText(inlineRulesContent))
        }

        return rulesList
    }

    // Native zero-dependency XLSX parser using ZipInputStream & XmlPullParser
    private fun parseXlsxInputStream(inputStream: InputStream): List<ExcelRuleRow> {
        val rules = mutableListOf<ExcelRuleRow>()
        val sharedStrings = mutableListOf<String>()

        var sheetStream: InputStream? = null
        val zip = ZipInputStream(inputStream)

        var entry = zip.nextEntry
        val zipBytes = mutableMapOf<String, ByteArray>()

        while (entry != null) {
            val name = entry.name.lowercase()
            if (name.contains("sharedstrings.xml") || name.contains("sheet1.xml") || name.contains("sheet.xml")) {
                zipBytes[name] = zip.readBytes()
            }
            entry = zip.nextEntry
        }

        // Read sharedStrings.xml
        val sharedEntryName = zipBytes.keys.find { it.contains("sharedstrings.xml") }
        if (sharedEntryName != null) {
            val sharedBytes = zipBytes[sharedEntryName]
            if (sharedBytes != null) {
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val parser = factory.newPullParser()
                parser.setInput(sharedBytes.inputStream(), "UTF-8")

                var eventType = parser.eventType
                var inTextTag = false

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            if (parser.name == "t") inTextTag = true
                        }
                        XmlPullParser.TEXT -> {
                            if (inTextTag) {
                                sharedStrings.add(parser.text ?: "")
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name == "t") inTextTag = false
                        }
                    }
                    eventType = parser.next()
                }
            }
        }

        // Read sheet1.xml
        val sheetEntryName = zipBytes.keys.find { it.contains("sheet1.xml") || it.contains("sheet.xml") }
        if (sheetEntryName != null) {
            val sheetBytes = zipBytes[sheetEntryName]
            if (sheetBytes != null) {
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val parser = factory.newPullParser()
                parser.setInput(sheetBytes.inputStream(), "UTF-8")

                var eventType = parser.eventType
                var currentCol = 0
                var currentCellType = ""
                var cellValue = ""
                var colA = ""
                var colB = ""

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            val tagName = parser.name
                            if (tagName == "row") {
                                colA = ""
                                colB = ""
                                currentCol = 0
                            } else if (tagName == "c") {
                                val rAttr = parser.getAttributeValue(null, "r") ?: ""
                                currentCellType = parser.getAttributeValue(null, "t") ?: ""
                                currentCol = if (rAttr.startsWith("B")) 1 else 0
                            } else if (tagName == "v") {
                                cellValue = ""
                            }
                        }
                        XmlPullParser.TEXT -> {
                            cellValue += parser.text ?: ""
                        }
                        XmlPullParser.END_TAG -> {
                            val tagName = parser.name
                            if (tagName == "v") {
                                val valueStr = if (currentCellType == "s") {
                                    val idx = cellValue.toIntOrNull() ?: -1
                                    if (idx in 0 until sharedStrings.size) sharedStrings[idx] else ""
                                } else {
                                    cellValue
                                }

                                if (currentCol == 0) colA = valueStr
                                else if (currentCol == 1) colB = valueStr
                            } else if (tagName == "row") {
                                if (colA.isNotBlank() && colB.isNotBlank()) {
                                    val keywords = splitKeywords(colA)
                                    rules.add(ExcelRuleRow(keywords = keywords, reply = colB.trim()))
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
        }

        return rules
    }

    private fun parseCsvInputStream(inputStream: InputStream): List<ExcelRuleRow> {
        val rules = mutableListOf<ExcelRuleRow>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line = reader.readLine()
        while (line != null) {
            val parts = line.split(Regex("[,\\t|]"))
            if (parts.size >= 2) {
                val colA = parts[0].trim()
                val colB = parts[1].trim()
                if (colA.isNotEmpty() && colB.isNotEmpty()) {
                    rules.add(ExcelRuleRow(keywords = splitKeywords(colA), reply = colB))
                }
            }
            line = reader.readLine()
        }
        return rules
    }

    private fun parseInlineRulesText(content: String): List<ExcelRuleRow> {
        val rules = mutableListOf<ExcelRuleRow>()
        val lines = content.split("\n")
        for (line in lines) {
            val parts = line.split(Regex("[:=,]"), limit = 2)
            if (parts.size >= 2) {
                val keywordsStr = parts[0].trim()
                val reply = parts[1].trim()
                if (keywordsStr.isNotEmpty() && reply.isNotEmpty()) {
                    rules.add(ExcelRuleRow(keywords = splitKeywords(keywordsStr), reply = reply))
                }
            }
        }
        return rules
    }

    private fun splitKeywords(rawColA: String): List<String> {
        return rawColA.split(Regex("[,/\\n]")).map { it.trim() }.filter { it.isNotEmpty() }
    }
}
