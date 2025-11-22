package takagi.ru.paysage.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 两层导航 UI 测试
 */
@RunWith(AndroidJUnit4::class)
class TwoLayerNavigationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun primaryNavigationRail_显示所有导航项() {
        var selectedItem = PrimaryNavItem.Library
        
        composeTestRule.setContent {
            PrimaryNavigationRail(
                selectedItem = selectedItem,
                onItemClick = { selectedItem = it }
            )
        }
        
        // 验证所有导航项都显示
        composeTestRule.onNodeWithContentDescription("打开书库菜单").assertExists()
        composeTestRule.onNodeWithContentDescription("打开设置菜单").assertExists()
        composeTestRule.onNodeWithContentDescription("打开关于信息").assertExists()
    }
    
    @Test
    fun primaryNavigationRail_点击导航项应该触发回调() {
        var clickedItem: PrimaryNavItem? = null
        
        composeTestRule.setContent {
            PrimaryNavigationRail(
                selectedItem = PrimaryNavItem.Library,
                onItemClick = { clickedItem = it }
            )
        }
        
        // 点击设置项
        composeTestRule.onNodeWithContentDescription("打开设置菜单").performClick()
        
        // 验证回调被触发
        assert(clickedItem == PrimaryNavItem.Settings)
    }
    
    @Test
    fun secondaryDrawerContent_显示正确的菜单内容() {
        composeTestRule.setContent {
            SecondaryDrawerContent(
                selectedPrimaryItem = PrimaryNavItem.Library,
                onItemClick = {}
            )
        }
        
        // 验证书库菜单项显示
        composeTestRule.onNodeWithText("书库").assertExists()
        composeTestRule.onNodeWithText("全部书籍").assertExists()
        composeTestRule.onNodeWithText("收藏").assertExists()
        composeTestRule.onNodeWithText("最近阅读").assertExists()
        composeTestRule.onNodeWithText("分类").assertExists()
    }
    
    @Test
    fun secondaryDrawerContent_切换第一层项应该更新内容() {
        var selectedPrimaryItem = PrimaryNavItem.Library
        
        composeTestRule.setContent {
            SecondaryDrawerContent(
                selectedPrimaryItem = selectedPrimaryItem,
                onItemClick = {}
            )
        }
        
        // 初始显示书库内容
        composeTestRule.onNodeWithText("全部书籍").assertExists()
        
        // 切换到设置
        selectedPrimaryItem = PrimaryNavItem.Settings
        composeTestRule.setContent {
            SecondaryDrawerContent(
                selectedPrimaryItem = selectedPrimaryItem,
                onItemClick = {}
            )
        }
        
        // 验证设置内容显示
        composeTestRule.onNodeWithText("主题设置").assertExists()
        composeTestRule.onNodeWithText("阅读设置").assertExists()
    }
    
    @Test
    fun secondaryDrawerContent_点击菜单项应该触发回调() {
        var clickedItem: SecondaryNavItem? = null
        
        composeTestRule.setContent {
            SecondaryDrawerContent(
                selectedPrimaryItem = PrimaryNavItem.Library,
                onItemClick = { clickedItem = it }
            )
        }
        
        // 点击"全部书籍"
        composeTestRule.onNodeWithText("全部书籍").performClick()
        
        // 验证回调被触发
        assert(clickedItem?.id == "all_books")
    }
}
