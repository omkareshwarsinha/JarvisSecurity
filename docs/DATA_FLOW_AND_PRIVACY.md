# MobiArmour Platform — Mobile Data Flow, State Management & Privacy Specs

**Document Version:** 3.0.0-DataFlowSpec  
**Classification:** Internal System Design & Data Protection Reference  

---

## 1. Unidirectional Data Flow (UDF) Architecture

MobiArmour employs Unidirectional Data Flow (UDF) powered by Jetpack Compose and Kotlin `StateFlow`. UI composables observe immutable state snapshots and dispatch actions via viewmodel intent handlers:

```
+-------------------------------------------------------------------------+
|                                User Action                              |
|   (Tap 'Start Scan', 'Toggle Firewall Rule', 'Whitelist App', etc.)     |
+-------------------------------------------------------------------------+
                                    |
                                    v
+-------------------------------------------------------------------------+
|                          ViewModel Intent Handler                       |
|   (e.g., ScanViewModel, FirewallViewModel, HardeningViewModel)           |
+-------------------------------------------------------------------------+
                                    |
          +-------------------------+-------------------------+
          |                                                   |
          v (Coroutines / Dispatchers.IO)                     v (Coroutines / Dispatchers.Default)
+-----------------------------------+   +------------------------------------+
|         Room SQLite Access        |   |       Domain Analysis Engine       |
| (Insert Event, Update Rule, etc.) |   | (ZipFile Parse, Perms Correlation) |
+-----------------------------------+   +------------------------------------+
          |                                                   |
          +-------------------------+-------------------------+
                                    |
                                    v
+-------------------------------------------------------------------------+
|                    StateFlow<UiState> Emission                          |
|             (Produces immutable UI state snapshot)                     |
+-------------------------------------------------------------------------+
                                    |
                                    v
+-------------------------------------------------------------------------+
|                     Jetpack Compose Composable Tree                     |
|           (Recomposes selectively based on updated state)               |
+-------------------------------------------------------------------------+
```

---

## 2. Component Data Flow Pipelines

### 2.1 Deep Scan Data Flow
1. **Trigger:** User taps "Run Comprehensive Security Audit".
2. **Execution:** `ScanViewModel` launches coroutine on `Dispatchers.IO`.
3. **Hardware Assessment:** `DeviceAuditor` queries `KeyguardManager`, `DevicePolicyManager`, `Settings.Global`, and hardware flags.
4. **App Forensics:** `PackageAuditor` iterates installed packages. Suspicious or third-party packages are streamed via `DeepCodeAuditor` through `java.util.zip.ZipFile`.
5. **Score Evaluation:** `ScoringEngine` processes device findings and application risks, computing the composite score and deductions.
6. **State Storage:** Findings and summary are stored in `ScanResultEntity` and `SecurityEventEntity` within `JarvisDatabase`.
7. **UI Notification:** `scanStateFlow` emits `ScanUiState.Completed(summary)`.

### 2.2 Local Firewall Data Flow
1. **Rule Change:** User blocks an untrusted app.
2. **Database Update:** `FirewallViewModel` inserts/updates `FirewallRuleEntity(packageName, isBlocked = true)` in Room.
3. **Service Rebuild:** `FirewallManager.rebuildVpn()` signals `JarvisFirewallService`.
4. **Interface Reconfiguration:** `JarvisFirewallService` reconstructs `tun0` interface adding the blocked package to `addAllowedApplication()`.
5. **Packet Sink:** All IP packets originating from the blocked package are routed to `tun0` and immediately dropped by the reader loop.
6. **Traffic Logging:** Drop events are inserted into `NetworkTrafficEntity` with an automatic prune step maintaining $\le 500$ records.

### 2.3 Remote Lockdown SMS Data Flow
1. **Inbound Broadcast:** Android OS triggers `Telephony.Sms.Intents.SMS_RECEIVED_ACTION`.
2. **Async Delegation:** `RemoteLockdownReceiver.onReceive()` calls `val pendingResult = goAsync()`.
3. **Authentication:** Sender phone number is normalized and verified against `trusted_devices` in Room.
4. **Parsing:** `SmsCommandParser.parse()` validates prefix and optional PIN argument.
5. **Execution:** If `#lockdown`:
   - `DevicePolicyManager.lockNow()` engages keyguard.
   - `MediaPlayer` plays emergency alarm at `AudioManager.STREAM_ALARM` max volume.
   - `LocationManager` queries best cached GPS/network coordinates.
   - `SmsManager` transmits reply SMS with coordinates.
6. **Lifecycle Release:** `pendingResult.finish()` is invoked in the `finally` block to return thread execution to the OS.

---

## 3. Data Classification, Minimization & Privacy Guarantees

| Data Category | Retention Location | Encryption / Sandbox | Data Minimization Principle |
|---|---|---|---|
| **Package Metadata** | RAM & Room DB | Private app sandbox `/data/user/0/` | Only package identifiers and permission flags stored. Zero user data from inspected apps is ever read. |
| **Audit Logs** | Room DB (`security_events`) | Private app sandbox | Limited to latest 100 system events. |
| **Firewall Traffic** | Room DB (`network_traffic`) | Private app sandbox | Bounded to 500 records. Zero payload content is logged—only destination IP, port, and protocol headers. |
| **Trusted Contacts** | Room DB (`trusted_devices`) | Private app sandbox | User-entered emergency phone numbers only; zero access to full device contact address book. |
| **Location Coordinates** | Ephemeral RAM only | Not persisted to disk | Acquired only during authorized `#location` SMS request, transmitted via SMS, and discarded. |
| **External Cloud Transmission** | None | N/A | **Zero bytes** transmitted to any cloud backend or telemetry server. |
