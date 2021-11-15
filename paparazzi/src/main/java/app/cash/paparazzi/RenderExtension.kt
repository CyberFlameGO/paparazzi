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
package app.cash.paparazzi

import android.view.View
import java.awt.image.BufferedImage

/**
 * An extension for overlaying additional information on top of each rendered frame.
 */
interface RenderExtension {
  // Allows the extension to render its own view hierarchy via `render` function for the given `contentView`.
  // This is called before the `contentView` is rendered by Paparazzi
  fun preRender(contentView: View, render: (View) -> BufferedImage)

  // Allows the extension to render on top of the bufferedImage output after rendering the `contentView`
  fun postRender(contentView: View, bufferedImage: BufferedImage): BufferedImage = bufferedImage
}
