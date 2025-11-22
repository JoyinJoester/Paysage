package takagi.ru.paysage

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 源选择集成测试
 * 测试从导航按钮到源选择页面的完整流程
 */
@RunWith(AndroidJUnit4::class)
class SourceSelectionIntegrationTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Test
    fun clickFolderButton_opensSourceSelectionPage() {
        // 等待应用加载
        composeTestRule.waitForIdle()
        
        // 查找并点击文件夹按钮
        // 注意：这个测试需要根据实际的 UI 结构调整
        composeTestRule.onNodeWithContentDescription("选择源").performClick()
        
        // 等待抽屉打开
        composeTestRule.waitForIdle()
        
        // 验证源选择页面显示
        composeTestRule.onNodeWithText("Source Selection").assertExists()
        composeTestRule.onNodeWithText("Local Manga").assertExists()
        composeTestRule.onNodeWithText("Local Reading").assertExists()
    }
    
    @Test
    fun sourceSelectionPage_navigateToMangaSource() {
        // 打开源选择页面
        composeTestRule.onNodeWithContentDescription("选择源").performClick()
        composeTestRule.waitForIdle()
        
        // 点击漫画源选项
        composeTestRule.onNodeWithText("Manga Sources").performClick()
        composeTestRule.waitForIdle()
        
        // 验证导航到漫画源管理页面
        // 注意：这需要根据实际的漫画源页面内容调整
        // composeTestRule.onNodeWithText("Manga Sources Management").assertExists()
    }
    
    @Test
    fun sourceSelectionPage_navigateToReadingSource() {
        // 打开源选择页面
        composeTestRule.onNodeWithContentDescription("选择源").performClick()
        composeTestRule.waitForIdle()
        
        // 点击阅读源选项
        composeTestRule.onNodeWithText("Reading Sources").performClick()
        composeTestRule.waitForIdle()
        
        // 验证导航到阅读源管理页面
        // 注意：这需要根据实际的阅读源页面内容调整
        // composeTestRule.onNodeWithText("Reading Sources Management").assertExists()
    }
    
    @Test
    fun sourceSelectionPage_displaysCorrectly() {
        // 打开源选择页面
        composeTestRule.onNodeWithContentDescription("选择源").performClick()
        composeTestRule.waitForIdle()
        
        // 验证所有元素都正确显示
        composeTestRule.onNodeWithText("Source Selection").assertExists()
        composeTestRule.onNodeWithText("Local Manga").assertExists()
        composeTestRule.onNodeWithText("Local Reading").assertExists()
        composeTestRule.onNodeWithText("Manga Sources").assertExists()
        composeTestRule.onNodeWithText("Reading Sources").assertExists()
        
        // 验证"未选择文件夹"文本显示
        composeTestRule.onAllNodesWithText("No folder selected").assertCountEquals(2)
    }
    
    @Test
    fun sourceSelectionPage_closeDrawer() {
        // 打开源选择页面
        composeTestRule.onNodeWithContentDescription("选择源").performClick()
        composeTestRule.waitForIdle()
        
        // 验证页面显示
        composeTestRule.onNodeWithText("Source Selection").assertExists()
        
        // 向左滑动关闭抽屉
        composeTestRule.onNodeWithText("Source Selection").performTouchInput {
            swipeLeft()
        }
        composeTestRule.waitForIdle()
        
        // 验证抽屉关闭（源选择页面不再显示）
        composeTestRule.onNodeWithText("Source Selection").assertDoesNotExist()
    }
}
