package com.dantariun.ecall.di

class DataModule {
}

/**
 * @Module
 * @InstallIn(SingletonComponent::class)
 * abstract class DataModule {
 *
 *     @Binds
 *     @Singleton
 *     abstract fun bindAuthRepository(
 *         authRepositoryImpl: AuthRepositoryImpl
 *     ): AuthRepository
 *
 *     @Binds
 *     @Singleton
 *     abstract fun bindLocationRepository(
 *         locationRepositoryImpl: LocationRepositoryImpl
 *     ): LocationRepository
 *
 *     // ... 다른 Repository들도 동일하게 바인딩
 * }
 */