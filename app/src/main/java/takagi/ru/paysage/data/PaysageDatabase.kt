package takagi.ru.paysage.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import takagi.ru.paysage.data.dao.BookDao
import takagi.ru.paysage.data.dao.BookSourceDao
import takagi.ru.paysage.data.dao.BookmarkDao
import takagi.ru.paysage.data.dao.CategoryDao
import takagi.ru.paysage.data.dao.FolderDao
import takagi.ru.paysage.data.dao.HistoryDao
import takagi.ru.paysage.data.dao.ReadingProgressDao
import takagi.ru.paysage.data.dao.ReaderConfigDao
import takagi.ru.paysage.data.dao.ReplaceRuleDao
import takagi.ru.paysage.data.model.Book
import takagi.ru.paysage.data.model.BookSource
import takagi.ru.paysage.data.model.Bookmark
import takagi.ru.paysage.data.model.Category
import takagi.ru.paysage.data.model.Folder
import takagi.ru.paysage.data.model.HistoryEntity
import takagi.ru.paysage.data.model.ReadingProgress
import takagi.ru.paysage.data.model.ReaderConfig
import takagi.ru.paysage.data.model.ReplaceRule

/**
 * Paysage 主数据库
 */
@Database(
    entities = [
        Book::class,
        BookSource::class,
        Bookmark::class,
        ReadingProgress::class,
        Category::class,
        Folder::class,
        HistoryEntity::class,
        ReaderConfig::class,
        ReplaceRule::class
    ],
    version = 11,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PaysageDatabase : RoomDatabase() {
    
    abstract fun bookDao(): BookDao
    abstract fun bookSourceDao(): BookSourceDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun categoryDao(): CategoryDao
    abstract fun folderDao(): FolderDao
    abstract fun historyDao(): HistoryDao
    abstract fun readerConfigDao(): ReaderConfigDao
    abstract fun replaceRuleDao(): ReplaceRuleDao
    
    companion object {
        @Volatile
        private var INSTANCE: PaysageDatabase? = null
        
        /**
         * 数据库迁移：版本4到版本5
         * 添加文件夹管理功能
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建folders表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS folders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        path TEXT NOT NULL UNIQUE,
                        parent_path TEXT NOT NULL,
                        module_type TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                """)
                
                // 创建索引
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_folders_parent_path ON folders(parent_path)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_folders_module_type ON folders(module_type)"
                )
            }
        }
        
        /**
         * 数据库迁移：版本5到版本6
         * 添加文件夹排序字段
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加 sort_order 列
                database.execSQL(
                    "ALTER TABLE folders ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0"
                )
                
                // 创建索引
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_folders_sort_order ON folders(sort_order)"
                )
            }
        }
        
        /**
         * 数据库迁移：版本6到版本7
         * 添加唯一约束防止跨模块重复
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 先清理重复数据
                database.execSQL("""
                    DELETE FROM folders 
                    WHERE id NOT IN (
                        SELECT MIN(id) 
                        FROM folders 
                        GROUP BY path, module_type
                    )
                """)
                
                // 添加唯一约束
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_folders_path_module 
                    ON folders(path, module_type)
                """)
            }
        }
        
        /**
         * 数据库迁移：版本7到版本8
         * 添加历史记录表
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建download_history表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS download_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        thumbnail_path TEXT,
                        file_type TEXT NOT NULL,
                        file_size INTEGER NOT NULL,
                        file_path TEXT NOT NULL,
                        download_time INTEGER NOT NULL,
                        progress REAL NOT NULL,
                        status TEXT NOT NULL
                    )
                """)
                
                // 创建索引
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_history_download_time ON download_history(download_time)"
                )
            }
        }
        
        /**
         * 数据库迁移：版本8到版本9
         * 将下载历史表转换为阅读历史表
         */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 删除旧的download_history表
                database.execSQL("DROP TABLE IF EXISTS download_history")
                
                // 创建新的reading_history表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS reading_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        book_id INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        author TEXT NOT NULL,
                        thumbnail_path TEXT,
                        file_type TEXT NOT NULL,
                        file_size INTEGER NOT NULL,
                        file_path TEXT NOT NULL,
                        last_read_time INTEGER NOT NULL,
                        progress REAL NOT NULL,
                        current_page INTEGER NOT NULL,
                        total_pages INTEGER NOT NULL
                    )
                """)
                
                // 创建索引
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_history_last_read_time ON reading_history(last_read_time)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_history_book_id ON reading_history(book_id)"
                )
            }
        }
        
        /**
         * 数据库迁移：版本9到版本10
         * 添加阅读器配置和替换规则表
         */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建reader_configs表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS reader_configs (
                        id INTEGER PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        bgColor INTEGER NOT NULL,
                        bgColorNight INTEGER NOT NULL,
                        bgImagePath TEXT NOT NULL,
                        bgType TEXT NOT NULL,
                        bgAlpha INTEGER NOT NULL,
                        textColor INTEGER NOT NULL,
                        textColorNight INTEGER NOT NULL,
                        textFont TEXT NOT NULL,
                        textSize INTEGER NOT NULL,
                        textBold TEXT NOT NULL,
                        letterSpacing REAL NOT NULL,
                        lineSpacing REAL NOT NULL,
                        paragraphSpacing INTEGER NOT NULL,
                        paragraphIndent TEXT NOT NULL,
                        paddingTop INTEGER NOT NULL,
                        paddingBottom INTEGER NOT NULL,
                        paddingLeft INTEGER NOT NULL,
                        paddingRight INTEGER NOT NULL,
                        titleMode TEXT NOT NULL,
                        titleSize INTEGER NOT NULL,
                        titleTopSpacing INTEGER NOT NULL,
                        titleBottomSpacing INTEGER NOT NULL,
                        pageMode TEXT NOT NULL,
                        showHeaderLine INTEGER NOT NULL,
                        showFooterLine INTEGER NOT NULL,
                        headerPaddingTop INTEGER NOT NULL,
                        headerPaddingBottom INTEGER NOT NULL,
                        headerPaddingLeft INTEGER NOT NULL,
                        headerPaddingRight INTEGER NOT NULL,
                        footerPaddingTop INTEGER NOT NULL,
                        footerPaddingBottom INTEGER NOT NULL,
                        footerPaddingLeft INTEGER NOT NULL,
                        footerPaddingRight INTEGER NOT NULL,
                        tipHeaderLeft TEXT NOT NULL,
                        tipHeaderMiddle TEXT NOT NULL,
                        tipHeaderRight TEXT NOT NULL,
                        tipFooterLeft TEXT NOT NULL,
                        tipFooterMiddle TEXT NOT NULL,
                        tipFooterRight TEXT NOT NULL,
                        tipColor INTEGER NOT NULL,
                        underline INTEGER NOT NULL,
                        textFullJustify INTEGER NOT NULL,
                        textBottomJustify INTEGER NOT NULL,
                        darkStatusIcon INTEGER NOT NULL,
                        hideStatusBar INTEGER NOT NULL,
                        hideNavigationBar INTEGER NOT NULL,
                        keepScreenOn INTEGER NOT NULL,
                        screenOrientation TEXT NOT NULL,
                        volumeKeyPage INTEGER NOT NULL,
                        autoReadSpeed INTEGER NOT NULL,
                        readAloudSpeed INTEGER NOT NULL,
                        textSelectAble INTEGER NOT NULL
                    )
                """)
                
                // 创建replace_rules表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS replace_rules (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        pattern TEXT NOT NULL,
                        replacement TEXT NOT NULL,
                        isRegex INTEGER NOT NULL,
                        isEnabled INTEGER NOT NULL,
                        scope TEXT NOT NULL,
                        `order` INTEGER NOT NULL,
                        bookIds TEXT NOT NULL,
                        sourceIds TEXT NOT NULL
                    )
                """)
                
                // 创建索引
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_replace_rules_enabled ON replace_rules(isEnabled)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_replace_rules_order ON replace_rules(`order`)"
                )
            }
        }
        
        /**
         * 数据库迁移：版本10到版本11
         * 添加覆盖翻页配置字段
         */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加覆盖翻页相关配置字段
                database.execSQL(
                    "ALTER TABLE reader_configs ADD COLUMN coverFlipAnimationDuration INTEGER NOT NULL DEFAULT 300"
                )
                database.execSQL(
                    "ALTER TABLE reader_configs ADD COLUMN coverFlipSwipeThreshold REAL NOT NULL DEFAULT 0.3"
                )
                database.execSQL(
                    "ALTER TABLE reader_configs ADD COLUMN coverFlipVelocityThreshold REAL NOT NULL DEFAULT 1000.0"
                )
                database.execSQL(
                    "ALTER TABLE reader_configs ADD COLUMN coverFlipShadowEnabled INTEGER NOT NULL DEFAULT 1"
                )
                database.execSQL(
                    "ALTER TABLE reader_configs ADD COLUMN coverFlipShadowMaxAlpha REAL NOT NULL DEFAULT 0.4"
                )
                database.execSQL(
                    "ALTER TABLE reader_configs ADD COLUMN coverFlipBounceEnabled INTEGER NOT NULL DEFAULT 1"
                )
                database.execSQL(
                    "ALTER TABLE reader_configs ADD COLUMN coverFlipBounceMaxDisplacement REAL NOT NULL DEFAULT 100.0"
                )
                database.execSQL(
                    "ALTER TABLE reader_configs ADD COLUMN coverFlipBounceDuration INTEGER NOT NULL DEFAULT 200"
                )
            }
        }
        
        /**
         * 数据库迁移：版本3到版本4
         * 添加分类系统相关字段和表
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. 添加categoryType字段到books表
                database.execSQL(
                    "ALTER TABLE books ADD COLUMN categoryType TEXT NOT NULL DEFAULT 'MANGA'"
                )
                
                // 2. 添加isOnline字段到books表
                database.execSQL(
                    "ALTER TABLE books ADD COLUMN isOnline INTEGER NOT NULL DEFAULT 0"
                )
                
                // 3. 添加sourceId字段到books表
                database.execSQL(
                    "ALTER TABLE books ADD COLUMN sourceId INTEGER"
                )
                
                // 4. 添加sourceUrl字段到books表
                database.execSQL(
                    "ALTER TABLE books ADD COLUMN sourceUrl TEXT"
                )
                
                // 5. 创建categoryType索引
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_books_categoryType ON books(categoryType)"
                )
                
                // 6. 创建复合索引 (categoryType, isOnline)
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_books_category_online ON books(categoryType, isOnline)"
                )
                
                // 7. 创建复合索引 (categoryType, lastReadAt)
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_books_category_read ON books(categoryType, lastReadAt)"
                )
                
                // 8. 创建book_sources表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS book_sources (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        baseUrl TEXT NOT NULL,
                        categoryType TEXT NOT NULL,
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        priority INTEGER NOT NULL DEFAULT 0,
                        searchRule TEXT,
                        bookInfoRule TEXT,
                        chapterListRule TEXT,
                        contentRule TEXT,
                        totalBooks INTEGER NOT NULL DEFAULT 0,
                        successRate REAL NOT NULL DEFAULT 0.0,
                        lastUsedAt INTEGER,
                        addedAt INTEGER NOT NULL
                    )
                """)
                
                // 9. 为book_sources表创建索引
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sources_categoryType ON book_sources(categoryType)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sources_enabled ON book_sources(isEnabled)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sources_priority ON book_sources(priority)"
                )
                
                // 10. 根据文件格式自动分类现有书籍
                // CBZ, CBR, CBT, CB7, ZIP, RAR, TAR, SEVEN_ZIP -> MANGA
                // PDF -> NOVEL
                database.execSQL("""
                    UPDATE books 
                    SET categoryType = CASE 
                        WHEN fileFormat IN ('CBZ', 'CBR', 'CBT', 'CB7', 'ZIP', 'RAR', 'TAR', 'SEVEN_ZIP') THEN 'MANGA'
                        WHEN fileFormat = 'PDF' THEN 'NOVEL'
                        ELSE 'MANGA'
                    END
                """)
            }
        }
        
        fun getDatabase(context: Context): PaysageDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PaysageDatabase::class.java,
                    "paysage_database"
                )
                    .addMigrations(
                        MIGRATION_3_4, 
                        MIGRATION_4_5, 
                        MIGRATION_5_6, 
                        MIGRATION_6_7, 
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
