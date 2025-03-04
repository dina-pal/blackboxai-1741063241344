package com.dinapal.busdakho.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    // Small shapes for buttons, text fields, etc.
    small = RoundedCornerShape(4.dp),
    
    // Medium shapes for cards, dialogs, etc.
    medium = RoundedCornerShape(8.dp),
    
    // Large shapes for bottom sheets, expanded FABs, etc.
    large = RoundedCornerShape(12.dp),

    // Extra large shapes for full-screen dialogs, etc.
    extraLarge = RoundedCornerShape(16.dp)
)

// Custom shapes for specific components
val BottomSheetShape = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)

val MapMarkerShape = RoundedCornerShape(50)

val CardShape = RoundedCornerShape(12.dp)

val ButtonShape = RoundedCornerShape(8.dp)

val SearchBarShape = RoundedCornerShape(24.dp)

val ChipShape = RoundedCornerShape(16.dp)

val FloatingActionButtonShape = RoundedCornerShape(16.dp)

val TopAppBarShape = RoundedCornerShape(
    bottomStart = 16.dp,
    bottomEnd = 16.dp
)

val DialogShape = RoundedCornerShape(16.dp)

val BottomNavigationShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp
)
