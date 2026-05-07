package com.example.signal.di

import android.content.Context
import androidx.room.Room
import com.example.signal.data.local.AppDatabase
import com.example.signal.data.repository.TaskRepository
import com.example.signal.data.repository.TaskRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()

    @Provides
    @Singleton
    fun provideTaskRepository(
        db: AppDatabase,
        @ApplicationContext context: Context
    ): TaskRepository = TaskRepositoryImpl(db, context)
}
