package com.par9uet.jm.ui.feature.user

import com.par9uet.jm.core.common.InitManager
import com.par9uet.jm.core.model.CollectComicOrderFilter
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.repository.impl.EmbeddedClientManager
import com.par9uet.jm.data.repository.impl.UserRepositoryImpl
import io.mockk.mockk
import io.mockk.Called
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectOrderSupportTest {
    @Test
    fun unsupportedOrderIsRejectedBeforeFetchingFavorites() = runTest {
        val client = mockk<EmbeddedClientManager>()
        val repository = UserRepositoryImpl(mockk<InitManager>(), client)
        val result = repository.getCollectComicList(1, CollectComicOrderFilter.UPDATE_TIME, 0)
        assertTrue(result is NetWorkResult.Error)
        verify { client wasNot Called }
    }
}
