package com.claudianapolitano.leyla

import android.app.Application
import com.claudianapolitano.leyla.core.AppPrefs
import com.claudianapolitano.leyla.core.LanguageManager
import com.claudianapolitano.leyla.core.PartnerPrefs
import com.claudianapolitano.leyla.core.TokenStore
import com.claudianapolitano.leyla.core.WidgetStore
import com.claudianapolitano.leyla.feature.cycle.CyclePrefs

class LeylaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenStore.init(this)
        PartnerPrefs.init(this)
        AppPrefs.init(this)
        CyclePrefs.init(this)
        LanguageManager.init(this)
        WidgetStore.init(this)
    }
}
