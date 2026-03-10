package com.lagradost.cloudstream3.ui.settings

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources

object Globals {
    var beneneCount = 0

    const val PHONE : Int = 0b001
    const val TV : Int = 0b010
    const val EMULATOR : Int = 0b100
    const val TV_MATERIAL : Int = 0b1000
    private const val INVALID = -1
    private var layoutId = INVALID

    fun Context.updateTv() {
        layoutId = resolvedAppLayoutMode().resolvedLayoutFlag
    }

    /** Returns true if the current orientation is landscape. */
    fun isLandscape(): Boolean =
        isLayout(TV or EMULATOR) ||
            Resources.getSystem().configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    /** Returns true if the layout is any of the flags,
     * so isLayout(TV or EMULATOR) is a valid statement for checking if the layout is in the emulator
     * or tv. Auto will become the "TV" or the "PHONE" layout.
     *
     * Valid flags are: PHONE, TV, EMULATOR, TV_MATERIAL
     * */
    fun isLayout(flags: Int) : Boolean {
        if (layoutId == INVALID) return false
        val currentLayout = if (layoutId == TV_MATERIAL) layoutId or TV else layoutId
        return (currentLayout and flags) != 0
    }
}
