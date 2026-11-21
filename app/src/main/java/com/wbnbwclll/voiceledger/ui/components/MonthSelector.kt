package com.wbnbwclll.voiceledger.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wbnbwclll.voiceledger.R
import com.wbnbwclll.voiceledger.util.MonthPeriod

/** 月份切换：‹ 2026年11月 › */
@Composable
fun MonthSelector(
    month: MonthPeriod,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val previousLabel = stringResource(R.string.month_previous)
    val nextLabel = stringResource(R.string.month_next)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        TextButton(
            onClick = onPrevious,
            modifier = Modifier.semantics { contentDescription = previousLabel }
        ) {
            Text(text = "‹", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = stringResource(R.string.month_label, month.year, month.month),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        TextButton(
            onClick = onNext,
            modifier = Modifier.semantics { contentDescription = nextLabel }
        ) {
            Text(text = "›", style = MaterialTheme.typography.titleLarge)
        }
    }
}
