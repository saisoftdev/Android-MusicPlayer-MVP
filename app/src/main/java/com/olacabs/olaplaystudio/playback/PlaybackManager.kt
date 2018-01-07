package com.olacabs.olaplaystudio.playback

import android.media.session.PlaybackState
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.olacabs.olaplaystudio.data.model.MediaDetail
import timber.log.Timber

/**
 * Created by sai on 5/18/17.
 *
 * Useful Links:
 * Reefer http://beust.com/weblog/2015/10/30/exploring-the-kotlin-standard-library/
 */

class PlaybackManager(val mPlayback: Playback,
                      val mServiceCallback: PlaybackServiceCallback) : Playback.Callback {

    val mMediaSessionCallback = MediaSessionCallback()
    val listOfMediaDetail = mutableListOf<MediaDetail>()
    private var currentPosition = 0


    init {
        mPlayback.state = PlaybackStateCompat.STATE_NONE
        mPlayback.setCallback(this)
    }


    private fun playMedia(id: Long) {
        currentPosition = id.toInt()
        playMedia()
    }

    private fun playMedia() {
        if (listOfMediaDetail.isNotEmpty()) {
            listOfMediaDetail.forEach { it.state = PlaybackStateCompat.STATE_NONE }
            listOfMediaDetail[currentPosition].state = PlaybackStateCompat.STATE_PLAYING
            listOfMediaDetail[currentPosition].apply { playingIndex = currentPosition }.url?.run { mPlayback.play(this) }
        } else
            Timber.d("listOfMediaDetail is empty")
    }

    private fun pauseMedia() {
        listOfMediaDetail[currentPosition].state = PlaybackStateCompat.STATE_PAUSED
        mServiceCallback.onPlaybackPause()
        mPlayback.pause()
    }

    private fun playPreviousMedia() {
        if (currentPosition == 0)
            currentPosition = listOfMediaDetail.size - 1
        else
            currentPosition -= 1
        playMedia()
    }

    private fun playNextMedia() {
        if (currentPosition == listOfMediaDetail.size - 1)
            currentPosition = 0
        else
            currentPosition += 1
        playMedia()
    }

    private fun stopMedia(focus: Boolean) {
        if (focus) handleStopRequest(null)
    }

    override fun onCompletion() {
        playNextMedia()
    }

    override fun onPlaybackStatusChanged(state: Int) {
        updatePlaybackState(null)
    }

    override fun onError(error: String) {
        updatePlaybackState(error)
    }


    fun handleStopRequest(withError: String?) {
        Timber.d("handleStopRequest: mState= %s error=%s", mPlayback.state, withError)
        mPlayback.stop(true)
        mServiceCallback.onPlaybackStop()
        updatePlaybackState(withError)
    }

    private fun updatePlaybackState(error: String?) {
        Timber.d("updatePlaybackState, playback state=%s", mPlayback.state)
        if (listOfMediaDetail.isNotEmpty())
            listOfMediaDetail.let {
                mServiceCallback.updateMetaData(listOfMediaDetail[currentPosition])//TODO send the selected item
                var position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN
                if (mPlayback.isConnected) {
                    position = mPlayback.currentStreamPosition.toLong()
                }

                val stateBuilder = PlaybackStateCompat.Builder()
                        .setActions(availableActions)
                var state = mPlayback.state

                // If there is an error message, send it to the playback state:
                if (error != null) {
                    // Error states are really only supposed to be used for errors that cause playback to
                    // stop unexpectedly and persist until the user takes action to fix it.
                    stateBuilder.setErrorMessage(PlaybackStateCompat.ERROR_CODE_UNKNOWN_ERROR, error)
                    state = PlaybackStateCompat.STATE_ERROR
                }

                stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime())

                // Set the activeQueueItemId if the current index is valid.
                mServiceCallback.onPlaybackStateUpdated(stateBuilder.build())

                if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_PAUSED) {
                    mServiceCallback.onNotificationRequired()
                }
            }
    }


    private val availableActions: Long
        get() {
            var actions = PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
            if (mPlayback.isPlaying) {
                actions = actions or PlaybackStateCompat.ACTION_PAUSE
            }
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            return actions
        }


    interface PlaybackServiceCallback {
        fun onPlaybackStart()

        fun onNotificationRequired()

        fun onPlaybackStop()

        fun onPlaybackPause()

        fun onPlaybackStateUpdated(newState: PlaybackStateCompat)

        fun updateMetaData(media: MediaDetail)
    }

    inner class MediaSessionCallback : MediaSessionCompat.Callback() {

        override fun onSkipToQueueItem(id: Long) {
            super.onSkipToQueueItem(id)
            playMedia(id)
        }

        override fun onPlay() {
            super.onPlay()
            playMedia()
        }

        override fun onPause() {
            super.onPause()
            pauseMedia()
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            playNextMedia()
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            playPreviousMedia()
        }


        override fun onStop() {
            super.onStop()
            stopMedia(true)
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            super.onCustomAction(action, extras)
        }
    }


}