package com.olacabs.olaplaystudio.ui.base

import android.os.Bundle
import android.support.v4.util.LongSparseArray
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.olacabs.olaplaystudio.OlaApplication
import com.olacabs.olaplaystudio.di.component.ActivityComponent
import com.olacabs.olaplaystudio.di.component.ConfigPersistentComponent
import com.olacabs.olaplaystudio.di.component.DaggerConfigPersistentComponent
import com.olacabs.olaplaystudio.di.module.ActivityModule
import timber.log.Timber
import java.util.concurrent.atomic.AtomicLong

/**
 * Abstract activity that every other Activity in this application must implement. It provides the
 * following functionality:
 * - Handles creation of Dagger components and makes sure that instances of
 * ConfigPersistentComponent are kept across configuration changes.
 * - Set up and handles a GoogleApiClient instance that can be used to access the Google sign in
 * api.
 * - Handles signing out when an authentication error event is received.
 */
abstract class MvpBaseActivity : AppCompatActivity() {

    private var mActivityComponent: ActivityComponent? = null
    private var mActivityId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)
        // Create the ActivityComponent and reuses cached ConfigPersistentComponent if this is
        // being called after a configuration change.
        mActivityId = savedInstanceState?.getLong(KEY_ACTIVITY_ID) ?: NEXT_ID.getAndIncrement()
        val configPersistentComponent: ConfigPersistentComponent
        if (sComponentsArray.get(mActivityId) == null) {
            Timber.i("Creating new ConfigPersistentComponent id=%d", mActivityId)
            configPersistentComponent = DaggerConfigPersistentComponent.builder()
                    .applicationComponent(OlaApplication[this].component)
                    .build()
            sComponentsArray.put(mActivityId, configPersistentComponent)
        } else {
            Timber.i("Reusing ConfigPersistentComponent id=%d", mActivityId)
            configPersistentComponent = sComponentsArray.get(mActivityId)
        }
        mActivityComponent = configPersistentComponent.activityComponent(ActivityModule(this))
        mActivityComponent?.inject(this)
    }

    abstract val layout: Int

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_ACTIVITY_ID, mActivityId)
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) {
            Timber.i("Clearing ConfigPersistentComponent id=%d", mActivityId)
            sComponentsArray.remove(mActivityId)
        }
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    fun activityComponent(): ActivityComponent = mActivityComponent as ActivityComponent

    companion object {

        private val KEY_ACTIVITY_ID = "KEY_ACTIVITY_ID"
        private val NEXT_ID = AtomicLong(0)
        private val sComponentsArray = LongSparseArray<ConfigPersistentComponent>()
    }

}
