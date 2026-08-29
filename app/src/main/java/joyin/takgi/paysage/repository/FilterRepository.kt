package joyin.takgi.paysage.repository

import joyin.takgi.paysage.data.FilterDao
import joyin.takgi.paysage.data.FilterRule
import joyin.takgi.paysage.data.FilterType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FilterRepository(private val filterDao: FilterDao) {

    val allRules: Flow<List<FilterRule>> = filterDao.getAll()

    suspend fun insert(rule: FilterRule) = filterDao.insert(rule)

    suspend fun update(rule: FilterRule) = filterDao.update(rule)

    suspend fun delete(rule: FilterRule) = filterDao.delete(rule)

    suspend fun shouldForward(sender: String, content: String): Boolean {
        val rules = allRules.first()
            .filter { it.isEnabled }
            .map { it.copy(value = it.value.trim()) }
            .filter { it.value.isNotEmpty() }

        val blacklists = rules.filter { it.type == FilterType.BLACKLIST }
        val whitelists = rules.filter { it.type == FilterType.WHITELIST }
        val bodyKeywordBlocks = rules.filter {
            it.type == FilterType.KEYWORD || it.type == FilterType.BODY_KEYWORD_BLOCK
        }
        val bodyKeywordAllows = rules.filter { it.type == FilterType.BODY_KEYWORD_ALLOW }
        val bodyRegexBlocks = rules.filter { it.type == FilterType.BODY_REGEX_BLOCK }

        if (blacklists.any { sender.contains(it.value, ignoreCase = true) }) return false

        if (whitelists.isNotEmpty() && !whitelists.any { sender.contains(it.value, ignoreCase = true) }) {
            return false
        }

        if (bodyKeywordBlocks.any { content.contains(it.value, ignoreCase = true) }) return false

        if (bodyRegexBlocks.any { it.matchesContent(content) }) return false

        if (bodyKeywordAllows.isNotEmpty()) {
            return bodyKeywordAllows.any { content.contains(it.value, ignoreCase = true) }
        }

        return true
    }

    private fun FilterRule.matchesContent(content: String): Boolean {
        return runCatching {
            Regex(value, RegexOption.IGNORE_CASE).containsMatchIn(content)
        }.getOrDefault(false)
    }
}
