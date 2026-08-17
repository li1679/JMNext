package com.par9uet.jm.retrofit

/**
 * 禁漫移动端 API 的签名密钥。
 * token 计算与响应体解密共用同一个值，两种 API 模式都是它 ——
 * 曾经内置模式误用了截断值 "18comicAPP"，导致所有走 Retrofit 的请求
 * 签名校验失败，启动即弹「网络错误」。
 */
const val API_TOKEN_SECRET = "185Hcomic3PAPP7R"

/**
 * 上报给服务端的客户端版本号。
 * 版本过旧时服务端可能直接拒绝请求，需跟随官方客户端更新。
 */
const val API_VERSION = "2.0.30"
