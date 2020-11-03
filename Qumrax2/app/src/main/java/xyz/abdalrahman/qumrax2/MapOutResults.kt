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


class MapOutResults: View {
    constructor(context: Context,attributeSet: AttributeSet): super(context, attributeSet)
    constructor(context: Context): super(context)
    var THRESHOLD = .45f // minsta procent en prediction måste ha för att visas på skärmen.
    var NUM_OBJECTS_DETECTED: Int = 8
    private var elements = mutableListOf<Classifier.Recognition?>()
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, android.R.color.white)
        strokeWidth = 10f
    }
    private val text = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = ContextCompat.getColor(context, android.R.color.black)
        textSize = 60f
    }
    private val textBackgroundColorSpan = Paint().apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, android.R.color.holo_red_dark)
    }
    override fun onDraw(canvas: Canvas?) {
        if (elements.isNotEmpty())
        for (x in 0 until NUM_OBJECTS_DETECTED){
            val value =  elements[x]
            if (value?.getScore()!! > THRESHOLD){
                val location = value.getLocation()
                val label = "${value.getTitle_()} ${value.getScore().times(100).toInt()}%"

                canvas?.apply {
                    drawRect(location, paint)
                    drawRect(location.left,location.top,location.right, location.top.minus(50),textBackgroundColorSpan)
                    drawText(label,location.left, location.top , text)
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

