package com.olacabs.olaplaystudio.di.module

import com.olacabs.olaplaystudio.data.DataManager
import com.olacabs.olaplaystudio.data.DataManagerImpl
import dagger.Binds
import dagger.Module

@Module
abstract class Bindings {

  @Binds
  internal abstract fun bindDataManger(manager: DataManagerImpl): DataManager

}