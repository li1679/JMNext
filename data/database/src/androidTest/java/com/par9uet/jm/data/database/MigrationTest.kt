package com.par9uet.jm.data.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 迁移脚本测试。需要连接设备或模拟器：
 *   ./gradlew :data:database:connectedDebugAndroidTest
 *
 * 数据库已移除破坏性重建兜底（见 DatabaseModule），缺迁移会让应用启动即崩，
 * 所以每次改表结构都应先让这里通过。
 *
 * 这里手工建出旧版本表结构再执行迁移，而不是用 MigrationTestHelper：
 * v2/v3 的 schema 快照在当初 exportSchema=false 时期从未生成过，
 * 而 createDatabase(旧版本) 必须读到对应快照才能工作。
 * schemas/ 下现已有 4.json，今后新增版本可以改用 MigrationTestHelper。
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private companion object {
        const val TEST_DB = "migration-test.db"

        /** v2 的表结构，取自加入分组字段之前的实体定义 */
        const val CREATE_V2 = """
            CREATE TABLE IF NOT EXISTS download_comics (
                id INTEGER NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                authorList TEXT NOT NULL,
                coverPath TEXT NOT NULL,
                zipPath TEXT NOT NULL,
                progress REAL NOT NULL,
                status TEXT NOT NULL,
                createTime INTEGER NOT NULL
            )
        """
    }

    private var db: SupportSQLiteDatabase? = null

    @After
    fun tearDown() {
        db?.close()
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(TEST_DB)
    }

    private fun openEmptyDb(): SupportSQLiteDatabase {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB)
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DB)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit
                override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) = Unit
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase.also { db = it }
    }

    private fun SupportSQLiteDatabase.columns(): Set<String> =
        query("PRAGMA table_info(download_comics)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            buildSet {
                while (cursor.moveToNext()) add(cursor.getString(nameIndex))
            }
        }

    /** 2→3 增加分组字段，且原有下载记录必须保留 */
    @Test
    fun migrate2To3_addsGroupColumnsAndKeepsRows() {
        val database = openEmptyDb()
        database.execSQL(CREATE_V2)
        database.execSQL(
            "INSERT INTO download_comics " +
                "(id, name, authorList, coverPath, zipPath, progress, status, createTime) " +
                "VALUES (1, 'keep-me', '[]', '', '', 0.0, 'complete', 0)"
        )

        MIGRATION_2_3.migrate(database)

        assertTrue(database.columns().containsAll(listOf("groupId", "groupName", "chapterName")))
        database.query(
            "SELECT name, groupId, groupName, chapterName FROM download_comics WHERE id = 1"
        ).use {
            assertTrue("迁移后原有下载记录丢失", it.moveToFirst())
            assertEquals("keep-me", it.getString(0))
            assertEquals(0, it.getInt(1))
            assertEquals("", it.getString(2))
            assertEquals("", it.getString(3))
        }
    }

    /** 3→4 增加 tagList，旧行应拿到默认的空 JSON 数组 */
    @Test
    fun migrate3To4_addsTagListWithDefault() {
        val database = openEmptyDb()
        database.execSQL(CREATE_V2)
        MIGRATION_2_3.migrate(database)
        database.execSQL(
            "INSERT INTO download_comics " +
                "(id, name, authorList, coverPath, zipPath, progress, status, createTime, " +
                "groupId, groupName, chapterName) " +
                "VALUES (2, 'tagged', '[]', '', '', 0.0, 'complete', 0, 0, '', '')"
        )

        MIGRATION_3_4.migrate(database)

        assertTrue(database.columns().contains("tagList"))
        database.query("SELECT tagList FROM download_comics WHERE id = 2").use {
            assertTrue(it.moveToFirst())
            assertEquals("[]", it.getString(0))
        }
    }

    /** 全链路：v2 建库后依次跑完所有迁移，列集合应与最新实体一致 */
    @Test
    fun migrateAll_producesLatestSchema() {
        val database = openEmptyDb()
        database.execSQL(CREATE_V2)

        ALL_MIGRATIONS.forEach { it.migrate(database) }

        assertEquals(
            setOf(
                "id", "name", "authorList", "tagList", "coverPath", "zipPath",
                "progress", "status", "createTime", "groupId", "groupName", "chapterName"
            ),
            database.columns()
        )
    }
}
