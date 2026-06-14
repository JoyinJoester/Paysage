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
        val rules = allRules.first().filter { it.isEnabled }

        val blacklists = rules.filter { it.type == FilterType.BLACKLIST }
        val whitelists = rules.filter { it.type == FilterType.WHITELIST }
        val keywords = rules.filter { it.type == FilterType.KEYWORD }

        if (blacklists.any { sender.contains(it.value) }) return false

        if (whitelists.isNotEmpty() && !whitelists.any { sender.contains(it.value) }) {
            return false
        }

        if (keywords.isNotEmpty()) {
            return keywords.any { content.contains(it.value, ignoreCase = true) }
        }

        return true
    }
}
