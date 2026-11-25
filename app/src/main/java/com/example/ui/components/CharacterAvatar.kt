package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

fun parseAccentColor(hex: String, fallback: Color = Color(0xFFE5B869)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

@Composable
fun CharacterAvatar(
    avatarKey: String,
    accentColorHex: String,
    characterName: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val accent = parseAccentColor(accentColorHex, MaterialTheme.colorScheme.primary)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(width = 2.dp, color = accent, shape = CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.28f),
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        when (avatarKey) {
            "knight" -> Image(
                painter = painterResource(id = R.drawable.img_avatar_knight),
                contentDescription = "$characterName portrait",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            "mage" -> Image(
                painter = painterResource(id = R.drawable.img_avatar_mage),
                contentDescription = "$characterName portrait",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            "crest_ember" -> Icon(
                imageVector = Icons.Rounded.LocalFireDepartment,
                contentDescription = "$characterName ember crest",
                tint = accent,
                modifier = Modifier.size(size * 0.54f)
            )
            "crest_moon" -> Icon(
                imageVector = Icons.Rounded.Bedtime,
                contentDescription = "$characterName moon crest",
                tint = accent,
                modifier = Modifier.size(size * 0.54f)
            )
            "crest_forest" -> Icon(
                imageVector = Icons.Rounded.Park,
                contentDescription = "$characterName verdant crest",
                tint = accent,
                modifier = Modifier.size(size * 0.54f)
            )
            "crest_crimson" -> Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = "$characterName crimson crest",
                tint = accent,
                modifier = Modifier.size(size * 0.54f)
            )
            else -> Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "$characterName crest",
                tint = accent,
                modifier = Modifier.size(size * 0.54f)
            )
        }
    }
}
