package com.lagradost.cloudstream3.tv.compat.home

import android.util.Log
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
        Log.d("SourceRepo", "=== getAvailableApis() called ===")
        Log.d("SourceRepo", "APIHolder.apis.size = ${allApis.size}")
        
        if (allApis.isEmpty()) {
            Log.w("SourceRepo", "WARNING: APIHolder.apis is EMPTY! Plugins may not be loaded yet.")
            Log.w("SourceRepo", "Returning empty list. Try opening menu again after a few seconds.")
            return emptyList()
        }
        
        // Log first 10 APIs
        allApis.take(10).forEach { 
            Log.d("SourceRepo", "  API: ${it.name} (hasMainPage=${it.hasMainPage})") 
        }
        
        // Jeśli są API z hasMainPage, używamy ich; w przeciwnym razie wszystkie
        val filtered = allApis.filter { it.hasMainPage }
        Log.d("SourceRepo", "Filtered (hasMainPage=true): ${filtered.size} APIs")
        
        return if (filtered.isNotEmpty()) {
            Log.d("SourceRepo", "Returning ${filtered.size} filtered APIs")
            filtered
        } else {
            Log.d("SourceRepo", "No APIs with hasMainPage=true, returning all ${allApis.size} APIs")
            allApis
        }
    }
    
    /**
     * Ustawia wybrany API provider.
     */
    fun selectApi(api: MainAPI) {
        _selectedApi.value = api
        Log.d("SourceRepo", "Selected API: ${api.name}")
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
        repeat(50) { attempt ->
            val availableApis = getAvailableApis()
            if (availableApis.isNotEmpty()) {
                Log.d("SourceRepo", "APIs available after ${attempt * 100}ms")
                if (_selectedApi.value == null) {
                    _selectedApi.value = availableApis.firstOrNull()
                }
                return _selectedApi.value
            }
            kotlinx.coroutines.delay(100)
        }
        Log.e("SourceRepo", "Timeout waiting for API providers (5s)")
        return null
    }
    
    /**
     * Inicjalizuje default API (call on app start or lazy).
     */
    fun initializeDefaultApi() {
        if (_selectedApi.value == null) {
            val availableApis = getAvailableApis()
            _selectedApi.value = availableApis.firstOrNull()
            Log.d("SourceRepo", "Lazy init - Available APIs: ${availableApis.size}")
            availableApis.take(3).forEach {
                Log.d("SourceRepo", "  - ${it.name}")
            }
            Log.d("SourceRepo", "Initialized with default API: ${_selectedApi.value?.name}")
        }
    }
}
