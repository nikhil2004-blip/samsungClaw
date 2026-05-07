package com.example.signal

import android.app.Application
import com.example.signal.worker.WorkManagerHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SignalApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Run a background sweep every 15 min so scheduled tasks move to
        // MISSED even when the main board UI is not open.
        WorkManagerHelper.scheduleMissedSweep(this)
        WorkManagerHelper.scheduleOverdueScan(this)
    }
}
