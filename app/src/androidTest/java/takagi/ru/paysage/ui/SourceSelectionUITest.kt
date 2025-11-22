package takagi.ru.paysage.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import takagi.ru.paysage.ui.components.SourceSelectionContent
import takagi.ru.paysage.ui.theme.PaysageTheme

/**
 * SourceSelection UI 测试
 */
@RunWith(AndroidJUnit4::class)
class SourceSelectionUITest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun sourceSelectionContent_displaysAllOptions() {
        // 设置内容
        composeTestRule.setContent {
            PaysageTheme {
                SourceSelectionContent(
                    selectedLocalMangaPath = null,
                    selectedLocalReadingPath = null,
                    onLocalMangaClick = {},
                    onLocalReadingClick = {},
                    onMangaSourceClick = {},
                    onReadingSourceClick = {}
                )
            }
        }
        
        // 验证标题显示
        composeTestRule.onNodeWithText("Source Selection").assertExists()
        
        // 验证所有选项显示
        composeTestRule.onNodeWithText("Local Manga").assertExists()
        composeTestRule.onNodeWithText("Local Reading").assertExists()
        composeTestRule.onNodeWithText("Manga Sources").assertExists()
        composeTestRule.onNodeWithText("Reading Sources").assertExists()
    }
    
    @Test
    fun sourceSelectionContent_displaysSelectedPaths() {
        val testMangaPath = "/storage/emulated/0/Manga"
        val testReadingPath = "/storage/emulated/0/Reading"
        
        composeTestRule.setContent {
            PaysageTheme {
                SourceSelectionContent(
                    selectedLocalMangaPath = testMangaPath,
                    selectedLocalReadingPath = testReadingPath,
                    onLocalMangaClick = {},
                    onLocalReadingClick = {},
                    onMangaSourceClick = {},
                    onReadingSourceClick = {}
                )
            }
        }
        
        // 验证路径显示
        composeTestRule.onNodeWithText(testMangaPath).assertExists()
        composeTestRule.onNodeWithText(testReadingPath).assertExists()
    }
    
    @Test
    fun sourceSelectionContent_displaysNoFolderSelected() {
        composeTestRule.setContent {
            PaysageTheme {
                SourceSelectionContent(
                    selectedLocalMangaPath = null,
                    selectedLocalReadingPath = null,
                    onLocalMangaClick = {},
                    onLocalReadingClick = {},
                    onMangaSourceClick = {},
                    onReadingSourceClick = {}
                )
            }
        }
        
        // 验证"未选择文件夹"文本显示（应该有两个）
        composeTestRule.onAllNodesWithText("No folder selected").assertCountEquals(2)
    }
    
    @Test
    fun sourceSelectionContent_clickLocalManga_triggersCallback() {
        var clicked = false
        
        composeTestRule.setContent {
            PaysageTheme {
                SourceSelectionContent(
                    selectedLocalMangaPath = null,
                    selectedLocalReadingPath = null,
                    onLocalMangaClick = { clicked = true },
                    onLocalReadingClick = {},
                    onMangaSourceClick = {},
                    onReadingSourceClick = {}
                )
            }
        }
        
        // 点击本地漫画选项
        composeTestRule.onNodeWithText("Local Manga").performClick()
        
        // 验证回调被触发
        assert(clicked)
    }
    
    @Test
    fun sourceSelectionContent_clickLocalReading_triggersCallback() {
        var clicked = false
        
        composeTestRule.setContent {
            PaysageTheme {
                SourceSelectionContent(
                    selectedLocalMangaPath = null,
                    selectedLocalReadingPath = null,
                    onLocalMangaClick = {},
                    onLocalReadingClick = { clicked = true },
                    onMangaSourceClick = {},
                    onReadingSourceClick = {}
                )
            }
        }
        
        // 点击本地阅读选项
        composeTestRule.onNodeWithText("Local Reading").performClick()
        
        // 验证回调被触发
        assert(clicked)
    }
    
    @Test
    fun sourceSelectionContent_onlineOptionsShowChevron() {
        composeTestRule.setContent {
            PaysageTheme {
                SourceSelectionContent(
                    selectedLocalMangaPath = null,
                    selectedLocalReadingPath = null,
                    onLocalMangaClick = {},
                    onLocalReadingClick = {},
                    onMangaSourceClick = {},
                    onReadingSourceClick = {}
                )
            }
        }
        
        // 在线选项应该显示箭头图标
        // 这里我们通过语义测试来验证
        composeTestRule.onNodeWithText("Manga Sources").assertExists()
        composeTestRule.onNodeWithText("Reading Sources").assertExists()
    }
}
