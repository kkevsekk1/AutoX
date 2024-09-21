package com.aiselp.autox.ui.material3.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BuildCard(
    title: String? = null,
    content: @Composable (ColumnScope.() -> Unit)
) {
    ElevatedCard(Modifier.padding(start = 16.dp, end = 16.dp)) {
        if (title != null) {
            Text(
                text = title,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 16.dp, start = 16.dp),
                textAlign = TextAlign.Center,
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) { content() }
    }
}
