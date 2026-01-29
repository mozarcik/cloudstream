package com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Custom icons for extensions UI
 */
object CustomIcons {
    val GitHub: ImageVector
        get() = Icons.Filled.GitHub

    private val Icons.Filled.GitHub: ImageVector
        get() {
            if (_github != null) {
                return _github!!
            }
            _github = materialIcon(name = "Filled.GitHub") {
                materialPath {
                    // Simplified GitHub octocat icon
                    moveTo(12f, 2f)
                    curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                    curveTo(2f, 16.418f, 4.865f, 20.167f, 8.839f, 21.489f)
                    curveTo(9.339f, 21.58f, 9.522f, 21.28f, 9.522f, 21.02f)
                    curveTo(9.522f, 20.791f, 9.513f, 20.205f, 9.508f, 19.41f)
                    curveTo(6.726f, 20.01f, 6.139f, 18.158f, 6.139f, 18.158f)
                    curveTo(5.685f, 17.017f, 5.029f, 16.718f, 5.029f, 16.718f)
                    curveTo(4.121f, 16.103f, 5.098f, 16.116f, 5.098f, 16.116f)
                    curveTo(6.101f, 16.186f, 6.629f, 17.131f, 6.629f, 17.131f)
                    curveTo(7.521f, 18.652f, 8.97f, 18.207f, 9.539f, 17.956f)
                    curveTo(9.631f, 17.31f, 9.889f, 16.865f, 10.175f, 16.62f)
                    curveTo(7.955f, 16.371f, 5.619f, 15.524f, 5.619f, 11.478f)
                    curveTo(5.619f, 10.399f, 6.01f, 9.517f, 6.649f, 8.829f)
                    curveTo(6.546f, 8.581f, 6.203f, 7.586f, 6.747f, 6.226f)
                    curveTo(6.747f, 6.226f, 7.586f, 5.962f, 9.496f, 7.283f)
                    curveTo(10.294f, 7.062f, 11.15f, 6.951f, 12f, 6.947f)
                    curveTo(12.85f, 6.951f, 13.706f, 7.062f, 14.504f, 7.283f)
                    curveTo(16.414f, 5.962f, 17.253f, 6.226f, 17.253f, 6.226f)
                    curveTo(17.797f, 7.586f, 17.454f, 8.581f, 17.351f, 8.829f)
                    curveTo(17.99f, 9.517f, 18.381f, 10.399f, 18.381f, 11.478f)
                    curveTo(18.381f, 15.533f, 16.041f, 16.368f, 13.813f, 16.611f)
                    curveTo(14.171f, 16.926f, 14.491f, 17.548f, 14.491f, 18.498f)
                    curveTo(14.491f, 19.869f, 14.479f, 20.973f, 14.479f, 21.02f)
                    curveTo(14.479f, 21.282f, 14.659f, 21.584f, 15.165f, 21.488f)
                    curveTo(19.135f, 20.163f, 22f, 16.416f, 22f, 12f)
                    curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                    close()
                }
            }
            return _github!!
        }

    private var _github: ImageVector? = null
}
