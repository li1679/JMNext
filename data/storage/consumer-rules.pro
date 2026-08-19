# 本模块的混淆规则，随模块一起提供给使用方。

# 阅读进度按 Gson 存盘。字段被混淆后同一版本内读写自洽不会立刻出错，
# 但混淆名在下次构建中改变时，用户的阅读历史会静默丢失。
-keep class com.par9uet.jm.data.storage.ComicReadHistory { *; }
