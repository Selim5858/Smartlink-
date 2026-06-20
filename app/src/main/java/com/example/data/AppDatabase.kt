package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [LinkButton::class, User::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, coroutineScope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "linkgecis_database"
                )
                .addCallback(DatabaseCallback(coroutineScope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.appDao()
                    // Populate default buttons
                    dao.insertLinkButton(
                        LinkButton(
                            label = "Google Arama Motoru",
                            targetUrl = "https://www.google.com",
                            popupTitle = "🔥 ÖZEL TEKLİF: Süper Akıllı Saat!",
                            popupMessage = "Sadece bugün geçerli %50 indirim fırsatını kaçırmayın. Premium metal kordon, kalp ritmi ölçer ve 10 gün pil ömrü ile şimdi sipariş verin!",
                            popupImageUrl = "watch",
                            countdownSeconds = 5
                        )
                    )
                    dao.insertLinkButton(
                        LinkButton(
                            label = "Hava Durumu Raporu",
                            targetUrl = "https://weather.com",
                            popupTitle = "🍿 Full HD Sinema Keyfi Evinde!",
                            popupMessage = "Reklamsız, donmadan binlerce ödüllü dizi ve filmi sınırsız izleyin. İlk ay tamamen ÜCRETSİZ!",
                            popupImageUrl = "ticket",
                            countdownSeconds = 4
                        )
                    )
                    dao.insertLinkButton(
                        LinkButton(
                            label = "Teknoloji Portalı",
                            targetUrl = "https://news.ycombinator.com",
                            popupTitle = "🚀 Kariyerinde Sıçrama Yap!",
                            popupMessage = "Sıfırdan ileri seviye Kotlin ve Jetpack Compose eğitimi şimdi %80 indirimle! Profesyonel sertifikalı geliştirici ol.",
                            popupImageUrl = "rocket",
                            countdownSeconds = 6
                        )
                    )
                }
            }
        }
    }
}
