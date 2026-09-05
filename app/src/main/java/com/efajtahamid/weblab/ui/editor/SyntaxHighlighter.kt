package com.efajtahamid.weblab.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.efajtahamid.weblab.data.model.EditorLanguage

/**
 * A deliberately lightweight, regex-based highlighter. It is not a full parser —
 * that would be overkill for a mobile editor — but it covers the token classes
 * that matter for readability: keywords, strings, comments, numbers, and tags.
 */
object SyntaxHighlighter {

    private val colorKeyword = Color(0xFFA855F7)
    private val colorString = Color(0xFF34D399)
    private val colorComment = Color(0xFF5B6472)
    private val colorNumber = Color(0xFFFBBF24)
    private val colorTag = Color(0xFF22D3EE)
    private val colorAttr = Color(0xFFFBBF24)
    private val colorDefault = Color(0xFFE5E7EB)

    private val jsKeywords = setOf(
        "const", "let", "var", "function", "return", "if", "else", "for", "while",
        "do", "switch", "case", "break", "continue", "class", "extends", "new",
        "this", "import", "export", "default", "from", "async", "await", "try",
        "catch", "finally", "throw", "typeof", "instanceof", "null", "undefined",
        "true", "false", "static", "get", "set", "require", "module", "exports"
    )
    private val cssKeywords = setOf(
        "important", "inherit", "initial", "unset", "none", "auto", "flex", "grid"
    )

    fun highlight(text: String, language: EditorLanguage): AnnotatedString = when (language) {
        EditorLanguage.JAVASCRIPT, EditorLanguage.TYPESCRIPT -> highlightJs(text)
        EditorLanguage.JSON -> highlightJs(text) // JSON tokens are a subset of JS tokens for our purposes
        EditorLanguage.CSS -> highlightCss(text)
        EditorLanguage.HTML -> highlightHtml(text)
        EditorLanguage.MARKDOWN, EditorLanguage.PLAIN -> buildAnnotatedString { append(text) }
    }

    private fun highlightJs(text: String): AnnotatedString = buildAnnotatedString {
        append(text)
        // Comments (line)
        Regex("//[^\\n]*").findAll(text).forEach { addStyleSpan(it.range, colorComment) }
        // Comments (block)
        Regex("/\\*[\\s\\S]*?\\*/").findAll(text).forEach { addStyleSpan(it.range, colorComment) }
        // Strings
        Regex("(\"([^\"\\\\]|\\\\.)*\")|('([^'\\\\]|\\\\.)*')|(`([^`\\\\]|\\\\.)*`)").findAll(text)
            .forEach { addStyleSpan(it.range, colorString) }
        // Numbers
        Regex("\\b\\d+(\\.\\d+)?\\b").findAll(text).forEach { addStyleSpan(it.range, colorNumber) }
        // Keywords
        Regex("\\b(${jsKeywords.joinToString("|")})\\b").findAll(text)
            .forEach { addStyleSpan(it.range, colorKeyword) }
    }

    private fun highlightCss(text: String): AnnotatedString = buildAnnotatedString {
        append(text)
        Regex("/\\*[\\s\\S]*?\\*/").findAll(text).forEach { addStyleSpan(it.range, colorComment) }
        Regex("\\.[a-zA-Z_-][a-zA-Z0-9_-]*|#[a-zA-Z_-][a-zA-Z0-9_-]*").findAll(text)
            .forEach { addStyleSpan(it.range, colorTag) }
        Regex("(\"([^\"\\\\]|\\\\.)*\")|('([^'\\\\]|\\\\.)*')").findAll(text)
            .forEach { addStyleSpan(it.range, colorString) }
        Regex("\\b(${cssKeywords.joinToString("|")})\\b").findAll(text)
            .forEach { addStyleSpan(it.range, colorKeyword) }
        Regex("[a-zA-Z-]+(?=\\s*:)").findAll(text).forEach { addStyleSpan(it.range, colorAttr) }
    }

    private fun highlightHtml(text: String): AnnotatedString = buildAnnotatedString {
        append(text)
        Regex("<!--[\\s\\S]*?-->").findAll(text).forEach { addStyleSpan(it.range, colorComment) }
        Regex("</?[a-zA-Z0-9-]+").findAll(text).forEach { addStyleSpan(it.range, colorTag) }
        Regex("[a-zA-Z-]+(?==\")").findAll(text).forEach { addStyleSpan(it.range, colorAttr) }
        Regex("\"([^\"\\\\]|\\\\.)*\"").findAll(text).forEach { addStyleSpan(it.range, colorString) }
    }

    private fun androidx.compose.ui.text.AnnotatedString.Builder.addStyleSpan(range: IntRange, color: Color) {
        if (range.isEmpty()) return
        addStyle(SpanStyle(color = color), range.first, range.last + 1)
    }
}
