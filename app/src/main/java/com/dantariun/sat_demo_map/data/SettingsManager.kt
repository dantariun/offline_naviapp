package com.dantariun.sat_demo_map.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException

// Context 확장 함수는 변경 없음
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(context: Context) {

    private val dataStore = context.dataStore

    // [수정 1] 여러 개의 설정 Key를 companion object에 정의합니다.
    companion object {
        // ViewModel에서 이 Key들을 사용하여 값을 읽어옵니다.
        val PRIVATE_LOCATION_KEY = booleanPreferencesKey("private_location_agree")
        val SATELLITE_COMMUNICATION_KEY = booleanPreferencesKey("satellite_communication_agree")
        val GENERATIVE_AI_INFORMATION_KEY = booleanPreferencesKey("generative_ai_information_agree")
        val ROUTE_DETOUR_SERVICE_KEY = booleanPreferencesKey("route_detour_service_agree")
        val EMERGENCY_ANALYSIS_KEY = booleanPreferencesKey("emergency_analysis_agree")
    }

    /**
     * [수정 2] 제네릭 데이터 저장 함수
     * 문자열 키를 받아 해당하는 설정 값을 저장합니다.
     */
    suspend fun setBoolean(keyString: String, value: Boolean) {
        val preferenceKey = booleanPreferencesKey(keyString) // 문자열로 Preferences.Key 생성
        dataStore.edit { preferences ->
            preferences[preferenceKey] = value
        }
    }

    /**
     * [수정 3] 제네릭 데이터 읽기 Flow
     * 특정 boolean 값이 아닌 Preferences 객체 전체의 변경을 감지합니다.
     * ViewModel에서 이 Flow를 구독하여 모든 설정의 변경을 한 번에 처리합니다.
     */
    val settingsFlow: Flow<Preferences> = dataStore.data
        .catch { exception ->
            // I/O 오류 발생 시 대처
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    // --- 기존 코드는 삭제 ---
    // val FEATURE_ENABLED_KEY = ...
    // suspend fun setFeatureEnabled(...) = ...
    // val featureEnabledFlow: Flow<Boolean> = ...
}