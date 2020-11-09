package xyz.abdalrahman.qumrax2




import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem

import android.view.View.*

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
import java.lang.Thread.sleep
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null
    private val context = this@MainActivity
    private lateinit var cameraExecutor: ExecutorService
    private val labelsPath by lazy { FileUtil.loadLabels(this, "labels.txt") }
    // Select back camera as a default
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (isPortrait()){
            Toast.makeText(
                context,
                getString(R.string.best_option),
                Toast.LENGTH_LONG
            ).show()
        }
        grant_permission_btn.setOnClickListener {
            Permissions.launchPermissionSettings(context)
        }


        if (Permissions.hasCameraPermission(context)) {
            openCamera()
        } else {

            Permissions.requestCameraPermission(context)
            sleep(2000)
            if (!Permissions.hasCameraPermission(context)){
                // TODO(test)
                noPermission.visibility = VISIBLE
            } else {
                openCamera()
            }

        }
        cameraExecutor = Executors.newSingleThreadExecutor()

        min_accu_lbl.text = "${getString(R.string.min_accuracy)}: 45%"
        object_to_be_detected.text = "${getString(R.string.object_to_be_detected)}: 8"


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


        switch1.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked)
                hideSystemUI()
            else
                showSystemUI()
        }


    }

    override fun onResume() {
        super.onResume()
        if (Permissions.hasCameraPermission(context)) {
            noPermission.visibility = GONE
        } else {
            noPermission.visibility = VISIBLE
        }
        openCamera()
    }



    /* open the camera */
    @SuppressLint("UnsafeExperimentalUsageError")
    private fun openCamera() {
        //get the camera
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        //get the TFlite model
        val ssdMobilenetV11Metadata1: SsdMobilenetV11Metadata1 = SsdMobilenetV11Metadata1.newInstance(
            context
        )
        //run the model
        val model = Outputss(ssdMobilenetV11Metadata1, labelsPath)
        // get the yuv/bitmap converter
        val converter = YuvToRgbConverter(context)

        // start a listener
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

            // here is where the analysis well take place (tflite model results)
            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(cameraExecutor, {imageProxy ->

                        // create a placeholder for the image
                        val bitmap: Bitmap = Bitmap.createBitmap(
                            imageProxy.width,
                            imageProxy.height,
                            Bitmap.Config.ARGB_8888
                        )
                        imageProxy.let {

                            // convert the image to bitmap to run in Tflite model
                            converter.yuvToRgb(imageProxy.image!!, bitmap)
                            // scale the image to fit in the model
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, false)

                            //process the image
                            val process = model.predict(scaledBitmap)
                            if (process != null) {
                                // pass the value to custom view MapOutResults
                                // TODO(pass the viewx in constructor)
                                mapOut.drawOnScreen(process, viewx)
                            }
                            // close the frame to make room for next frame
                            imageProxy.close()
                        }
                    })
                }
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    context, cameraSelector, preview, imageCapture, imageAnalyzer
                )
            } catch (exc: Exception) { }
        }, ContextCompat.getMainExecutor(context))
    }

    /*
    when the app is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

   /* the result of permission handling, and what to do when permissions are granted or not*/
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        if (Permissions.shouldShowRequestPermissionRationale(context)) {
            if (Permissions.hasCameraPermission(context)) {
                openCamera()
            } else {
                Toast.makeText(
                    context,
                    getString(R.string.permission_not_granted),
                    Toast.LENGTH_LONG
                ).show()
                // TODO(test)
                noPermission.visibility = VISIBLE
            }
        }
    }

    /*Inflates the menu*/
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    /*it does action when an item is selected from the menu*/
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.about -> {
                openHelp()
                true
            }
            else -> true
        }
    }

    /*redirect to abdalrahman.xyz/#about page for help*/
    private fun openHelp() {
        val viewIntent = Intent(
            "android.intent.action.VIEW",
            Uri.parse("http://www.abdalrahman.xyz/#about")
        )
        startActivity(viewIntent)
    }

    /*Hide All UI Component including nav and status bar*/
    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or SYSTEM_UI_FLAG_LAYOUT_STABLE
                or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or SYSTEM_UI_FLAG_FULLSCREEN)
        sittings.visibility = GONE
    }

    /*Show All UI Component*/
    private fun showSystemUI() {

        window.decorView.systemUiVisibility = (SYSTEM_UI_FLAG_LAYOUT_STABLE
                or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        if (isPortrait())
            sittings.translationY = -100F
        if (!isPortrait())
            switch1.translationX = -100f
        sittings.visibility = VISIBLE

    }

    /*Checks if phone is in Portrait mode which is when the phone is vertical*/
    private fun isPortrait(): Boolean{
        // returns true if it is in portrait mode
        return (context.resources.configuration.orientation == 1)
    }
}




