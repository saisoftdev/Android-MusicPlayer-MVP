package com.olacabs.olaplaystudio

import android.app.Application
import android.content.Context
import android.widget.Toast
import com.chibatching.kotpref.Kotpref
import com.olacabs.olaplaystudio.di.component.ApplicationComponent
import com.olacabs.olaplaystudio.di.component.DaggerApplicationComponent
import com.olacabs.olaplaystudio.di.module.ApplicationModule
import com.olacabs.olaplaystudio.utils.regOnce
import net.danlew.android.joda.JodaTimeAndroid
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import timber.log.Timber

/**
 * Created by sai on 16/12/17.
 */

open class OlaApplication : Application() {

    private var toast: Toast? = null
    private var mApplicationComponent: ApplicationComponent? = null

    var component: ApplicationComponent
        get() {
            if (mApplicationComponent == null) {
                mApplicationComponent = DaggerApplicationComponent.builder()
                        .applicationModule(ApplicationModule(this))
                        .build()
            }
            return mApplicationComponent as ApplicationComponent
        }
        set(applicationComponent) {
            mApplicationComponent = applicationComponent
        }

    override fun onCreate() {
        super.onCreate()
        JodaTimeAndroid.init(this);
        Kotpref.init(this)
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        EventBus.getDefault().regOnce(this)
    }

    @Subscribe
    fun onShowToastEvent(event: ShowToastEvent) {
        toast?.cancel()
        toast = Toast.makeText(this, event.message, Toast.LENGTH_SHORT)
                .apply { show() }
    }

    companion object {
        class ShowToastEvent(val message: String)

        operator fun get(context: Context): OlaApplication {
            return context.applicationContext as OlaApplication
        }
    }
}