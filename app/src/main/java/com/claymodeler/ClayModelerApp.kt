package com.claymodeler

import android.app.Application
import org.acra.config.dialog
import org.acra.data.StringFormat
import org.acra.ktx.initAcra

class ClayModelerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        initAcra {
            reportFormat = StringFormat.KEY_VALUE_LIST
            
            dialog {
                title = "ClayModeler Crashed"
                text = "The app has crashed. You can help by sharing the crash report."
                commentPrompt = "What were you doing when the crash occurred?"
                resIcon = android.R.drawable.ic_dialog_alert
            }
        }
    }
}
