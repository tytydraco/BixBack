package com.draco.bixback

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.AsyncTask
import android.os.Handler
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.util.*

class AccessibilityService : AccessibilityService() {

    private val logcatCommand = "logcat -s WindowManager -d -t 10"
    private val bixLogKeyword = "interceptKeyTi s_result = -1"
    private val bixPostLogKeyword = "interceptKeyTq s_result=1"
    private val volumeExclude = "interceptKeyTq s_result=8"
    private val screenshotExclude = "Destroying surface"
    private val screenshotExclude2 = "Relative Window"
    private val screenshotExclude3 = "finishDrawingLocked"

    private val minSpace = 300 // minimum time before next press can be registered
    private val timerMs: Long = 100 // how many ms to scan for the most recent logs
    private var lastOccurrence: Long = 0 // keep track of the time since the last press occurred (for minSpace)

    private var torchState = false

    override fun onAccessibilityEvent(event: AccessibilityEvent) { getBixbyLogs() }
    override fun onServiceConnected() { getBixbyLogs() }
    override fun onInterrupt() {}

    private fun bixPressFun() {
        val prefs = getSharedPreferences("bixBackPrefs", Context.MODE_PRIVATE)
        val action = prefs.getString("action", "back")

        if (action == "flash") {
            toggleTorch()
        } else {
            GlobalTask(this, action).execute()
        }
    }

    private fun toggleTorch() {
        val mCameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        if (mCameraManager.getCameraCharacteristics(mCameraManager.cameraIdList[0]).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
            if (torchState) {
                mCameraManager.setTorchMode(mCameraManager.cameraIdList[0], false)
            } else {
                mCameraManager.setTorchMode(mCameraManager.cameraIdList[0], true)
            }
            torchState = !torchState
        }
    }

    private fun getBixbyLogs() {
        // Make sure keypress doesn't happen too soon
        if (Calendar.getInstance().timeInMillis - lastOccurrence >= minSpace) {
            val bixbyLogCommand = run(logcatCommand)
            if (bixbyLogCommand.contains(bixLogKeyword) && bixbyLogCommand.contains(bixPostLogKeyword) &&
                    !bixbyLogCommand.contains(volumeExclude) &&
                    !bixbyLogCommand.contains(screenshotExclude) &&
                    !bixbyLogCommand.contains(screenshotExclude2) &&
                    !bixbyLogCommand.contains(screenshotExclude3)) {
                bixPressFun()
                lastOccurrence = Calendar.getInstance().timeInMillis
            }
        }

        // Repeat scan indefinitely
        val handler = Handler()
        handler.postDelayed( {
            getBixbyLogs()
        }, timerMs)
    }

    class GlobalTask internal constructor(context: AccessibilityService, action: String) : AsyncTask<Void, Void, Void>() {
        private val activityReference: WeakReference<AccessibilityService> = WeakReference(context)
        private val orAction = action

        override fun doInBackground(vararg voids: Void): Void? {
            try {
                Thread.sleep(50)
            } catch (e: InterruptedException) {}

            var globalAction: Int = GLOBAL_ACTION_BACK
            when (orAction) {
                "back" -> globalAction = GLOBAL_ACTION_BACK
                "home" -> globalAction = GLOBAL_ACTION_HOME
                "recent" -> globalAction = GLOBAL_ACTION_RECENTS
                "notifications" -> globalAction = GLOBAL_ACTION_NOTIFICATIONS
                "quick_settings" -> globalAction = GLOBAL_ACTION_QUICK_SETTINGS
                "power_dialog" -> globalAction = GLOBAL_ACTION_POWER_DIALOG
                "toggle_split_screen" -> globalAction = GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN
            }
            activityReference.get()?.performGlobalAction(globalAction)
            return null
        }
    }

    private fun run(command: String): String {
        try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuffer()
            var line: String? = null
            while ({ line = reader.readLine(); line }() != null) {
                output.append(line)
            }
            reader.close()
            process.waitFor()
            return output.toString()
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
    }
}
