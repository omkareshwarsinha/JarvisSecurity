# MobiArmour Platform — Technology Stack & Engineering Specifications

**Document Version:** 3.0.0-TechStackSpec  
**Classification:** Technical Architecture Reference  
**Audience:** Security Engineers, System Architects, Android Platform Developers  

---

## 1. Primary Technology Stack Overview

| Dimension | Selected Technology | Version / API Target | Maturity & Rationale |
|---|---|---|---|
| **Primary Language** | **Kotlin** | 2.0.21 (Compose Compiler 2.0) | Strict type safety, nullability guarantees, high-performance coroutines. `[BUILDABLE NOW]` |
| **UI Framework** | **Jetpack Compose** | Material Design 3 (M3) 1.3+ | Declarative, state-driven rendering; eliminates legacy XML view hierarchies and reduces memory overhead. `[BUILDABLE NOW]` |
| **Persistence Engine** | **Android Room Database** | 2.6.1 + KSP | Local-only, SQLite-backed persistence with reactive Kotlin `Flow` streams; operates entirely offline. `[BUILDABLE NOW]` |
| **Concurrency & Async** | **Kotlin Coroutines & Flow** | kotlinx-coroutines 1.8+ | Structured concurrency; segregates IO-heavy ZIP/SQLite tasks from computation and UI threads. `[BUILDABLE NOW]` |
| **Architecture Pattern** | **Clean Architecture + MVVM** | AndroidX Lifecycle & ViewModel | Unidirectional Data Flow (UDF), lifecycle-aware state retention across configuration changes. `[BUILDABLE NOW]` |
| **Network Interception** | **Android VpnService** | Linux `tun` Virtual Driver | Userspace per-app packet routing and software network isolation without requiring root. `[ADVANCED]` |
| **Static APK Forensics** | **Java ZipFile & X.509 Cryptography** | `java.util.zip`, `java.security.cert` | On-device static ZIP archive inspection, DEX file header enumeration, certificate signature hashing. `[ADVANCED]` |
| **Device Security / MDM** | **DevicePolicyManager & KeyguardManager** | Android Enterprise Framework | Hardware-backed keyguard state verification, storage encryption assessment, programmatic lock. `[BUILDABLE NOW]` |
| **Sensor & Telemetry** | **LocationManager, BatteryManager** | Android Framework Core | Emergency location acquisition, battery consumption hygiene, display brightness control. `[BUILDABLE NOW]` |

---

## 2. Layered Module Specifications

### 2.1 UI Presentation Layer (Jetpack Compose & Material 3)
- **Centralized Design System:** Centralized in `com.example.ui.theme` with support for high-contrast dark palette (`JarvisBackground: #0A0E17`) and adaptive light palette (`#F5F7FB`).
- **Glassmorphic Surface Design:** Implemented in `GlassCard.kt` through layered translucent alpha compositing, subtle borders (`JarvisBorderSubtle`), and standardized corner radiuses (16dp-20dp) engineered to eliminate expensive real-time blur shaders that degrade frame rate on low-tier GPUs.
- **Top Bar & Navigation:** `JarvisTopBar.kt` provides unified branding, operational status, and dynamic theme switching. Navigation handles routing across Dashboard, Scanner, Application Auditor, Firewall, Permissions, Hardening, and Toolkit screens.

### 2.2 Domain & Static Analysis Engines
- **`PackageAuditor.kt`:** Queries Android's `PackageManager` to retrieve package metadata, user vs. system classification, target SDK versioning, debug flags, exported components, and granted dangerous permissions.
- **`DeepCodeAuditor.kt`:**
  - Streams APK archives directly via `java.util.zip.ZipFile` from `appInfo.sourceDir`.
  - Enumerates compiled DEX files (`classes.dex`, `classes2.dex` ...).
  - Inspects native architecture directories (`lib/arm64-v8a`, `lib/armeabi-v7a`, `lib/x86_64`).
  - Evaluates X.509 cryptographic signing certificates to detect debug keystores (`CN=Android Debug`).
  - Matches package and class signatures against heuristic catalogs of aggressive adware SDKs and known stalkerware patterns.
  - Detects stealth applications lacking `Intent.CATEGORY_LAUNCHER`.
- **`ScoringEngine.kt`:** Implements a deterministic, explainable risk model producing sub-scores across Device Posture, Application Security, and Network Exposure.

### 2.3 Local Persistence Layer (Room Database)
- **Database Descriptor:** `JarvisDatabase.kt` (Room SQLite, schema version 3).
- **Entities & Tables:**
  - `ScanResultEntity`: Historical audit scores, component deductions, and run timestamps.
  - `SecurityEventEntity`: Chronological timeline of security-relevant system and user events.
  - `FirewallRuleEntity`: Per-application firewall policies (`packageName`, `appName`, `isBlocked`, `updatedAt`).
  - `NetworkTrafficEntity`: Egress traffic log containing destination IP, port, protocol, and block status.
  - `TrustedDeviceEntity`: Whitelisted phone numbers authorized for emergency C2 SMS commands.
  - `TrustedAppEntity`: User-whitelisted applications excluded from threat deductions.
  - `PhishingHistoryEntity`: History of scanned web links and heuristic deception scores.

### 2.4 Network Enforcement Engine (`JarvisFirewallService.kt`)
- **Virtual Network Interface:** Creates a virtual network interface (`tun0`) with IP `10.0.0.2/32`.
- **Selective Routing:** Utilizes `VpnService.Builder.addAllowedApplication()` for blocked packages. Packets for blocked packages are routed to the local tunnel and dropped without outbound forwarding.
- **Software-Enforced Network Isolation:** Captures all IPv4 (`0.0.0.0/0`) and IPv6 (`::/0`) routes to prevent external data egress across all device applications.
- **Reconfiguration Pipeline:** Implements `rebuildVpn()` to tear down and re-establish the `ParcelFileDescriptor` when rules change without prompting the user for redundant VPN permission dialogs.

### 2.5 Out-of-Band Remote Recovery Engine (`RemoteLockdownReceiver.kt`)
- **Broadcast Receiver:** Listens for `Telephony.Sms.Intents.SMS_RECEIVED_ACTION`.
- **Sender Verification:** Validates incoming sender phone numbers against the authorized `TrustedDeviceEntity` registry.
- **Action Execution:**
  - `#lockdown`: Dispatches `DevicePolicyManager.lockNow()`, sounds emergency siren, and requests location fix.
  - `#siren`: Sounds maximum volume emergency acoustic beacon.
  - `#location`: Queries real-time GPS coordinates and replies via SMS.
  - `#silence`: Terminates acoustic beacon.

---

## 3. Platform Limitations & Technical Constraints

1. **VpnService Exclusivity:** Android allows only one active `VpnService` interface across the entire operating system. Enabling MobiArmour's firewall will disconnect any existing corporate or commercial VPN tunnel.
2. **Plaintext HTTPS Visibility:** As a non-proxying packet filter, MobiArmour inspects packet headers (IP, port, protocol) but does not decrypt TLS application data.
3. **Background Execution Constraints:** On Android 10+ (API 29+), background broadcast execution and location acquisition are heavily throttled by platform power management policies (Doze Mode, App Standby Buckets).
4. **Uninstallation Permissions:** Android third-party apps cannot silently uninstall other apps. Uninstallation is executed via user confirmation through `Intent.ACTION_DELETE`.
