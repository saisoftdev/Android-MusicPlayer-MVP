package com.olacabs.olaplaystudio.data

import io.reactivex.disposables.Disposable
import java.util.*

interface DataManager {
    fun getMediaList(callBack: DataManagerImpl.MediaListCallBack): Disposable
}