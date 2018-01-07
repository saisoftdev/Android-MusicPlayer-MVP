package com.olacabs.olaplaystudio.di.component

import com.olacabs.olaplaystudio.di.module.FragmentModule
import com.olacabs.olaplaystudio.di.ConfigPersistent
import com.olacabs.olaplaystudio.di.module.ActivityModule
import dagger.Component

/**
 * A dagger component that will live during the lifecycle of an Activity or Fragment but it won't
 * be destroy during configuration changes. Check [MvpBaseActivity] and [BaseFragment] to
 * see how this components survives configuration changes.
 * Use the [ConfigPersistent] scope to annotate dependencies that need to survive
 * configuration changes (for example Presenters).
 */
@ConfigPersistent
@Component(dependencies = [(ApplicationComponent::class)])
interface ConfigPersistentComponent {

    fun activityComponent(activityModule: ActivityModule): ActivityComponent

    fun fragmentComponent(fragmentModule: FragmentModule): FragmentComponent

}
