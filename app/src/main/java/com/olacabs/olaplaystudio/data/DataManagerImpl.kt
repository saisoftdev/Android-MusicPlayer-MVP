package com.olacabs.olaplaystudio.data

import com.olacabs.olaplaystudio.BuildConfig
import com.olacabs.olaplaystudio.data.model.MediaDetail
import com.olacabs.olaplaystudio.data.remote.MvpService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataManagerImpl @Inject constructor(private val mMvpService: MvpService) : DataManager {

    override fun getMediaList(callBack: MediaListCallBack):Disposable {
       return mMvpService.getMediaList().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    if (response.isSuccessful) {
                        if (response.body() != null)
                            callBack.onSuccess(response.body()!!)
                        else
                            callBack.onError("GetMediaList response body is null")
                    } else
                        callBack.onError(response.errorBody().toString())
                }, { error ->
                    if (BuildConfig.DEBUG) error.printStackTrace()
                    callBack.onError(error.message ?: "GetMediaList error body is null")
                })
    }


    interface MediaListCallBack {
        fun onSuccess(listOfMedia: List<MediaDetail>)
        fun onError(message: String)
    }
}