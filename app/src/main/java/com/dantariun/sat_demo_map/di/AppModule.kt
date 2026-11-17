package com.dantariun.sat_demo_map.di

import android.content.Context
import com.dantariun.sat_demo_map.data.SettingsManager
import com.dantariun.sat_demo_map.data.db.AppDatabase
import com.dantariun.sat_demo_map.data.db.dao.ShadowZoneDao
import com.dantariun.sat_demo_map.data.db.dao.TileInfoDao
import com.dantariun.sat_demo_map.data.db.repository.ShadowZoneRepository
import com.dantariun.sat_demo_map.data.db.repository.TileRepository
import com.dantariun.sat_demo_map.domain.usecase.FindRouteAvoidingAreaUseCase
import com.dantariun.sat_demo_map.domain.usecase.FindRouteUseCase
import com.dantariun.sat_demo_map.domain.usecase.FindRouteWithWaypointsUseCase
import com.dantariun.sat_demo_map.domain.usecase.GetDisasterZonesUseCase
import com.dantariun.sat_demo_map.domain.usecase.InitGraphHopperUseCase
import com.dantariun.sat_demo_map.domain.usecase.ReverseGeocodeUseCase
import com.dantariun.sat_demo_map.util.graphHopper.SpeedCalculator
import com.dantariun.sat_demo_map.util.common.NetworkStatusListener
import com.dantariun.sat_demo_map.util.graphHopper.GraphHopperManager
import com.dantariun.sat_demo_map.util.graphHopper.RouteAnalyzer
import com.dantariun.sat_demo_map.util.graphHopper.RouteRefiner
import com.dantariun.sat_demo_map.util.map.GpsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // 앱 전역에서 사용될 의존성
object AppModule {

    @Provides
    @Singleton // GraphHopperManager는 앱 내에서 하나의 인스턴스만 존재하도록 설정
    fun provideGraphHopperManager(@ApplicationContext context: Context): GraphHopperManager {
        return GraphHopperManager(context)
    }

    @Provides
    fun provideInitGraphHopperUseCase(graphHopperManager: GraphHopperManager): InitGraphHopperUseCase {
        return InitGraphHopperUseCase(graphHopperManager)
    }

    @Provides
    fun provideFindRouteUseCase(graphHopperManager: GraphHopperManager): FindRouteUseCase {
        return FindRouteUseCase(graphHopperManager)
    }

    @Provides
    fun provideFindRouteWithWaypointsUseCase(graphHopperManager: GraphHopperManager) : FindRouteWithWaypointsUseCase {
        return FindRouteWithWaypointsUseCase(graphHopperManager)
    }

    @Provides
    fun provideFindRouteAvoidingAreaUseCase(graphHopperManager: GraphHopperManager):FindRouteAvoidingAreaUseCase {
        return FindRouteAvoidingAreaUseCase(graphHopperManager)
    }

    @Provides
    fun provideReverseGeocodeUseCase(graphHopperManager: GraphHopperManager):ReverseGeocodeUseCase {
        return ReverseGeocodeUseCase(graphHopperManager)
    }

    @Provides
    fun provideGetDisasterZonesUseCase(): GetDisasterZonesUseCase {
        return GetDisasterZonesUseCase()
    }


    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }

    @Provides
    fun provideRouteAnalyzer(): RouteAnalyzer {
        return RouteAnalyzer()
    }

    @Provides
    fun provideSpeedCalculator(): SpeedCalculator {
        return SpeedCalculator()
    }

    @Provides
    fun provideRouteRefiner(): RouteRefiner {
        return RouteRefiner()
    }

    @Provides
    fun provideGpsManager(@ApplicationContext context: Context): GpsManager {
        return GpsManager(context)
    }

    @Provides
    @Singleton // 앱 전체에서 단 하나의 인스턴스만 사용하도록 보장
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideTileInfoDao(appDatabase: AppDatabase): TileInfoDao {
        return appDatabase.tileInfoDao()
    }

    // ShadowZoneDao 인스턴스를 제공하는 방법
    // Hilt가 AppDatabase를 주입받아 이 함수를 실행합니다.
    @Provides
    @Singleton
    fun provideShadowZoneDao(appDatabase: AppDatabase): ShadowZoneDao {
        return appDatabase.shadowZoneDao()
    }

    // ShadowZoneRepository 인스턴스를 제공하는 방법
    // Hilt가 ShadowZoneDao를 주입받아 이 함수를 실행합니다.
    @Provides
    @Singleton
    fun provideShadowZoneRepository(shadowZoneDao: ShadowZoneDao): ShadowZoneRepository {
        return ShadowZoneRepository(shadowZoneDao)
    }

    @Provides
    @Singleton
    fun provideTileRepository(tileInfoDao: TileInfoDao): TileRepository {
        return TileRepository(tileInfoDao)
    }

    // NetworkStatusListener 인스턴스를 제공하는 방법
    @Provides
    @Singleton
    fun provideNetworkStatusListener(@ApplicationContext context: Context): NetworkStatusListener {
        return NetworkStatusListener(context)
    }
}