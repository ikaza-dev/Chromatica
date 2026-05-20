package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun DynamicAlbumArt(
    title: String,
    artist: String,
    modifier: Modifier = Modifier
) {
    // Generate deterministic colors based on title & artist hash
    val colorCombo = remember(title, artist) {
        val hash = abs((title + artist).hashCode())
        val schemes = listOf(
            listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFFEC4899)), // Violet-Fuchsia-Pink
            listOf(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF6366F1)), // Cyan-Blue-Indigo
            listOf(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF3B82F6)), // Emerald-Cyan-Blue
            listOf(Color(0xFFF43F5E), Color(0xFFD946EF), Color(0xFF8B5CF6)), // Rose-Fuchsia-Purple
            listOf(Color(0xFFF97316), Color(0xFFEF4444), Color(0xFFEC4899)), // Orange-Red-Pink
            listOf(Color(0xFF84CC16), Color(0xFF10B981), Color(0xFF06B6D4)), // Lime-Emerald-Cyan
            listOf(Color(0xFF14B8A6), Color(0xFFF59E0B), Color(0xFFEF4444))  // Teal-Amber-Red
        )
        schemes[hash % schemes.size]
    }

    val initial = remember(title) {
        if (title.isNotEmpty()) {
            title.take(1).uppercase()
        } else {
            "♫"
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colorCombo[0].copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Background multi-layer soft radial gradients
            val brush = Brush.radialGradient(
                colors = listOf(colorCombo[1].copy(alpha = 0.85f), colorCombo[2].copy(alpha = 0.2f)),
                center = Offset(width * 0.35f, height * 0.3f),
                radius = width * 1.1f
            )
            drawRect(brush = brush)

            // Dynamic geometrical shapes overlay for premium depth
            val hashModifier = abs((title).hashCode())
            val drawAltCircle = hashModifier % 2 == 0
            
            if (drawAltCircle) {
                drawCircle(
                    color = colorCombo[2].copy(alpha = 0.35f),
                    radius = width * 0.38f,
                    center = Offset(width * 0.75f, height * 0.75f)
                )
                drawCircle(
                    color = colorCombo[0].copy(alpha = 0.25f),
                    radius = width * 0.18f,
                    center = Offset(width * 0.25f, height * 0.8f)
                )
            } else {
                drawCircle(
                    color = colorCombo[0].copy(alpha = 0.40f),
                    radius = width * 0.45f,
                    center = Offset(width * 0.8f, height * 0.2f)
                )
                drawCircle(
                    color = colorCombo[2].copy(alpha = 0.25f),
                    radius = width * 0.22f,
                    center = Offset(width * 0.15f, height * 0.65f)
                )
            }
        }

        // Central visual overlay - neat icon or track initial with a custom glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .drawBehind {
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.16f),
                        radius = size.minDimension * 0.26f,
                        center = center
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLarge.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.4f),
                        offset = Offset(0f, 4f),
                        blurRadius = 8f
                    )
                )
            )
        }
    }
}
