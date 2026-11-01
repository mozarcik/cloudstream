package com.lagradost.cloudstream3.tv.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Filled.CustomDownload: ImageVector
    get() {
        if (_customDownload != null) {
            return _customDownload!!
        }
        _customDownload = materialIcon(name = "Filled.CustomDownload") {

            // === Arrow down ===
            materialPath {
                moveTo(12.0f, 16.0f)
                lineTo(7.0f, 11.0f)
                lineTo(8.4f, 9.6f)
                lineTo(11.0f, 12.2f)
                verticalLineTo(4.0f)
                horizontalLineTo(13.0f)
                verticalLineTo(12.2f)
                lineTo(15.6f, 9.6f)
                lineTo(17.0f, 11.0f)
                close()
            }

            // === Tray / base ===
            materialPath {
                moveTo(6.0f, 20.0f)
                curveTo(4.9f, 20.0f, 4.0f, 19.1f, 4.0f, 18.0f)
                verticalLineTo(15.0f)
                horizontalLineTo(6.0f)
                verticalLineTo(18.0f)
                horizontalLineTo(18.0f)
                verticalLineTo(15.0f)
                horizontalLineTo(20.0f)
                verticalLineTo(18.0f)
                curveTo(20.0f, 19.1f, 19.1f, 20.0f, 18.0f, 20.0f)
                close()
            }
        }
        return _customDownload!!
    }

private var _customDownload: ImageVector? = null
