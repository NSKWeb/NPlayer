package com.example.nplayer.data.prefs

/** How the app picks between the light and dark colour schemes. */
enum class ThemeMode {
    /** Follow the device's own light/dark setting. */
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        /** Maps a stored value onto a mode, falling back to [SYSTEM] for unknown input. */
        fun fromStorage(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}
