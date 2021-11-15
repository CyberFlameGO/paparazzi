/*
 * Copyright (C) 2021 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.paparazzi.accessibility

import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import app.cash.paparazzi.RenderExtension
import app.cash.paparazzi.accessibility.RenderSettings.DEFAULT_DESCRIPTION_BACKGROUND_COLOR
import app.cash.paparazzi.accessibility.RenderSettings.DEFAULT_RECT_SIZE
import app.cash.paparazzi.accessibility.RenderSettings.DEFAULT_TEXT_COLOR
import app.cash.paparazzi.accessibility.RenderSettings.DEFAULT_TEXT_SIZE
import app.cash.paparazzi.accessibility.RenderSettings.getColor
import app.cash.paparazzi.accessibility.RenderSettings.toColorInt
import java.awt.image.BufferedImage

class AccessibilityRenderExtension : RenderExtension {
  private var renderExtensionImage: BufferedImage? = null

  override fun preRender(
    contentView: View,
    render: (View) -> BufferedImage
  ) {
    val linearLayout = LinearLayout(contentView.context).apply {
      orientation = LinearLayout.VERTICAL
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      setBackgroundColor(DEFAULT_DESCRIPTION_BACKGROUND_COLOR.toColorInt())
    }

    fun buildAccessibilityRow(view: View, iterableTextForAccessibility: CharSequence): View {
      val context = view.context
      val color = getColor(view).toColorInt()
      val margin = view.dip(8)
      val innerMargin = view.dip(4)

      fun buildLayoutParams(
        width: Int = ViewGroup.LayoutParams.MATCH_PARENT,
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
      ): ViewGroup.LayoutParams {
        return ViewGroup.LayoutParams(
          width,
          height
        )
      }

      return LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = buildLayoutParams()
        setPaddingRelative(margin, innerMargin, margin, innerMargin)

        addView(View(context).apply {
          layoutParams = buildLayoutParams(dip(DEFAULT_RECT_SIZE), dip(DEFAULT_RECT_SIZE))
          background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(color, color)
          ).apply {
            cornerRadius = dip(DEFAULT_RECT_SIZE / 4f)
          }
          setPaddingRelative(innerMargin, innerMargin, innerMargin, innerMargin)
        })
        addView(TextView(context).apply {
          layoutParams = buildLayoutParams()
          text = iterableTextForAccessibility
          textSize = DEFAULT_TEXT_SIZE
          setTextColor(DEFAULT_TEXT_COLOR.toColorInt())
          setPaddingRelative(innerMargin, 0, innerMargin, 0)
        })
      }
    }

    fun renderAccessibility(view: View) {
      if (view.isImportantForAccessibility && !view.iterableTextForAccessibility.isNullOrBlank()) {
        linearLayout.addView(buildAccessibilityRow(view, view.iterableTextForAccessibility))
      }

      if (view is ViewGroup) {
        (0 until view.childCount).forEach {
          renderAccessibility(view.getChildAt(it))
        }
      }
    }

    renderAccessibility(contentView)

    renderExtensionImage = render(linearLayout)
  }

  override fun postRender(
    contentView: View,
    bufferedImage: BufferedImage
  ): BufferedImage {
    val graphics = bufferedImage.createGraphics()
    val location = IntArray(2)

    fun renderAccessibilityBounds(view: View, location: IntArray) {
      if (view.isImportantForAccessibility && !view.iterableTextForAccessibility.isNullOrBlank()) {
        view.getLocationInWindow(location)
        graphics.color = getColor(view)
        graphics.fillRect(location[0], location[1], view.width, view.height)
      }

      if (view is ViewGroup) {
        (0 until view.childCount).forEach {
          renderAccessibilityBounds(view.getChildAt(it), location)
        }
      }
    }

    renderAccessibilityBounds(contentView, location)
    val output = BufferedImage(
      bufferedImage.width + (renderExtensionImage?.width ?: 0),
      bufferedImage.height,
      bufferedImage.type
    ).apply {
      val outputGraphics = createGraphics()
      outputGraphics.drawImage(bufferedImage, 0, 0, null)
      renderExtensionImage?.let {
        outputGraphics.drawImage(it, bufferedImage.width, 0, null)
      }

      // Release render extension image from memory once its drawn to the output.
      renderExtensionImage = null
    }
    return output
  }
}

private fun View.dip(value: Float): Float =
  TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    value,
    resources.displayMetrics
  )

private fun View.dip(value: Int): Int =
  dip(value.toFloat()).toInt()
