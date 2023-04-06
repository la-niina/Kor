package kor.app

import android.content.Context

object utils {
    fun Context.dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp.toFloat() * density + 0.5f).toInt()
    }

}