package com.olacabs.olaplaystudio.data.model

import android.support.v4.media.session.PlaybackStateCompat

/**
 * Created by sai on 22/11/17.
 */
data class MediaDetail(val song: String?, val url: String?, val artists: String?,
                       val cover_image: String?,
                       var index: Int = 0,
                       var playingIndex: Int = 0,
                       var state: Int = PlaybackStateCompat.STATE_NONE,
                       var fav: Boolean = false,
                       var isDownloaded: Boolean = false)