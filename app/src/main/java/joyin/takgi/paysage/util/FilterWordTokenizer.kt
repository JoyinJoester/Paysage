package joyin.takgi.paysage.util

/**
 * 从转发日志正文里挑出适合作为过滤关键词的候选词。
 * 按空白与中英文标点切分,保留长度 >= 2 的片段并按出现顺序去重;
 * 纯逻辑无 Android 依赖,便于单元测试。
 */
object FilterWordTokenizer {

    fun tokenize(content: String): List<String> =
        content
            .split(*SPLIT_DELIMITERS)
            .map { it.trim() }
            .filter { it.length >= MIN_TOKEN_LENGTH }
            .distinct()

    private val SPLIT_DELIMITERS = charArrayOf(
        ' ', '\t', '\n', '\r',
        ',', '.', ';', ':', '?', '!', '~', '*', '#', '@', '&', '(', ')', '[', ']', '{', '}', '"', '\'',
        '，', '。', '；', '：', '？', '！', '“', '”', '‘', '’', '（', '）', '《', '》', '、', '…', '—'
    )

    private const val MIN_TOKEN_LENGTH = 2
}
