package com.example.utils

import android.content.Context
import android.util.DisplayMetrics

object DisplayUtils {

    fun dpToPx(context: Context, dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    fun pxToDp(context: Context, px: Float): Int {
        val density = context.resources.displayMetrics.density
        return (px / density).toInt()
    }

    fun getScreenSize(context: Context): Pair<Int, Int> {
        val metrics: DisplayMetrics = context.resources.displayMetrics
        return Pair(metrics.widthPixels, metrics.heightPixels)
    }
}
