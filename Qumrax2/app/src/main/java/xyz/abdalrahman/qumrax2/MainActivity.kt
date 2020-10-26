package xyz.abdalrahman.qumrax2

import android.annotation.SuppressLint
import android.graphics.Bitmap


import android.os.Bundle
import android.text.Layout
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import org.tensorflow.lite.support.common.FileUtil
import xyz.abdalrahman.qumrax2.ml.SsdMobilenetV11Metadata1
import java.io.File

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null
    private val context = this@MainActivity
    private lateinit var cameraExecutor: ExecutorService
    private val labelsPath by lazy { FileUtil.loadLabels(this, "labels.txt") }
    // Select back camera as a default
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Permissions.hasCameraPermission(context)) {
            openCamera()
        } else {
            Permissions.requestCameraPermission(context)
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    @SuppressLint("UnsafeExperimentalUsageError")
    private fun openCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val ssdMobilenetV11Metadata1: SsdMobilenetV11Metadata1 = SsdMobilenetV11Metadata1.newInstance(context)
        val model = Outputss(ssdMobilenetV11Metadata1, labelsPath)
        val converter = YuvToRgbConverter(context)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                it.setSurfaceProvider(viewx.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
                        val bitmap: Bitmap = Bitmap.createBitmap(
                            imageProxy.width,
                            imageProxy.height,
                            Bitmap.Config.ARGB_8888
                        )
                        imageProxy.let {
                            converter.yuvToRgb(imageProxy.image!!, bitmap)
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, false)
                            val process = model.predict(scaledBitmap)
                            if (process != null) {
                                mapOut.drawOnScreen(process, viewx)
                            }
                            imageProxy.close()
                        }
                    })
                }
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("TAG", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (Permissions.shouldShowRequestPermissionRationale(this)) {
            if (Permissions.hasCameraPermission(this)) {
                openCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
}




