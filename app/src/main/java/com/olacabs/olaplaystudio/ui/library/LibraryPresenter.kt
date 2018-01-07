package com.olacabs.olaplaystudio.ui.library

import android.app.Application
import android.app.DownloadManager
import android.content.Context.DOWNLOAD_SERVICE
import android.net.Uri
import android.os.Environment
import com.olacabs.olaplaystudio.data.DataManager
import com.olacabs.olaplaystudio.data.DataManagerImpl
import com.olacabs.olaplaystudio.data.local.AppPrefs
import com.olacabs.olaplaystudio.data.model.MediaDetail
import com.olacabs.olaplaystudio.di.ConfigPersistent
import com.olacabs.olaplaystudio.playback.PublishMediaLoadEvent
import com.olacabs.olaplaystudio.ui.base.BasePresenter
import com.olacabs.olaplaystudio.utils.clearAndAddAll
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import javax.inject.Inject


/**
 * Created by sai on 16/12/17.
 */

@ConfigPersistent
class LibraryPresenter @Inject
constructor(private val mDataManager: DataManager) : BasePresenter<LibraryView>() {
    @Inject lateinit var mLibraryAdapter: LibraryAdapter
    @Inject lateinit var mAppPrefs: AppPrefs
    @Inject lateinit var mApplication: Application
    val mMediaListOriginal: ArrayList<MediaDetail> = arrayListOf()
    var showFavorites = false

    override fun attachView(mvpView: LibraryView) {
        super.attachView(mvpView)
        mLibraryAdapter.setClickListener(object : LibraryAdapter.ClickListener {
            override fun onMediaClick(mediaDetail: MediaDetail) {
                Timber.i("Play request for %s", mediaDetail.song)
                mvpView.playSelected(mediaDetail)
            }

            override fun onFavClick(position: Int, mediaDetail: MediaDetail) {
                favClicked(mediaDetail, position)
            }

            override fun onDownloadClick(mediaDetail: MediaDetail) {
                onDownloadClicked(mediaDetail)
            }
        })
    }

    private fun onDownloadClicked(mediaDetail: MediaDetail) {
        mvpView?.downloadFile(mediaDetail)
    }

    private fun favClicked(mediaDetail: MediaDetail, position: Int) {
        if (mAppPrefs.favList.contains(mediaDetail.song)) {
            mAppPrefs.removeFromFav(mediaDetail.song ?: "")
            mLibraryAdapter.mMediaList[position].fav = false
        } else {
            mediaDetail.song?.let { mAppPrefs.addToFav(it.trim()) }
            mLibraryAdapter.mMediaList[position].fav = true
        }
        mLibraryAdapter.notifyItemChanged(position)
    }

    private val fetchMediaCallBack = object : DataManagerImpl.MediaListCallBack {
        override fun onSuccess(listOfMedia: List<MediaDetail>) {
            mvpView?.showProgress(false)
            listOfMedia.forEachIndexed { index, mediaDetail ->
                mediaDetail.index = index
                mediaDetail.fav = mAppPrefs.favList.find { it == mediaDetail.song } !== null
            }
            mMediaListOriginal.clearAndAddAll(listOfMedia)
            mLibraryAdapter.mMediaList.clearAndAddAll(mMediaListOriginal)
            updateMediaUI(mediaDetail = mMediaListOriginal.first())
            EventBus.getDefault().post(PublishMediaLoadEvent(mMediaListOriginal))
            mLibraryAdapter.notifyDataSetChanged()
        }

        override fun onError(message: String) {
            mvpView?.showProgress(false)
            mvpView?.showError(message)
        }

    }

    private fun updateMediaUI(mediaDetail: MediaDetail) {
        mvpView?.updateMediaUi(mediaDetail)
    }

    fun fetchMediaList() {
        checkViewAttached()
        showFavorites = false
        mvpView?.showProgress(true)
        addDisposable(mDataManager.getMediaList(fetchMediaCallBack))
    }

    fun notifyAdapter() {
        mLibraryAdapter.notifyDataSetChanged()
    }

    fun toggleFav(): Boolean {
        showFavorites = !showFavorites
        if (showFavorites) {
            val favorites = mMediaListOriginal.filter { it.fav }
            if (favorites.isNotEmpty()) {
                mLibraryAdapter.mMediaList.clearAndAddAll(favorites)
            } else {
                mvpView?.showError("No favorites found")
                return true
            }
        } else {
            mLibraryAdapter.mMediaList.clearAndAddAll(mMediaListOriginal)
        }
        mLibraryAdapter.notifyDataSetChanged()
        mvpView?.updateFavMenuIcon(showFavorites)
        return true
    }

    fun downloadFile(mediaDetail: MediaDetail) {
        mediaDetail.url?.let { mediaUrl ->
            val r = DownloadManager.Request(Uri.parse(mediaUrl))
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, mediaDetail.song)
            r.allowScanningByMediaScanner()
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            val dm = mApplication.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(r)
        }
        mLibraryAdapter.mMediaList.find { it == mediaDetail }?.isDownloaded = true
        mLibraryAdapter.notifyDataSetChanged()
    }

    fun filterList(text: String) {
        mLibraryAdapter.run {
            mLibraryAdapter.mMediaList.clearAndAddAll(mMediaListOriginal.filter { it.song!!.contains(text,true)})
            notifyDataSetChanged()
        }
    }
}