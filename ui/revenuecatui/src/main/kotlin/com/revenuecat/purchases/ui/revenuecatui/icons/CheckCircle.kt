@file:Suppress("MagicNumber", "ObjectPropertyName")

package com.revenuecat.purchases.ui.revenuecatui.icons

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

internal val CheckCircle: ImageVector
    get() {
        if (_checkCircle != null) {
            return _checkCircle!!
        }
        _checkCircle = materialIcon(name = "Filled.CheckCircle") {
            materialPath {
                moveTo(12.0f, 2.0f)
                curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
                reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
                reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
                close()
                moveTo(10.0f, 17.0f)
                lineToRelative(-5.0f, -5.0f)
                lineToRelative(1.41f, -1.41f)
                lineToRelative(3.59f, 3.58f)
                lineTo(17.59f, 6.59f)
                lineTo(19.0f, 8.0f)
                lineToRelative(-9.0f, 9.0f)
                close()
            }
        }
        return _checkCircle!!
    }

private var _checkCircle: ImageVector? = null
