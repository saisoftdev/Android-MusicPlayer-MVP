package com.olacabs.olaplaystudio.playback

import android.app.Service
import android.content.Intent
import android.os.*
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.olacabs.olaplaystudio.data.model.MediaDetail
import com.olacabs.olaplaystudio.utils.clearAndAddAll
import com.olacabs.olaplaystudio.utils.regOnce
import com.olacabs.olaplaystudio.utils.unRegOnce
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Created by sai on 16/12/17.
 */
class MusicService : Service(), PlaybackManager.PlaybackServiceCallback {
    //NotNull
    private val mDelayedStopHandler = DelayedStopHandler(this)
    private val eventBus = EventBus.getDefault()

    //Lazy
    private val mSession: MediaSessionCompat by lazy {
        MediaSessionCompat(this, "MusicService")
    }
    val mSessionToken: MediaSessionCompat.Token by lazy {
        mSession.sessionToken
    }
    private val mTransportControls: MediaControllerCompat.TransportControls by lazy {
        MediaControllerCompat(this, mSession).transportControls
    }
    private val mMediaNotificationManager: MediaNotificationManager by lazy {
        try {
            MediaNotificationManager(this)
        } catch (e: RemoteException) {
            throw IllegalStateException("Could not create a MediaNotificationManager", e)
        }
    }
    val mPlaybackManager: PlaybackManager by lazy {
        PlaybackManager(mPlayback = LocalPlayback(this), mServiceCallback = this)
    }

    override fun onCreate() {
        Timber.d("onCreate")
        super.onCreate()

        //Init MediaSessionCompat and TransportControls
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mSession.setCallback(mPlaybackManager.mMediaSessionCallback)

        //Reg This call to EventBus
        eventBus.regOnce(this)
    }

    @Subscribe(sticky = true)
    fun onGetAllMediaEventResponse(event: PublishMediaLoadEvent) {
        mPlaybackManager.listOfMediaDetail.clearAndAddAll(event.list)
        eventBus.removeStickyEvent(event)
    }

    override fun onPlaybackStart() {
        Timber.d("onPlaybackStart ")
        mSession.isActive = true
        mDelayedStopHandler.removeCallbacksAndMessages(null)
        // The service needs to continue running even after the bound client (usually a
        // MediaController) disconnects, otherwise the music playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.
        startService(Intent(applicationContext, MusicService::class.java))
    }

    override fun onNotificationRequired() {
        Timber.d("onNotificationRequired")
        mMediaNotificationManager.startNotification()
    }

    override fun onPlaybackStop() {
        Timber.d("onPlaybackStop ")
        mSession.isActive = false
        resetDelayHandler()
        stopForeground(true)
    }

    override fun onPlaybackStateUpdated(newState: PlaybackStateCompat) {
        Timber.d("onPlaybackStateUpdated newState = %s  ", newState.state)
        mSession.setPlaybackState(newState)
    }

    override fun updateMetaData(media: MediaDetail) {
        Timber.d("updateMetaData")
        mSession.setMetadata(MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, media.artists)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, media.song)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, media.cover_image)
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, media.index.toLong())
                .build())
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        // Service is being killed, so make sure we release our resources
        mPlaybackManager.handleStopRequest(null)
        mMediaNotificationManager.stopNotification()

        mDelayedStopHandler.removeCallbacksAndMessages(null)
        mSession.release()

        eventBus.unRegOnce(this)
    }

    override fun onStartCommand(startIntent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand ")
        startIntent?.let { intent ->
            val action = intent.action
            val command = intent.getStringExtra(CMD_NAME)

            if (ACTION_CMD == action)
                when (command) {
                    CMD_STOP -> mPlaybackManager.handleStopRequest(null)
                    else -> ""

                }
            else
                MediaButtonReceiver.handleIntent(mSession, startIntent)


        }
        resetDelayHandler()
        return Service.START_STICKY
    }

    private fun resetDelayHandler() {
        // Reset the delay handler to enqueue a message to stop the service if
        // nothing is playing.
        mDelayedStopHandler.removeCallbacksAndMessages(null)
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY.toLong())
    }

    override fun onPlaybackPause() {
        Timber.d("onPlaybackPause ")
        resetDelayHandler()
        stopForeground(true)
    }

    private class DelayedStopHandler internal constructor(service: MusicService) : Handler() {
        private val mWeakReference: WeakReference<MusicService> = WeakReference(service)

        override fun handleMessage(msg: Message) {
            val service = mWeakReference.get()
            if (service != null) {
                if (service.mPlaybackManager.mPlayback.isPlaying) {
                    Timber.d("Ignoring delayed stop since the media player is in use.")
                    return
                }
                Timber.d("Stopping service with delay handler.")
                //                service.saveLastPlayedInfo();
                service.stopSelf()
            }
        }
    }

    companion object {
        val ACTION_CMD = "com.olacabs.olaplaystudio.ACTION_CMD"
        val CMD_PAUSE = "CMD_PAUSE"
        val CMD_NAME = "CMD_NAME"
        val CMD_STOP = "CMD_STOP"
        private val STOP_DELAY = 80000//1.30min
    }

    private val mBinder = LocalBinder()

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    inner class LocalBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }
}

data class PublishMediaLoadEvent(val list: List<MediaDetail>)