package com.easyplay.paintboard

import android.graphics.MaskFilter
import android.graphics.Path

data class DrawingPath(
        val path: Path,
        val strokeColor: Int,
        val strokeWidth: Float,
        val maskFilter: MaskFilter?,
)
