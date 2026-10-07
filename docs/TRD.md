# MobiArmour Platform — Technical Requirements Document (TRD)

**Document Version:** 3.0.0-TRD  
**Document Status:** Approved & Implemented  
**Classification:** Confidential / System Architecture  
**System Name:** MobiArmour Mobile Endpoint Defense Platform  
**Target Platform:** Android 8.0 (API Level 26) through Android 15 (API Level 35)  

---

## 1. Executive Technical Summary

MobiArmour is an autonomous, on-device mobile endpoint detection, response, and device hardening platform engineered exclusively in Kotlin and Jetpack Compose. The system operates under a **Zero-Cloud-Dependency** operational model: all static code forensics, permission correlation engines, packet inspection sinks, heuristic phishing analyses, and administrative lockdown routines execute entirely within local hardware and sandbox boundaries without external telemetry dispatch.

---

## 2. System Architecture & Component Interactions

```
+-----------------------------------------------------------------------------------+
|                        MobiArmour Presentation Layer (Jetpack Compose)            |
|  [Dashboard]  [Scanner]  [App Auditor]  [Firewall]  [Hardening]  [Lost Device]    |
+-----------------------------------------------------------------------------------+
                                        | (StateFlow / UDF)
                                        v
+-----------------------------------------------------------------------------------+
|                           Domain & Core Engines Layer                             |
|  +------------------------+  +-----------------------+  +----------------------+  |
|  |   PackageAuditor.kt    |  |   DeepCodeAuditor.kt  |  |   DeviceAuditor.kt   |  |
|  +------------------------+  +-----------------------+  +----------------------+  |
|  +------------------------+  +-----------------------+  +----------------------+  |
|  |   PhishingDetector.kt  |  |   ScoringEngine.kt    |  | ContinuousMonitor.kt |  |
|  +------------------------+  +-----------------------+  +----------------------+  |
|  +------------------------+  +-----------------------+  +----------------------+  |
|  |  SmsCommandParser.kt   |  | LostDeviceManager.kt  |  | PerformanceOpt.kt    |  |
|  +------------------------+  +-----------------------+  +----------------------+  |
+-----------------------------------------------------------------------------------+
             |                                             |
             v                                             v
+------------------------------------+  +-------------------------------------------+
|    Local Persistence (Room DB)     |  |       Android Platform & OS Subsystems    |
| - ScanResultDao                    |  | - VpnService (tun0 Local Drop Sink)       |
| - SecurityEventDao                 |  | - DevicePolicyManager (Device Admin API)  |
| - FirewallRuleDao                  |  | - KeyguardManager & Biometrics            |
| - NetworkTrafficDao (Retention=500)|  | - TelephonyManager / SMS Broadcasts       |
| - TrustedDeviceDao / TrustedAppDao |  | - LocationManager (GPS/Network Cache)     |
+------------------------------------+  +-------------------------------------------+
```

---

## 3. Detailed Technical Requirements

### TR-1: Device Posture & Configuration Auditing
- **TR-1.1:** The system shall interrogate `KeyguardManager.isDeviceSecure()` to determine if hardware screen locking (PIN/Pattern/Password/Biometrics) is activated.
- **TR-1.2:** The system shall audit `Settings.Global.DEVELOPMENT_SETTINGS_ENABLED` and `Settings.Global.ADB_ENABLED` to verify developer options and USB debugging status.
- **TR-1.3:** The system shall detect root artifacts and test keys via checking `Build.TAGS.contains("test-keys")` and probing known su binary locations (`/system/bin/su`, `/system/xbin/su`, `/sbin/su`, `/system/app/Superuser.apk`).
- **TR-1.4:** The system shall inspect device encryption posture via `DevicePolicyManager.getStorageEncryptionStatus()`.
- **TR-1.5:** The system shall evaluate SELinux enforcing state and Android security patch latency against the current calendar baseline.

### TR-2: Application Static Analysis & Forensic Engine
- **TR-2.1:** The system shall enumerate installed packages via `PackageManager.getInstalledPackages()` using `PackageManager.GET_PERMISSIONS`, `GET_SIGNING_CERTIFICATES`, and `GET_ACTIVITIES`.
- **TR-2.2:** `DeepCodeAuditor` shall stream APK files directly using `java.util.zip.ZipFile` on `appInfo.sourceDir` to identify:
  - Multi-DEX footprints (`classes.dex`, `classes2.dex`, etc.).
  - Embedded native binaries (`lib/arm64-v8a`, `lib/armeabi-v7a`, `lib/x86_64`).
  - Embedded debug certificates (`CN=Android Debug`).
  - Adware SDK signatures and stalkerware telemetry endpoints.
- **TR-2.3:** The system shall flag applications operating with `android:debuggable=true` or missing `Intent.CATEGORY_LAUNCHER` (hidden/stealth apps).

### TR-3: Local VPN Firewall & Network Containment
- **TR-3.1:** The firewall shall be implemented as a local `VpnService` (`JarvisFirewallService`) creating a virtual interface `tun0` (`10.0.0.2/32`).
- **TR-3.2:** In **Selective Firewall Mode**, the system shall route only explicitly blocked packages into the drop sink via `VpnService.Builder.addAllowedApplication(blockedPackage)`. Host app traffic and unblocked apps must never enter the drop sink.
- **TR-3.3:** In **Device Air-Gap Mode**, the system shall bind all IPv4 (`0.0.0.0/0`) and IPv6 (`::/0`) routing to block egress across all apps except whitelisted critical services.
- **TR-3.4:** The service shall log drop events into `NetworkTrafficEntity` with destination IP, port, protocol, and classification heuristic, capped at a sliding window of 500 records via `pruneOldTraffic()`.

### TR-4: Out-of-Band Remote Lockdown (SMS C2 Channel)
- **TR-4.1:** Emergency commands received via SMS shall be intercepted by `RemoteLockdownReceiver` using `Telephony.Sms.Intents.SMS_RECEIVED_ACTION`.
- **TR-4.2:** The receiver must invoke `val pendingResult = goAsync()` to prevent process termination during asynchronous database validation, location queries, and SMS dispatch.
- **TR-4.3:** The sender phone number must be normalized and verified against allowlisted entries in `trusted_devices`.
- **TR-4.4:** `SmsCommandParser` shall enforce prefix conventions (`/`, `#`, `!`) and optional PIN arguments (`#lockdown 1234`), strictly rejecting casual or conversational sentences containing keywords.
- **TR-4.5:** Permitted actions:
  - `#lockdown`: Trigger `DevicePolicyManager.lockNow()`, trigger locator siren, query cached location, reply via SMS.
  - `#siren`: Play emergency acoustic beacon on `AudioManager.STREAM_ALARM` at maximum volume.
  - `#location`: Query best cached location from `LocationManager` and dispatch SMS with Google Maps coordinates.
  - `#silence`: Terminate siren audio stream.

### TR-5: Local Database & Data Durability
- **TR-5.1:** All tables shall be managed via `JarvisDatabase` (Room SQLite version 3).
- **TR-5.2:** Destructive schema migration is strictly prohibited; updates must use explicit migrations (`MIGRATION_1_2`, `MIGRATION_2_3`) with `.fallbackToDestructiveMigrationOnDowngrade()`.
- **TR-5.3:** A one-tap zero-trace database purge must be available in Settings to delete all audit records, firewall rules, and security events.

---

## 4. Non-Functional Requirements (NFR)

| ID | Category | Requirement Specification |
|---|---|---|
| **NFR-1** | **Performance** | Full device audit (300+ installed packages) must complete in under 2.5 seconds on mid-tier ARM64 hardware without blocking the main UI thread. |
| **NFR-2** | **Memory** | Background monitoring and VPN idle memory footprint must remain under 35 MB RAM. |
| **NFR-3** | **Battery** | Background continuous monitor must consume < 0.5% battery per 24-hour cycle. |
| **NFR-4** | **Privacy** | Zero internet socket connections initiated by MobiArmour itself; 100% on-device operations. |
| **NFR-5** | **Reliability** | Zero ANR (Application Not Responding) events across all scan and broadcast execution paths. |
