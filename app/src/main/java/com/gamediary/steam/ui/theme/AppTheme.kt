package com.gamediary.steam.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AppThemeMode(val title: String, val subtitle: String) {
    LIGHT("☀️ Светлая", "Чистый минимализм"),
    DARK("🌙 Тёмная (OLED/Steam Deck)", "Глубокий графит и голубой неон"),
    RETRO_NES("🕹️ Ретро NES (8-Bit)", "Серо-красная палитра Famicom/NES"),
    RETRO_SNES("🟣 Ретро SNES (16-Bit)", "Лавандово-фиолетовый Super Nintendo"),
    SEGA_MEGA_DRIVE("⚡ Sega Mega Drive (16-Bit)", "Черный корпус, золото 16-BIT и синий Sega"),
    PS1_CLASSIC("💿 PlayStation 1 (1994 Grey)", "Теплый серый корпус PSX и бирюзовый акцент"),
    PS5_DUALSENSE("🚀 PlayStation 5 (DualSense)", "Космический темно-синий и неон PS5"),
    XBOX_NEXUS("🟢 Xbox (Nexus Green)", "Карбоновый черный и фирменный зеленый Xbox"),
    SWITCH_NEON("🕹️ Nintendo Switch (Neon Dark)", "Темный графит и неоновые акценты Joy-Con"),
    STEAM_2004_VGUI("🪖 Steam 2004 (VGUI Olive)", "Оливково-золотой интерфейс эпохи Half-Life 2")
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    secondary = Color(0xFF059669),
    background = Color(0xFFF6F7FB),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF151922),
    onSurface = Color(0xFF151922),
    surfaceVariant = Color(0xFFEEF1F8),
    onSurfaceVariant = Color(0xFF5A6275)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF0F172A),
    secondary = Color(0xFF34D399),
    background = Color(0xFF0F141C),
    surface = Color(0xFF1B2432),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF243044),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val NesRetroColors = darkColorScheme(
    primary = Color(0xFFE60012),
    onPrimary = Color.White,
    secondary = Color(0xFF00A800),
    background = Color(0xFF181818),
    surface = Color(0xFF282828),
    onBackground = Color(0xFFF0F0F0),
    onSurface = Color(0xFFF0F0F0),
    surfaceVariant = Color(0xFF383838),
    onSurfaceVariant = Color(0xFFD8D8D8)
)

private val SnesRetroColors = darkColorScheme(
    primary = Color(0xFF7B5CE6),
    onPrimary = Color.White,
    secondary = Color(0xFF00B894),
    background = Color(0xFF221E30),
    surface = Color(0xFF302A44),
    onBackground = Color(0xFFECEAF6),
    onSurface = Color(0xFFECEAF6),
    surfaceVariant = Color(0xFF42395E),
    onSurfaceVariant = Color(0xFFC5BFE0)
)

// Sega Mega Drive / Genesis: Glossy Black, Sega Blue, 16-BIT Gold & Magenta accents
private val SegaMegaDriveColors = darkColorScheme(
    primary = Color(0xFF0066CC),
    onPrimary = Color.White,
    secondary = Color(0xFFD4AF37),
    background = Color(0xFF0C0D14),
    surface = Color(0xFF181B28),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF23283B),
    onSurfaceVariant = Color(0xFFD4AF37)
)

// PlayStation 1 (1994 Grey): Warm PSX Console Grey, Classic Logo Teal & Red
private val Ps1ClassicColors = lightColorScheme(
    primary = Color(0xFF00A499),
    onPrimary = Color.White,
    secondary = Color(0xFFDF0024),
    background = Color(0xFFB8B8C0),
    surface = Color(0xFFDCDCE2),
    onBackground = Color(0xFF1A1A24),
    onSurface = Color(0xFF1A1A24),
    surfaceVariant = Color(0xFFC8C8D0),
    onSurfaceVariant = Color(0xFF454555)
)

// PlayStation 5 (DualSense Cosmic Blue)
private val Ps5DualSenseColors = darkColorScheme(
    primary = Color(0xFF0070D1),
    onPrimary = Color.White,
    secondary = Color(0xFF38BDF8),
    background = Color(0xFF060B19),
    surface = Color(0xFF101A33),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF19274A),
    onSurfaceVariant = Color(0xFF93C5FD)
)

// Xbox Series / Original Nexus Green
private val XboxNexusColors = darkColorScheme(
    primary = Color(0xFF107C10),
    onPrimary = Color.White,
    secondary = Color(0xFF22C55E),
    background = Color(0xFF0A110B),
    surface = Color(0xFF131F15),
    onBackground = Color(0xFFECFDF5),
    onSurface = Color(0xFFECFDF5),
    surfaceVariant = Color(0xFF1C2E20),
    onSurfaceVariant = Color(0xFF86EFAC)
)

// Nintendo Switch (Neon Joy-Con Dark UI: Dark Slate, Neon Red & Neon Cyan)
private val SwitchNeonColors = darkColorScheme(
    primary = Color(0xFFFF4554),
    onPrimary = Color.White,
    secondary = Color(0xFF00C3E3),
    background = Color(0xFF181A20),
    surface = Color(0xFF242832),
    onBackground = Color(0xFFF5F7FA),
    onSurface = Color(0xFFF5F7FA),
    surfaceVariant = Color(0xFF2F3542),
    onSurfaceVariant = Color(0xFFA0AEC0)
)

// Steam 2004 Classic VGUI (Olive Drab & Half-Life 2 Gold)
private val Steam2004VguiColors = darkColorScheme(
    primary = Color(0xFFC4B550),
    onPrimary = Color(0xFF1F241B),
    secondary = Color(0xFF7E9653),
    background = Color(0xFF3E4637),
    surface = Color(0xFF4C5844),
    onBackground = Color(0xFFDEDFD6),
    onSurface = Color(0xFFDEDFD6),
    surfaceVariant = Color(0xFF333B2D),
    onSurfaceVariant = Color(0xFFA6B09B)
)

@Composable
fun SteamGameDiaryTheme(
    themeMode: AppThemeMode,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.LIGHT -> LightColors
        AppThemeMode.DARK -> DarkColors
        AppThemeMode.RETRO_NES -> NesRetroColors
        AppThemeMode.RETRO_SNES -> SnesRetroColors
        AppThemeMode.SEGA_MEGA_DRIVE -> SegaMegaDriveColors
        AppThemeMode.PS1_CLASSIC -> Ps1ClassicColors
        AppThemeMode.PS5_DUALSENSE -> Ps5DualSenseColors
        AppThemeMode.XBOX_NEXUS -> XboxNexusColors
        AppThemeMode.SWITCH_NEON -> SwitchNeonColors
        AppThemeMode.STEAM_2004_VGUI -> Steam2004VguiColors
    }

    val isRetroMonospace = themeMode in setOf(
        AppThemeMode.RETRO_NES,
        AppThemeMode.RETRO_SNES,
        AppThemeMode.SEGA_MEGA_DRIVE,
        AppThemeMode.PS1_CLASSIC,
                AppThemeMode.STEAM_2004_VGUI
    )
    val fontFamily = if (isRetroMonospace) FontFamily.Monospace else FontFamily.Default

    val typography = Typography(
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    )

    val shapes = when (themeMode) {
        AppThemeMode.RETRO_NES -> Shapes(
            small = CutCornerShape(0.dp),
            medium = CutCornerShape(0.dp),
            large = CutCornerShape(2.dp)
        )
        AppThemeMode.STEAM_2004_VGUI, AppThemeMode.PS1_CLASSIC -> Shapes(
            small = RoundedCornerShape(2.dp),
            medium = RoundedCornerShape(3.dp),
            large = RoundedCornerShape(4.dp)
        )
        AppThemeMode.RETRO_SNES, AppThemeMode.SEGA_MEGA_DRIVE -> Shapes(
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(6.dp),
            large = RoundedCornerShape(8.dp)
        )
        AppThemeMode.PS5_DUALSENSE -> Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(24.dp)
        )
        else -> Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(18.dp)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
        content = content
    )
}
