package com.olacabs.olaplaystudio.ui.library

import android.Manifest
import android.annotation.SuppressLint
import android.app.SearchManager
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.os.RemoteException
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.olacabs.olaplaystudio.R
import com.olacabs.olaplaystudio.data.model.MediaDetail
import com.olacabs.olaplaystudio.playback.BaseMediaActivity
import com.olacabs.olaplaystudio.playback.MusicService
import com.olacabs.olaplaystudio.utils.hide
import com.olacabs.olaplaystudio.utils.show
import com.olacabs.olaplaystudio.utils.showAsToast
import com.olacabs.olaplaystudio.utils.visible
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_library.*
import kotlinx.android.synthetic.main.content_fullscreen_player.*
import kotlinx.android.synthetic.main.controls_panel.*
import permissions.dispatcher.*
import timber.log.Timber
import javax.inject.Inject


/**
 * Created by sai on 16/12/17.
 */
@RuntimePermissions
class LibraryActivity(override val layout: Int = R.layout.activity_library) : BaseMediaActivity(), LibraryView {


    @Inject lateinit var mPicasso: Picasso
    @Inject lateinit var presenter: LibraryPresenter
    private val transition: TransitionDrawable by lazy { controls_sub_parent.background as TransitionDrawable }
    private var mMediaController: MediaControllerCompat? = null

    override fun showError(message: String) = message.showAsToast()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityComponent().inject(this)
        presenter.attachView(this)

        //Fetch media from server
        presenter.fetchMediaList()

        //Initialize views
        initViews()
    }

    private fun initViews() {
        media_list.adapter = presenter.mLibraryAdapter
        swipe_refresh_layout.setOnRefreshListener {
            presenter.fetchMediaList()
            invalidateOptionsMenu()
        }
        setSupportActionBar(toolbar)
        slide_back_btn.hide()

        transition.isCrossFadeEnabled = true
        sliding_layout.setDragView(R.id.controls_sub_parent)
        sliding_layout.addPanelSlideListener(object : SlidingUpPanelLayout.PanelSlideListener {

            override fun onPanelSlide(panel: View, slideOffset: Float) {
                //setActionBarTranslation(slidingUpPanelLayout.getCurrentParallaxOffset());
            }

            override fun onPanelStateChanged(panel: View, previousState: SlidingUpPanelLayout.PanelState,
                                             newState: SlidingUpPanelLayout.PanelState) {

                if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    slidingHidePlay(true)
                }
                if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    slidingHidePlay(false)
                }
            }
        })


        arrayListOf(play_btn, slide_play_ib).forEach { it.setOnClickListener { handlePlayButtonClick() } }
        previous_btn.setOnClickListener {
            mMediaController?.transportControls?.skipToPrevious()
        }
        next_btn.setOnClickListener {
            mMediaController?.transportControls?.skipToNext()
        }
    }

    private fun slidingHidePlay(b: Boolean) {
        if (b) {
            if (!slide_back_btn.isShown) {
                transition.startTransition(200)
                slide_back_btn.show()
                detail_layout.hide()
                play_pause_layout.hide()
            }
        } else {
            if (slide_back_btn.isShown) {
                transition.reverseTransition(200)
                detail_layout.show()
                play_pause_layout.show()
                slide_back_btn.hide()
            }
        }
    }

    override fun showProgress(show: Boolean) {
        progress_bar.visible(show)
        if (!show) swipe_refresh_layout.isRefreshing = show
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detachView()
    }

    override fun updateMediaUi(mediaDetail: MediaDetail) {
        if (mMediaController?.metadata == null)
            mediaDetail.run {
                slide_title.text = song ?: ""
                title_tv.text = song ?: ""

                slide_artist.text = artists ?: ""
                artist_tv.text = artists ?: ""

                cover_image?.let {
                    mPicasso.load(it)
                            .placeholder(R.drawable.album_placeholder)
                            .into(background_image)
                }
            }

    }

    override fun onBackPressed() {
        if (sliding_layout.panelState == SlidingUpPanelLayout.PanelState.EXPANDED) {
            sliding_layout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        } else {
            super.onBackPressed()
        }
    }

    override fun connectToSession(token: MediaSessionCompat.Token) {
        try {
            mMediaController = MediaControllerCompat(this, token)
            mMediaController?.registerCallback(mMediaControllerCallback)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private val mMediaControllerCallback = object : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            Timber.d("mediaControllerCallback onPlaybackStateChanged: state = %s", state?.state)
            state?.let { playStateChange(it.state) }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            Timber.d("mediaControllerCallback onMetadataChanged %s",
                    metadata?.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER))
            metadata?.run {
                metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE).run {
                    slide_title.text = this
                    title_tv.text = this
                }
                metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST).run {
                    slide_artist.text = this
                    artist_tv.text = this
                }
                metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)?.let {
                    mPicasso.load(it)
                            .placeholder(R.drawable.album_placeholder)
                            .into(background_image)
                }
                presenter.notifyAdapter()
            }

        }
    }

    private fun playStateChange(state: Int) {
        Timber.d("playStateChange %s", state)
        when (state) {
            PlaybackStateCompat.STATE_BUFFERING -> showMediaProgress(true)
            PlaybackStateCompat.STATE_PLAYING -> {
                updatePlayButton(true)
            }
            else -> updatePlayButton(false)
        }
    }

    private fun showMediaProgress(visibility: Boolean) {
        media_progress_bar.visible(visibility)
    }

    private fun updatePlayButton(isPlaying: Boolean) {
        showMediaProgress(false)
        if (isPlaying) {
            slide_play_ib?.setImageResource(R.drawable.ic_action_v_pause)
            play_btn?.setImageResource(R.drawable.ic_action_v_pause)
        } else {
            slide_play_ib?.setImageResource(R.drawable.ic_action_v_play)
            play_btn?.setImageResource(R.drawable.ic_action_v_play)
        }
    }


    override fun onMusicServiceConnected(service: MusicService) {
        mMediaController?.metadata?.run {
            mMediaController?.playbackState?.let { playStateChange(it.state) }
            getString(MediaMetadataCompat.METADATA_KEY_TITLE).run {
                slide_title.text = this
                title_tv.text = this
            }
            getString(MediaMetadataCompat.METADATA_KEY_ARTIST).run {
                slide_artist.text = this
                artist_tv.text = this
            }
            getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)?.let {
                mPicasso.load(it)
                        .placeholder(R.drawable.album_placeholder)
                        .into(background_image)
            }
        }
    }

    override fun onMusicServiceDisconnected() {

    }

    private fun handlePlayButtonClick() {
        val state = mMediaController?.playbackState
        val controls = mMediaController?.transportControls
        if (state != null) {
            when (state.state) {
                PlaybackStateCompat.STATE_PLAYING // fall through
                    , PlaybackStateCompat.STATE_BUFFERING -> controls?.pause()
                PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.STATE_STOPPED -> {
                    controls?.play()
                }
                else -> Timber.d("onClick with state %s", state.state)
            }
        } else {
            controls?.play()
        }
    }

    override fun playSelected(mediaDetail: MediaDetail) {
        val controls = mMediaController?.transportControls
        controls?.skipToQueueItem(mediaDetail.index.toLong())
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.music_toolbar, menu)
        initSearchView(menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun initSearchView(menu: Menu?) {
        val searchView = menu?.findItem(R.id.menu_search)?.actionView as SearchView
        val searchManager = getSystemService(AppCompatActivity.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { presenter.filterList(it) }
                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_fav -> presenter.toggleFav()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun updateFavMenuIcon(showFavorites: Boolean) {
        invalidateOptionsMenu()
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_fav)?.setIcon(
                if (presenter.showFavorites)
                    R.drawable.ic_media_favorite_fill
                else
                    R.drawable.ic_media_favorite_border)
        return super.onPrepareOptionsMenu(menu)

    }


    override fun downloadFile(mediaDetail: MediaDetail) {
        downloadFileRequestWithPermissionCheck(mediaDetail)
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun downloadFileRequest(mediaDetail: MediaDetail) {
        presenter.downloadFile(mediaDetail)
        "Download started, check your downloads folder".showAsToast()
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun showRationaleForCamera(request: PermissionRequest) {
        AlertDialog.Builder(this)
                .setMessage("Need storage permission to download the file")
                .setPositiveButton("Allow", { _, _ -> request.proceed() })
                .setNegativeButton("Deny", { _, _ -> request.cancel() })
                .show()
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun showDeniedForCamera() {
        "Storage permissions were denied. Please consider granting it in order to access the storage!".showAsToast()
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun showNeverAskForCamera() {
        "Storage permission was denied with never ask again.".showAsToast()
    }

    @SuppressLint("NeedOnRequestPermissionsResult")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }
    override fun onPause() {
        super.onPause()
        mMediaController?.unregisterCallback(mMediaControllerCallback)
    }
}
