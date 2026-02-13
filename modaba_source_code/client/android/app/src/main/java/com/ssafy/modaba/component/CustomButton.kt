package com.ssafy.modaba.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CustomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isOutlined: Boolean = false,
    loading: Boolean = false
) {
    val buttonModifier = modifier.fillMaxWidth().height(48.dp)
    if (isOutlined) {
        OutlinedButton(onClick = onClick, modifier = buttonModifier, enabled = enabled) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(text)
        }
    } else {
        Button(onClick = onClick, modifier = buttonModifier, enabled = enabled) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(text)
        }
    }
}
