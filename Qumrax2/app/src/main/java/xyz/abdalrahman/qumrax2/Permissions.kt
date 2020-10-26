package xyz.abdalrahman.qumrax2

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object Permissions {
    private val CAMERA_PERMISSION = Manifest.permission.CAMERA
    private val CAMERA_PERMEISSION_CODE = 0

    // Checks if Camera permission is allowed
    // Kollar om Camera är tillåten för appen
    fun hasCameraPermission(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            CAMERA_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // gör en Begär för Camera Permissions
    fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(CAMERA_PERMISSION),
            CAMERA_PERMEISSION_CODE
        )
    }

    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, CAMERA_PERMISSION)

    }

    // öppnar inställningar åt användare ifall är Camera inte tillåten
    fun launchPermissionSettings(activity: Activity) {
        activity.startActivity(
            Intent().also { intent ->
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = Uri.fromParts(
                    "package",
                    activity.packageName,
                    null
                )

            }
        )
    }

}