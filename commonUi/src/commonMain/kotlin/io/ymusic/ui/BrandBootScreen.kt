package io.ymusic.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.ymusic.ui.resources.Res
import io.ymusic.ui.resources.about_tagline
import io.ymusic.ui.resources.app_name
import io.ymusic.ui.resources.startmusic_logo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Branded cold-start / boot chrome shared by Android and desktop shells so the splash → first
 * Compose frame stays on the StartMusicV3 identity (logo, name, tagline, [Brand] type).
 */
@Composable
fun BrandBootScreen(
    background: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Box(modifier.fillMaxSize().background(background), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Image(
                painterResource(Res.drawable.startmusic_logo),
                contentDescription = stringResource(Res.string.app_name),
                modifier = Modifier.size(88.dp).clip(RoundedCornerShape(Brand.SplashCorner)),
            )
            Text(
                stringResource(Res.string.app_name),
                style = Brand.Title.copy(fontWeight = FontWeight.Bold, color = DesignColors.InkStrong),
            )
            Text(
                stringResource(Res.string.about_tagline),
                style = Brand.Caption.copy(color = DesignColors.InkSecondary),
            )
            content()
        }
    }
}
