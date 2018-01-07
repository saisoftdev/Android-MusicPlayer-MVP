package com.olacabs.olaplaystudio.data.remote


import com.olacabs.olaplaystudio.data.model.MediaDetail
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.http.GET

interface MvpService {

    @GET("/studio")
    fun getMediaList(): Observable<Response<List<MediaDetail>>>

}
