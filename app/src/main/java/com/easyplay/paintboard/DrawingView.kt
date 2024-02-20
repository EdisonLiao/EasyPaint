package com.easyplay.paintboard

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.properties.Delegates

class DrawingView(
    context: Context,
    attrs: AttributeSet
): View(context, attrs) {

    private var drawPaint: Paint = Paint()
    private var path: Path = Path()
    private var drawingPathList = mutableListOf<DrawingPath>()

    private var currentStrokeWidth: Float = DEFAULT_BRUSH_SIZE
    private var currentStrokeColor: Int = DEFAULT_COLOR
    private var currentMaskFilter: MaskFilter? = null

    private var startX: Float = 0F
    private var startY: Float = 0F

    init {
        drawPaint.apply {
            isAntiAlias = true
            color = currentStrokeColor
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = currentStrokeWidth
            maskFilter = BlurMaskFilter(strokeWidth, BlurMaskFilter.Blur.SOLID)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()

        for(drawingPath in drawingPathList) {
            drawPaint.apply {
                strokeWidth = drawingPath.strokeWidth
                color = drawingPath.strokeColor
                maskFilter = drawingPath.maskFilter
            }
            canvas.drawPath(drawingPath.path, drawPaint)
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val touchX = event?.x
        val touchY = event?.y

        if(touchX == null || touchY == null) {
            return false
        }

        when(event.action) {
            MotionEvent.ACTION_DOWN -> touchStart(touchX, touchY)
            MotionEvent.ACTION_MOVE -> touchMove(touchX, touchY)
            MotionEvent.ACTION_UP -> touchUp(touchX, touchY)
        }

        invalidate()
        return true
    }

    private fun touchStart(x: Float, y: Float) {
        path = Path()
        drawingPathList.add(getCurrentDrawingPath())

        path.reset()
        path.moveTo(x, y)

        startX = x
        startY = y
    }

    private fun touchMove(x: Float, y: Float) {
        val dx = abs(x - startX)
        val dy = abs(y - startY)

        if(dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            path.quadTo(startX, startY, (x + startX) / 2, (y + startY) / 2)

            startX = x
            startY = y
        }
    }

    private fun touchUp(x: Float, y: Float) {
        path.lineTo(x, y)
    }

    private fun getCurrentDrawingPath() = DrawingPath(
            path,
            currentStrokeColor,
            currentStrokeWidth,
            currentMaskFilter
    )

    fun setBrushSize(width: Float) {
        currentStrokeWidth = width
    }

    fun setBrushColor(color: Int) {
        currentStrokeColor = color
    }

    fun setBlurEffect(blurEffect: BlurMaskFilter.Blur?) {
        if(blurEffect == null) {
            currentMaskFilter = null
            return
        }

        currentMaskFilter = BlurMaskFilter(currentStrokeWidth, blurEffect)
    }

    fun undo() {
        drawingPathList.removeLast()
        invalidate()
    }

    fun clearView() {
        drawingPathList.clear()

        path.reset()
        invalidate()
    }

    fun save(): Pair<String, Boolean> {

        /*COPYING PAINTING ON BITMAP CANVAS*/
        val drawBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val drawCanvas = Canvas(drawBitmap)
        drawCanvas.drawColor(DEFAULT_BG_COLOR)

        for(drawingPath in drawingPathList) {
            drawPaint.apply {
                strokeWidth = drawingPath.strokeWidth
                color = drawingPath.strokeColor
                maskFilter = drawingPath.maskFilter
            }
            drawCanvas.drawPath(drawingPath.path, drawPaint)
        }

        /*COMPRESSING BITMAP AND SAVING AS PNG IMAGE*/
        val timeStamp = SimpleDateFormat(MainActivity.FILE_NAME_FORMAT, Locale.UK).format(System.currentTimeMillis())

        val filePath = context.getExternalFilesDir(MainActivity.PAINTINGS_DIR)?.path + "/Paint$timeStamp.png"
        val file = File(filePath)

        val isFileSaved = try {
            val outputStream = FileOutputStream(file)
            drawBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
            drawBitmap.recycle()
            true

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }

        return Pair(filePath, isFileSaved)
    }

    companion object {
        private const val DEFAULT_BRUSH_SIZE = 20F
        private const val DEFAULT_COLOR: Int = Color.GRAY
        private const val DEFAULT_BG_COLOR: Int = Color.WHITE
        private const val TOUCH_TOLERANCE = 4f
    }
}