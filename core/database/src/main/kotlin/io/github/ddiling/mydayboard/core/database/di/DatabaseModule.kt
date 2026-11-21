package io.github.ddiling.mydayboard.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.ddiling.mydayboard.core.database.MyDayboardDatabase
import io.github.ddiling.mydayboard.core.database.dao.TrackerDao
import io.github.ddiling.mydayboard.core.database.dao.TrackerEventDao
import io.github.ddiling.mydayboard.core.database.dao.TrackerReasonDao
import javax.inject.Singleton

/** One database per process: Room's invalidation tracking only works within one instance. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): MyDayboardDatabase =
        Room.databaseBuilder(context, MyDayboardDatabase::class.java, MyDayboardDatabase.NAME).build()

    @Provides
    fun provideTrackerDao(database: MyDayboardDatabase): TrackerDao = database.trackerDao()

    @Provides
    fun provideTrackerEventDao(database: MyDayboardDatabase): TrackerEventDao = database.trackerEventDao()

    @Provides
    fun provideTrackerReasonDao(database: MyDayboardDatabase): TrackerReasonDao = database.trackerReasonDao()
}
