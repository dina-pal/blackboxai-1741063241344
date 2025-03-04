package com.dinapal.busdakho.di

import android.content.Context
import androidx.room.Room
import com.dinapal.busdakho.data.local.BusDakhoDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            BusDakhoDatabase::class.java,
            "busdakho_database"
        ).build()
    }

    single { get<BusDakhoDatabase>().busDao() }
    single { get<BusDakhoDatabase>().routeDao() }
    single { get<BusDakhoDatabase>().userDao() }
    
    single { 
        androidContext().getSharedPreferences("busdakho_prefs", Context.MODE_PRIVATE)
    }
}

val networkModule = module {
    single { 
        createKtorClient()
    }
    
    single {
        createApiService(get())
    }
}

val repositoryModule = module {
    single { createBusRepository(get(), get()) }
    single { createRouteRepository(get(), get()) }
    single { createUserRepository(get(), get()) }
}

val viewModelModule = module {
    factory { createBusTrackingViewModel(get()) }
    factory { createJourneyPlanningViewModel(get()) }
    factory { createProfileViewModel(get()) }
}
