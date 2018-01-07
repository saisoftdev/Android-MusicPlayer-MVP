package com.olacabs.olaplaystudio.di.component

import android.app.Application
import android.content.Context
import com.olacabs.olaplaystudio.data.DataManager
import com.olacabs.olaplaystudio.data.local.AppPrefs
import com.olacabs.olaplaystudio.data.remote.MvpService
import com.olacabs.olaplaystudio.di.ApplicationContext
import com.olacabs.olaplaystudio.di.module.ApplicationModule
import com.olacabs.olaplaystudio.di.module.Bindings
import com.squareup.picasso.Picasso
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class, Bindings::class])
interface ApplicationComponent {

    @ApplicationContext
    fun context(): Context

    fun application(): Application

    fun dataManager(): DataManager

    fun mvpService(): MvpService

    fun providePicasso(): Picasso

    fun provideAppPrefs(): AppPrefs
}
