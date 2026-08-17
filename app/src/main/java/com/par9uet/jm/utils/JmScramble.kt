package com.par9uet.jm.utils

/**
 * 禁漫图片扰乱还原算法。
 *
 * 图片在服务端被按行切成若干块并上下翻转排列，客户端需要按相同的
 * 分块数还原。分块数只取决于 aid、scrambleId 和文件名，与图片内容无关。
 *
 * 抽成不依赖 Android 的纯函数，便于用单元测试锁定行为：
 * 这段逻辑一旦算错就是整页错版，且错误结果会被写进缓存长期沿用。
 */

/** 该值之前的本子固定切成 10 块 */
const val SCRAMBLE_268850 = 268850

/** 2023-02-08 起启用新的取模基数 */
const val SCRAMBLE_421926 = 421926

/** 页面结构变动导致 scramble_id 解析失败时的兜底值 */
const val DEFAULT_SCRAMBLE_ID = 220980

/**
 * 计算图片的纵向分块数。
 *
 * @param aid photo 页面里的 `var aid`，即章节自身的 id；多章本子与本子 id 不同
 * @param scrambleId 服务端下发的 scramble_id，早于它的本子未做扰乱
 * @param pageStr 图片文件名，不含扩展名（如 "00001"）
 * @return 分块数；返回 0 表示该图无需还原
 */
fun calculateScrambleSeed(aid: Int, scrambleId: Int, pageStr: String): Int {
    if (aid < scrambleId) return 0
    if (aid < SCRAMBLE_268850) return 10

    val modulus = if (aid < SCRAMBLE_421926) 10 else 8
    val keyMd5 = md5("$aid$pageStr")
    return (keyMd5.last().code % modulus) * 2 + 2
}
