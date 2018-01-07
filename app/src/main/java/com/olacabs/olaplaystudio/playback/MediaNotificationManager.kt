/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.olacabs.olaplaystudio.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.RemoteException
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.jakewharton.picasso.OkHttp3Downloader
import com.olacabs.olaplaystudio.R
import com.olacabs.olaplaystudio.playback.utils.ResourceHelper
import com.olacabs.olaplaystudio.ui.library.LibraryActivity
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import timber.log.Timber
import javax.inject.Inject


/**
 * Keeps track of a notification and updates it automatically for a given
 * MediaSession. Maintaining a visible notification (usually) guarantees that the music service
 * won't be killed during playback.
 */
class MediaNotificationManager(private val mService: MusicService) : BroadcastReceiver() {
    private var mSessionToken: MediaSessionCompat.Token? = null
    private var mController: MediaControllerCompat? = null
    private var mTransportControls: MediaControllerCompat.TransportControls? = null

    private var mPlaybackState: PlaybackStateCompat? = null
    private var mMetadata: MediaMetadataCompat? = null

    private var mNotificationManager: NotificationManager? = null

    private val mPlayIntent: PendingIntent
    private val mPauseIntent: PendingIntent
    private val mPreviousIntent: PendingIntent
    private val mNextIntent: PendingIntent
    private val mStopIntent: PendingIntent

    private val mNotificationColor: Int

    private var mStarted = false

    private val mCb = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            mPlaybackState = state
            Timber.d("Received new playback state %s", state)
            if (state.state == PlaybackStateCompat.STATE_STOPPED || state.state == PlaybackStateCompat.STATE_NONE) {
                stopNotification()
            } else {
                val notification = createNotification()
                if (notification != null) {
                    mNotificationManager?.notify(NOTIFICATION_ID, notification)
                }
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            mMetadata = metadata
            Timber.d("Received new metadata %s", metadata)
            val notification = createNotification()
            if (notification != null) {
                mNotificationManager?.notify(NOTIFICATION_ID, notification)
            }
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            Timber.d("Session was destroyed, resetting to the new session token")
            try {
                updateSessionToken()
            } catch (e: RemoteException) {
                Timber.e(e, "could not connect media controller")
            }
        }
    }

    private var loadtarget: Target? = null

    init {
        updateSessionToken()

        mNotificationColor = ResourceHelper.getThemeColor(mService, R.attr.colorPrimary,
                Color.DKGRAY)

        mNotificationManager = mService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val pkg = mService.packageName
        mPauseIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)
        mPlayIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)
        mPreviousIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)
        mNextIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)
        mStopIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                Intent(ACTION_STOP).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        mNotificationManager?.cancelAll()
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before [.stopNotification] is called.
     */
    fun startNotification() {
        if (!mStarted) {
            mMetadata = mController!!.metadata
            mPlaybackState = mController!!.playbackState

            // The notification must be updated after setting started to true
            val notification = createNotification()
            if (notification != null) {
                mController!!.registerCallback(mCb)
                val filter = IntentFilter()
                filter.addAction(ACTION_NEXT)
                filter.addAction(ACTION_PAUSE)
                filter.addAction(ACTION_PLAY)
                filter.addAction(ACTION_PREV)
                mService.registerReceiver(this, filter)

                mService.startForeground(NOTIFICATION_ID, notification)
                mStarted = true
            }
        }
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    fun stopNotification() {
        if (mStarted) {
            mStarted = false
            mController!!.unregisterCallback(mCb)
            try {
                mNotificationManager?.cancel(NOTIFICATION_ID)
                mService.unregisterReceiver(this)
            } catch (ex: IllegalArgumentException) {
                // ignore if the receiver is not registered.
            }

            mService.stopForeground(true)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        when (action) {
            ACTION_PAUSE -> mTransportControls!!.pause()
            ACTION_PLAY -> mTransportControls!!.play()
            ACTION_NEXT -> mTransportControls!!.skipToNext()
            ACTION_PREV -> mTransportControls!!.skipToPrevious()
            else -> Timber.w("Unknown intent ignored. Action=%s", action)
        }
    }

    /**
     * Update the state based on a change on the session token. Called either when
     * we are running for the first time or when the media session owner has destroyed the session
     * (see [android.media.session.MediaController.Callback.onSessionDestroyed])
     */
    @Throws(RemoteException::class)
    private fun updateSessionToken() {
        val freshToken = mService.mSessionToken
        if (mSessionToken == null) {
            if (mController != null) {
                mController!!.unregisterCallback(mCb)
            }
            mSessionToken = freshToken
            mController = MediaControllerCompat(mService, mSessionToken!!)
            mTransportControls = mController!!.transportControls
            if (mStarted) {
                mController!!.registerCallback(mCb)
            }

        }
    }

    private fun createContentIntent(description: MediaDescriptionCompat): PendingIntent {
        Timber.d("PendingIntent")
        val openUI = Intent(mService, LibraryActivity::class.java)
        openUI.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        openUI.action = Intent.ACTION_MAIN
        openUI.addCategory(Intent.CATEGORY_LAUNCHER)
        return PendingIntent.getActivity(mService, REQUEST_CODE, openUI,
                PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private fun createNotification(): Notification? {
        Timber.d("updateNotificationMetadata. mMetadata=%s", mMetadata.toString())
        if (mMetadata == null || mPlaybackState == null) {
            return null
        }

        val description = mMetadata!!.description

        var fetchArtUrl: String? = null
        var art: Bitmap? = null


        if (description.iconUri != null) {
            // This sample assumes the iconUri will be a valid URL formatted String, but
            // it can actually be any valid Android Uri formatted String.
            // async fetch the album art icon
            val artUrl = description.iconUri!!.toString()
            if (art == null) {
                fetchArtUrl = artUrl
                // use a placeholder art while the remote art is being downloaded
                art = BitmapFactory.decodeResource(mService.resources,
                        R.mipmap.ic_launcher)
            }
        }

        // Notification channels are only supported on Android O+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val notificationBuilder = NotificationCompat.Builder(mService, CHANNEL_ID)

        notificationBuilder.addAction(R.drawable.ic_action_v_previous,
                mService.getString(R.string.label_previous), mPreviousIntent)
        addPlayPauseAction(notificationBuilder)
        notificationBuilder.addAction(R.drawable.ic_action_v_next,
                mService.getString(R.string.label_next), mNextIntent)


        notificationBuilder
                .setStyle(MediaStyle()
                        // show only play/pause in compact view
                        .setShowActionsInCompactView(0, 1, 2)
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(mStopIntent)
                        .setMediaSession(mSessionToken))
                .setDeleteIntent(mStopIntent)
                .setColor(mNotificationColor)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setContentIntent(createContentIntent(description))
                .setContentTitle(description.title)
                .setContentText(description.subtitle)
                .setLargeIcon(art)


        setNotificationPlaybackState(notificationBuilder)
//        if (fetchArtUrl != null) {
//            fetchBitmapFromURLAsync(fetchArtUrl, notificationBuilder)
//        }

        return notificationBuilder.build()
    }

    private fun addPlayPauseAction(notificationBuilder: NotificationCompat.Builder) {
        val label: String
        val icon: Int
        val intent: PendingIntent
        if (mPlaybackState?.state == PlaybackStateCompat.STATE_PLAYING || mPlaybackState?.state == PlaybackStateCompat.STATE_BUFFERING) {
            label = mService.getString(R.string.label_pause)
            icon = R.drawable.ic_action_v_pause
            intent = mPauseIntent
        } else {
            label = mService.getString(R.string.label_play)
            icon = R.drawable.ic_action_v_play
            intent = mPlayIntent
        }
        notificationBuilder.addAction(NotificationCompat.Action(icon, label, intent))
    }


    private fun setNotificationPlaybackState(builder: NotificationCompat.Builder) {
        Timber.d("updateNotificationPlaybackState. mPlaybackState=%s", mPlaybackState)
        if (mPlaybackState == null || !mStarted) {
            Timber.d("updateNotificationPlaybackState. cancelling notification!")
            mService.stopForeground(true)
            return
        }

        // Make sure that the notification can be dismissed by the user when we are not playing:
        builder.setOngoing(mPlaybackState!!.state == PlaybackStateCompat.STATE_PLAYING)
    }

    private fun fetchBitmapFromURLAsync(bitmapUrl: String,
                                        builder: NotificationCompat.Builder) {
        loadBitmap(bitmapUrl, builder)
    }

    private fun loadBitmap(url: String, builder: NotificationCompat.Builder) {
        if (loadtarget == null)
            loadtarget = object : Target {
                override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                    Timber.d("fetchBitmapFromURLAsync: set bitmap to %s", url)
                    builder.setLargeIcon(bitmap)
                    addPlayPauseAction(builder)
                    mNotificationManager?.notify(NOTIFICATION_ID, builder.build())
                }

                override fun onBitmapFailed(errorDrawable: Drawable?) {

                }

                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {

                }
            }
        val downloader = OkHttp3Downloader(mService)
        Picasso.Builder(mService).downloader(downloader).build()
                .load(url).into(loadtarget!!)
    }

    /**
     * Creates Notification Channel. This is required in Android O+ to display notifications.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        if (mNotificationManager?.getNotificationChannel(CHANNEL_ID) == null) {
            val notificationChannel = NotificationChannel(CHANNEL_ID,
                    mService.getString(R.string.notification_channel),
                    NotificationManager.IMPORTANCE_LOW)

            notificationChannel.description = mService.getString(R.string.notification_channel_description)

            mNotificationManager?.createNotificationChannel(notificationChannel)
        }
    }

    companion object {

        private val CHANNEL_ID = "com.olacabs.olaplaystudio.MUSIC_CHANNEL_ID"

        private val NOTIFICATION_ID = 412
        private val REQUEST_CODE = 100

        val ACTION_PAUSE = "com.olacabs.olaplaystudio.pause"
        val ACTION_PLAY = "com.olacabs.olaplaystudio.play"
        val ACTION_PREV = "com.olacabs.olaplaystudio.prev"
        val ACTION_NEXT = "com.olacabs.olaplaystudio.next"
        val ACTION_STOP = "com.olacabs.olaplaystudio.stop"
    }
}
