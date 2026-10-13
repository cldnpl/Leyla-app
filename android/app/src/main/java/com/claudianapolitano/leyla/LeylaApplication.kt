package com.claudianapolitano.leyla

import android.app.Application
import com.claudianapolitano.leyla.core.AppPrefs
import com.claudianapolitano.leyla.core.LanguageManager
import com.claudianapolitano.leyla.core.PartnerPrefs
import com.claudianapolitano.leyla.core.TokenStore
import com.claudianapolitano.leyla.core.WidgetStore

class LeylaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenStore.init(this)
        PartnerPrefs.init(this)
        AppPrefs.init(this)
        LanguageManager.init(this)
        WidgetStore.init(this)
    }
}
