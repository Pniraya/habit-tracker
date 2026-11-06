package com.habittracker.core.designsystem

/**
 * Presentation rules taken from the concept mockups, kept free of Compose types so the JVM test
 * runner can pin them without loading the UI toolkit.
 */
object HabitVisuals {

    const val ICON_SPROUT: String = "sprout"
    const val ICON_WATER: String = "water"
    const val ICON_BOOK: String = "book"
    const val ICON_BREATHE: String = "breathe"
    const val ICON_MOON: String = "moon"
    const val ICON_HEART: String = "heart"

    /** Prefix of an icon picked from the user's gallery; the rest is a file name in filesDir/icons. */
    const val CUSTOM_PREFIX: String = "custom:"

    const val COLOR_MOSS: String = "#5B8C6A"
    const val COLOR_APRICOT: String = "#F2A65A"
    const val COLOR_BLUE: String = "#8FAFC4"

    /** Selectable icons, in the order the design file lays them out. */
    val ICONS: List<String> = listOf(ICON_SPROUT, ICON_WATER, ICON_BOOK, ICON_BREATHE, ICON_MOON, ICON_HEART)

    /** Selectable colours, in the order the design file lays them out. */
    val COLORS: List<String> = listOf(COLOR_MOSS, COLOR_APRICOT, COLOR_BLUE)

    private const val FALLBACK_COLOR: Int = 0xFF5B8C6A.toInt()

    /**
     * Habits created before the icon picker existed carry icon "star" (the old default), which has
     * no artwork. Anything unknown falls back to the sprout so a row never renders an empty tile.
     */
    fun iconKey(icon: String): String = if (icon in ICONS || isCustom(icon)) icon else ICON_SPROUT

    fun isCustom(icon: String): Boolean = icon.startsWith(CUSTOM_PREFIX)

    /**
     * File name behind a `custom:` value, or null when the value is not custom or would escape the
     * icons directory. The check is pure so it can be tested without Android.
     */
    fun customFileName(icon: String): String? {
        if (!isCustom(icon)) return null
        val name = icon.removePrefix(CUSTOM_PREFIX)
        return name.takeIf {
            it.isNotBlank() && !it.contains('/') && !it.contains('\\') && !it.contains("..")
        }
    }

    fun colorKey(color: String): String = if (color in COLORS) color else COLOR_MOSS

    /** Parses "#RRGGBB"; the stored value is free text, so an unusable value must not crash the list. */
    fun parseColor(hex: String, fallback: Int = FALLBACK_COLOR): Int {
        val body = hex.trim().removePrefix("#")
        if (body.length != 6) return fallback
        val parsed = body.toLongOrNull(16) ?: return fallback
        return (0xFF000000L or parsed).toInt()
    }

    /** 0f..1f for the progress ring; an empty plan is empty progress, not a division by zero. */
    fun ringSweep(done: Int, total: Int): Float {
        if (total <= 0) return 0f
        return (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    /** The encouragement line in the progress card, copied verbatim from the mockup. */
    fun todayMessage(done: Int, total: Int): String = when {
        total <= 0 -> "还没有习惯，先添加一个吧"
        done >= total -> "今天也做到了"
        else -> "再完成 " + (total - done) + " 个，今天就圆满了"
    }

    /**
     * The header badge is a single number, while streaks are tracked per habit. The badge shows the
     * best ongoing streak so the number is always a real streak of a real habit, never an invented
     * total. Zero hides the badge, matching the old per-row rule.
     */
    fun bestStreak(streaks: List<Int>): Int = streaks.maxOrNull()?.coerceAtLeast(0) ?: 0

    fun badgeText(streak: Int): String = "连续 " + streak + " 天"
}
