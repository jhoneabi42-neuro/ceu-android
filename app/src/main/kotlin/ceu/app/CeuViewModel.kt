package ceu.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceu.core.orchestrate.PendingView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CeuViewModel(
    private val session: CeuSession = CeuRuntime.session(),
) : ViewModel() {
    var draft by mutableStateOf("")
        private set

    var transcript by mutableStateOf("")
        private set

    var stateName by mutableStateOf(session.orchestrator.state.name)
        private set

    var pending by mutableStateOf<PendingView?>(null)
        private set

    var remainingSeconds by mutableStateOf(0L)
        private set

    var auditLines by mutableStateOf<List<String>>(emptyList())
        private set

    var busy by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(250)
                refreshClock()
            }
        }
    }

    fun onDraftChange(value: String) {
        draft = value
    }

    fun send() {
        if (busy) return
        val text = draft
        draft = ""
        session.input.offer(text)
        launchTurn { session.orchestrator.consumeInput() }
    }

    fun confirm() {
        launchTurn { session.orchestrator.confirm() }
    }

    fun cancel() {
        launchTurn { session.orchestrator.cancel() }
    }

    private fun launchTurn(block: suspend () -> Any?) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                block()
            } finally {
                publish()
                busy = false
            }
        }
    }

    private fun refreshClock() {
        val current = session.orchestrator.pendingView()
        pending = current
        remainingSeconds = current?.let { session.orchestrator.remainingSeconds(it) } ?: 0L
        stateName = session.orchestrator.state.name
    }

    private fun publish() {
        transcript = session.output.transcript()
        auditLines = session.audit.snapshot().takeLast(8).map { record ->
            val action = record.actionId ?: "-"
            "${record.sequence} ${record.kind.name} $action ${record.code}"
        }
        refreshClock()
    }
}
