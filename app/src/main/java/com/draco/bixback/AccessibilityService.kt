package com.draco.bixback

import android.accessibilityservice.AccessibilityService
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

    val bixPress = "BixbyEventHandler: :Ph::task :: handleKeyEvent :: isConsumed false"
    val logcatCommand = "logcat -s BixbyEventHandler -d -t 100"

    private val minSpace = 500
    private val timerMs: Long = 100
    private var lastOccurrence: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        getBixbyLogs()
    }

    override fun onServiceConnected() {
        Log.v(TAG, "onServiceConnected")
        getBixbyLogs()
    }


    override fun onInterrupt() {
        Log.v(TAG, "onInterrupt")
    }

    private fun getBixbyLogs() {
        try {
            val x = run(logcatCommand)
            if (Calendar.getInstance().timeInMillis - lastOccurrence >= minSpace) {
                if (x.contains(bixPress)) {
                    bixPressFun()
                    lastOccurrence = Calendar.getInstance().timeInMillis
                }
            }
            val handler = Handler()
            handler.postDelayed( {
                getBixbyLogs()
            }, timerMs)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun bixPressFun() {
        GlobalTask(this).execute()
        //Log.d("ButtonPressed", "The Bixby Button was pressed")
    }

    class GlobalTask internal constructor(context: AccessibilityService) : AsyncTask<Void, Void, Void>() {
        private val activityReference: WeakReference<AccessibilityService> = WeakReference(context)
        override fun doInBackground(vararg voids: Void): Void? {
            try {
                Thread.sleep(50)
            } catch (e: InterruptedException) { }
            activityReference.get()?.performGlobalAction(GLOBAL_ACTION_BACK)
            return null
        }
    }

    companion object {
        private val TAG = AccessibilityService::class.java.simpleName
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
