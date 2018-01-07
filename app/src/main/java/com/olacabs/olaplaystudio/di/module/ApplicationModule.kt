package com.olacabs.olaplaystudio.di.module

import android.app.Application
import android.content.Context
import com.jakewharton.picasso.OkHttp3Downloader
import com.olacabs.olaplaystudio.data.local.AppPrefs
import com.olacabs.olaplaystudio.data.remote.MvpService
import com.olacabs.olaplaystudio.data.remote.MvpServiceFactory
import com.olacabs.olaplaystudio.di.ApplicationContext
import com.squareup.picasso.Picasso
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ApplicationModule(private val mApplication: Application) {

    @Provides
    internal fun provideApplication(): Application {
        return mApplication
    }

    @Provides
    @ApplicationContext
    internal fun provideContext(): Context {
        return mApplication
    }

    @Provides
    @Singleton
    internal fun provideMvpStarterService(): MvpService {
        return MvpServiceFactory.makeStarterService()
    }

    @Provides
    @Singleton
    internal fun providePicasso(): Picasso {
        val downloader = OkHttp3Downloader(mApplication)
        return Picasso.Builder(mApplication).downloader(downloader).build()
    }

    @Provides
    @Singleton
    internal fun provideAppPrefs(): AppPrefs {
        return AppPrefs(mApplication)
    }
}
