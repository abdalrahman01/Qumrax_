package xyz.abdalrahman.qumrax2

import android.content.Context

import android.graphics.Canvas
import android.graphics.Paint

import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MapOutResults: View {
    constructor(context: Context,attributeSet: AttributeSet): super(context, attributeSet)
    constructor(context: Context): super(context)
    private val THRESHOLD = .45F // minst 60 % en prediction måste ha för att visas på skärmen.
    var elements = mutableListOf<Classifier.Recognition?>()
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, android.R.color.holo_red_dark)
        strokeWidth = 10f
    }
    private val text = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = ContextCompat.getColor(context, android.R.color.black)
        textSize = 60f
    }
    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            elements.forEach {
                if (it?.getScore()!! > THRESHOLD) {
                    val location = it.getLocation()
                    drawRect(location, paint )
                    drawText(it.getTitle_() + " " + it.getRoundedScore(), location.left, location.top + 50, text)
                }
            }
        }
    }
    fun drawOnScreen(element: List<Classifier.Recognition?>? , window: PreviewView){

        elements.clear()
        element!!.forEach {
            val scaledLocation = mapPredictionsCoordinateToView(it?.getLocation()!!, window)
            it.setLocation(scaledLocation)
        }
        elements.addAll(element)
        mapOut.invalidate()

    }
    private fun mapPredictionsCoordinateToView(location: RectF, window: PreviewView): RectF {

        // 300 * 300  is the image were fed into the tflite model
        val toWidth = window.width
        val toHeight = window.height

        val newLocation = RectF(
            location.left.times(toWidth),
            location.top.times(toHeight),
            location.right.times(toWidth),
            location.bottom.times(toHeight)
        )

        return newLocation

    }
}

