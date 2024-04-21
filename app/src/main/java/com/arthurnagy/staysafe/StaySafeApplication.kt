package com.arthurnagy.staysafe

import android.app.Application
import androidx.room.Room
import com.arthurnagy.staysafe.core.PreferenceManager
import com.arthurnagy.staysafe.core.StatementLocalSource
import com.arthurnagy.staysafe.core.db.StaySafeDatabase
import com.arthurnagy.staysafe.feature.documentdetail.DocumentDetailViewModel
import com.arthurnagy.staysafe.feature.home.HomeViewModel
import com.arthurnagy.staysafe.feature.newdocument.NewDocumentViewModel
import com.arthurnagy.staysafe.feature.newdocument.signature.SignatureViewModel
import com.arthurnagy.staysafe.feature.newdocument.statement.personaldata.StatementPersonalDataViewModel
import com.arthurnagy.staysafe.feature.newdocument.statement.routedata.StatementRouteDataViewModel
import com.arthurnagy.staysafe.feature.newdocument.statement.type.StatementTypeViewModel
import com.arthurnagy.staysafe.feature.shared.ThemeHelper
import com.arthurnagy.staysafe.feature.shared.provider.FileProvider
import com.arthurnagy.staysafe.feature.shared.provider.StringProvider
import com.google.android.gms.ads.MobileAds
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import timber.log.Timber

class StaySafeApplication : Application() {

    private val preferenceManager: PreferenceManager by inject()

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        startKoin {
            androidContext(this@StaySafeApplication)
            modules(appModule)
        }
        preferenceManager.theme?.let { ThemeHelper.applyTheme(it) }
        MobileAds.initialize(applicationContext) {}
    }

    private val appModule = module {
        factory { StringProvider(androidContext()) }
        factory { FileProvider(androidContext()) }

        single { PreferenceManager(androidContext()) }
        single {
            Room.databaseBuilder(androidContext(), StaySafeDatabase::class.java, "stay-safe-db")
                .addMigrations(StaySafeDatabase.MIGRATION_3_4)
                .addMigrations(StaySafeDatabase.MIGRATION_4_5)
                .addMigrations(StaySafeDatabase.MIGRATION_5_6)
                .build()
        }
        factory { get<StaySafeDatabase>().statementDao() }

        factory { StatementLocalSource(get()) }

        viewModel { HomeViewModel(statementLocalSource = get()) }

        viewModel { NewDocumentViewModel(statementLocalSource = get()) }

        viewModel { (newDocumentViewModel: NewDocumentViewModel) -> StatementTypeViewModel(newDocumentViewModel) }
        viewModel { (newDocumentViewModel: NewDocumentViewModel) -> StatementPersonalDataViewModel(newDocumentViewModel) }
        viewModel { (newDocumentViewModel: NewDocumentViewModel) -> StatementRouteDataViewModel(newDocumentViewModel) }
        viewModel { (newDocumentViewModel: NewDocumentViewModel) ->
            SignatureViewModel(
                newDocumentViewModel,
                statementLocalSource = get(),
                fileProvider = get()
            )
        }

        viewModel { (documentId: Long) -> DocumentDetailViewModel(documentId, statementLocalSource = get()) }
    }
}