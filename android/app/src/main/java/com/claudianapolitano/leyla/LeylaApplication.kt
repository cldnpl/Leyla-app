package com.claudianapolitano.leyla

import android.app.Application
import com.claudianapolitano.leyla.core.AppPrefs
import com.claudianapolitano.leyla.core.PartnerPrefs
import com.claudianapolitano.leyla.core.TokenStore

class LeylaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenStore.init(this)
        PartnerPrefs.init(this)
        AppPrefs.init(this)
    }
}
