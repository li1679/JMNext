# JMNext

JMNext 是一个使用 Kotlin 和 Jetpack Compose 编写的 Android 漫画阅读客户端，支持 Android 6.0 及以上版本。

项目基于 [JMComic-Api-Java](https://github.com/jukomu/JMComic-Api-Java) 提供内置数据源能力，致力于提供简洁、稳定的漫画浏览、阅读和下载体验。

## 主要功能

- 首页推荐、分类浏览、每周推荐
- 漫画搜索、编号直达和剪贴板识别
- 漫画详情、章节阅读和评论互动
- 卷轴、翻页、点击等多种阅读模式
- 收藏夹管理、阅读历史和每日签到
- 后台下载、离线阅读和 PDF 导出
- 全局标签排除与搜索标签筛选
- 日间、夜间、自定义配色和网格布局设置
- 应用锁、图标伪装、数据备份与恢复

## 技术信息

- UI：Jetpack Compose、Material 3
- 架构：多模块、MVVM、Repository
- 依赖注入：Koin
- 网络与图片：OkHttp、Retrofit、Coil
- 本地数据：Room、SharedPreferences
- 分页与后台任务：Paging 3、WorkManager
- 数据源：[JMComic-Api-Java](https://github.com/jukomu/JMComic-Api-Java) 内置 API

## 项目信息

- 包名：`com.jmnext.reader`
- 最低系统：Android 6.0（API 23）
- 开源协议：[GPL-3.0](LICENSE)

本项目仅提供客户端实现，不提供或存储漫画内容资源。请遵守所在地区的法律法规。

客户端部分基于 [jm-mobile](https://github.com/Dedicatus546/jm-mobile) 修改而来，感谢相关开源项目的贡献。
