package com.mindfulanki.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.mindfulanki.R

/** Space Grotesk is shipped as a single variable font; bind weights via the wght axis. */
@OptIn(ExperimentalTextApi::class)
private fun spaceGrotesk(weight: Int) = Font(
    resId = R.font.space_grotesk,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Primary UI typeface (Latin), per the design. */
val SpaceGrotesk = FontFamily(
    spaceGrotesk(400),
    spaceGrotesk(500),
    spaceGrotesk(600),
    spaceGrotesk(700),
)

/** Arabic script face for card content (the design uses Scheherazade New). */
val ScheherazadeNew = FontFamily(
    Font(R.font.scheherazade_new_regular, FontWeight.Normal),
    Font(R.font.scheherazade_new_medium, FontWeight.Medium),
    Font(R.font.scheherazade_new_semibold, FontWeight.SemiBold),
    Font(R.font.scheherazade_new_bold, FontWeight.Bold),
)

private val ArabicScript = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]")

/** Pick Scheherazade New for Arabic text, Space Grotesk otherwise (cards are language-agnostic). */
fun scriptFamily(text: String): FontFamily =
    if (ArabicScript.containsMatchIn(text)) ScheherazadeNew else SpaceGrotesk

/** Material 3 type scale rebound to Space Grotesk across every slot. */
val AppTypography: Typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = SpaceGrotesk),
        displayMedium = displayMedium.copy(fontFamily = SpaceGrotesk),
        displaySmall = displaySmall.copy(fontFamily = SpaceGrotesk),
        headlineLarge = headlineLarge.copy(fontFamily = SpaceGrotesk),
        headlineMedium = headlineMedium.copy(fontFamily = SpaceGrotesk),
        headlineSmall = headlineSmall.copy(fontFamily = SpaceGrotesk),
        titleLarge = titleLarge.copy(fontFamily = SpaceGrotesk),
        titleMedium = titleMedium.copy(fontFamily = SpaceGrotesk),
        titleSmall = titleSmall.copy(fontFamily = SpaceGrotesk),
        bodyLarge = bodyLarge.copy(fontFamily = SpaceGrotesk),
        bodyMedium = bodyMedium.copy(fontFamily = SpaceGrotesk),
        bodySmall = bodySmall.copy(fontFamily = SpaceGrotesk),
        labelLarge = labelLarge.copy(fontFamily = SpaceGrotesk),
        labelMedium = labelMedium.copy(fontFamily = SpaceGrotesk),
        labelSmall = labelSmall.copy(fontFamily = SpaceGrotesk),
    )
}
