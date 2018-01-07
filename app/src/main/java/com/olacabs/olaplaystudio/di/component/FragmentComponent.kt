package com.olacabs.olaplaystudio.di.component

import com.olacabs.olaplaystudio.di.PerFragment
import com.olacabs.olaplaystudio.di.module.FragmentModule
import dagger.Subcomponent

/**
 * This component inject dependencies to all Fragments across the application
 */
@PerFragment
@Subcomponent(modules = [(FragmentModule::class)])
interface FragmentComponent