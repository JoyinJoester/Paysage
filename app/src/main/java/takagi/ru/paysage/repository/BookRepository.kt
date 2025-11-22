package takagi.ru.paysage.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import takagi.ru.paysage.data.PaysageDatabase
import takagi.ru.paysage.data.model.Book
import takagi.ru.paysage.data.model.BookFormat
import takagi.ru.paysage.data.model.SyncOptions
import takagi.ru.paysage.data.model.SyncResult
import takagi.ru.paysage.utils.FileParser
import takagi.ru.paysage.utils.FileScanner
import takagi.ru.paysage.utils.ScannedFile
import java.io.File
import java.io.IOException
import android.database.sqlite.SQLiteException

private const val TAG = "BookRepository"
private const val BATCH_SIZE = 100 // 每批处理的文件数量
private const val MAX_PARALLEL_TASKS = 4 // 最大并行任务数

/**
 * 书库 Repository
 * 管理书籍的增删改查和扫描
 */
class BookRepository(private val context: Context) {
    
    private val database = PaysageDatabase.getDatabase(context)
    private val bookDao = database.bookDao()
    private val fileScanner = FileScanner(context)
    private val fileParser = FileParser(context)
    
    // 并行任务信号量，限制同时执行的协程数量
    private val parallelSemaphore = Semaphore(MAX_PARALLEL_TASKS)
    
    // Flow 数据流
    fun getAllBooksFlow(): Flow<List<Book>> = bookDao.getAllBooksFlow()
    
    fun getFavoriteBooksFlow(): Flow<List<Book>> = bookDao.getFavoriteBooksFlow()
    
    fun getBookByIdFlow(bookId: Long): Flow<Book?> = bookDao.getBookByIdFlow(bookId)
    
    fun getBooksByCategoryFlow(category: String): Flow<List<Book>> = 
        bookDao.getBooksByCategoryFlow(category)
    
    fun searchBooksFlow(query: String): Flow<List<Book>> = bookDao.searchBooksFlow(query)
    
    fun getBooksByFormatFlow(format: BookFormat): Flow<List<Book>> = 
        bookDao.getBooksByFormatFlow(format)
    
    fun getAllCategoriesFlow(): Flow<List<String>> = bookDao.getAllCategoriesFlow()
    
    fun getLastReadBookFlow(): Flow<Book?> = bookDao.getLastReadBookFlow()
    
    /**
     * 获取最近阅读的书籍流
     */
    fun getRecentBooksFlow(limit: Int = 20): Flow<List<Book>> = 
        bookDao.getRecentBooksFlow(limit.coerceIn(1, 100))
    
    /**
     * 获取分类及其书籍数量流
     */
    fun getCategoriesWithCountFlow(): Flow<List<takagi.ru.paysage.data.model.CategoryInfo>> = 
        bookDao.getCategoriesWithCount().map { categoryCounts ->
            categoryCounts.map { categoryCount ->
                takagi.ru.paysage.data.model.CategoryInfo(
                    name = categoryCount.category,
                    bookCount = categoryCount.count
                )
            }
        }
    
    // 基本操作
    suspend fun getBookById(bookId: Long): Book? = bookDao.getBookById(bookId)
    
    suspend fun getLastReadBook(): Book? = bookDao.getLastReadBook()
    
    suspend fun getAllBooks(): List<Book> = bookDao.getAllBooks()
    
    suspend fun insertBook(book: Book): Long = bookDao.insertBook(book)
    
    suspend fun updateBook(book: Book) = bookDao.updateBook(book)
    
    suspend fun deleteBook(book: Book) = bookDao.deleteBook(book)
    
    suspend fun toggleFavorite(bookId: Long, isFavorite: Boolean) = 
        bookDao.updateFavorite(bookId, isFavorite)
    
    suspend fun updateReadingProgress(bookId: Long, page: Int) = 
        bookDao.updateReadingProgress(bookId, page, System.currentTimeMillis())
    
    /**
     * 扫描并导入书籍
     * 使用分批处理和并行扫描优化性能
     */
    suspend fun scanAndImportBooks(directory: File? = null, useParallel: Boolean = false): ScanResult = withContext(Dispatchers.IO) {
        try {
            val scannedFiles = if (directory != null) {
                fileScanner.scanDirectory(directory)
            } else {
                fileScanner.scanDefaultDirectories()
            }
            
            val existingBooks = getAllBooks()
            val existingPaths = existingBooks.map { it.filePath }.toSet()
            
            // 分批处理文件
            val batches = scannedFiles.chunked(BATCH_SIZE)
            val newBooks = mutableListOf<Book>()
            val updatedBooks = mutableListOf<Book>()
            
            if (useParallel) {
                // 并行处理批次
                batches.forEach { batch ->
                    val results = batch.map { scannedFile ->
                        async {
                            parallelSemaphore.withPermit {
                                processScannedFile(scannedFile, existingBooks, existingPaths)
                            }
                        }
                    }.awaitAll()
                    
                    results.forEach { result ->
                        when (result) {
                            is ProcessResult.NewBook -> newBooks.add(result.book)
                            is ProcessResult.UpdatedBook -> updatedBooks.add(result.book)
                            is ProcessResult.NoChange -> { /* 无操作 */ }
                        }
                    }
                    
                    // 批量插入新书籍
                    if (newBooks.isNotEmpty()) {
                        batchInsertBooks(newBooks)
                        newBooks.clear()
                    }
                }
            } else {
                // 串行处理批次
                for (batch in batches) {
                    for (scannedFile in batch) {
                        when (val result = processScannedFile(scannedFile, existingBooks, existingPaths)) {
                            is ProcessResult.NewBook -> newBooks.add(result.book)
                            is ProcessResult.UpdatedBook -> updatedBooks.add(result.book)
                            is ProcessResult.NoChange -> { /* 无操作 */ }
                        }
                    }
                    
                    // 批量插入新书籍
                    if (newBooks.isNotEmpty()) {
                        batchInsertBooks(newBooks)
                        newBooks.clear()
                    }
                }
            }
            
            ScanResult(
                totalScanned = scannedFiles.size,
                newBooks = newBooks.size,
                updatedBooks = updatedBooks.size,
                books = newBooks + updatedBooks
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ScanResult(0, 0, 0, emptyList())
        }
    }
    
    /**
     * 处理单个扫描文件
     */
    private suspend fun processScannedFile(
        scannedFile: ScannedFile,
        existingBooks: List<Book>,
        existingPaths: Set<String>
    ): ProcessResult {
        return if (scannedFile.path in existingPaths) {
            // 更新已存在的书籍
            val existingBook = existingBooks.find { it.filePath == scannedFile.path }
            if (existingBook != null && existingBook.fileSize != scannedFile.size) {
                val updatedBook = existingBook.copy(
                    fileSize = scannedFile.size,
                    lastModifiedAt = scannedFile.lastModified
                )
                updateBook(updatedBook)
                ProcessResult.UpdatedBook(updatedBook)
            } else {
                ProcessResult.NoChange
            }
        } else {
            // 添加新书籍
            val book = createBookFromScannedFile(scannedFile)
            if (book != null) {
                ProcessResult.NewBook(book)
            } else {
                ProcessResult.NoChange
            }
        }
    }
    
    /**
     * 批量插入书籍
     */
    private suspend fun batchInsertBooks(books: List<Book>) {
        if (books.isEmpty()) return
        
        try {
            // 使用事务批量插入
            withContext(Dispatchers.IO) {
                books.forEach { book ->
                    bookDao.insertBook(book)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error batch inserting books", e)
            // 如果批量插入失败，尝试逐个插入
            books.forEach { book ->
                try {
                    insertBook(book)
                } catch (e: Exception) {
                    Log.e(TAG, "Error inserting book: ${book.title}", e)
                }
            }
        }
    }
    
    /**
     * 处理结果密封类
     */
    private sealed class ProcessResult {
        data class NewBook(val book: Book) : ProcessResult()
        data class UpdatedBook(val book: Book) : ProcessResult()
        object NoChange : ProcessResult()
    }
    
    /**
     * 扫描并导入书籍（从 URI）
     */
    suspend fun scanAndImportBooksFromUri(uri: Uri): ScanResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Scanning URI: $uri")
            val scannedFiles = fileScanner.scanDirectoryUri(uri)
            Log.d(TAG, "Found ${scannedFiles.size} files")
            
            val existingBooks = getAllBooks()
            val existingPaths = existingBooks.map { it.filePath }.toSet()
            
            val newBooks = mutableListOf<Book>()
            val updatedBooks = mutableListOf<Book>()
            
            for (scannedFile in scannedFiles) {
                if (scannedFile.path in existingPaths) {
                    // 更新已存在的书籍
                    val existingBook = existingBooks.find { it.filePath == scannedFile.path }
                    if (existingBook != null && existingBook.fileSize != scannedFile.size) {
                        val updatedBook = existingBook.copy(
                            fileSize = scannedFile.size,
                            lastModifiedAt = scannedFile.lastModified
                        )
                        updateBook(updatedBook)
                        updatedBooks.add(updatedBook)
                    }
                } else {
                    // 添加新书籍
                    val book = createBookFromScannedFileUri(scannedFile)
                    if (book != null) {
                        insertBook(book)
                        newBooks.add(book)
                        Log.d(TAG, "Added new book: ${book.title}")
                    }
                }
            }
            
            ScanResult(
                totalScanned = scannedFiles.size,
                newBooks = newBooks.size,
                updatedBooks = updatedBooks.size,
                books = newBooks + updatedBooks
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning URI", e)
            e.printStackTrace()
            ScanResult(0, 0, 0, emptyList())
        }
    }
    
    /**
     * 从扫描文件创建书籍对象
     */
    private suspend fun createBookFromScannedFile(scannedFile: ScannedFile): Book? {
        return try {
            Log.d(TAG, "Creating book from file: ${scannedFile.name}")
            val file = File(scannedFile.path)
            
            if (!file.exists() || !file.canRead()) {
                Log.w(TAG, "File not accessible: ${scannedFile.path}")
                return null
            }
            
            val pageCount = fileParser.getPageCount(file)
            if (pageCount <= 0) {
                Log.w(TAG, "Invalid page count for file: ${scannedFile.name}")
                return null
            }
            
            // 创建临时 ID 用于生成封面文件名
            val tempId = System.currentTimeMillis()
            val coverPath = fileParser.saveCover(file, tempId)
            
            val book = Book(
                title = scannedFile.name,
                filePath = scannedFile.path,
                fileSize = scannedFile.size,
                fileFormat = scannedFile.format,
                totalPages = pageCount,
                coverPath = coverPath,
                lastModifiedAt = scannedFile.lastModified
            )
            
            Log.d(TAG, "Book created successfully: ${book.title} with $pageCount pages")
            book
        } catch (e: Exception) {
            Log.e(TAG, "Error creating book from file: ${scannedFile.name}", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 从扫描文件创建书籍对象（URI 版本）
     */
    private suspend fun createBookFromScannedFileUri(scannedFile: ScannedFile): Book? {
        return try {
            Log.d(TAG, "Creating book from URI: ${scannedFile.name}")
            val uri = Uri.parse(scannedFile.path)
            
            val pageCount = fileParser.getPageCountFromUri(uri)
            if (pageCount <= 0) {
                Log.w(TAG, "Invalid page count for URI: ${scannedFile.name}")
                return null
            }
            
            // 创建临时 ID 用于生成封面文件名
            val tempId = System.currentTimeMillis()
            val coverPath = fileParser.saveCoverFromUri(uri, tempId)
            
            val book = Book(
                title = scannedFile.name,
                filePath = scannedFile.path,
                fileSize = scannedFile.size,
                fileFormat = scannedFile.format,
                totalPages = pageCount,
                coverPath = coverPath,
                lastModifiedAt = scannedFile.lastModified
            )
            
            Log.d(TAG, "Book created successfully from URI: ${book.title} with $pageCount pages")
            book
        } catch (e: Exception) {
            Log.e(TAG, "Error creating book from URI: ${scannedFile.name}", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 生成书籍封面
     */
    suspend fun generateCover(bookId: Long): String? = withContext(Dispatchers.IO) {
        try {
            val book = getBookById(bookId) ?: return@withContext null
            val file = File(book.filePath)
            
            if (!file.exists()) return@withContext null
            
            val coverPath = fileParser.saveCover(file, bookId)
            if (coverPath != null) {
                updateBook(book.copy(coverPath = coverPath))
            }
            coverPath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 批量生成封面
     */
    suspend fun generateCoversForAll(): Int = withContext(Dispatchers.IO) {
        var count = 0
        val books = getAllBooks().filter { it.coverPath == null }
        
        for (book in books) {
            val coverPath = generateCover(book.id)
            if (coverPath != null) {
                count++
            }
        }
        count
    }
    
    /**
     * 获取统计信息
     */
    suspend fun getStatistics(): LibraryStatistics {
        val totalBooks = bookDao.getBookCount()
        val allBooks = getAllBooks()
        
        return LibraryStatistics(
            totalBooks = totalBooks,
            readBooks = allBooks.count { it.isFinished },
            readingBooks = allBooks.count { it.currentPage > 0 && !it.isFinished },
            favoriteBooks = allBooks.count { it.isFavorite },
            totalPages = allBooks.sumOf { it.totalPages }
        )
    }
    
    /**
     * 执行维护操作
     * 仅执行维护相关的任务，不扫描新文件
     */
    suspend fun executeMaintenance(options: SyncOptions): SyncResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()
        var deletedBooks = 0
        var updatedBooks = 0
        var generatedThumbnails = 0
        
        try {
            // 检查权限
            if (!checkStoragePermission()) {
                errors.add("缺少存储权限")
                return@withContext SyncResult(
                    newBooks = 0,
                    updatedBooks = 0,
                    deletedBooks = 0,
                    generatedThumbnails = 0,
                    duration = System.currentTimeMillis() - startTime,
                    errors = errors
                )
            }
            
            // 移出已删除文件
            if (options.removeDeletedFiles) {
                try {
                    deletedBooks = withRetry { removeDeletedFiles() }
                } catch (e: Exception) {
                    errors.add("移除已删除文件失败: ${e.message}")
                    Log.e(TAG, "Error removing deleted files", e)
                }
            }
            
            // 更新修改过的文件
            if (options.updateModifiedFiles) {
                try {
                    updatedBooks = withRetry { updateModifiedFiles() }
                } catch (e: Exception) {
                    errors.add("更新修改文件失败: ${e.message}")
                    Log.e(TAG, "Error updating modified files", e)
                }
            }
            
            // 生成缺失的缩略图
            if (options.generateMissingThumbnails) {
                try {
                    generatedThumbnails = withRetry { generateCoversForAll() }
                } catch (e: Exception) {
                    errors.add("生成缩略图失败: ${e.message}")
                    Log.e(TAG, "Error generating thumbnails", e)
                }
            }
        } catch (e: SecurityException) {
            errors.add("权限错误: ${e.message}")
            Log.e(TAG, "Security error during maintenance", e)
        } catch (e: Exception) {
            errors.add("维护操作失败: ${e.message}")
            Log.e(TAG, "Error during maintenance", e)
        }
        
        SyncResult(
            newBooks = 0,
            updatedBooks = updatedBooks,
            deletedBooks = deletedBooks,
            generatedThumbnails = generatedThumbnails,
            duration = System.currentTimeMillis() - startTime,
            errors = errors
        )
    }
    
    /**
     * 执行增量同步
     * 仅扫描新增和修改的文件
     */
    suspend fun executeIncrementalSync(options: SyncOptions): SyncResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()
        
        try {
            // 检查权限
            if (!checkStoragePermission()) {
                errors.add("缺少存储权限")
                return@withContext SyncResult(
                    newBooks = 0,
                    updatedBooks = 0,
                    deletedBooks = 0,
                    generatedThumbnails = 0,
                    duration = System.currentTimeMillis() - startTime,
                    errors = errors
                )
            }
            
            // 扫描文件
            val scanResult = try {
                withRetry { scanAndImportBooks(useParallel = options.parallelSync) }
            } catch (e: IOException) {
                errors.add("文件扫描失败: ${e.message}")
                Log.e(TAG, "IO error during scan", e)
                return@withContext SyncResult(
                    newBooks = 0,
                    updatedBooks = 0,
                    deletedBooks = 0,
                    generatedThumbnails = 0,
                    duration = System.currentTimeMillis() - startTime,
                    errors = errors
                )
            } catch (e: SQLiteException) {
                errors.add("数据库错误: ${e.message}")
                Log.e(TAG, "Database error during scan", e)
                return@withContext SyncResult(
                    newBooks = 0,
                    updatedBooks = 0,
                    deletedBooks = 0,
                    generatedThumbnails = 0,
                    duration = System.currentTimeMillis() - startTime,
                    errors = errors
                )
            }
            
            // 执行维护操作
            var deletedBooks = 0
            var generatedThumbnails = 0
            
            if (options.removeDeletedFiles) {
                try {
                    deletedBooks = withRetry { removeDeletedFiles() }
                } catch (e: Exception) {
                    errors.add("移除已删除文件失败: ${e.message}")
                    Log.e(TAG, "Error removing deleted files", e)
                }
            }
            
            if (options.generateMissingThumbnails) {
                try {
                    generatedThumbnails = withRetry { generateCoversForAll() }
                } catch (e: Exception) {
                    errors.add("生成缩略图失败: ${e.message}")
                    Log.e(TAG, "Error generating thumbnails", e)
                }
            }
            
            return@withContext SyncResult(
                newBooks = scanResult.newBooks,
                updatedBooks = scanResult.updatedBooks,
                deletedBooks = deletedBooks,
                generatedThumbnails = generatedThumbnails,
                duration = System.currentTimeMillis() - startTime,
                errors = errors
            )
        } catch (e: SecurityException) {
            errors.add("权限错误: ${e.message}")
            Log.e(TAG, "Security error during incremental sync", e)
            return@withContext SyncResult(
                newBooks = 0,
                updatedBooks = 0,
                deletedBooks = 0,
                generatedThumbnails = 0,
                duration = System.currentTimeMillis() - startTime,
                errors = errors
            )
        } catch (e: Exception) {
            errors.add("增量同步失败: ${e.message}")
            Log.e(TAG, "Error during incremental sync", e)
            return@withContext SyncResult(
                newBooks = 0,
                updatedBooks = 0,
                deletedBooks = 0,
                generatedThumbnails = 0,
                duration = System.currentTimeMillis() - startTime,
                errors = errors
            )
        }
    }
    
    /**
     * 执行完整同步
     * 重新扫描所有文件
     */
    suspend fun executeFullSync(options: SyncOptions): SyncResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()
        
        try {
            // 检查权限
            if (!checkStoragePermission()) {
                errors.add("缺少存储权限")
                return@withContext SyncResult(
                    newBooks = 0,
                    updatedBooks = 0,
                    deletedBooks = 0,
                    generatedThumbnails = 0,
                    duration = System.currentTimeMillis() - startTime,
                    errors = errors
                )
            }
            
            // 扫描所有文件
            val scanResult = try {
                withRetry { scanAndImportBooks(useParallel = options.parallelSync) }
            } catch (e: IOException) {
                errors.add("文件扫描失败: ${e.message}")
                Log.e(TAG, "IO error during scan", e)
                return@withContext SyncResult(
                    newBooks = 0,
                    updatedBooks = 0,
                    deletedBooks = 0,
                    generatedThumbnails = 0,
                    duration = System.currentTimeMillis() - startTime,
                    errors = errors
                )
            } catch (e: SQLiteException) {
                errors.add("数据库错误: ${e.message}")
                Log.e(TAG, "Database error during scan", e)
                return@withContext SyncResult(
                    newBooks = 0,
                    updatedBooks = 0,
                    deletedBooks = 0,
                    generatedThumbnails = 0,
                    duration = System.currentTimeMillis() - startTime,
                    errors = errors
                )
            }
            
            // 执行维护操作
            var deletedBooks = 0
            var generatedThumbnails = 0
            
            if (options.removeDeletedFiles) {
                try {
                    deletedBooks = withRetry { removeDeletedFiles() }
                } catch (e: Exception) {
                    errors.add("移除已删除文件失败: ${e.message}")
                    Log.e(TAG, "Error removing deleted files", e)
                }
            }
            
            if (options.updateModifiedFiles) {
                try {
                    withRetry { updateModifiedFiles() }
                } catch (e: Exception) {
                    errors.add("更新修改文件失败: ${e.message}")
                    Log.e(TAG, "Error updating modified files", e)
                }
            }
            
            if (options.generateMissingThumbnails) {
                try {
                    generatedThumbnails = withRetry { generateCoversForAll() }
                } catch (e: Exception) {
                    errors.add("生成缩略图失败: ${e.message}")
                    Log.e(TAG, "Error generating thumbnails", e)
                }
            }
            
            return@withContext SyncResult(
                newBooks = scanResult.newBooks,
                updatedBooks = scanResult.updatedBooks,
                deletedBooks = deletedBooks,
                generatedThumbnails = generatedThumbnails,
                duration = System.currentTimeMillis() - startTime,
                errors = errors
            )
        } catch (e: SecurityException) {
            errors.add("权限错误: ${e.message}")
            Log.e(TAG, "Security error during full sync", e)
            return@withContext SyncResult(
                newBooks = 0,
                updatedBooks = 0,
                deletedBooks = 0,
                generatedThumbnails = 0,
                duration = System.currentTimeMillis() - startTime,
                errors = errors
            )
        } catch (e: Exception) {
            errors.add("完整同步失败: ${e.message}")
            Log.e(TAG, "Error during full sync", e)
            return@withContext SyncResult(
                newBooks = 0,
                updatedBooks = 0,
                deletedBooks = 0,
                generatedThumbnails = 0,
                duration = System.currentTimeMillis() - startTime,
                errors = errors
            )
        }
    }
    
    /**
     * 移除已删除的文件
     * 删除文件路径不存在的书籍记录
     */
    private suspend fun removeDeletedFiles(): Int {
        val allBooks = getAllBooks()
        var deletedCount = 0
        
        for (book in allBooks) {
            val file = File(book.filePath)
            if (!file.exists()) {
                deleteBook(book)
                deletedCount++
            }
        }
        
        return deletedCount
    }
    
    /**
     * 更新修改过的文件
     * 刷新文件已被修改的书籍数据
     */
    private suspend fun updateModifiedFiles(): Int {
        val allBooks = getAllBooks()
        var updatedCount = 0
        
        for (book in allBooks) {
            val file = File(book.filePath)
            if (file.exists() && file.lastModified() != book.lastModifiedAt) {
                try {
                    val pageCount = fileParser.getPageCount(file)
                    val updatedBook = book.copy(
                        fileSize = file.length(),
                        lastModifiedAt = file.lastModified(),
                        totalPages = pageCount
                    )
                    updateBook(updatedBook)
                    updatedCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating book: ${book.title}", e)
                }
            }
        }
        
        return updatedCount
    }
    
    /**
     * 带重试机制的操作执行
     * 最多重试3次，每次重试间隔递增
     */
    private suspend fun <T> withRetry(
        maxRetries: Int = 3,
        initialDelay: Long = 100,
        maxDelay: Long = 1000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(maxRetries - 1) { attempt ->
            try {
                return block()
            } catch (e: IOException) {
                Log.w(TAG, "IO error on attempt ${attempt + 1}, retrying...", e)
            } catch (e: SecurityException) {
                Log.e(TAG, "Security error on attempt ${attempt + 1}", e)
                throw e // 权限错误不重试
            } catch (e: SQLiteException) {
                Log.w(TAG, "Database error on attempt ${attempt + 1}, retrying...", e)
            } catch (e: Exception) {
                Log.w(TAG, "Error on attempt ${attempt + 1}, retrying...", e)
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block() // 最后一次尝试，如果失败则抛出异常
    }
    
    /**
     * 检查存储权限
     */
    private fun checkStoragePermission(): Boolean {
        return try {
            context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 更新书籍标签
     */
    suspend fun updateBookTags(bookId: Long, tags: List<String>) = withContext(Dispatchers.IO) {
        val book = bookDao.getBookById(bookId)
        book?.let {
            bookDao.updateBook(it.copy(tags = tags))
        }
    }
    
    /**
     * 更新排序偏好
     */
    suspend fun updateSortPreference(bookId: Long, preference: String) = withContext(Dispatchers.IO) {
        val book = bookDao.getBookById(bookId)
        book?.let {
            bookDao.updateBook(it.copy(sortPreference = preference))
        }
    }
    
    /**
     * 获取最近阅读的书籍（挂起函数）
     */
    suspend fun getRecentBooks(limit: Int = 20): List<Book> = withContext(Dispatchers.IO) {
        try {
            bookDao.getRecentBooksFlow(limit.coerceIn(1, 100)).first()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recent books", e)
            emptyList()
        }
    }
}

/**
 * 扫描结果
 */
data class ScanResult(
    val totalScanned: Int,
    val newBooks: Int,
    val updatedBooks: Int,
    val books: List<Book>
)

/**
 * 书库统计信息
 */
data class LibraryStatistics(
    val totalBooks: Int,
    val readBooks: Int,
    val readingBooks: Int,
    val favoriteBooks: Int,
    val totalPages: Int
)
