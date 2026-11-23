package id.dinusrealm.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import id.dinusrealm.app.core.i18n.LocalAppLanguage
import id.dinusrealm.app.core.model.AppLanguage
import id.dinusrealm.app.core.model.ThemeMode

@Immutable
data class DinusFruitPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val ink: Color,
    val inkMuted: Color,
    val lime: Color,
    val onLime: Color,
    val limeSoft: Color,
    val cardActive: Color,
    val onCardActive: Color,
    val onCardActiveMuted: Color,
    val dashedDivider: Color,
    val dashedDividerActive: Color,
    val sky: Color,
    val skySoft: Color,
    val cobalt: Color,
    val cobaltSoft: Color,
    val peach: Color,
    val peachSoft: Color,
    val mint: Color,
    val mintSoft: Color,
    val mango: Color,
    val mangoSoft: Color,
    val berry: Color,
    val berrySoft: Color,
    val strawberry: Color,
    val strawberrySoft: Color
) {
    fun cycleAccent(index: Int): Pair<Color, Color> = when (Math.floorMod(index, 4)) {
        0 -> cardActive to onCardActive
        1 -> surface to ink
        2 -> limeSoft to onLime
        else -> surfaceVariant to ink
    }
}

val LightFruitPalette = DinusFruitPalette(
    isDark = false,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFFE7EAEE),
    border = Color(0xFFE4E7EC),
    ink = Color(0xFF16181A),
    inkMuted = Color(0xFF767C85),
    lime = Color(0xFFBDF90B),
    onLime = Color(0xFF141618),
    limeSoft = Color(0xFFECFCCB),
    cardActive = Color(0xFF1D2024),
    onCardActive = Color(0xFFFFFFFF),
    onCardActiveMuted = Color(0xFF9BA1AA),
    dashedDivider = Color(0xFFDFE3E8),
    dashedDividerActive = Color(0xFF343942),
    sky = Color(0xFF1D2024),
    skySoft = Color(0xFFECFCCB),
    cobalt = Color(0xFF1D2024),
    cobaltSoft = Color(0xFFEBEEF2),
    peach = Color(0xFF3F3F46),
    peachSoft = Color(0xFFEAECEF),
    mint = Color(0xFF15803D),
    mintSoft = Color(0xFFE6F9ED),
    mango = Color(0xFFB45309),
    mangoSoft = Color(0xFFFEF3C7),
    berry = Color(0xFF3F3F46),
    berrySoft = Color(0xFFEAECEF),
    strawberry = Color(0xFFE11D48),
    strawberrySoft = Color(0xFFFFEEF1)
)

val DarkFruitPalette = DinusFruitPalette(
    isDark = true,
    background = Color(0xFF0E0F11),
    surface = Color(0xFF17191D),
    surfaceVariant = Color(0xFF22252B),
    border = Color(0xFF262A32),
    ink = Color(0xFFF4F5F7),
    inkMuted = Color(0xFF8C929D),
    lime = Color(0xFFB8F50A),
    onLime = Color(0xFF121417),
    limeSoft = Color(0xFF222C0F),
    cardActive = Color(0xFF22262E),
    onCardActive = Color(0xFFF6F7F9),
    onCardActiveMuted = Color(0xFF989FA9),
    dashedDivider = Color(0xFF272B33),
    dashedDividerActive = Color(0xFF383E49),
    sky = Color(0xFFA3E635),
    skySoft = Color(0xFF222C0F),
    cobalt = Color(0xFFF4F5F7),
    cobaltSoft = Color(0xFF22252B),
    peach = Color(0xFFE4E4E7),
    peachSoft = Color(0xFF22252B),
    mint = Color(0xFF4ADE80),
    mintSoft = Color(0xFF14291C),
    mango = Color(0xFFFBBF24),
    mangoSoft = Color(0xFF2B2110),
    berry = Color(0xFFD4D4D8),
    berrySoft = Color(0xFF22252B),
    strawberry = Color(0xFFFB7185),
    strawberrySoft = Color(0xFF2D151B)
)

val LocalFruitPalette = staticCompositionLocalOf { LightFruitPalette }

private val LightColorScheme = lightColorScheme(
    primary = LightFruitPalette.lime,
    onPrimary = LightFruitPalette.onLime,
    primaryContainer = LightFruitPalette.limeSoft,
    onPrimaryContainer = LightFruitPalette.ink,
    secondary = LightFruitPalette.cardActive,
    onSecondary = LightFruitPalette.onCardActive,
    secondaryContainer = LightFruitPalette.surface,
    onSecondaryContainer = LightFruitPalette.ink,
    tertiary = LightFruitPalette.mint,
    onTertiary = Color.White,
    tertiaryContainer = LightFruitPalette.mintSoft,
    onTertiaryContainer = LightFruitPalette.ink,
    background = LightFruitPalette.background,
    onBackground = LightFruitPalette.ink,
    surface = LightFruitPalette.surface,
    onSurface = LightFruitPalette.ink,
    surfaceVariant = LightFruitPalette.surfaceVariant,
    onSurfaceVariant = LightFruitPalette.inkMuted,
    outline = LightFruitPalette.border,
    error = LightFruitPalette.strawberry,
    onError = Color.White,
    errorContainer = LightFruitPalette.strawberrySoft,
    onErrorContainer = LightFruitPalette.ink
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkFruitPalette.lime,
    onPrimary = DarkFruitPalette.onLime,
    primaryContainer = DarkFruitPalette.limeSoft,
    onPrimaryContainer = DarkFruitPalette.ink,
    secondary = DarkFruitPalette.cardActive,
    onSecondary = DarkFruitPalette.onCardActive,
    secondaryContainer = DarkFruitPalette.surface,
    onSecondaryContainer = DarkFruitPalette.ink,
    tertiary = DarkFruitPalette.mint,
    onTertiary = Color(0xFF06261B),
    tertiaryContainer = DarkFruitPalette.mintSoft,
    onTertiaryContainer = DarkFruitPalette.ink,
    background = DarkFruitPalette.background,
    onBackground = DarkFruitPalette.ink,
    surface = DarkFruitPalette.surface,
    onSurface = DarkFruitPalette.ink,
    surfaceVariant = DarkFruitPalette.surfaceVariant,
    onSurfaceVariant = DarkFruitPalette.inkMuted,
    outline = DarkFruitPalette.border,
    error = DarkFruitPalette.strawberry,
    onError = Color(0xFF2C0810),
    errorContainer = DarkFruitPalette.strawberrySoft,
    onErrorContainer = DarkFruitPalette.ink
)

private val DinusShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val DinusTypography = Typography(
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.15).sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 14.sp
    )
)

object DinusTheme {
    val palette: DinusFruitPalette
        @Composable get() = LocalFruitPalette.current
}

@Composable
fun DinusRealmTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    language: AppLanguage = AppLanguage.ENGLISH,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = if (useDark) DarkFruitPalette else LightFruitPalette
    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = !useDark
            controller.isAppearanceLightNavigationBars = !useDark
        }
    }

    CompositionLocalProvider(
        LocalFruitPalette provides palette,
        LocalAppLanguage provides language
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DinusTypography,
            shapes = DinusShapes,
            content = content
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
