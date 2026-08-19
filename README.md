# JMNext

一个使用 Kotlin + Jetpack Compose 编写的 Android 漫画阅读客户端。支持在线浏览、离线下载、收藏同步与本地阅读，兼容 Android 6.0 及以上设备。

- **版本**：1.0
- **包名**：`com.jmnext.reader`
- **最低系统**：Android 6.0（API 23）

---

## 功能

**浏览与阅读**
- 首页推荐、分类浏览、每周必看
- 关键词搜索，支持 `+包含` / `-排除` 语法与历史记录
- 卷轴 / 翻页 / 点击三种阅读模式，可调预加载页数
- 车牌号（漫画编号）直达，支持剪贴板自动识别

**账号**
- 登录与自动登录、每日签到
- 收藏管理，支持多收藏夹的创建、重命名、移动
- 阅读历史、评论历史，可发表与点赞评论

**下载**
- 按章节批量下载，后台任务断点续传
- 下载完成后可完全离线阅读
- 支持导出为 PDF

**个性化**
- 日间 / 夜间 / 跟随系统，内置多套配色方案与自定义取色
- 各页面网格列数可独立调整
- 标签屏蔽，支持屏蔽模板
- 应用锁（密码 / 图案）、桌面图标伪装
- 收藏与设置的备份和恢复

---

## 技术栈

| 领域 | 选型 |
| --- | --- |
| UI | Jetpack Compose、Material 3 |
| 架构 | 分层多模块 + MVVM + Repository |
| 依赖注入 | Koin |
| 网络 | Retrofit、OkHttp |
| 图片 | Coil |
| 本地存储 | Room、SharedPreferences |
| 分页 | Paging 3 |
| 后台任务 | WorkManager |
| 数据源 | JMComic-Api-Java（内置）/ HTTP API（网络） |

---

## 工程结构

项目按职责拆分为独立的 Gradle 模块，依赖方向单向向下，由编译器强制，跨层引用会直接编译失败。

```
:app                  应用壳：Application、Activity、导航图、Manifest、资源、界面层
:core:designsystem    主题与无业务语义的通用 Compose 组件
:domain               服务层：跨页面共享的业务状态、下载编排、图片加载与解扰
:data:repository      仓库层：对上暴露统一接口，对下屏蔽数据源差异
:data:network         Retrofit 接口、拦截器链、响应加解密
:data:database        Room 实体、DAO、迁移
:data:storage         本地持久化与加密存储
:core:model           领域数据模型与界面状态模型
:core:common          日志、格式化、图片解扰算法等无依赖工具
```

依赖关系：

```
                    :app
                     │
     ┌───────────────┼────────────────┐
     ▼               ▼                ▼
:core:designsystem  :domain    :data:repository
     │               │                │
     │               ▼                ▼
     └────────► :data:storage   :data:network
                     │          :data:database
                     ▼                │
              :core:common ◄──────────┘
                     │
                     ▼
              :core:model
```

界面层在 `:app` 内按业务域组织，避免所有页面堆在同一目录：

```
app/src/main/java/com/par9uet/jm/
├── JmApplication.kt   Koin 装配、运行环境注入
├── MainActivity.kt    启动主题、Compose 入口
├── App.kt             应用锁 / 引导 / 全局提示的外层容器
├── di/                ViewModel 注册
├── ui/component/      漫画业务组件（封面、标签、网格、评论）
├── ui/feature/        按域划分的页面与 ViewModel
│   ├── home/          首页、底部导航、顶部栏
│   ├── search/        搜索、搜索结果、车牌号提取
│   ├── detail/        详情、章节、评论、相关推荐
│   ├── reader/        阅读器（卷轴 / 翻页 / 工具栏）
│   ├── download/      下载列表与下载详情
│   ├── user/          登录、签到、收藏、历史
│   ├── settings/      设置、配色、屏蔽、备份、更新、日志
│   └── shared/        跨域共用的全局 ViewModel 与占位页
└── ui/state/          导航期共享的轻量状态
```

---

## 构建

### 环境要求

- JDK 17
- Android SDK：compileSdk 36、build-tools 36.0.0
- Gradle 由 wrapper 自动下载，无需预装

### 步骤

```bash
git clone https://github.com/li1679/JMNext.git
cd JMNext
./gradlew :app:assembleRelease
```

产物位于 `app/build/outputs/apk/release/`。

运行测试：

```bash
./gradlew test
```

### 说明

- 依赖优先走阿里云镜像，缺件时自动回落官方源
- 默认不启用代理。需要时在 `gradle.properties` 中取消 `systemProp.*.proxy*` 的注释并改成自己的端口
- 版本号在 `version.properties` 中维护，由构建脚本读取
- release 默认使用 debug 签名，正式分发前请在 `app/build.gradle.kts` 中替换为自己的签名配置

---

## 关键实现

这几处逻辑对显示正确性影响较大，修改前建议先读懂，配套单元测试位于各模块的 `src/test/`。

### 图片解扰

服务端下发的图片被按行切块并倒序排列，需要按相同的分块数还原。分块数由 `:core:common` 的 `JmScramble.kt` 计算，规则与服务端一致：

| 条件 | 分块数 |
| --- | --- |
| `aid < scrambleId` | 0（无需还原） |
| `aid < 268850` | 10 |
| `aid < 421926` | `md5(aid+文件名) 末位码值 % 10 * 2 + 2` |
| 其余 | `md5(aid+文件名) 末位码值 % 8 * 2 + 2` |

注意两点：`aid` 必须取章节自身的 id（多章本子与本子 id 不同，用错会整章错位）；判定用 `<` 而非 `<=`。

### 解码缓存

缓存文件名为 `页码_s分块数.webp`。分块数写进文件名是必要的：缓存键若只有页码，一次错误解码的结果会被下载流程长期复用，且难以察觉。

### 下载目录

章节目录命名为 `章节名_章节id`。章节 id 不可省略——章节名可能为空或在同一本书内重复，仅用名字会让多个章节写进同一目录，而页面文件名是 `0.webp / 1.webp …`，后写入的章节会覆盖前一章，表现为几章内容混在一起。

读取时保留了对旧命名目录的回退，用于兼容早期版本下载的内容。

### 网络层

请求经过的拦截器顺序为：初始化等待 → 错误提示 → 线路选择 → 签名。

- 线路失效时按候选列表自动回退，并记住最后可用的线路
- 签名时间戳每个请求单独生成，避免应用长期驻留后台后过期
- 404 只写入日志不弹提示（线路问题用户无法处理，已有自动回退兜底）；提示统一在 `ToastManager` 去重节流

### 分层约束

下层模块拿不到宿主应用的 `BuildConfig` 与 `R`，因此这两项由 `:app` 在启动时注入一次：

- `AppEnv.init(BuildConfig.DEBUG)` —— 决定日志是否输出到 logcat
- `NotificationIcon.init(R.drawable.…)` —— 通知使用的小图标

服务层不引用任何 Activity。更新完成通知通过 `getLaunchIntentForPackage` 打开应用，路由信息由 Intent extra 传递。

---

## 常见问题

**Gradle 同步失败**
检查 JDK 是否为 17。若卡在依赖下载，可能是镜像回源异常，稍后重试或在 `settings.gradle.kts` 中调整仓库顺序。

**提示网络错误**
通常是当前 API 线路失效。应用会自动尝试其他线路，也可在「设置 → 连接 → API」中手动切换。

**图片能加载但显示错乱**
属于解扰参数问题，参见上文「图片解扰」。可先在「设置 → 缓存清理」中清除解码缓存再试。

**下载的内容与章节对不上**
早期版本的目录命名缺陷所致，删除该漫画后重新下载即可。

**安装时提示签名冲突**
本项目使用独立包名与签名，与其他同类应用无法互相覆盖，需先卸载旧应用。

---

## 免责声明

本项目仅供学习与技术交流，仅实现客户端功能，不提供、不存储任何内容资源。使用者应遵守所在地区的法律法规，并自行承担使用过程中的全部风险与责任。

---

## License

本项目以 [GPL-3.0](LICENSE) 开源。

客户端部分基于 [jm-mobile](https://github.com/Dedicatus546/jm-mobile) 魔改而来，数据源能力来自 [JMComic-Api-Java](https://github.com/jukomu/JMComic-Api-Java)，在此致谢。
