package com.par9uet.jm.data.repository.impl

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/** Adapts dynamic source slugs to the SDK's enum-only categories query. */
internal class CategoryRequestScope : Interceptor {
    private val category = ThreadLocal<String>()

    // The SDK executes HTTP synchronously. Never suspend inside this block.
    fun <T> withCategory(slug: String, block: () -> T): T {
        require(slug.isNotBlank()) { "分类标识为空" }
        val previous = category.get()
        category.set(slug)
        return try {
            block()
        } finally {
            if (previous == null) category.remove() else category.set(previous)
        }
    }

    internal fun adapt(request: Request): Request {
        val slug = category.get() ?: return request
        if (request.url.encodedPath != "/categories/filter") return request
        return request.newBuilder().url(request.url.newBuilder().setQueryParameter("c", slug).build()).build()
    }

    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(adapt(chain.request()))
}
