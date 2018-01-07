package com.olacabs.olaplaystudio.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import com.olacabs.olaplaystudio.ui.base.MvpBaseActivity
import timber.log.Timber

/**
 * Created by saiki on 27-02-2016.
 */

abstract class BaseMediaActivity : MvpBaseActivity() {
    var service: MusicService? = null
    private var mBound = false

    protected abstract fun connectToSession(token: MediaSessionCompat.Token)
    protected abstract fun onMusicServiceConnected(service: MusicService)
    protected abstract fun onMusicServiceDisconnected()


    override fun onStart() {
        super.onStart()
        boundService()
    }

    override fun onStop() {
        super.onStop()
        unBoundService()
    }

    private val mConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName,
                                        service: IBinder) {
            val binder = service as MusicService.LocalBinder
            this@BaseMediaActivity.service = binder.service
            mBound = true
            Timber.d("Service connected")
            connectToSession(this@BaseMediaActivity.service!!.mSessionToken)
            onMusicServiceConnected(this@BaseMediaActivity.service!!)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    private fun unBoundService() {
        if (mBound) {
            unbindService(mConnection)
            mBound = false
            onMusicServiceDisconnected()
        }
    }

    private fun boundService() {
        val intent = Intent(this, MusicService::class.java)
        startService(intent)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }
}