package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.automation.XlsSmartReplyEngine
import java.io.File
import java.io.FileOutputStream

class ExcelPickerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TARGET_ORDER = "extra_target_order"
        var onExcelPickedListener: ((targetOrder: Int, filePathOrUri: String, parsedRulesText: String) -> Unit)? = null

        fun launch(context: Context, targetOrder: Int) {
            val intent = Intent(context, ExcelPickerActivity::class.java).apply {
                putExtra(EXTRA_TARGET_ORDER, targetOrder)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private var targetOrder: Int = 1

    private val excelPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore permission error if not persistable
            }

            var savedPathOrUri = uri.toString()
            var extractedRulesText = ""

            try {
                // Copy selected excel file to internal app storage for reliable background access
                val extension = if (uri.toString().endsWith(".csv", ignoreCase = true)) "csv" else "xlsx"
                val destFile = File(filesDir, "target_excel_${targetOrder}.$extension")
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(destFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                if (destFile.exists() && destFile.length() > 0) {
                    savedPathOrUri = destFile.absolutePath
                }

                // Extract and format rules from Excel file
                val rules = XlsSmartReplyEngine.loadRules(this, savedPathOrUri, "")
                if (rules.isNotEmpty()) {
                    extractedRulesText = rules.joinToString("\n") { row ->
                        "${row.keywords.joinToString(", ")} : ${row.reply}"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            onExcelPickedListener?.invoke(targetOrder, savedPathOrUri, extractedRulesText)
            Toast.makeText(this, "Excel file & rules saved for Target #$targetOrder", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No Excel file selected", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetOrder = intent.getIntExtra(EXTRA_TARGET_ORDER, 1)
        excelPickerLauncher.launch("*/*")
    }
}
