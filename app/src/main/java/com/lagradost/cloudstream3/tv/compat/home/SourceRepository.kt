package com.lagradost.cloudstream3.tv.compat.home

import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainAPI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository zarządzający wybranym API providerem.
 * Singleton state dla całej aplikacji TV.
 */
object SourceRepository {
    
    private val _selectedApi = MutableStateFlow<MainAPI?>(null)
    val selectedApi: Flow<MainAPI?> = _selectedApi.asStateFlow()
    
    /**
     * Zwraca listę dostępnych API providers.
     */
    fun getAvailableApis(): List<MainAPI> {
        val allApis = APIHolder.apis
        if (allApis.isEmpty()) {
            return emptyList()
        }

        // Jeśli są API z hasMainPage, używamy ich; w przeciwnym razie wszystkie
        val filtered = allApis.filter { it.hasMainPage }

        return if (filtered.isNotEmpty()) filtered else allApis
    }
    
    /**
     * Ustawia wybrany API provider.
     */
    fun selectApi(api: MainAPI) {
        _selectedApi.value = api
    }
    
    /**
     * Zwraca obecnie wybrany API lub pierwszy dostępny.
     * Auto-inicjalizuje jeśli jeszcze nie było inicjalizacji.
     */
    fun getCurrentApiOrDefault(): MainAPI {
        // Lazy initialization - initialize on first access
        if (_selectedApi.value == null) {
            initializeDefaultApi()
        }
        return _selectedApi.value ?: throw IllegalStateException("No API providers available")
    }
    
    /**
     * Suspending function that waits for API providers to be available.
     * Retries up to 50 times with 100ms delay (5 seconds total).
     * Returns first available API or null if timeout.
     */
    suspend fun waitForApiOrNull(): MainAPI? {
        repeat(50) {
            val availableApis = getAvailableApis()
            if (availableApis.isNotEmpty()) {
                if (_selectedApi.value == null) {
                    _selectedApi.value = availableApis.firstOrNull()
                }
                return _selectedApi.value
            }
            kotlinx.coroutines.delay(100)
        }
        return null
    }
    
    /**
     * Inicjalizuje default API (call on app start or lazy).
     */
    fun initializeDefaultApi() {
        if (_selectedApi.value == null) {
            val availableApis = getAvailableApis()
            _selectedApi.value = availableApis.firstOrNull()
        }
    }
}
