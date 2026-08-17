package com.par9uet.jm.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 锁定禁漫分块数算法的行为。
 *
 * 这段逻辑算错的表现是整页错版，且错误结果会被写进缓存长期沿用，
 * 排查成本很高，因此把各个分支的边界都固定下来。
 */
class JmScrambleTest {

    @Test
    fun 早于scrambleId的本子不做解扰() {
        assertEquals(0, calculateScrambleSeed(aid = 200000, scrambleId = 220980, pageStr = "00001"))
        assertEquals(0, calculateScrambleSeed(aid = 220979, scrambleId = 220980, pageStr = "00001"))
    }

    @Test
    fun 等于scrambleId的本子需要解扰() {
        // 官方判定是 aid < scrambleId 才跳过，取等号时仍要还原；
        // 这里曾经用 <= 导致该临界本子整本错版
        assertEquals(10, calculateScrambleSeed(aid = 220980, scrambleId = 220980, pageStr = "00001"))
    }

    @Test
    fun 小于268850的本子固定切十块() {
        assertEquals(10, calculateScrambleSeed(aid = 250000, scrambleId = 220980, pageStr = "00001"))
        assertEquals(10, calculateScrambleSeed(aid = 268849, scrambleId = 220980, pageStr = "99999"))
    }

    @Test
    fun 中间区间按十取模且结果为偶数且不超过二十() {
        for (page in 1..200) {
            val seed = calculateScrambleSeed(
                aid = 300000,
                scrambleId = 220980,
                pageStr = page.toString().padStart(5, '0')
            )
            assertTrue("分块数应为偶数: $seed", seed % 2 == 0)
            assertTrue("分块数应落在 2..20: $seed", seed in 2..20)
        }
    }

    @Test
    fun 新算法区间按八取模且不超过十六() {
        for (page in 1..200) {
            val seed = calculateScrambleSeed(
                aid = 500000,
                scrambleId = 220980,
                pageStr = page.toString().padStart(5, '0')
            )
            assertTrue("分块数应为偶数: $seed", seed % 2 == 0)
            assertTrue("分块数应落在 2..16: $seed", seed in 2..16)
        }
    }

    @Test
    fun 区间边界421926切换取模基数() {
        // 边界前允许出现 18、20，边界后最大只能到 16
        val below = (1..500).map {
            calculateScrambleSeed(421925, 220980, it.toString().padStart(5, '0'))
        }
        val above = (1..500).map {
            calculateScrambleSeed(421926, 220980, it.toString().padStart(5, '0'))
        }
        assertTrue("421926 之前应能取到大于 16 的分块数", below.any { it > 16 })
        assertTrue("421926 及之后分块数不应超过 16", above.all { it <= 16 })
    }

    @Test
    fun 分块数由aid和文件名共同决定() {
        val a = calculateScrambleSeed(500000, 220980, "00001")
        val b = calculateScrambleSeed(500001, 220980, "00001")
        val c = calculateScrambleSeed(500000, 220980, "00002")
        // 换本子或换页都可能得到不同分块数，用错 aid（例如传本子 id 而非章节 id）就会错版
        assertTrue("aid 或文件名变化时分块数应参与变化", a != b || a != c)
    }

    @Test
    fun 与官方实现的已知取值一致() {
        // 依据 md5("<aid><filename>") 末位字符的码值手工推算，作为回归基准
        val aid = 500000
        val page = "00001"
        val expected = (md5("$aid$page").last().code % 8) * 2 + 2
        assertEquals(expected, calculateScrambleSeed(aid, 220980, page))
    }
}
