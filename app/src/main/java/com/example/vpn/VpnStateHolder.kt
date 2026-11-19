package com.example.vpn

import com.example.data.DnsLeakCheckResult
import com.example.data.VpnConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object VpnStateHolder {

    private val _connectionState = MutableStateFlow<VpnConnectionState>(VpnConnectionState.Disconnected)
    val connectionState: StateFlow<VpnConnectionState> = _connectionState.asStateFlow()

    private val _downloadBytesPerSec = MutableStateFlow(0L)
    val downloadBytesPerSec: StateFlow<Long> = _downloadBytesPerSec.asStateFlow()

    private val _uploadBytesPerSec = MutableStateFlow(0L)
    val uploadBytesPerSec: StateFlow<Long> = _uploadBytesPerSec.asStateFlow()

    private val _totalDownloadBytes = MutableStateFlow(0L)
    val totalDownloadBytes: StateFlow<Long> = _totalDownloadBytes.asStateFlow()

    private val _totalUploadBytes = MutableStateFlow(0L)
    val totalUploadBytes: StateFlow<Long> = _totalUploadBytes.asStateFlow()

    private val _speedHistoryDown = MutableStateFlow(List(24) { 0f })
    val speedHistoryDown: StateFlow<List<Float>> = _speedHistoryDown.asStateFlow()

    private val _speedHistoryUp = MutableStateFlow(List(24) { 0f })
    val speedHistoryUp: StateFlow<List<Float>> = _speedHistoryUp.asStateFlow()

    private val _lastDnsCheck = MutableStateFlow<DnsLeakCheckResult?>(null)
    val lastDnsCheck: StateFlow<DnsLeakCheckResult?> = _lastDnsCheck.asStateFlow()

    fun updateConnectionState(state: VpnConnectionState) {
        _connectionState.value = state
        if (state is VpnConnectionState.Disconnected || state is VpnConnectionState.Error) {
            _downloadBytesPerSec.value = 0L
            _uploadBytesPerSec.value = 0L
        }
    }

    fun resetSessionCounters() {
        _downloadBytesPerSec.value = 0L
        _uploadBytesPerSec.value = 0L
        _totalDownloadBytes.value = 0L
        _totalUploadBytes.value = 0L
        _speedHistoryDown.value = List(24) { 0f }
        _speedHistoryUp.value = List(24) { 0f }
    }

    fun pushTrafficSample(downBps: Long, upBps: Long) {
        _downloadBytesPerSec.value = downBps
        _uploadBytesPerSec.value = upBps
        _totalDownloadBytes.update { it + downBps }
        _totalUploadBytes.update { it + upBps }
        _speedHistoryDown.update { history ->
            (history.drop(1) + downBps.toFloat())
        }
        _speedHistoryUp.update { history ->
            (history.drop(1) + upBps.toFloat())
        }
    }

    fun updateDnsCheck(result: DnsLeakCheckResult) {
        _lastDnsCheck.value = result
    }
}
