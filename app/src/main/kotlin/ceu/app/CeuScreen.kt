package ceu.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CeuScreen(viewModel: CeuViewModel = viewModel()) {
    val responseDescription = stringResource(R.string.response_region)
    val auditDescription = stringResource(R.string.audit_label)
    val response = viewModel.transcript.ifBlank { stringResource(R.string.response_empty) }
    val pending = viewModel.pending
    val pendingText = when {
        pending == null -> stringResource(R.string.pending_none)
        viewModel.remainingSeconds <= 0L -> stringResource(
            R.string.pending_expired,
            pending.actionName,
            pending.tokenPrefix,
        )
        else -> stringResource(
            R.string.pending_active,
            pending.actionName,
            viewModel.remainingSeconds.toInt(),
            pending.tokenPrefix,
        )
    }
    val auditText = if (viewModel.auditLines.isEmpty()) {
        stringResource(R.string.audit_empty)
    } else {
        viewModel.auditLines.joinToString(separator = "\n")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.title_ceu),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.subtitle_phase),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.banner_limits),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.state_label) + ": " + viewModel.stateName,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = pendingText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = responseDescription
                },
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = response,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = auditDescription,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = auditText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = auditDescription },
            )
        }
        OutlinedTextField(
            value = viewModel.draft,
            onValueChange = viewModel::onDraftChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.input_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { viewModel.send() }),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = viewModel::send,
                enabled = !viewModel.busy,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_send))
            }
            Button(
                onClick = viewModel::confirm,
                enabled = !viewModel.busy,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_confirm))
            }
            OutlinedButton(
                onClick = viewModel::cancel,
                enabled = !viewModel.busy,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    }
}
