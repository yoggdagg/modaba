package com.ssafy.modaba.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun ModabaLogo(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val taglineStyle = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = MaterialTheme.typography.headlineLarge.fontFamily,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp
        )
        Text(
            text = "모여서 다함께 바깥에,",
            style = taglineStyle,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = "모다바",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
