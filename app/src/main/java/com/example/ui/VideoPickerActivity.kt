package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class VideoPickerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TARGET_ORDER = "extra_target_order"
        var onVideoPickedListener: ((targetOrder: Int, uriStr: String) -> Unit)? = null

        fun launch(context: Context, targetOrder: Int) {
            val intent = Intent(context, VideoPickerActivity::class.java).apply {
                putExtra(EXTRA_TARGET_ORDER, targetOrder)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private var targetOrder: Int = 1

    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if permission not persistable for this URI
            }

            val uriString = uri.toString()
            onVideoPickedListener?.invoke(targetOrder, uriString)
            Toast.makeText(this, "Video selected from gallery", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No video selected", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetOrder = intent.getIntExtra(EXTRA_TARGET_ORDER, 1)
        videoPickerLauncher.launch("video/*")
    }
}
