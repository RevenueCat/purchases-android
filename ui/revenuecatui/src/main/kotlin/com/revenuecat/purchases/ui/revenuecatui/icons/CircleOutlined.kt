@file:Suppress("MagicNumber", "ObjectPropertyName")

package com.revenuecat.purchases.ui.revenuecatui.icons

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

internal val CircleOutlined: ImageVector
    get() {
        if (_circleOutlined != null) {
            return _circleOutlined!!
        }
        _circleOutlined = materialIcon(name = "Outlined.Circle") {
            materialPath {
                // Circle outline path
                moveTo(12.0f, 2.0f)
                curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                curveTo(2.0f, 17.52f, 6.48f, 22.0f, 12.0f, 22.0f)
                curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
                curveTo(22.0f, 6.48f, 17.52f, 2.0f, 12.0f, 2.0f)
                close()
                moveTo(12.0f, 20.0f)
                curveTo(7.58f, 20.0f, 4.0f, 16.42f, 4.0f, 12.0f)
                curveTo(4.0f, 7.58f, 7.58f, 4.0f, 12.0f, 4.0f)
                curveTo(16.42f, 4.0f, 20.0f, 7.58f, 20.0f, 12.0f)
                curveTo(20.0f, 16.42f, 16.42f, 20.0f, 12.0f, 20.0f)
                close()
            }
        }
        return _circleOutlined!!
    }

private var _circleOutlined: ImageVector? = null
