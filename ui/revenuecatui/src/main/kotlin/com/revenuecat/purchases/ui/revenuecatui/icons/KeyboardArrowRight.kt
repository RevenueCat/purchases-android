@file:Suppress("MagicNumber", "ObjectPropertyName")

package com.revenuecat.purchases.ui.revenuecatui.icons

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

internal val KeyboardArrowRight: ImageVector
    get() {
        if (_keyboardArrowRight != null) {
            return _keyboardArrowRight!!
        }
        _keyboardArrowRight = materialIcon(
            name = "AutoMirrored.Filled.KeyboardArrowRight",
            autoMirror = true,
        ) {
            materialPath {
                moveTo(8.59f, 16.59f)
                lineTo(13.17f, 12.0f)
                lineTo(8.59f, 7.41f)
                lineTo(10.0f, 6.0f)
                lineToRelative(6.0f, 6.0f)
                lineToRelative(-6.0f, 6.0f)
                lineToRelative(-1.41f, -1.41f)
                close()
            }
        }
        return _keyboardArrowRight!!
    }

private var _keyboardArrowRight: ImageVector? = null
