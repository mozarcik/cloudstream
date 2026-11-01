package com.lagradost.cloudstream3.tv.compat.theme

import android.content.Context
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.utils.UIHelper.colorFromAttribute

@StyleRes
private fun legacyPrimaryOverlayStyle(optionValue: String): Int {
    return when (optionValue) {
        "Normal" -> R.style.OverlayPrimaryColorNormal
        "DandelionYellow" -> R.style.OverlayPrimaryColorDandelionYellow
        "CarnationPink" -> R.style.OverlayPrimaryColorCarnationPink
        "Orange" -> R.style.OverlayPrimaryColorOrange
        "DarkGreen" -> R.style.OverlayPrimaryColorDarkGreen
        "Maroon" -> R.style.OverlayPrimaryColorMaroon
        "NavyBlue" -> R.style.OverlayPrimaryColorNavyBlue
        "Grey" -> R.style.OverlayPrimaryColorGrey
        "White" -> R.style.OverlayPrimaryColorWhite
        "CoolBlue" -> R.style.OverlayPrimaryColorCoolBlue
        "Brown" -> R.style.OverlayPrimaryColorBrown
        "Purple" -> R.style.OverlayPrimaryColorPurple
        "Green" -> R.style.OverlayPrimaryColorGreen
        "GreenApple" -> R.style.OverlayPrimaryColorGreenApple
        "Red" -> R.style.OverlayPrimaryColorRed
        "Banana" -> R.style.OverlayPrimaryColorBanana
        "Party" -> R.style.OverlayPrimaryColorParty
        "Pink" -> R.style.OverlayPrimaryColorPink
        "Lavender" -> R.style.OverlayPrimaryColorLavender
        "Blue" -> R.style.OverlayPrimaryColorBlue
        "Monet" -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                R.style.OverlayPrimaryColorMonet
            } else {
                R.style.OverlayPrimaryColorNormal
            }
        }
        "Monet2" -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                R.style.OverlayPrimaryColorMonetTwo
            } else {
                R.style.OverlayPrimaryColorNormal
            }
        }
        else -> R.style.OverlayPrimaryColorNormal
    }
}

@ColorInt
fun Context.resolveLegacyPrimaryPreviewColor(optionValue: String): Int {
    val themedContext = ContextThemeWrapper(this, 0)
    themedContext.theme.setTo(theme)
    themedContext.theme.applyStyle(legacyPrimaryOverlayStyle(optionValue), true)
    return themedContext.colorFromAttribute(R.attr.colorPrimary)
}
