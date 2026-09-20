package com.par9uet.jm.data.repository.impl

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CategoryRequestScopeTest {
    private val request = Request.Builder().url("https://example.com/categories/filter?c=0&o=mv&page=2").build()

    @Test
    fun onlyCategoryParameterChangesWithinScope() {
        val scope = CategoryRequestScope()
        scope.withCategory("doujin_chinese") {
            val adapted = scope.adapt(request)
            assertEquals("doujin_chinese", adapted.url.queryParameter("c"))
            assertEquals("mv", adapted.url.queryParameter("o"))
            assertEquals("2", adapted.url.queryParameter("page"))
            val other = request.newBuilder().url("https://example.com/search?c=0").build()
            assertSame(other, scope.adapt(other))
        }
        assertSame(request, scope.adapt(request))
    }

    @Test
    fun exceptionRestoresScopeAndDoesNotLeakIntoOtherThreads() {
        val scope = CategoryRequestScope()
        scope.withCategory("doujin") {
            val result = java.util.concurrent.FutureTask { scope.adapt(request) }
            val thread = Thread(result)
            thread.start()
            assertSame(request, result.get(5, java.util.concurrent.TimeUnit.SECONDS))
            try {
                scope.withCategory("single") { throw IllegalStateException("failure") }
            } catch (_: IllegalStateException) {
                assertEquals("doujin", scope.adapt(request).url.queryParameter("c"))
            }
        }
        assertSame(request, scope.adapt(request))
    }
}
