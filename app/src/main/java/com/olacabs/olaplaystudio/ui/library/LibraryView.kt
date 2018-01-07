package com.olacabs.olaplaystudio.ui.library

import com.olacabs.olaplaystudio.data.model.MediaDetail
import com.olacabs.olaplaystudio.ui.base.MvpView
import permissions.dispatcher.NeedsPermission

/**
 * Created by sai on 16/12/17.
 */
interface LibraryView : MvpView {
    fun showProgress(show: Boolean)
    fun showError(message: String)
    fun updateMediaUi(mediaDetail: MediaDetail)
    fun playSelected(mediaDetail: MediaDetail)
    fun updateFavMenuIcon(showFavorites: Boolean)
    fun downloadFile(mediaDetail: MediaDetail)
}