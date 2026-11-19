package io.github.ddiling.mydayboard

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import io.github.ddiling.mydayboard.core.dashboard.DashboardCard

/**
 * Declares the set that features add their cards to with `@IntoSet`. Without this declaration Hilt would
 * fail to build while no feature contributes a card; with it, an empty dashboard is valid.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DashboardModule {
    @Multibinds
    abstract fun dashboardCards(): Set<DashboardCard>
}
