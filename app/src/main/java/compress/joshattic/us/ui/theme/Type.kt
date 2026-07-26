@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package compress.joshattic.us.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import compress.joshattic.us.R

val GoogleSansFlex = FontFamily(
    Font(resId = R.font.google_sans_flex, weight = FontWeight.Thin,
         variationSettings = FontVariation.Settings(FontVariation.weight(100))),
    Font(resId = R.font.google_sans_flex, weight = FontWeight.ExtraLight,
         variationSettings = FontVariation.Settings(FontVariation.weight(200))),
    Font(resId = R.font.google_sans_flex, weight = FontWeight.Light,
         variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    Font(resId = R.font.google_sans_flex, weight = FontWeight.Normal,
         variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(resId = R.font.google_sans_flex, weight = FontWeight.Medium,
         variationSettings = FontVariation.Settings(FontVariation.weight(480))),
    Font(resId = R.font.google_sans_flex, weight = FontWeight.SemiBold,
         variationSettings = FontVariation.Settings(FontVariation.weight(540))),
    Font(resId = R.font.google_sans_flex, weight = FontWeight.Bold,
         variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(resId = R.font.google_sans_flex, weight = FontWeight.ExtraBold,
         variationSettings = FontVariation.Settings(FontVariation.weight(650))),
    Font(resId = R.font.google_sans_flex, weight = FontWeight.Black,
         variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

val Typography = Typography(
    displayLarge   = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Bold,    fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium  = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Bold,    fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall   = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Bold,    fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge  = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Bold,    fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Bold,    fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Bold,    fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge     = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Bold,    fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium    = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Bold,    fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall     = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Bold,    fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge      = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Normal,  fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.3.sp),
    bodyMedium     = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Normal,  fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
    bodySmall      = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Normal,  fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
    labelLarge     = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium,  fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium    = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium,  fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelSmall     = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium,  fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
)