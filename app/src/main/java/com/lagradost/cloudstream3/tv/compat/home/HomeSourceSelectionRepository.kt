package com.lagradost.cloudstream3.tv.compat.home

import com.lagradost.cloudstream3.utils.DataStoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface HomeSourceSelectionRepository {
    suspend fun getSelectedSourceName(): String?

    suspend fun setSelectedSourceName(sourceName: String?)
}

class LegacyHomeSourceSelectionRepository : HomeSourceSelectionRepository {
    override suspend fun getSelectedSourceName(): String? = withContext(Dispatchers.IO) {
        DataStoreHelper.currentHomePage?.takeIf { sourceName ->
            sourceName.isNotBlank()
        }
    }

    override suspend fun setSelectedSourceName(sourceName: String?) {
        withContext(Dispatchers.IO) {
            DataStoreHelper.currentHomePage = sourceName?.takeIf { name ->
                name.isNotBlank()
            }
        }
    }
}
