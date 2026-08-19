# 本模块的混淆规则，随模块一起提供给使用方。

# 接口响应模型的字段名即 Gson 的 JSON 键，不能被混淆
-keep class com.par9uet.jm.data.network.model.** { *; }
