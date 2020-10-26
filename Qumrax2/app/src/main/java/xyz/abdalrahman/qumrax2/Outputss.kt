package xyz.abdalrahman.qumrax2

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.metadata.MetadataExtractor
import org.tensorflow.lite.support.metadata.schema.TensorMetadata
import xyz.abdalrahman.qumrax2.ml.SsdMobilenetV11Metadata1
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.*

class Outputss(
    private val tflitemodel: SsdMobilenetV11Metadata1,
    private val labels: List<String>
): Classifier{
    private val NUMOBJS = 8 // object that will be detected
    // https://www.tensorflow.org/lite/models/object_detection/overview#output
/*
    Locations =>
    Multidimensional array of [10][4] floating point values between 0 and 1,
    the inner arrays representing bounding boxes in
    the form [top, left, bottom, right]
*/
    private lateinit var Locations: Array<FloatArray>
/*
    Classes =>
    Array of 10 integers (output as floating point values)
    each indicating the index of a class label from the labels file
*/
    private lateinit var Classes: FloatArray
/*
    Scores =>
    Array of 10 floating point values between 0 and 1 representing
    probability that a class was detected
*/
    private lateinit var Scores: FloatArray

    fun predict(image: Bitmap):  List<Classifier.Recognition?>?{
        Locations = Array(NUMOBJS){FloatArray(4)}
        Classes = FloatArray(NUMOBJS)
        Scores = FloatArray(NUMOBJS)
        // convert Bitmap to tensorImage
        val tensorImage = TensorImage.fromBitmap(image)
        // do the predictions   python "(model.predict() )"
        val outputs = tflitemodel.process(tensorImage)
        val locations = outputs.locationsAsTensorBuffer.floatArray
        val classes = outputs.classesAsTensorBuffer.floatArray
        val scores = outputs.scoresAsTensorBuffer.floatArray
        val recognitions = ArrayList<Classifier.Recognition?>(NUMOBJS)
        // reshape 1D array to 2D array :: from array[40] to array[10][4]
        val rect = mutableListOf<Float>()
        val objectDetected = mutableListOf<FloatArray>()
        for (it in locations){
            if (rect.size < 4) {
                rect.add(it)
            }
            else{
                objectDetected.add(rect.toFloatArray())
                rect.clear()
            }
        }
        Locations = objectDetected.toTypedArray()
        Classes = classes
        Scores = scores
        for (i in 0 until NUMOBJS) {
            val detection = RectF(
                Locations[i][1], // left
                Locations[i][0], // top
                Locations[i][3], // right
                Locations[i][2]  // bottom
            )
            val label: String = labels[1 + Classes[i].toInt()]
            recognitions.add(
                Classifier.Recognition(
                    i.toString(),
                    label,
                    Scores[i],
                    detection
                )
            )
        }
        return recognitions
}
}

