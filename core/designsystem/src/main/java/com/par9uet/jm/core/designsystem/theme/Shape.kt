package com.par9uet.jm.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 圆角阶梯。
 *
 * 比 M3 默认值收窄了一档。大圆角读起来偏柔软、可爱，而这套界面要的是
 * 克制与秩序感——层级交给留白和字重，圆角只负责去掉尖角。
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)
