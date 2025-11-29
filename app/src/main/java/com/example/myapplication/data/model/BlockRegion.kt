package com.example.myapplication.data.model

import android.graphics.Rect

data class BlockRegion(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun toRect(): Rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())

    fun contains(x: Float, y: Float): Boolean = x >= left && x <= right && y >= top && y <= bottom
}
