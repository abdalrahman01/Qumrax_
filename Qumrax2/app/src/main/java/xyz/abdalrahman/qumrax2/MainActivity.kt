package xyz.abdalrahman.qumrax2




import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import org.tensorflow.lite.support.common.FileUtil
import xyz.abdalrahman.qumrax2.ml.SsdMobilenetV11Metadata1
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

        if (context.resources.configuration.orientation == 1){
            Toast.makeText(
                this,
                getString(R.string.best_option),
                Toast.LENGTH_LONG
            ).show()
        }

        if (Permissions.hasCameraPermission(context)) {
            openCamera()
        } else {
            Permissions.requestCameraPermission(context)
        }
        cameraExecutor = Executors.newSingleThreadExecutor()


        min_accu_scroll.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {

            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mapOut.THRESHOLD = progress/100.0f
                min_accu_lbl.text = "${getString(R.string.min_accuracy)}: ${progress}%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })
        object_to_be_detected_scroll.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                mapOut.NUM_OBJECTS_DETECTED = progress
                object_to_be_detected.text =
                    getString(R.string.object_to_be_detected) + ": " + progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        } )

    }
    @SuppressLint("UnsafeExperimentalUsageError")
    private fun openCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val ssdMobilenetV11Metadata1: SsdMobilenetV11Metadata1 = SsdMobilenetV11Metadata1.newInstance(
            context
        )
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
                    imageAnalysis.setAnalyzer(cameraExecutor, {imageProxy ->
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

            }
        }, ContextCompat.getMainExecutor(this))
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.about -> {
                openHelp()
                true
            }
            else -> true
        }
    }

    private fun openHelp() {
        val viewIntent = Intent(
            "android.intent.action.VIEW",
            Uri.parse("http://www.abdalrahman.xyz/#about")
        )
        startActivity(viewIntent)
    }
}




