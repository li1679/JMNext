package com.par9uet.jm.di

import com.par9uet.jm.ui.feature.detail.ComicDetailViewModel
import com.par9uet.jm.ui.feature.reader.ComicReadViewModel
import com.par9uet.jm.ui.feature.home.ComicViewModel
import com.par9uet.jm.ui.feature.category.CategoryViewModel
import com.par9uet.jm.ui.feature.download.DownloadComicDetailViewModel
import com.par9uet.jm.ui.feature.download.DownloadViewModel
import com.par9uet.jm.ui.feature.shared.GlobalViewModel
import com.par9uet.jm.ui.feature.settings.CheckUpdateViewModel
import com.par9uet.jm.ui.feature.user.UserViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** 界面层只注册 ViewModel，其余对象由各自模块注册，在 JmApplication 汇总。 */
val appModule = module {
    viewModel { CategoryViewModel(get(), get(), get()) }
    viewModel { GlobalViewModel(getAll(), get()) }
    viewModel { ComicViewModel(get(), get(), get()) }
    viewModel { ComicDetailViewModel(get(), get(), get(), get(), get()) }
    viewModel { ComicReadViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { UserViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { DownloadViewModel(get(), get(), get()) }
    viewModel { DownloadComicDetailViewModel(get()) }
    viewModel { CheckUpdateViewModel(get()) }
}
