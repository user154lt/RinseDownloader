package com.leet.rinsedownloader.ui.components

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DefaultTextButton(
    modifier: Modifier = Modifier,
    label: String = "",
    onClick: () -> Unit = {},
){
    TextButton(
        modifier = modifier,
        onClick = onClick,
    ){
        Text(
            text = label,
        )
    }
}