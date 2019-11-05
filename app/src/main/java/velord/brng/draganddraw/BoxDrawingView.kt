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
        val (boxStartPoint, anglePoint) = initTouchPointOnActionEvent(event)

        var action = ""
        when (event!!.action) {
            MotionEvent.ACTION_DOWN -> {
                action = "ACTION_DOWN"
                // Reset drawing state
                actionDown(boxStartPoint)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                action = "ACTION_DOWN"
                actionPointerDown(anglePoint)
            }
            MotionEvent.ACTION_MOVE -> {
                action = "ACTION_MOVE"
                actionMove(boxStartPoint, anglePoint)
            }
            MotionEvent.ACTION_UP -> {
                action = "ACTION_UP"
                actionUp(boxStartPoint)
                actionUpContentDescription(event)
            }
            MotionEvent.ACTION_CANCEL -> {
                action = "ACTION_CANCEL"
                actionCancel()
            }
        }

        Log.i(TAG,
            "$action at x=${boxStartPoint?.x}, y=${boxStartPoint?.y}")

        return true
    }

    override fun onDraw(canvas: Canvas?) {
        //fill the background
        canvas!!.drawPaint(backgroundPaint)

        boxen.forEach { box ->
            val angle = box.angle
            val px = (box.start.x + box.end.x) / 2
            val py = (box.start.y + box.end.y) / 2
            canvas.apply {
                save()
                rotate(angle, px, py)
                drawRect(
                    box.left, box.top,
                    box.right, box.bottom,
                    boxPaint
                )
                restore()
            }
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

    private fun transformToPortraitOrLandscape(
        box: MutableList<Box>): MutableList<Box> {
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

    private fun updateCurrentBoxEnd(current: PointF) {
        currentBox?.let {
            it.end = current
            invalidate()
        }
    }

    private fun updateCurrentBoxAngle(anglePoint: PointF) {
        currentBox?.let {
            val boxOrigin = it.start
            val pointerOrigin = it.anglePointer
            val angle2 =
                Math.atan2(
                    (anglePoint.y - boxOrigin.y).toDouble(),
                    (anglePoint.x - boxOrigin.x).toDouble())
                    .toFloat()
            val angle1 =
                Math.atan2(
                    (pointerOrigin.y - boxOrigin.y).toDouble(),
                    (pointerOrigin.x - boxOrigin.x).toDouble())
                    .toFloat()
            var calculatedAngle =
                Math.toDegrees((angle2 - angle1).toDouble())
                    .toFloat()
            if (calculatedAngle < 0) calculatedAngle += 360f
            it.angle = calculatedAngle

            Log.d(TAG, "Set Box Angle $calculatedAngle");
            invalidate()
        }
    }

    private fun actionCancel() {
        currentBox = null
    }

    private fun actionUp(boxStartPoint: PointF?) {
        boxStartPoint?.let {
            updateCurrentBoxEnd(boxStartPoint)
        }
        currentBox = null
    }

    private fun actionMove(boxStartPoint: PointF?,
                           anglePoint: PointF?) {
        boxStartPoint?.let {
            updateCurrentBoxEnd(boxStartPoint)
        }
        anglePoint?.let {
            updateCurrentBoxAngle(anglePoint)
        }
    }

    private fun actionPointerDown(anglePoint: PointF?) {
        currentBox?.let {box ->
            anglePoint?.let {
                box.anglePointer = anglePoint
            }
        }
    }

    private fun actionDown(boxStartPoint: PointF?) {
        boxStartPoint?.let {
            currentBox = Box(boxStartPoint).also {
                boxen.add(it)
            }
        }
    }

    private fun initTouchPointOnActionEvent(
        event: MotionEvent?): Pair<PointF?, PointF?> {
        var touchPoint: PointF? = null
        var touchPoint2: PointF? = null
        for (i in 0 until event!!.pointerCount) {
            if (event.getPointerId(i) == 0)
                touchPoint = PointF(event.x, event.y)
            if (event.getPointerId(i) == 1)
                touchPoint2 = PointF(event.x, event.y)
        }
        return touchPoint to touchPoint2
    }

    private fun actionUpContentDescription(event: MotionEvent?) {
        sendAccessibilityEvent(event!!.action)
        val coveredPercent = calculateCoveredView()
        this.contentDescription =
            "View have been covered on $coveredPercent" +
                    " percent by ${boxen.size} boxes"
    }
    //calculation coverage view in percent by boxes
    private fun calculateCoveredView(): Int {
        val metrics = this.resources.displayMetrics
        val dh = metrics.heightPixels
        val dw = metrics.widthPixels
        //all point in view
        val viewPoints = dh * dw
        //how many point coverage all boxes
        // todo() must be implemented
        val boxesPoints = 1
        //percentage how many boxes coverage all view
        return (boxesPoints / (viewPoints.toFloat() / 100)).toInt()
    }

}