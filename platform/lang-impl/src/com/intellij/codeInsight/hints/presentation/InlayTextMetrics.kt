// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.ide.ui.AntialiasingType
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.impl.FontInfo
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.ApiStatus
import java.awt.Font
import java.awt.FontMetrics
import java.awt.font.FontRenderContext
import kotlin.math.ceil
import kotlin.math.max

@ApiStatus.Internal
class InlayTextMetricsStorage(val editor: EditorImpl) {
  private var smallTextMetrics : InlayTextMetrics? = null
  private var normalTextMetrics : InlayTextMetrics? = null

  val smallTextSize: Int
    @RequiresEdt
    get() = max(1, editor.colorsScheme.editorFontSize - 1)


  val normalTextSize: Int
    @RequiresEdt
    get() = editor.colorsScheme.editorFontSize

  @RequiresEdt
  fun getFontMetrics(small: Boolean): InlayTextMetrics {
    var metrics: InlayTextMetrics?
    if (small) {
      metrics = smallTextMetrics
      val fontSize = smallTextSize
      if (metrics == null || !metrics.isActual(smallTextSize)) {
        metrics = InlayTextMetrics.create(editor, fontSize)
        smallTextMetrics = metrics
      }
    } else {
      metrics = normalTextMetrics
      val fontSize = normalTextSize
      if (metrics == null || !metrics.isActual(normalTextSize)) {
        metrics = InlayTextMetrics.create(editor, fontSize)
        normalTextMetrics = metrics
      }
    }
    return metrics
  }
}

class InlayTextMetrics(
  private val editor: EditorImpl,
  val fontHeight: Int,
  val fontBaseline: Int,
  private val fontMetrics: FontMetrics
) {
  companion object {
    fun create(editor: EditorImpl, size: Int) : InlayTextMetrics {
      val editorFont = EditorUtil.getEditorFont()
      val font = editorFont.deriveFont(size.toFloat())
      val context = getCurrentContext(editor)
      val metrics = FontInfo.getFontMetrics(font, context)
      // We assume this will be a better approximation to a real line height for a given font
      val fontHeight = ceil(font.createGlyphVector(context, "Albpq@").visualBounds.height).toInt()
      val fontBaseline = ceil(font.createGlyphVector(context, "Alb").visualBounds.height).toInt()
      return InlayTextMetrics(editor, fontHeight, fontBaseline, metrics)
    }

    private fun getCurrentContext(editor: Editor): FontRenderContext {
      val editorContext = FontInfo.getFontRenderContext(editor.contentComponent)
      return FontRenderContext(editorContext.transform,
                               AntialiasingType.getKeyForCurrentScope(false),
                               UISettings.editorFractionalMetricsHint)
    }
  }

  val font: Font
    get() = fontMetrics.font

  // Editor metrics:
  val ascent: Int
    get() = editor.ascent
  val descent: Int
    get() = editor.descent

  fun isActual(size: Int) : Boolean {
    if (size != font.size) return false
    if (font.family != EditorColorsManager.getInstance().globalScheme.editorFontName) return false
    return getCurrentContext(editor).equals(fontMetrics.fontRenderContext)
  }

  /**
   * Offset from the top edge of drawing rectangle to rectangle with text.
   */
  fun offsetFromTop(): Int = (editor.lineHeight - fontHeight) / 2

  fun getStringWidth(text: String): Int {
    return fontMetrics.stringWidth(text)
  }
}