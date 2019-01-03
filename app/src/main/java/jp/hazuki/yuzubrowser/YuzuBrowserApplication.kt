/*
 * Copyright (C) 2017-2019 Hazuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.hazuki.yuzubrowser

import android.app.Activity
import android.app.Application
import android.content.ContentProvider
import android.content.Context
import android.webkit.WebView
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.core.CrashlyticsCore
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasContentProviderInjector
import io.fabric.sdk.android.Fabric
import jp.hazuki.yuzubrowser.core.utility.log.Logger
import jp.hazuki.yuzubrowser.di.DaggerAppComponent
import jp.hazuki.yuzubrowser.legacy.BrowserApplication
import jp.hazuki.yuzubrowser.legacy.settings.data.AppData
import jp.hazuki.yuzubrowser.legacy.utils.AppUtils
import jp.hazuki.yuzubrowser.legacy.utils.CrashlyticsUtils
import jp.hazuki.yuzubrowser.provider.ProviderManager
import javax.inject.Inject

class YuzuBrowserApplication : Application(), BrowserApplication, HasActivityInjector, HasContentProviderInjector {

    override val applicationId = BuildConfig.APPLICATION_ID
    override val permissionAppSignature = PERMISSION_MYAPP_SIGNATURE
    override val browserState = BrowserStateImpl()
    override val providerManager = ProviderManager()
    override val context: Context
        get() = this
    @Inject
    lateinit var dispatchingAndroidActivityInjector: DispatchingAndroidInjector<Activity>
    @Inject
    lateinit var dispatchingAndroidContentProviderInjector: DispatchingAndroidInjector<ContentProvider>

    override fun onCreate() {
        super.onCreate()
        val crashlytics = Crashlytics.Builder()
                .core(CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build()
        Fabric.with(this, crashlytics, Answers())
        CrashlyticsUtils.setChromeVersion(this)
        CrashlyticsUtils.setWebViewMode()
        AppUtils.registNotification(this)

        Logger.d(TAG, "onCreate()")
        browserState.isNeedLoad = false
        ErrorReportServer.initialize(this)
        AppData.load(this)
        ErrorReportServer.setDetailedLog(AppData.detailed_log.get())
        if (AppData.slow_rendering.get()) {
            WebView.enableSlowWholeDocumentDraw()
        }
        Logger.isDebug = BuildConfig.DEBUG
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        DaggerAppComponent.builder()
                .create(this)
                .inject(this)
    }

    override fun activityInjector(): AndroidInjector<Activity> {
        return dispatchingAndroidActivityInjector
    }

    override fun contentProviderInjector(): AndroidInjector<ContentProvider> {
        return dispatchingAndroidContentProviderInjector
    }

    companion object {
        private const val TAG = "YuzuBrowserApplication"
        const val PERMISSION_MYAPP_SIGNATURE = BuildConfig.APPLICATION_ID + ".permission.myapp.signature"
        lateinit var instance: YuzuBrowserApplication
    }
}
