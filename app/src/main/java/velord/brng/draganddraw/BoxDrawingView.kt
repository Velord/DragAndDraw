package velord.brng.draganddraw

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View


private const val TAG = "BoxDrawingView"
private const val BOXES = "boxes"
private const val SUPER_STATE = "superState"

class BoxDrawingView(context: Context,
                     attrs: AttributeSet? = null) : View(context, attrs) {

    private var currentBox: Box? = null
    private val boxen = mutableListOf<Box>()
    private val boxPaint = Paint().apply {
        color = 0x22ff0000.toInt()
    }
    private val backgroundPaint = Paint().apply {
        color = 0xfff8efe0.toInt()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val current = PointF(event!!.x, event.y)
        var action = ""
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                action = "ACTION_DOWN"
                // Reset drawing state
                currentBox = Box(current).also {
                    boxen.add(it)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                action = "ACTION_MOVE"
                updateCurrentBox(current)
            }
            MotionEvent.ACTION_UP -> {
                action = "ACTION_UP"
                updateCurrentBox(current)
                currentBox = null
            }
            MotionEvent.ACTION_CANCEL -> {
                action = "ACTION_CANCEL"
                currentBox = null
            }
        }

        Log.i(TAG, "$action at x=${current.x}, y=${current.y}")

        return true
    }

    override fun onDraw(canvas: Canvas?) {
        //fill the background
        canvas!!.drawPaint(backgroundPaint)

        boxen.forEach { box ->
            canvas.drawRect(
                box.left, box.top,
                box.right, box.bottom,
                boxPaint
            )
        }
    }

    override fun setSaveEnabled(enabled: Boolean) {
        super.setSaveEnabled(true)
    }

    override fun onSaveInstanceState(): Parcelable? {
        return Bundle().apply {
            putParcelable(SUPER_STATE, super.onSaveInstanceState())

            putParcelableArrayList(BOXES, ArrayList<Parcelable>(boxen))
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        var newState = state
        if (newState is Bundle) {
            this.boxen.addAll(
                transformToPortraitOrLandscape(
                    newState
                    .getParcelableArrayList<Box>(BOXES)!!
                    .toMutableList()
                )
            )
            newState = newState.getParcelable(SUPER_STATE)
        }
        super.onRestoreInstanceState(newState)
    }

    private fun transformToPortraitOrLandscape(box: MutableList<Box>): MutableList<Box> {
        val metrics = this.resources.displayMetrics
        val dh = metrics.heightPixels
        val dw = metrics.widthPixels
        //stretch or squeeze
        fun toNewDimension(box: Box): Pair<PointF, PointF> {
            val coeffX: Float = (dw.toFloat() / dh.toFloat())
            val coeffY: Float = (dh.toFloat() / dw.toFloat())
            var x: Float = coeffX * box.start.x
            var y: Float = coeffY * box.start.y
            val newStart = PointF(x, y)

            x = coeffX * box.end.x
            y = coeffY * box.end.y
            val newEnd = PointF(x, y)

            return newStart to newEnd
        }

        return box.apply {
            forEach {
                val newPoints: Pair<PointF, PointF> = toNewDimension(it)
                it.start = newPoints.first
                it.end = newPoints.second
            }
        }
    }

    private fun updateCurrentBox(current: PointF) {
        currentBox?.let {
            it.end = current
            invalidate()
        }
    }

}