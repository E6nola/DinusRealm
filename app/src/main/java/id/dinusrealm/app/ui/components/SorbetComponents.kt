package id.dinusrealm.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import id.dinusrealm.app.core.i18n.tr
import id.dinusrealm.app.ui.theme.DinusIcons
import id.dinusrealm.app.ui.theme.DinusTheme
import java.text.NumberFormat
import java.util.Locale

fun tabular(style: TextStyle): TextStyle = style.copy(fontFeatureSettings = "tnum")

fun formatRupiah(amount: Long): String {
    val formatted = NumberFormat.getNumberInstance(Locale("in", "ID")).format(amount)
    return "Rp$formatted"
}

@Composable
fun DashedDivider(
    color: Color = DinusTheme.palette.dashedDivider,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        )
    }
}

@Composable
fun SorbetCard(
    modifier: Modifier = Modifier,
    containerColor: Color = DinusTheme.palette.surface,
    borderColor: Color = DinusTheme.palette.border,
    cornerRadius: Dp = 24.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val p = DinusTheme.palette
    val showBorder = p.isDark || borderColor != p.border
    val stroke = if (showBorder) BorderStroke(1.dp, borderColor) else null

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(cornerRadius),
            color = containerColor,
            border = stroke,
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(cornerRadius),
            color = containerColor,
            border = stroke,
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}

/**
 * Neo-Swiss flat screen header matching both Light Alabaster & Dark Matte Obsidian themes
 * with optional Acid Lime KPI pill.
 */
@Composable
fun CurvedHeroHeader(
    title: String,
    subtitle: String,
    gradientStart: Color,
    gradientEnd: Color,
    onBack: (() -> Unit)? = null,
    rightStat: String? = null,
    rightStatLabel: String? = null,
    bottomContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    val p = DinusTheme.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(p.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)
        ) {
            if (onBack != null || !rightStat.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (onBack != null) {
                        Surface(
                            onClick = onBack,
                            shape = CircleShape,
                            color = p.surface,
                            border = if (p.isDark) BorderStroke(1.dp, p.border) else null,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = DinusIcons.Back,
                                    contentDescription = tr("Back", "Kembali"),
                                    tint = p.ink,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.size(1.dp))
                    }

                    if (!rightStat.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = p.lime
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = rightStat,
                                    style = tabular(MaterialTheme.typography.titleLarge).copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    ),
                                    color = p.onLime
                                )
                                if (!rightStatLabel.isNullOrBlank()) {
                                    Text(
                                        text = rightStatLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = p.onLime.copy(alpha = 0.78f)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 23.sp
                ),
                color = p.ink
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = p.inkMuted
                )
            }

            if (bottomContent != null) {
                Spacer(Modifier.height(12.dp))
                bottomContent()
            }
        }
        HorizontalDivider(thickness = 1.dp, color = p.border)
    }
}

/**
 * Tactile Swiss Bento schedule/course row card (Matte Charcoal when active/live, Alabaster/Graphite otherwise).
 */
@Composable
fun ColorfulCourseCard(
    colorStart: Color,
    colorEnd: Color,
    badgeCode: String,
    title: String,
    subtitle: String,
    trailingPill: String,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val p = DinusTheme.palette
    val liveAlert = trailingPill.equals("Attend", ignoreCase = true) ||
        trailingPill.equals("Attend Now", ignoreCase = true) ||
        trailingPill.equals("Bisa Absen", ignoreCase = true) ||
        trailingPill.equals("Absen", ignoreCase = true) ||
        trailingPill.equals("Absen Dibuka", ignoreCase = true)
    val attended = trailingPill.equals("Present", ignoreCase = true) ||
        trailingPill.equals("Hadir", ignoreCase = true)
    val active = isHighlighted || liveAlert

    val container = if (active) p.cardActive else p.surface
    val titleColor = if (active) p.onCardActive else p.ink
    val subColor = if (active) p.onCardActiveMuted else p.inkMuted
    val stroke = if (p.isDark) BorderStroke(1.dp, p.border) else null

    val badgeBg = when {
        active -> p.lime
        p.isDark -> p.surfaceVariant
        else -> Color.White
    }
    val badgeInk = if (active) p.onLime else p.ink

    val pillBg = when {
        liveAlert -> p.lime
        attended -> p.mintSoft
        active -> Color.White.copy(alpha = 0.12f)
        p.isDark -> p.surfaceVariant
        else -> Color.White
    }
    val pillInk = when {
        liveAlert -> p.onLime
        attended -> p.mint
        active -> p.onCardActive
        else -> p.inkMuted
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = container,
        border = stroke,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = badgeBg,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                    Text(
                        text = badgeCode.ifBlank { "-" },
                        style = tabular(MaterialTheme.typography.labelSmall).copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = badgeInk,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        style = tabular(MaterialTheme.typography.labelMedium),
                        color = subColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trailingPill.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = pillBg
                ) {
                    Text(
                        text = trailingPill,
                        style = tabular(MaterialTheme.typography.labelSmall).copy(fontWeight = FontWeight.Bold),
                        color = pillInk,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Smart Bento module tile: circular icon badge -> bold title & live metric -> dashed rule -> status + chevron.
 */
@Composable
fun SoftGridTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    softBg: Color,
    progress: Float,
    modifier: Modifier = Modifier,
    isFeatured: Boolean = false,
    statusLabel: String = "Open",
    isActiveToggle: Boolean = false,
    onClick: () -> Unit
) {
    val p = DinusTheme.palette
    val container = if (isFeatured) p.cardActive else p.surface
    val titleColor = if (isFeatured) p.onCardActive else p.ink
    val mutedColor = if (isFeatured) p.onCardActiveMuted else p.inkMuted
    val dividerColor = if (isFeatured) p.dashedDividerActive else p.dashedDivider
    val stroke = if (p.isDark) BorderStroke(1.dp, p.border) else null
    val wellBg = when {
        isFeatured && p.isDark -> p.lime
        isFeatured -> Color.White
        p.isDark -> p.surfaceVariant
        else -> Color.White
    }
    val wellIconTint = when {
        isFeatured && p.isDark -> p.onLime
        isFeatured -> Color(0xFF141618)
        else -> p.ink
    }

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = container,
        border = stroke,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                shape = CircleShape,
                color = wellBg,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = wellIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = tabular(MaterialTheme.typography.labelSmall),
                color = mutedColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))
            DashedDivider(color = dividerColor)
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusLabel,
                    style = tabular(MaterialTheme.typography.labelLarge),
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                )
                Icon(
                    imageVector = DinusIcons.ChevronRight,
                    contentDescription = null,
                    tint = mutedColor,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun CircularRingBadge(
    percentage: Double,
    accent: Color,
    trackColor: Color = DinusTheme.palette.surfaceVariant,
    size: Dp = 54.dp
) {
    val p = DinusTheme.palette
    val clamped = percentage.coerceIn(0.0, 100.0)
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = 5.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = ((clamped / 100.0) * 360.0).toFloat(),
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${clamped.toInt()}%",
            style = tabular(MaterialTheme.typography.labelSmall).copy(fontWeight = FontWeight.Bold),
            color = p.ink
        )
    }
}

@Composable
fun FruitBadge(
    text: String,
    accent: Color,
    softBg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(100.dp),
        color = softBg
    ) {
        Text(
            text = text,
            style = tabular(MaterialTheme.typography.labelSmall).copy(fontWeight = FontWeight.SemiBold),
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun FruitIconBadge(
    icon: ImageVector,
    accent: Color,
    softBg: Color,
    size: Dp = 42.dp,
    iconSize: Dp = 20.dp,
    contentDescription: String? = null
) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = softBg
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = accent,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun SorbetTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val p = DinusTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = p.surface,
                border = if (p.isDark) BorderStroke(1.dp, p.border) else null,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = DinusIcons.Back,
                        contentDescription = tr("Back", "Kembali"),
                        tint = p.ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = p.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = p.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

@Composable
fun SorbetEmptyState(
    icon: ImageVector,
    accent: Color,
    softBg: Color,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val p = DinusTheme.palette
    SorbetCard(modifier = modifier, contentPadding = PaddingValues(24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FruitIconBadge(icon = icon, accent = accent, softBg = softBg, size = 56.dp, iconSize = 26.dp)
            Spacer(Modifier.height(14.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = p.ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = p.inkMuted, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = p.lime, contentColor = p.onLime)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SorbetMessageStrip(
    message: String,
    accent: Color,
    softBg: Color,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    SorbetCard(
        modifier = modifier,
        containerColor = softBg,
        borderColor = accent.copy(alpha = 0.28f),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = DinusTheme.palette.ink,
                modifier = Modifier.weight(1f)
            )
            if (onDismiss != null) {
                TextButton(onClick = onDismiss) {
                    Text(tr("Close", "Tutup"), color = accent, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun SorbetSegmentTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    accent: Color = DinusTheme.palette.lime,
    modifier: Modifier = Modifier
) {
    val p = DinusTheme.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        tabs.forEachIndexed { idx, label ->
            val selected = idx == selectedIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(idx) }
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (selected) p.ink else p.inkMuted,
                    maxLines = 1
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(if (selected) p.lime else Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun SorbetLoadingBox(
    label: String = tr("Loading data...", "Memuat data..."),
    accent: Color = DinusTheme.palette.lime
) {
    val p = DinusTheme.palette
    SorbetCard(contentPadding = PaddingValues(28.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.5.dp,
                color = if (accent == p.lime && !p.isDark) p.onLime else accent
            )
            Spacer(Modifier.height(12.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = p.inkMuted)
        }
    }
}

@Composable
fun KeyValueRow(
    label: String,
    value: String,
    valueColor: Color = DinusTheme.palette.ink
) {
    val p = DinusTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = p.inkMuted,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value.ifBlank { "-" },
            style = tabular(MaterialTheme.typography.bodyMedium).copy(fontWeight = FontWeight.Medium),
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f)
        )
    }
}

fun renderHtmlPlain(raw: String): String {
    if (raw.isBlank()) return ""
    return HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_COMPACT)
        .toString()
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}
