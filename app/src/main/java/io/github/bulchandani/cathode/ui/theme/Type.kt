package io.github.bulchandani.cathode.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography
import io.github.bulchandani.cathode.R

val VT323 = FontFamily(Font(R.font.vt323_regular))

val IbmPlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_bold, FontWeight.Bold),
)

object CathodeText {
    val Display = TextStyle(
        fontFamily = VT323,
        fontWeight = FontWeight.Normal,
        fontSize = 56.sp,
        letterSpacing = 4.sp,
    )
    val Headline = TextStyle(
        fontFamily = VT323,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        letterSpacing = 2.sp,
    )
    val Section = TextStyle(
        fontFamily = IbmPlexMono,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 1.sp,
    )
    val Body = TextStyle(
        fontFamily = IbmPlexMono,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    )
    val Data = TextStyle(
        fontFamily = IbmPlexMono,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp,
    )
    val Caption = TextStyle(
        fontFamily = IbmPlexMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
    )
}

val CathodeTypography = Typography(
    displayLarge = CathodeText.Display,
    displayMedium = CathodeText.Display,
    headlineLarge = CathodeText.Headline,
    headlineMedium = CathodeText.Headline,
    titleLarge = CathodeText.Section,
    titleMedium = CathodeText.Section,
    bodyLarge = CathodeText.Body,
    bodyMedium = CathodeText.Body,
    labelMedium = CathodeText.Data,
    labelSmall = CathodeText.Caption,
)
