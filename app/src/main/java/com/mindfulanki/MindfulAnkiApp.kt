package com.mindfulanki

import android.app.Application
import android.content.Context
import com.mindfulanki.data.ReviewRepository
import com.mindfulanki.data.apkg.ApkgImportService
import com.mindfulanki.data.db.AppDatabase

/** Tiny manual DI container — keeps the app dependency-light (no Hilt). */
class AppContainer(context: Context) {
    private val database = AppDatabase.get(context)
    val reviewRepository = ReviewRepository(database)
    val importService = ApkgImportService(context, database)
}

class MindfulAnkiApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
