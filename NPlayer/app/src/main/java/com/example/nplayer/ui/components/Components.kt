package com.example.nplayer.ui.components

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFramePercent

/** The single grid specification shared by Home, Search, Folders and Favourites. */
val MediaGridCells = GridCells.Adaptive(minSize = 116.dp)
val MediaGridPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 24.dp)
val MediaGridHorizontalSpacing = 12.dp
val MediaGridVerticalSpacing = 16.dp

/**
 * How far into a video the grid thumbnail is taken, as a fraction of its length.
 * Frame 0 is black in a lot of encodes (fade-in, slate, encoder leader), which reads
 * as "no thumbnail" against a dark card. A proportion rather than a fixed offset is
 * used so a clip shorter than the offset still resolves to a real frame.
 */
private const val THUMBNAIL_FRAME_FRACTION = 0.1

/**
 * Builds the Coil request for a card's artwork. A `content://` video is asked for a
 * frame past the opening, so a black first frame does not look like a missing image;
 * anything else (an album cover, a plain image URI) is loaded unchanged.
 */
@Composable
fun thumbnailRequest(context: Context, model: Any): Any =
    if (model is Uri && context.contentResolver.getType(model)?.startsWith("video/") == true) {
        remember(model) {
            ImageRequest.Builder(context)
                .data(model)
                .videoFramePercent(THUMBNAIL_FRAME_FRACTION)
                .build()
        }
    } else {
        model
    }

/**
 * Grid card for a device file. [imageModel] accepts a `content://` [android.net.Uri]
 * for video thumbnails, or null for audio, which shows a music glyph instead.
 *
 * A duration pill sits on the artwork and a soft bottom gradient keeps it legible
 * over bright frames; both are decoration and carry no extra semantics.
 */
@Composable
fun PosterCard(
    title: String,
    imageModel: Any?,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 2f / 3f,
    placeholderIcon: ImageVector = Icons.Filled.MusicNote,
    badge: String? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 2.dp),
    ) {
        val context = LocalContext.current
        // A failure falls back to the placeholder glyph rather than an empty box, so a
        // file MediaMetadataRetriever cannot decode still reads as a card.
        var failed by remember(imageModel) { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (imageModel != null && !failed) {
                AsyncImage(
                    model = thumbnailRequest(context, imageModel),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    onError = { failed = true },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = placeholderIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp),
                )
            }

            // Legibility scrim behind the duration pill.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.6f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.45f),
                        ),
                    ),
            )

            if (badge != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.62f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp, start = 2.dp, end = 2.dp),
            )
        }
    }
}

/** Section heading with an accent bar and a trailing count. */
@Composable
fun SectionHeader(
    text: String,
    count: Int? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Centred message with an optional action, used for empty and blocked states. */
@Composable
fun MessageState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(22.dp))
                Button(onClick = onAction, shape = RoundedCornerShape(14.dp)) { Text(actionLabel) }
            }
        }
    }
}
