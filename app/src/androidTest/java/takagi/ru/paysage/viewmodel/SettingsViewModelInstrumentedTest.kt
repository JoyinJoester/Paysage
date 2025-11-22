package takagi.ru.paysage.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import takagi.ru.paysage.data.model.Language
import takagi.ru.paysage.repository.SettingsRepository

/**
 * SettingsViewModel Instrumented 测试
 * 测试语言切换和 Activity 重建事件机制
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsViewModelInstrumentedTest {
    
    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepository
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsRepository = SettingsRepository(context)
    }
    
    @Test
    fun updateLanguage_shouldEmitRecreateActivityEvent() = runTest {
        // 创建 ViewModel
        val viewModel = SettingsViewModel(context as android.app.Application)
        
        // 用于收集事件的标志
        var eventReceived = false
        
        // 在后台收集事件
        val job = kotlinx.coroutines.launch {
            viewModel.recreateActivityEvent.collect {
                eventReceived = true
            }
        }
        
        // 等待一小段时间确保收集器已启动
        kotlinx.coroutines.delay(100)
        
        // 更新语言
        viewModel.updateLanguage(Language.ENGLISH)
        
        // 等待事件处理
        kotlinx.coroutines.delay(500)
        
        // 验证事件已发射
        assertTrue("recreateActivityEvent 应该被发射", eventReceived)
        
        // 清理
        job.cancel()
    }
    
    @Test
    fun updateLanguage_shouldPersistLanguageBeforeEmittingEvent() = runTest {
        // 创建 ViewModel
        val viewModel = SettingsViewModel(context as android.app.Application)
        
        // 更新语言为英语
        viewModel.updateLanguage(Language.ENGLISH)
        
        // 等待持久化完成
        kotlinx.coroutines.delay(500)
        
        // 验证语言已持久化
        val settings = settingsRepository.settingsFlow.first()
        assertEquals("语言应该被持久化为 ENGLISH", Language.ENGLISH, settings.language)
    }
    
    @Test
    fun updateLanguage_shouldUpdateSettingsState() = runTest {
        // 创建 ViewModel
        val viewModel = SettingsViewModel(context as android.app.Application)
        
        // 更新语言为中文
        viewModel.updateLanguage(Language.CHINESE)
        
        // 等待状态更新
        kotlinx.coroutines.delay(500)
        
        // 验证 ViewModel 状态已更新
        val settings = viewModel.settings.first()
        assertEquals("ViewModel 状态应该反映新语言", Language.CHINESE, settings.language)
    }
    
    @Test
    fun recreateActivityEvent_shouldNotReplayEvents() = runTest {
        // 创建 ViewModel
        val viewModel = SettingsViewModel(context as android.app.Application)
        
        // 更新语言触发事件
        viewModel.updateLanguage(Language.ENGLISH)
        
        // 等待事件处理
        kotlinx.coroutines.delay(500)
        
        // 现在开始收集事件（在事件已发射之后）
        var eventCount = 0
        val job = kotlinx.coroutines.launch {
            viewModel.recreateActivityEvent.collect {
                eventCount++
            }
        }
        
        // 等待一段时间
        kotlinx.coroutines.delay(500)
        
        // 验证没有收到旧事件（因为 replay = 0）
        assertEquals("不应该收到已发射的事件", 0, eventCount)
        
        // 清理
        job.cancel()
    }
    
    @Test
    fun multipleLanguageChanges_shouldEmitMultipleEvents() = runTest {
        // 创建 ViewModel
        val viewModel = SettingsViewModel(context as android.app.Application)
        
        // 用于计数事件
        var eventCount = 0
        
        // 在后台收集事件
        val job = kotlinx.coroutines.launch {
            viewModel.recreateActivityEvent.collect {
                eventCount++
            }
        }
        
        // 等待收集器启动
        kotlinx.coroutines.delay(100)
        
        // 多次更改语言
        viewModel.updateLanguage(Language.ENGLISH)
        kotlinx.coroutines.delay(200)
        
        viewModel.updateLanguage(Language.CHINESE)
        kotlinx.coroutines.delay(200)
        
        viewModel.updateLanguage(Language.SYSTEM)
        kotlinx.coroutines.delay(200)
        
        // 验证收到了多个事件
        assertEquals("应该收到 3 个事件", 3, eventCount)
        
        // 清理
        job.cancel()
    }
}
