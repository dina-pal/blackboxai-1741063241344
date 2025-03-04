package com.dinapal.busdakho

import android.app.Application
import com.dinapal.busdakho.di.appModule
import com.dinapal.busdakho.di.networkModule
import com.dinapal.busdakho.di.repositoryModule
import com.dinapal.busdakho.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class BusDakhoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@BusDakhoApplication)
            modules(
                listOf(
                    appModule,
                    networkModule,
                    repositoryModule,
                    viewModelModule
                )
            )
        }
    }
}
