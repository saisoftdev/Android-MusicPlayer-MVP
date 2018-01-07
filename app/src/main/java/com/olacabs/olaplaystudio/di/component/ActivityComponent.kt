package com.olacabs.olaplaystudio.di.component

import com.olacabs.olaplaystudio.di.PerActivity
import com.olacabs.olaplaystudio.di.module.ActivityModule
import com.olacabs.olaplaystudio.ui.base.MvpBaseActivity
import com.olacabs.olaplaystudio.ui.library.LibraryActivity
import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = [(ActivityModule::class)])
interface ActivityComponent {
    fun inject(baseActivity: MvpBaseActivity)
    fun inject(libraryActivity: LibraryActivity)
}