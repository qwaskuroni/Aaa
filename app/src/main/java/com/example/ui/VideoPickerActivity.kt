package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.permission.PermissionUtils

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

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Proceed to video picker regardless, as GetContent contract works via system picker
        videoPickerLauncher.launch("video/*")
    }

    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var savedPathOrUri = uri.toString()
            try {
                // Copy selected video to app's private internal storage so background WindowManager overlay can read it anytime
                val destFile = java.io.File(filesDir, "target_video_${targetOrder}.mp4")
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    java.io.FileOutputStream(destFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                if (destFile.exists() && destFile.length() > 0) {
                    savedPathOrUri = destFile.absolutePath
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            onVideoPickedListener?.invoke(targetOrder, savedPathOrUri)
            Toast.makeText(this, "Video saved for target #$targetOrder", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No video selected", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetOrder = intent.getIntExtra(EXTRA_TARGET_ORDER, 1)

        if (!PermissionUtils.hasStoragePermission(this)) {
            val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_VIDEO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            permissionLauncher.launch(permissionToRequest)
        } else {
            videoPickerLauncher.launch("video/*")
        }
    }
}
