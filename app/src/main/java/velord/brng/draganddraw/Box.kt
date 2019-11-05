package velord.brng.draganddraw

import android.graphics.PointF
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Box(var start: PointF,
               var end: PointF = start,
               var angle: Float = 0.0F) : Parcelable {


    var anglePointer: PointF = PointF(0.0F, 0.0F)

    val left: Float
        get() = Math.min(start.x, end.x)

    val right: Float
        get() = Math.max(start.x, end.x)

    val top: Float
        get() = Math.min(start.y, end.y)

    val bottom: Float
        get() = Math.max(start.y, end.y)
}