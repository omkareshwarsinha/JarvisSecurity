# MobiArmour Platform — System Architecture & Engineering Design

**Status:** Living Engineering Baseline  
**Architectural Model:** 7-Layer Defense-in-Depth + Reactive Unidirectional Data Flow (UDF)  
**Security Boundary:** Unprivileged Android Userspace + Optional Device Administrator Privileges  
**Target Runtimes:** Android 9.0 (API 28) through Android 15+ (API 35)  

---

## 1. Multi-Layer System Architecture

MobiArmour is structured into seven distinct architectural layers designed to isolate data ingestion, forensic processing, network enforcement, and presentation.

```
+-----------------------------------------------------------------------------------+
| LAYER 7: PRESENTATION LAYER (Jetpack Compose UI & Glassmorphic Design System)      |
|  - DashboardScreen   - AppsScreen (Threat Action Center)   - FirewallScreen       |
|  - HardeningScreen   - EventsScreen (Timeline)             - LogsScreen           |
|  - PhishingScreen    - LostDeviceScreen                    - SettingsScreen       |
+-----------------------------------------+-----------------------------------------+
                                          | StateFlow / SharedFlow (UDF)
                                          v
+-----------------------------------------------------------------------------------+
| LAYER 6: INCIDENT RESPONSE & REMEDIATION LAYER                                    |
|  - Universal Uninstaller Bridge (Intent.ACTION_DELETE + Settings Fallback)        |
|  - Out-of-Band Remote Lockdown Controller (SMS C2 + DevicePolicyManager)          |
|  - Firewall Quarantine Dispatcher                                                 |
|  - Phishing Gate Interception Activity                                            |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
| LAYER 5: SECURITY INTELLIGENCE & CORRELATION LAYER                                |
|  - Multi-Vector Risk Engine (Permission × Persistence × Network × Signature)      |
|  - Explainable Risk Generator (Human-Readable Contextual Diagnostic Messages)     |
|  - Event Timeline Aggregator & Anomaly Correlator                                |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
| LAYER 4: NETWORK ENFORCEMENT LAYER                                                |
|  - JarvisFirewallService (Android VpnService Virtual Interface tun0)              |
|  - Selective Per-App Routing Engine (addAllowedApplication for Blocked Apps)      |
|  - Software-Enforced Network Isolation ("Software Air-Gap" Null-Sink)            |
|  - Low-Overhead IPv4/IPv6 Packet Parser & Audit Logger                            |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
| LAYER 3: APPLICATION ANALYSIS & FORENSICS LAYER                                   |
|  - PackageAuditor (Manifest Flags, Exported Components, Permission Profiles)      |
|  - DeepCodeAuditor (ZipFile APK Container Inspection, Native .so discovery)       |
|  - X.509 Certificate Extractor & Verification (Debug Keystore vs. Vendor Release) |
|  - Stealth & Iconless Application Heuristic Engine                                |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
| LAYER 2: SECURITY COLLECTION & LOCAL DATA PERSISTENCE LAYER                       |
|  - DeviceAuditor (Keyguard, ADB, Storage Encryption, SU Binary Inspection)        |
|  - SecurityRepository (Unified Reactive Domain Gateway)                           |
|  - Room Local Database (SQLite v3 in App-Private Storage: rules, logs, events)    |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
| LAYER 1: ANDROID OPERATING SYSTEM PLATFORM LAYER                                  |
|  - Linux Kernel 4.x / 5.x / 6.x      - SELinux Policy Enforcement                 |
|  - Android Application Sandbox (UID)  - VpnService Network Driver                  |
|  - Package Manager Service (PMS)      - Device Policy Subsystem                    |
+-----------------------------------------------------------------------------------+
```

---

## 2. Component Interaction Workflows

### 2.1 Static Forensic & Threat Detection Pipeline
```
[User Audit or Background Lifecycle Trigger]
                    |
                    v
    [PackageAuditor.auditInstalledApplications()]
                    |
                    v
       [Enumerate ApplicationInfo List via PMS]
                    |
       +------------+------------+
       |                         |
       v                         v
[System vs. User Flag]    [Permission Profile Extraction]
       |                         |
       +------------+------------+
                    |
                    v
    [DeepCodeAuditor.inspectPackageApk(packageInfo)]
                    |
       +------------+------------+-------------------------+
       |                         |                         |
       v                         v                         v
[ZipFile Stream Parser]   [X.509 Certificate Hash]   [Native .so Library Audit]
(Count classes*.dex)      (Flag CN=Android Debug)     (Audit ABI Architectures)
       |                         |                         |
       +------------+------------+-------------------------+
                    |
                    v
    [Evaluate Stealth App Heuristics]
    Condition: Is User App AND Requests Background Privileges
               AND OMITTED Intent.CATEGORY_LAUNCHER Activities?
                    |
          +---------+---------+
          |                   |
          v                   v
     [Flag as THREAT]    [Standard Heuristic Risk]
     - isMalwareThreat   - Permission Risk Score
     - isHiddenApp       - Target SDK Penalty
     - Assign CRITICAL   - Exported Component Risk
          |                   |
          +---------+---------+
                    |
                    v
     [Room Database Persistence: ScannedAppEntity]
                    |
                    v
     [StateFlow Emission to JarvisMainViewModel]
                    |
                    v
     [AppsScreen: Threat Action Center Rendered]
```

---

### 2.2 Direct Threat Quarantine & Uninstallation Flow
```
[Threat Identified in Application Auditor]
                    |
                    v
     [User Taps "UNINSTALL" Action Button]
                    |
                    v
       [AppsScreen.launchUninstallPackage()]
                    |
       +------------+------------+
       | (Primary Path)          | (Exception Catch / Fallback)
       v                         v
[Intent.ACTION_DELETE]    [Settings.ACTION_APPLICATION_DETAILS_SETTINGS]
(Uri: package:$pkg)       (Direct System App Management Settings)
       |
       v
[Android Package Installer System Prompt Rendered to User]
       |
       +------------+------------+
       | (Confirmed)             | (Cancelled)
       v                         v
[OS Deletes Package]       [No State Change]
       |
       v
[OS Dispatches Broadcast: Intent.ACTION_PACKAGE_REMOVED]
       |
       v
[MainActivity BroadcastReceiver Intercepts Event]
       |
       v
[JarvisMainViewModel.handlePackageRemoved(pkg)]
       |
       +-------------------------+-------------------------+
       |                         |                         |
       v                         v                         v
[Purge from Scanned Apps]  [Delete Firewall Rules]   [Recalculate Posture Score]
       |                         |                         |
       +-------------------------+-------------------------+
                                 |
                                 v
                 [UI State Automatically Updated]
```

---

### 2.3 Selective Per-App Firewall & Software-Enforced Isolation
```
[User Enables Firewall or Changes Block Rules]
                    |
                    v
[JarvisFirewallService.onStartCommand()]
                    |
                    v
[Query FirewallRuleEntity WHERE isBlocked == true]
                    |
         +----------+----------+
         | (Selective Mode)    | (Software Air-Gap Mode)
         v                     v
[Builder.addAllowedApplication [Builder.addRoute("0.0.0.0", 0)
 for each blocked package]      Builder.addRoute("::", 0)
                                Capture ALL network traffic]
         |                     |
         +----------+----------+
                    |
                    v
   [VpnService.Builder.establish() -> ParcelFileDescriptor]
                    |
                    v
        [Virtual Interface tun0 Active]
                    |
       +------------+------------+
       | (Blocked App Traffic)   | (Unblocked App Traffic)
       v                         v
[Routed into tun0]        [Bypasses VPN Entirely]
       |                         |
       v                         v
[Packets Dropped / Logged] [Direct OS Network Stack (Zero Overhead)]
```

---

## 3. Concurrency & Threading Architecture

MobiArmour employs Kotlin Coroutines and structured concurrency to eliminate main thread stalls:
- **`Dispatchers.IO`**: Dedicated to APK ZIP archive decompression, byte-stream parsing, SQLite Room queries, and VPN file descriptor read loops.
- **`Dispatchers.Default`**: Dedicated to deterministic scoring heuristics, risk matrix correlation, and list filtering algorithms.
- **`Dispatchers.Main.immediate`**: Dedicated strictly to StateFlow emissions and Jetpack Compose state updates.
- **`serviceScope` (`CoroutineScope(SupervisorJob() + Dispatchers.IO)`)**: Manages long-lived background packet listening within `JarvisFirewallService`, ensuring service lifecycle isolation from UI Activities.

---

## 4. Security Boundaries & Assumptions

1. **Non-Root Execution:** MobiArmour assumes a standard, unprivileged Android runtime environment. It does not require or assume root permissions.
2. **Standard API Dependency:** All posture checks and package queries rely on official public or system Android SDK APIs (`PackageManager`, `DevicePolicyManager`, `KeyguardManager`, `VpnService`).
3. **Defense Against Rogue Apps:** Malicious apps are assumed to operate within normal Android application sandbox limits unless a platform privilege escalation vulnerability exists.
4. **Transparent Failure Modes:** If an OEM strips an Android API or restricts background capabilities, MobiArmour records an explicit degradation state (`CANNOT_VERIFY`) rather than falsifying a secure rating.
