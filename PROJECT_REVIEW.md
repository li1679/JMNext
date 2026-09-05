# JMNext 项目审查与待确认修改清单

日期：2026-09-05。阶段：首轮整改已实施，后续 UI 深度细化仍按覆盖表推进。

本版范围已按用户最新要求调整：只考虑新应用，不做旧 Android 系统适配，也不做旧备份、旧设置、旧数据库、旧下载目录的数据迁移。下列删除均为待实施方案，本轮未删除文件、清空数据或修改应用身份。最低 Android 版本尚未指定，应在实现前确定具体基线；不默认把“新应用”解释为仅支持某一个最新版本。

## 范围与证据边界

- 清点了 303 个 Git 跟踪文件、226 个 Kotlin 源码及测试文件；检查导航入口、各功能 UI、共享组件、设计系统、存储、网络、下载和构建配置。采用跨模块扫描与关键调用链精读，不宣称逐行审完全部源码。
- 首页分类的整页移动、错误状态缺失、下载文件处理等结论有代码依据；视觉方向是整改建议。
- `adb devices -l` 无连接设备；未取得当前版本的运行截图、帧率、TalkBack 或大字体实测。全 UI 的运行验收应在实施阶段完成。
- 已应用 Android 开发测试和 UI 审美规范技能；Compose 约束顺序与触控规则通过 Context7 核对 Android 官方文档。

## 优先修复的问题

P1：影响核心体验、数据完整性或保护语义，应优先处理。P2：明确的功能、适配或可维护性问题。下列位置均相对项目根目录，行号对应本轮审查的源码。

| 编号 | 优先级 | 问题与触发条件 | 证据位置 | 建议修改与验收 |
|---|---|---|---|---|
| R01 | P1 | 首页分类切换时，搜索、快捷入口、标题和分类栏随漫画列表一起横向移动，形成整页跳转感。 | `app/src/main/java/com/par9uet/jm/ui/feature/home/HomeScreen.kt:234`、`:254`、`:279` | 公共头部和分类栏移出横向切换区域；只有列表转换。以分类稳定标识管理选中与滚动位置，快速连点只响应最终目标；返回首页恢复分类和位置。动效形式待确认。 |
| R02 | P1 | 下载直接写最终 `.webp`，恢复时仅凭文件存在跳过；进程退出、磁盘满或压缩失败后，残缺文件可能被当成成功缓存。 | `domain/src/main/java/com/par9uet/jm/domain/worker/DownloadComicWorker.kt:210`、`:231` | 同目录临时文件写入，检查写入结果后原子提交；恢复时检查有效性；失败清理临时产物。增加写入中断与损坏文件恢复测试。 |
| R03 | P1 | 备份的密码/图案只用于校验，`data` 仍是可直接读取和修改的 JSON；修改保护元信息还能绕开应用内核验。 | `domain/src/main/java/com/par9uet/jm/domain/store/BackupManager.kt:102`、`:127`、`:134` | 推荐认证加密、随机盐与成熟密码派生方案，只定义新备份格式；旧格式直接明确拒绝，不提供迁移。或明确改为“恢复校验”，不能让用户误以为文件已加密。 |
| R04 | P1 | 搜索首次失败分支只有错误文字并提前返回，没有重试入口；首页未使用其 `isError/errorMsg`，请求失败会被展示成无内容。共享分页网格只处理追加失败。 | `app/src/main/java/com/par9uet/jm/ui/feature/search/ComicSearchResultScreen.kt:153`；`app/src/main/java/com/par9uet/jm/ui/feature/home/HomeScreen.kt:207`；`app/src/main/java/com/par9uet/jm/ui/component/PullRefreshAndLoadMoreGrid.kt:64` | 统一首次加载、刷新失败、追加失败、真正空数据和全部被过滤的状态；提供相应重试操作，刷新失败保留现有内容。 |
| R05 | P2 | 下载速度按漫画分组存放，但各章节开始会重置同组样本、结束会移除整组；并发更新普通 Map 和读改写 StateFlow 也可能丢计数。当前“速度”计算的是压缩后的文件字节。 | `core/common/src/main/java/com/par9uet/jm/core/common/DownloadSpeedTracker.kt:25`、`:36`、`:48`；`domain/src/main/java/com/par9uet/jm/domain/worker/DownloadComicWorker.kt:235` | 以任务管理生命周期，再汇总分组；串行或原子更新采样；明确显示网络速度还是落盘速度。测试同组多章节开始、结束、暂停、重试。 |
| R06 | P2 | 缓存清理直接相加父目录和子目录，选择“全部应用缓存”及其子项会重复估算；忽略删除返回值，却按删除前大小提示已清理。 | `app/src/main/java/com/par9uet/jm/ui/feature/settings/CacheCleanupScreen.kt:160`、`:196`、`:203` | 对选中路径去重和消除包含关系；检查失败并报告实际清理结果；保持文件与数据库一致。 |
| R07 | P2 | 签到日历允许前后 500 个月，但所有月份按日号读取同一份签到数据，错误复用本月标记；“已连续”展示的是遍历所得最长连续段。 | `app/src/main/java/com/par9uet/jm/ui/feature/user/SignInScreen.kt:110`、`:114`、`:233`、`:253` | 依据接口真实能力限制月份或按月加载；按日期排序计算，区分当前连续与最长连续。测试跨月和中断签到。 |
| R08 | P2 | 登录和阅读工具栏先 `fillMaxWidth()` 再 `widthIn(max=...)`，前者收紧最小宽度，使后面的最大宽度限制失效。下载页另有基于屏幕而非容器尺寸的布局判定。 | `app/src/main/java/com/par9uet/jm/ui/feature/user/LoginScreen.kt:132`；`app/src/main/java/com/par9uet/jm/ui/feature/reader/ToolsBar.kt:65`；`app/src/main/java/com/par9uet/jm/ui/feature/download/DownloadScreen.kt:337` | 按实际窗口约束确定断点；先限宽再填充，并在父布局中居中。验证平板、横屏和分屏。 |
| R09 | P2 | 阅读进度条仅有 Canvas 与原始指针处理，没有可访问的进度调节语义；提取预览里的编号 Chip 有点击语义但回调为空。 | `app/src/main/java/com/par9uet/jm/ui/feature/reader/ToolsBar.kt:163`；`app/src/main/java/com/par9uet/jm/ui/feature/search/ExtractCodeScreen.kt:235` | 优先标准 Slider，或补齐范围、值和设置进度语义；编号改为普通标记或真实复制操作。实测 TalkBack。 |
| R10 | P2 | 首页初始骨架默认固定 3 列，真实网格按 118dp 自适应；搜索骨架也固定 3 列，无法匹配用户自定义列数。 | `app/src/main/java/com/par9uet/jm/ui/feature/home/HomeScreen.kt:170`；`app/src/main/java/com/par9uet/jm/ui/feature/search/ComicSearchResultScreen.kt:57`；`app/src/main/java/com/par9uet/jm/ui/component/AdaptiveComicGrid.kt:6` | 骨架和内容共用列数、封面比例、标题高度、间距和容器尺寸，避免加载完成重新排版。同步检查历史与收藏的骨架。 |
| R11 | P2 | 首页加载每次直接新建协程，没有正在加载去重或最后请求判定；快速重复刷新存在旧响应覆盖新响应和 loading 提前结束的风险。 | `app/src/main/java/com/par9uet/jm/ui/feature/home/ComicViewModel.kt:61` | 根据请求语义去重或取消旧任务，用受控响应顺序测试证明最终状态。此项是代码可见的竞态风险，尚未设备复现。 |
| R12 | P2 | 解密、反序列化和备份解析多处把异常变成 null/空数据，难以区分首次使用、数据损坏和真实读取失败。 | `data/storage/src/main/java/com/par9uet/jm/data/storage/SecureStorage.kt:24`；`domain/src/main/java/com/par9uet/jm/domain/store/BackupManager.kt:166` | 返回明确错误结果；仅验证新格式，不静默用默认配置掩盖损坏，尤其核查应用锁与恢复流程。 |
| R13 | P1 | 本地存储加密失败时静默退回 `plain:` 加 Base64；除旧 Android 的主动分支外，新系统上密钥或加密操作失败也会触发，敏感设置和登录数据可能失去加密。 | `data/storage/src/main/java/com/par9uet/jm/data/storage/CryptoManager.kt:60`、`:76` | 只保留 Keystore 认证加密路径；失败明确上报且不写入明文；删除旧明文与旧 IV 排列读取路径。 |
| R14 | P1 | 两条网络链路发送 Cookie 时都没有按目标域、路径、secure 标记和有效期筛选：一条返回全部 Cookie，另一条直接拼接 Cookie 头。 | `data/network/src/main/java/com/par9uet/jm/data/network/Retrofit.kt:48`；`data/repository/src/main/java/com/par9uet/jm/data/repository/impl/EmbeddedClientManager.kt:54` | 统一 Cookie 匹配与存储；仅对匹配请求发送，清理过期项，明确跨域登录策略；加跨域、路径、过期和并发用例。代码缺陷已确认，未声称已发生凭证外泄。 |
| R15 | P2 | 域名初始化超时后强行 `setInitialized(true)`，把等待结束等同于初始化成功，实际准备失败被隐藏。 | `data/repository/src/main/java/com/par9uet/jm/data/repository/impl/EmbeddedClientManager.kt:50`、`:87` | 移除旧系统假成功补丁；保留合理超时与协程取消，并返回真实错误和重试入口。 |
| R16 | P2 | 单页下载解码的 `catch (Exception)` 包装了取消异常，并把不同原因统一描述为超时；外层本来用于取消的分支可能收不到原始取消类型。 | `domain/src/main/java/com/par9uet/jm/domain/worker/DownloadComicWorker.kt:225` | 区分单页超时、用户/父任务取消和真实失败；取消原样传播，错误保留原因。具体 WorkManager 最终状态需通过暂停/删除测试验证。 |
| R17 | P2 | Release 网络配置默认信任用户安装的 CA；旧 TLS helper 对所有版本启用 TLS 1.0/1.1 并自定义 TLS 1.2 上下文。 | `app/src/main/res/xml/network_security_config.xml:7`；`core/common/src/main/java/com/par9uet/jm/core/common/TlsCompat.kt:15` | 新应用使用现代系统/OkHttp 默认安全配置；用户 CA 如仅为调试则限制在 debug。删除旧系统 TLS 补丁和冲突配置，不能简单删除所有 AndroidX Compat API。 |
| R18 | P2 | Release 与 debug 使用同一调试签名，发布身份和调试身份没有分离。 | `app/build.gradle.kts:95` | 新应用建立明确的发布签名配置，凭据放外部安全配置；不把私钥纳入仓库，也不因取消旧数据迁移而自动生成或替换证书。 |
| R19 | P2 | 下载异常进入退避重试时立即返回 `Result.retry()`，没有同步清理速度或更新等待状态；状态可能仍显示 downloading，且该分支未记录异常原因。 | `domain/src/main/java/com/par9uet/jm/domain/worker/DownloadComicWorker.kt:117` | 将实际执行、等待重试、暂停、失败状态区分清楚，按生命周期清理采样并记录可诊断的原因。设备上验证断网和服务端错误。 |

下载取消和重下还需要补专项验证：`DownloadManager.cancelDownloads()` 发起取消后立即返回，重下路径在重新入队前删旧文件。应核验任务停止、写入和删文件的时序，不能仅凭“调用过 cancel”假定旧任务已停止写入。此项暂列风险，不计为已复现故障。

## 新应用基线清理

以下是用户确认的新范围带来的整改项，不全部属于现有功能缺陷。

| 编号 | 清理项目 | 位置与处理 |
|---|---|---|
| N01 | 统一最低 Android 版本 | 目前 app/core/data/domain 各模块 `minSdk=23`，README 也承诺 Android 6。按待确定的新基线统一构建和文档，不再为更低系统保留分支。 |
| N02 | 删除低版本专用补丁 | `TlsCompat.kt`、`CryptoManager.kt`、`bitmap.kt`、`EmbeddedClientManager.kt` 中的旧系统分支；主题、模糊、通知的版本判断仅删除低于新基线的部分。新支持范围内的权限差异仍要正确处理。 |
| N03 | 删除旧备份格式支持 | `BackupManager.kt` 不再解析 v1/v2/v3 历史结构与旧保护方式；新格式有明确标识和版本，不支持的文件明确报错。 |
| N04 | 删除旧设置与密文迁移 | `LocalSettingStorage.kt` 的旧字段判断、`appLockType` 迁移、旧模板生成；`LocalSetting.homeExcludedTags` 及双写；旧明文、旧 IV 排列解密。保留新安装默认值与新格式校验。 |
| N05 | 移除历史数据库迁移 | `Migrations.kt` 中 2→3、3→4 的迁移及专用旧库测试退出新基线；建立新应用 schema 与建库测试。当前 schema 导出仍有价值，不添加静默删库兜底。 |
| N06 | 移除旧下载布局读取 | `ComicDownloadCache.kt` 的 legacy 路径、`ComicReadViewModel.kt` 和 `PdfExport.kt` 的旧目录/旧归档读取链路只在确认属于历史格式后移除；新下载、阅读和导出共用唯一格式。 |
| N07 | 清理相关依赖、说明与测试 | 同步移除失去调用的 helper、常量、注释和旧格式测试；desugaring/AndroidX 依赖依据新支持范围和实际引用决定，不能按名字或旧注释批量删除。新应用未来升级仍需维护自己的格式契约。 |

## 代码规范与删除清单

| 编号 | 拟整改内容 | 具体范围与边界 |
|---|---|---|
| C01 | 删除未调用的旧实现 | 全仓 Kotlin 引用搜索仅发现定义的 `ComicLazyGrid`、`AppendListUIState`、`PageAppendUIState`、`ListUIState`、`AutoLogin`、`BaseRepository.safeStringCall`、`DownloadSpeedTracker.getSpeed`、`LocalSettingManager.updateHomeExcludedTags`。删除前复核全部文件、反射、序列化与对外使用，再编译；不能把“文件名旧”当成删除依据。`LoadMore` 仅被旧 `ComicLazyGrid` 调用，应一起复核。 |
| C02 | 删除未使用资源 | Lint 提示 8 个 drawable：`baseline_login_24`、`bookmarks_icon`、`chevron_left_icon`、`comment_icon`、`favorite_icon`、`login_icon`、`logout_icon`、`search_icon`。复核动态资源访问后删除。 |
| C03 | 精简无用注释与配置 | 删除叙述赋值/布局动作的注释、历史修改过程、已注释废代码、模板说明及被移除的兼容分支说明；保留算法、并发与当前格式约束。例：`AppScreen.kt` 导入间多余空行、`TabScreen.kt` 未使用路由/设置订阅、`Comic.kt` 未使用 shadow 导入、空 `composeCompiler {}`、`gradle.properties` 大段模板和历史代理说明。 |
| C04 | 统一格式与 Compose 接口 | 统一 import、Modifier 参数位置和默认值、可见性、常量与文件命名。处理 Lint 的 4 条 Modifier 规范、14 条基础类型 State 提示、2 条 offset 读取提示、2 条 Locale 警告。选用一套适配当前 Kotlin/AGP 的格式检查工具，不叠加多个相同工具。 |
| C05 | 按职责整理代码位置 | 阅读页 766 行、设置页 734 行、引导页 670 行、收藏页 643 行。按页面状态、动作处理、弹窗和可复用控件拆分；首页/搜索/每周目前共享 `ComicViewModel`，调整时明确作用域和返回恢复契约。已按 feature 分包的结构继续使用。 |
| C06 | 收紧模块边界 | 页面中的网络请求、文件处理和下载数据库动作经 ViewModel/服务处理；纯 UI 组件通过参数接收状态与事件。主题组件目前直接依赖存储，建议由 app 读取设置后传入主题，使 designsystem 可以独立预览和测试。按实际依赖调整 `api/implementation`。 |
| C07 | 收敛重复状态和样式 | 首页索引包装与 Pager 双重保存需整理；标签扩展色三组字段现已全相同，可统一颜色角色，但保留内容/角色/作品的语义分组。整理重复 Context 转 Activity helper、设置行、状态页和骨架逻辑。 |
| C08 | 整理资源和构建约定 | 用户可见字符串归入资源，清理散落的 Unicode 转义及不一致文案；新增代码按统一约定组织。APK 导出从 assemble 的固定外部路径副作用改为明确的可配置任务，避免换位置后意外写出项目。 |

不会作为“无用东西”删除：`LICENSE`、Gradle Wrapper、新应用的 Room schema、用户主题选项及现有功能。历史迁移与兼容代码按 N01–N07 清理。`.gradle`、`.kotlin`、`build` 是生成缓存，已被忽略，不应混同废源码批量清理。发布包压缩资源已开启，不能承诺删几个图标会显著减少 APK。

## 全 UI 整改覆盖表

本表是基于现有 UI 源码的方案，细节以实施后的设备截图为准。默认建议保留现有中性色及可选主题，不替用户决定品牌重设计。

| 页面/组件 | 拟调整方向 | 验收重点 |
|---|---|---|
| 首页、底部导航、每周推荐 | 分离公共头部与列表切换；减少标题/分类重复；统一标签选中与滚动指示，公共区域不横向移动。纵向滚动时是否吸顶在实现前明确。 | 远距离切分类、快速连点、手势切换、每类滚动位置、返回恢复、首次失败。 |
| 搜索输入、结果、提取 | 合并重复的输入装饰；保持搜索/排除条件清楚可编辑；补重试；提取过程使用单一加载反馈，预览编号不做空点击。 | 软键盘、小屏长文本、过滤全空、输入错误、重复操作、旋转恢复。 |
| 详情、章节、评论 | 保持真实封面可读和阅读主操作突出；梳理标题/作者/标签/统计层级；统一章节选择和评论回复控件。已有头图装饰是否简化属于视觉确认项。 | 长标题、超长标签、评论展开、章节当前态、选集/阅读按钮、大字体。 |
| 在线/离线阅读 | 减少工具栏厚重容器与阴影；统一上下章、页码、进度、缩放重置；补进度可访问性，保持漫画为主要内容。 | 阅读模式切换、捏合与翻页冲突、恢复页码、章节切换、返回、系统栏、低内存。 |
| 下载列表、下载详情 | 用清晰行结构统一排队/下载/暂停/重试/完成状态；速度和总进度口径一致；批量操作与单项操作位置统一。 | 并行章节、暂停继续、磁盘失败、删除/重下、封面与网格切换、PDF 导出。 |
| 收藏、收藏夹、阅读历史、评论历史 | 统一筛选行、选择态和批量操作；避免多个同权重操作挤占内容；复用空/错状态与骨架。 | 登录前后、长收藏夹名、全选、移动、删除、返回后选择态。 |
| 我的、登录 | 减少重复等级/经验信息与装饰容器；登录表单正确限宽，错误跟随对应输入或提交区域。 | 平板表单宽度、键盘、大字体、登录完成返回路径。 |
| 签到 | 先纠正月份与数据对应，再统一日历、今日/已签/奖励状态；收敛重复进度装饰。 | 当前月、跨月、断签、当天更新、横屏和大字体。 |
| 设置总页、排除标签 | 统一设置行、分组标题、开关、选择值、滑块和间距；减少整节套卡片；长标签和模板管理保持可操作。 | 状态一致、保存/取消、长名称、字体放大。 |
| 配色 | 使用色样和明确选中语义；保留现有预设、自定义和莫奈入口；主题在正文、弹窗、禁用和错误态一致。 | 深浅色、系统主题、对比度、自定义次色/错误色是否真正生效。 |
| 应用锁设置、解锁、首次引导 | 统一密码键盘、图案、选项控件和反馈；降低不必要的解释文字密度，保留必需说明与选择。 | 小屏/横屏、错误抖动、TalkBack、组合解锁、进后台恢复、引导跳过。 |
| 备份恢复、缓存清理 | 将大说明卡和大操作卡收敛为清楚的内容选择、操作和结果；保护语义与实现一致；明确目录备份不含图片。 | 新格式备份/恢复闭环、不支持格式拒绝、错误密码、损坏文件、部分失败、取消、重复路径和真实清理大小。 |
| 更新、版本弹窗、关于、日志 | 统一更新状态与按钮层级；日志刷新/复制/清空用紧凑工具栏；关于页突出必要版本信息。 | 更新失败重试、后台下载、通知返回、日志长行、大字体和弹窗高度。 |
| 启动加载、警告、所有通用弹窗 | 统一 loading/empty/error/success；减少轮播式功能提示和重复进度指示；统一容器、按钮、字阶与间距。 | 慢网、离线、不同主题、键盘和系统栏遮挡、减少动画设置。 |

原生规范判断：已有 Material 3、NavigationBar/NavigationRail、生命周期感知状态收集、Lazy 列表和稳定封面比例，具备可继续整改的原生基础。主要缺口是自绘控件语义、部分限宽、状态展示和页面间视觉一致性。本轮不以静态代码给运行性能或视觉效果打实测分数。

## 需要确认的决策

1. 首页分类效果：推荐公共头部不动、列表柔和淡入淡出；另一选项是公共头部不动、只有列表左右滑动。保留手势时需统一手势跟随与点击切换规则。
2. 审美幅度：推荐保留现有风格做系统性精修；另一选项是明显重新设计，同时保留全部功能和主题选项。
3. 备份保护：推荐为新应用定义真正加密的单一新格式，不支持旧备份迁移；另一选项是明文 JSON 并准确标注仅恢复校验。
4. 新应用最低 Android 版本尚未指定，实施前需明确具体 API 基线。旧系统和旧数据兼容已确认不做，无需再次确认这两项。
5. 发布签名和包名属于新应用发布配置；需要实际发布身份与密钥来源，不能从“不迁移旧数据”推断出应自动更换包名或覆盖密钥。

## 自动检查结果

- `:app:lintDebug --offline --console=plain`：执行成功，0 错误、23 警告、14 提示。报告位于 `app/build/reports/lint-results-debug.html`。
- 23 条警告：8 未使用资源、4 Modifier 参数、2 容器尺寸、2 Locale、2 依赖更新提示、2 高版本属性提示、2 状态 offset、1 用户 CA。高版本属性提示不等同运行缺陷；依赖升级提示不等于必须升级，也不据此声称存在漏洞。
- `:app:testDebugUnitTest :core:common:testDebugUnitTest :data:repository:testDebugUnitTest :domain:testDebugUnitTest --offline --console=plain --quiet`：24 项测试通过，0 失败、0 错误、0 跳过。
- 未运行：设备 UI、Room instrumentation 迁移测试、截图对照、性能采样和 release R8 构建。本轮无连接设备，且当前阶段未实施修改；既有测试通过不代表上述新发现已有覆盖。
- 应用源码、构建配置和资源未改；仅新增本审查文档。自动检查产生忽略目录下的构建报告。

## 确认后的实施与验收顺序

1. 首页切换、错误状态与稳定骨架；以首页任务为第一个可独立验收批次。
2. 下载文件完整性、进度并发、取消时序、缓存清理和签到数据修复；根据已确认选择处理备份保护。
3. 删除有证据的废实现，统一格式、注释和文件职责；同步更新调用方、依赖与必要回归测试。
4. 按覆盖表统一所有 UI，保留业务入口与用户数据；手机、平板、深浅色、大字体和横屏分别验收。
5. 最终运行单元测试、Lint、新应用建库与存储格式测试、Compose UI 测试、release 构建；关键首页切换/阅读/下载采集运行证据。使用每帧预算而不是主观动画时长判定流畅度。不测试旧版本数据迁移。

删除项、接口变更和行为变化均在最终修改清单逐项说明；发现依赖用户偏好或新应用行为约定的歧义时先问清楚。

## 官方核对资料

- Compose constraints and modifier order: https://developer.android.com/develop/ui/compose/layouts/constraints-modifiers
- Compose accessibility defaults: https://developer.android.com/develop/ui/compose/accessibility/api-defaults
