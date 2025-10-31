package com.example.nexus.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.nexus.data.DtnStore
import com.example.nexus.data.NexaService
import com.example.nexus.data.RealNexaService
import com.example.nexus.data.database.ChannelDao
import com.example.nexus.data.database.DtnMessageDao
import com.example.nexus.data.database.NexaDatabase
import com.example.nexus.data.repositories.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // These dependencies will live once, for the entire app's lifecycle.
object AppModule {

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add new tables for Channels
            db.execSQL("CREATE TABLE IF NOT EXISTS `channels` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `channel_members` (`channelId` TEXT NOT NULL, `userId` TEXT NOT NULL, PRIMARY KEY(`channelId`, `userId`))")

            // Add new columns to messages table
            db.execSQL("ALTER TABLE `messages` ADD COLUMN `data` TEXT")
            db.execSQL("ALTER TABLE `messages` ADD COLUMN `messageType` TEXT NOT NULL DEFAULT 'TEXT'")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add new columns to channels table
            db.execSQL("ALTER TABLE `channels` ADD COLUMN `description` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `channels` ADD COLUMN `isPublic` INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `dtn_messages` ADD COLUMN `messageType` TEXT NOT NULL DEFAULT 'MESSAGE'")
        }
    }

    @Provides
    @Singleton // @Singleton ensures only one instance of the database is ever created.
    fun provideNexaDatabase(@ApplicationContext context: Context): NexaDatabase {
        return Room.databaseBuilder(
            context,
            NexaDatabase::class.java, "nexa-database"
        ).addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideUserRepository(@ApplicationContext context: Context): UserRepository {
        return UserRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideSecurityRepository(@ApplicationContext context: Context): SecurityRepository {
        return SecurityRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideMessageRepository(
        db: NexaDatabase,
        contactRepository: ContactRepository
    ): MessageRepository {
        return MessageRepositoryImpl(db.messageDao(), contactRepository)
    }


    @Provides
    @Singleton
    fun provideConnectionRepository(
        @ApplicationContext context: Context
    ): ConnectionRepository {
        return ConnectionRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideContactRepository(db: NexaDatabase): ContactRepository {
        return ContactRepositoryImpl(db.contactDao())
    }

    @Provides
    @Singleton
    fun provideChannelDao(db: NexaDatabase): ChannelDao {
        return db.channelDao()
    }

    @Provides
    @Singleton
    fun provideChannelRepository(channelDao: ChannelDao): ChannelRepository {
        return ChannelRepositoryImpl(channelDao)
    }

    @Provides
    @Singleton
    fun provideDtnMessageDao(db: NexaDatabase): DtnMessageDao {
        return db.dtnMessageDao()
    }

    @Provides
    @Singleton
    fun provideDtnRepository(dtnMessageDao: DtnMessageDao): DtnRepository {
        return DtnRepositoryImpl(dtnMessageDao)
    }

    @Provides
    @Singleton
    fun provideDtnStore(
        dtnRepository: DtnRepository,
        dtnSettingsRepository: DtnSettingsRepository
    ): DtnStore {
        return DtnStore(dtnRepository, dtnSettingsRepository)
    }

    @Provides
    @Singleton
    fun provideNexaService(
        userRepository: UserRepository,
        connectionRepository: ConnectionRepository,
        securityRepository: SecurityRepository,
        messageRepository: MessageRepository,
        contactRepository: ContactRepository,
        channelRepository: ChannelRepository
    ): NexaService {
        return RealNexaService(
            userRepository,
            connectionRepository,
            securityRepository,
            messageRepository,
            contactRepository,
            channelRepository
        )
    }
}