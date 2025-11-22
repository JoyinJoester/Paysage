package takagi.ru.paysage.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * SourceSelectionViewModel 单元测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SourceSelectionViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SourceSelectionViewModel
    private lateinit var application: Application
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = RuntimeEnvironment.getApplication()
        viewModel = SourceSelectionViewModel(application)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `test initial state is null`() = runTest {
        // 初始状态应该为 null
        assertNull(viewModel.selectedLocalMangaPath.value)
        assertNull(viewModel.selectedLocalReadingPath.value)
        assertNull(viewModel.error.value)
    }
    
    @Test
    fun `test update local manga path`() = runTest {
        // 注意：由于路径验证需要实际文件系统，这里只测试基本流程
        // 实际项目中可能需要 mock 文件系统
        val testPath = "/test/manga/path"
        
        viewModel.updateLocalMangaPath(testPath)
        advanceUntilIdle()
        
        // 由于路径不存在，应该设置错误
        assertEquals("Invalid manga folder path", viewModel.error.value)
    }
    
    @Test
    fun `test update local reading path`() = runTest {
        val testPath = "/test/reading/path"
        
        viewModel.updateLocalReadingPath(testPath)
        advanceUntilIdle()
        
        // 由于路径不存在，应该设置错误
        assertEquals("Invalid reading folder path", viewModel.error.value)
    }
    
    @Test
    fun `test clear local manga path`() = runTest {
        viewModel.clearLocalMangaPath()
        advanceUntilIdle()
        
        assertNull(viewModel.selectedLocalMangaPath.value)
    }
    
    @Test
    fun `test clear local reading path`() = runTest {
        viewModel.clearLocalReadingPath()
        advanceUntilIdle()
        
        assertNull(viewModel.selectedLocalReadingPath.value)
    }
    
    @Test
    fun `test clear error`() = runTest {
        // 先设置一个错误
        viewModel.updateLocalMangaPath("/invalid/path")
        advanceUntilIdle()
        
        // 清除错误
        viewModel.clearError()
        
        assertNull(viewModel.error.value)
    }
}
