package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FilterPreset
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant

@Composable
fun FilterPreviewCard(
    filter: FilterPreset,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientColors = when (filter) {
        FilterPreset.ORIGINAL -> listOf(Color(0xFF888888), Color(0xFFCCCCCC))
        FilterPreset.VINTAGE -> listOf(Color(0xFFFFB74D), Color(0xFF8D6E63))
        FilterPreset.BLACK_WHITE -> listOf(Color(0xFF222222), Color(0xFFFFFFFF))
        FilterPreset.CINEMATIC -> listOf(Color(0xFF00E5FF), Color(0xFFFF9100))
        FilterPreset.PORTRAIT -> listOf(Color(0xFFFFAB91), Color(0xFFFF80AB))
        FilterPreset.VIVID -> listOf(Color(0xFFFF1744), Color(0xFFFFD600))
        FilterPreset.CYBERPUNK -> listOf(Color(0xFF7C4DFF), Color(0xFF00E5FF))
        FilterPreset.GOLDEN_HOUR -> listOf(Color(0xFFFF6D00), Color(0xFFFFD700))
        FilterPreset.NORDIC_CHILL -> listOf(Color(0xFF00B0FF), Color(0xFFE0F7FA))
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) CyanNeon.copy(alpha = 0.18f) else DarkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) CyanNeon else Color.White.copy(alpha = 0.1f)
        ),
        modifier = modifier
            .width(82.dp)
            .clickable { onSelect() }
            .testTag("filter_item_${filter.name}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.sweepGradient(gradientColors))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = filter.displayName,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) CyanNeon else Color.White.copy(alpha = 0.9f),
                maxLines = 1
            )
        }
    }
}
