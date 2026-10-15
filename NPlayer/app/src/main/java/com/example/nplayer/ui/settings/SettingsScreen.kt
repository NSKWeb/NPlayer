package com.example.nplayer.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nplayer.BuildConfig
import com.example.nplayer.R
import com.example.nplayer.data.media.FileSortOrder
import com.example.nplayer.data.media.FolderSortOrder
import com.example.nplayer.data.prefs.OrientationMode
import com.example.nplayer.data.prefs.OverlayItem
import com.example.nplayer.data.prefs.ResumeMode
import com.example.nplayer.data.prefs.SEEK_STEP_PRESETS
import com.example.nplayer.data.prefs.ThemeMode

@Composable
fun SettingsScreen(
    onOpenPrivateFolder: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // --- Appearance ----------------------------------------------------
        SectionHeader(stringResource(R.string.appearance))
        Text(stringResource(R.string.theme_title), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(R.string.theme_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        ThemeModeSelector(selected = state.themeMode, onSelect = viewModel::setThemeMode)

        if (dynamicColorAvailable) {
            Spacer(Modifier.height(16.dp))
            SettingToggle(
                title = stringResource(R.string.dynamic_color_title),
                subtitle = stringResource(R.string.dynamic_color_subtitle),
                checked = state.dynamicColor,
                onCheckedChange = viewModel::setDynamicColor,
            )
        }

        Divider()

        // --- Playback ------------------------------------------------------
        SectionHeader(stringResource(R.string.settings_playback))
        SettingLabel(
            title = stringResource(R.string.resume_title),
            subtitle = stringResource(R.string.resume_subtitle),
        )
        Spacer(Modifier.height(10.dp))
        EnumSelector(
            options = ResumeMode.entries,
            selected = state.resumeMode,
            label = { it.label },
            onSelect = viewModel::setResumeMode,
        )

        Spacer(Modifier.height(18.dp))
        SettingToggle(
            title = stringResource(R.string.remember_brightness_title),
            subtitle = stringResource(R.string.remember_brightness_subtitle),
            checked = state.rememberBrightness,
            onCheckedChange = viewModel::setRememberBrightness,
        )
        Spacer(Modifier.height(16.dp))
        SettingToggle(
            title = stringResource(R.string.remember_background_title),
            subtitle = stringResource(R.string.remember_background_subtitle),
            checked = state.rememberBackgroundPlay,
            onCheckedChange = viewModel::setRememberBackgroundPlay,
        )
        Spacer(Modifier.height(16.dp))
        SettingToggle(
            title = stringResource(R.string.remember_aspect_title),
            subtitle = stringResource(R.string.remember_aspect_subtitle),
            checked = state.rememberAspectRatio,
            onCheckedChange = viewModel::setRememberAspectRatio,
        )
        Spacer(Modifier.height(16.dp))
        SettingToggle(
            title = stringResource(R.string.remember_speed_title),
            subtitle = stringResource(R.string.remember_speed_subtitle),
            checked = state.rememberSpeed,
            onCheckedChange = viewModel::setRememberSpeed,
        )

        Spacer(Modifier.height(18.dp))
        SettingLabel(title = stringResource(R.string.orientation_title))
        Spacer(Modifier.height(10.dp))
        EnumSelector(
            options = OrientationMode.entries,
            selected = state.orientationMode,
            label = { it.label },
            onSelect = viewModel::setOrientationMode,
        )

        Spacer(Modifier.height(18.dp))
        SettingLabel(title = stringResource(R.string.seek_step_title))
        Spacer(Modifier.height(10.dp))
        ChipRow(
            options = SEEK_STEP_PRESETS,
            selected = state.seekStepSeconds,
            label = { "${it}s" },
            onSelect = viewModel::setSeekStepSeconds,
        )

        Spacer(Modifier.height(18.dp))
        SettingToggle(
            title = stringResource(R.string.autoplay_title),
            subtitle = stringResource(R.string.autoplay_subtitle),
            checked = state.autoplayNext,
            onCheckedChange = viewModel::setAutoplayNext,
        )
        Spacer(Modifier.height(16.dp))
        SettingToggle(
            title = stringResource(R.string.auto_pip_title),
            subtitle = stringResource(R.string.auto_pip_subtitle),
            checked = state.autoPip,
            onCheckedChange = viewModel::setAutoPip,
        )
        Spacer(Modifier.height(16.dp))
        SettingToggle(
            title = stringResource(R.string.gestures_title),
            subtitle = stringResource(R.string.gestures_subtitle),
            checked = state.gesturesEnabled,
            onCheckedChange = viewModel::setGesturesEnabled,
        )

        Divider()

        // --- Subtitles -----------------------------------------------------
        SectionHeader(stringResource(R.string.settings_subtitles))
        SettingToggle(
            title = stringResource(R.string.subtitles_default_title),
            subtitle = stringResource(R.string.subtitles_default_subtitle),
            checked = state.showSubtitlesByDefault,
            onCheckedChange = viewModel::setShowSubtitlesByDefault,
        )
        Spacer(Modifier.height(18.dp))
        SettingLabel(title = stringResource(R.string.preferred_audio_language_title))
        Spacer(Modifier.height(10.dp))
        LanguageSelector(
            selected = state.preferredAudioLanguage,
            onSelect = viewModel::setPreferredAudioLanguage,
        )
        Spacer(Modifier.height(18.dp))
        SettingLabel(title = stringResource(R.string.preferred_subtitle_language_title))
        Spacer(Modifier.height(10.dp))
        LanguageSelector(
            selected = state.preferredSubtitleLanguage,
            onSelect = viewModel::setPreferredSubtitleLanguage,
        )

        Divider()

        // --- Display -------------------------------------------------------
        SectionHeader(stringResource(R.string.settings_display))
        SettingLabel(
            title = stringResource(R.string.display_overlay_title),
            subtitle = stringResource(R.string.display_overlay_subtitle),
        )
        Spacer(Modifier.height(8.dp))
        OverlayItem.entries.forEach { item ->
            SettingToggle(
                title = item.label,
                subtitle = null,
                checked = item in state.overlayItems,
                onCheckedChange = { viewModel.setOverlayItem(item, it) },
            )
            Spacer(Modifier.height(10.dp))
        }

        Divider()

        // --- Music ---------------------------------------------------------
        SectionHeader(stringResource(R.string.settings_music))
        SettingToggle(
            title = stringResource(R.string.show_music_title),
            subtitle = stringResource(R.string.show_music_subtitle),
            checked = state.showMusic,
            onCheckedChange = viewModel::setShowMusic,
        )

        Divider()

        // --- File arrangement ----------------------------------------------
        SectionHeader(stringResource(R.string.settings_arrangement))
        SettingLabel(title = stringResource(R.string.file_sort_title))
        Spacer(Modifier.height(10.dp))
        EnumSelector(
            options = FileSortOrder.entries,
            selected = state.fileSortOrder,
            label = { it.label },
            onSelect = viewModel::setFileSortOrder,
        )
        Spacer(Modifier.height(18.dp))
        SettingLabel(title = stringResource(R.string.folder_sort_title))
        Spacer(Modifier.height(10.dp))
        EnumSelector(
            options = FolderSortOrder.entries,
            selected = state.folderSortOrder,
            label = { it.label },
            onSelect = viewModel::setFolderSortOrder,
        )
        Spacer(Modifier.height(18.dp))
        SettingToggle(
            title = stringResource(R.string.folders_first_title),
            subtitle = stringResource(R.string.folders_first_subtitle),
            checked = state.foldersFirst,
            onCheckedChange = viewModel::setFoldersFirst,
        )

        Divider()

        // --- Storage -------------------------------------------------------
        SectionHeader(stringResource(R.string.settings_storage))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.clear_cache_title),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    stringResource(R.string.clear_cache_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.lastCleared
                        ?.let { stringResource(R.string.clear_cache_done, it) }
                        ?: state.cacheSize,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = viewModel::clearCache) {
                Text(stringResource(R.string.clear_cache_button))
            }
        }

        Divider()

        // --- Security ------------------------------------------------------
        SettingToggle(
            title = stringResource(R.string.biometric_title),
            subtitle = stringResource(R.string.biometric_subtitle),
            checked = state.biometricLock,
            onCheckedChange = viewModel::setBiometricLock,
        )

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenPrivateFolder)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.private_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.private_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(20.dp))
        AboutCard()

        Spacer(Modifier.height(24.dp))
    }
}

/** A titled group heading, with the divider that separates it from the group above. */
@Composable
private fun SectionHeader(title: String) {
    Spacer(Modifier.height(16.dp))
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(12.dp))
}

/** The rule between groups. */
@Composable
private fun Divider() {
    Spacer(Modifier.height(20.dp))
    HorizontalDivider()
}

@Composable
private fun SettingLabel(title: String, subtitle: String? = null) {
    Text(title, style = MaterialTheme.typography.bodyLarge)
    if (subtitle != null) {
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A segmented picker for a small enum. */
@Composable
private fun <T> EnumSelector(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(label(option), maxLines = 1) },
            )
        }
    }
}

/** A wrapping row of small choices, used for the seek-step values. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

/**
 * The language choices offered. The list is deliberately short and fixed: reading a
 * track's language tag is cheap, but there is no bundled catalogue of every ISO tag,
 * so the common ones are named and anything else is left to the per-file picker.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageSelector(selected: String?, onSelect: (String?) -> Unit) {
    val any = stringResource(R.string.language_any)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(any) },
        )
        LANGUAGE_CHOICES.forEach { (tag, name) ->
            FilterChip(
                selected = selected == tag,
                onClick = { onSelect(tag) },
                label = { Text(name) },
            )
        }
    }
}

/** Language tag to display name, for the preferred-language pickers. */
private val LANGUAGE_CHOICES = listOf(
    "en" to "English",
    "hi" to "Hindi",
    "ta" to "Tamil",
    "te" to "Telugu",
    "ml" to "Malayalam",
    "kn" to "Kannada",
    "bn" to "Bengali",
    "mr" to "Marathi",
    "es" to "Spanish",
    "fr" to "French",
    "de" to "German",
    "ja" to "Japanese",
    "ko" to "Korean",
    "zh" to "Chinese",
)

/** App identity card: name, what it does, and the build version. */
@Composable
private fun AboutCard() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.about_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val modes = ThemeMode.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                icon = { Icon(mode.icon, contentDescription = null) },
                label = { Text(mode.label()) },
            )
        }
    }
}

private val ThemeMode.icon: ImageVector
    get() = when (this) {
        ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
        ThemeMode.LIGHT -> Icons.Filled.LightMode
        ThemeMode.DARK -> Icons.Filled.DarkMode
    }

@Composable
private fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    },
)
