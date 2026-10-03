# MobiArmour Platform — Product Requirements Document (PRD)

**Product Name:** MobiArmour Mobile Security & Threat Intelligence Platform  
**Target Environment:** Android 9.0 (API 28) through Android 15+ (API 35)  
**Document Version:** 3.0.0-ArchitectureSpec  
**Classification:** Technical Architecture Specification & Product Blueprint  
**Status:** Living Architectural Specification (Iterative Engineering Baseline)  

---

## 1. Executive Summary & Vision

**MobiArmour** is an offline-first Android security platform architected for defensive transparency, on-device risk assessment, granular network traffic control, and actionable device posture management. In contrast to conventional consumer security utilities that rely on predatory monetization, inflated threat counters, proprietary black-box telemetries, and battery-draining artificial scanning loops, MobiArmour treats Android security as an observable, verifiable systems engineering domain.

MobiArmour operates on a strict **zero-telemetry, offline-first execution model**. All static APK heuristics, DEX byte-stream analyses, local VPN packet routing, state-machine device audits, and risk correlations execute deterministically on the physical device.

MobiArmour is designed with a long-term architectural trajectory:
```
[Physical Device & OS]
         ↓
[Layer 1: Device Security Posture Engine]
         ↓
[Layer 2: Application Risk Engine & DEX Forensics]
         ↓
[Layer 3: Network Enforcement Layer (VpnService)]
         ↓
[Layer 4: Security Event Timeline & Telemetry Bus]
         ↓
[Layer 5: Correlation & Security Intelligence Engine]
         ↓
[Layer 6: Incident Response & Remediation Engine]
         ↓
[Enterprise Device Trust & Multi-Device Governance (Roadmap)]
```

---

## 2. Feature Maturity & Implementation Classification System

To maintain strict technical integrity and avoid unsupported marketing claims, every functional capability in MobiArmour is governed by two orthogonal dimensions: **Maturity Class** and **Implementation Reality**.

### 2.1 Maturity Classes
- **`[BUILDABLE NOW]`**: Implemented or fully implementable using documented, standard Android public SDK APIs without special entitlements or unverified OS behavior.
- **`[ADVANCED]`**: Achievable through advanced system integration (e.g., `VpnService` network tunneling, Device Administrator bindings, low-level bytecode extraction) requiring meticulous lifecycle and performance tuning.
- **`[PLATFORM-DEPENDENT]`**: Functional capabilities subject to OEM customizations, proprietary vendor battery managers, Knox/MIUI/ColorOS behavioral forks, or specific Android API level gates.
- **`[RESEARCH]`**: Experimental heuristics, control-flow graph (CFG) analysis, on-device symbolic execution, or statistical ML models currently in research exploration.
- **`[ROADMAP]`**: Formally designed architectural targets scheduled for subsequent engineering phases.
- **`[NOT SUPPORTED BY NORMAL ANDROID]`**: System actions fundamentally prohibited for non-root, non-system-signed third-party applications (e.g., silent uninstallation without user consent, kernel-level packet filters, arbitrary private sandbox access).

### 2.2 Implementation Reality States
- `NOT IMPLEMENTED`: Architectural blueprint defined; zero codebase presence.
- `PROTOTYPE`: Initial experimental code or stub exists; lacks comprehensive edge-case handling.
- `PARTIALLY IMPLEMENTED`: Core functional pathway active; secondary edge cases or integration points in progress.
- `IMPLEMENTED`: Code complete, integrated within core ViewModel/Repository pipeline.
- `TESTED`: Covered by automated JVM/Robolectric unit tests or reproducible manual harness.
- `PRODUCTION VALIDATED`: Empirically validated across diverse physical OEM test benches under real-world conditions.

---

## 3. Core Functional Modules & Specification

### Module 1: Device Security Posture Engine
- **Objective:** Evaluate local OS integrity, hardware-backed cryptographic protections, system configuration flags, and platform-level exposure surfaces.
- **Maturity:** `[BUILDABLE NOW]`
- **Implementation Status:** `IMPLEMENTED` / `TESTED`
- **Key Signals Extracted:**
  - OS Patch Level vs. Modern Platform Baseline (`Build.VERSION.SECURITY_PATCH`)
  - Keyguard & Biometric Lock State (`KeyguardManager.isDeviceSecure`)
  - Storage Encryption State (`DevicePolicyManager.getStorageEncryptionStatus`)
  - Developer Options & Android Debug Bridge (`Settings.Global.ADB_ENABLED`)
  - Side-Loading Entitlements (`REQUEST_INSTALL_PACKAGES` distribution)
  - Common Root & SU Binary Indicators (`/system/xbin/su`, `/system/bin/su`, `/sbin/su`, Magisk mounts)
  - Active System Accessibility Handlers
- **Architectural Heuristic:** Signals are normalized into an additive Device Risk Assessment model. The score is explicitly documented as an *operational risk model*, not a mathematically proven guarantee of compromise.

---

### Module 2: Application Security & Risk Analysis Engine
- **Objective:** Ingest installed package binaries, extract structural and manifest telemetry, and apply deterministic heuristic rules to flag elevated risk postures.
- **Maturity:** `[BUILDABLE NOW]`
- **Implementation Status:** `IMPLEMENTED` / `TESTED`
- **Engine Pipeline:**
  ```
  APK / Installed Package
           ↓
  Static Archive Parser (ZipFile / Manifest Reader)
           ↓
  Feature Extraction (Permissions, Exported Components, SDK Limits, DEX Count)
           ↓
  Deterministic Rule Engine (Known Trojan patterns, Adware SDK footprints)
           ↓
  Risk Correlation Matrix
           ↓
  Risk Score (0 - 100) + Human-Readable Justification
           ↓
  Guided Remediation Action (Uninstall Intent / Settings Deep Link / Firewall Isolation)
  ```
- **Analysis Scope:**
  - System vs. User application classification
  - Minimum, Target, and Compile SDK thresholds
  - Debuggable build configuration flags (`ApplicationInfo.FLAG_DEBUGGABLE`)
  - Exported Activities, Receivers, Services, and Content Providers lacking explicit permission gates
  - Cryptographic Signing Identity (Google Play / OEM Release vs. Debug Keystore `CN=Android Debug`)
  - Dynamic Native Libraries (`.so` architectures in `/lib/{arch}/`)
  - Multi-DEX footprints (`classes.dex`, `classes2.dex`, ...)

---

### Module 3: Stealth App & Malware Risk Correlation Engine
- **Objective:** Discover stealth applications designed to evade the user's launcher screen and correlate multi-vector indicators indicative of stalkerware or trojan behavior.
- **Maturity:** `[ADVANCED]`
- **Implementation Status:** `IMPLEMENTED` / `TESTED`
- **Technical Heuristic:**
  - Non-system applications holding background persistence privileges (`RECEIVE_BOOT_COMPLETED`, foreground services) or high-risk telemetry access (SMS, Call Log, Background Location, Camera, Audio) that purposefully omit `Intent.CATEGORY_LAUNCHER` from all declared activities.
- **Failure Modes & False Positive Strategy:**
  - Legitimate utility background plugins or licensing engines that lack launcher activities are accounted for via an explicit **User Whitelist / Trust Registry**, preventing alert fatigue.
- **Remediation Pipeline:**
  - Initiates standard OS package uninstallation via `Intent.ACTION_DELETE` (`package:$packageName`).
  - Where uninstallation is denied or for system-resident packages, provides immediate deep linking to OS Application Detail settings and allows 1-tap local firewall isolation.

---

### Module 4: APK & DEX Static Forensics
- **Objective:** Deep inspection of raw APK container structures, binary manifests, certificate chains, and bytecode indicators without requiring device root privileges.
- **Maturity:** `[ADVANCED]` (Basic extraction) / `[RESEARCH]` (Control-flow graph & symbolic execution)
- **Implementation Status:** `IMPLEMENTED` (Static structure extraction) / `ROADMAP` (Advanced bytecode CFG)
- **Forensic Capabilities:**
  - Parsing compressed APK containers via streaming `ZipFile`
  - Enumeration of native dynamic libraries (`.so`) across ABI architectures (`arm64-v8a`, `armeabi-v7a`, `x86_64`)
  - Extraction and inspection of X.509 certificate fingerprints (SHA-256)
  - String table heuristic scanning for suspicious hardcoded IPs, C2 URLs, or dynamic loading payloads (`DexClassLoader`, `PathClassLoader`)
- **Limitations:** Cannot inspect dynamically fetched, decrypted, or reflection-loaded DEX payloads executing strictly in memory without an isolated instrumentation sandbox.

---

### Module 5: Per-Application Network Firewall
- **Objective:** Enforce deterministic local network filtering, allowlisting, and blocklisting on a per-package basis using the standard Android VPN infrastructure.
- **Maturity:** `[ADVANCED]`
- **Implementation Status:** `IMPLEMENTED` / `TESTED`
- **Architectural Mechanics:**
  - Uses `VpnService` to establish a virtual network interface (`tun0`).
  - Employs **Selective Routing** (`VpnService.Builder.addAllowedApplication`): only applications marked as *Blocked* are assigned to the VPN tunnel interface.
  - Packets entering the tunnel for blocked applications are discarded into a null sink, effectively terminating their internet access.
  - Unblocked applications remain completely outside the VPN routing table, preserving full direct socket throughput with zero intermediate userspace copy overhead.
- **Explicit Technical Caveats:**
  - *No Plaintext HTTPS Decryption:* MobiArmour does not terminate or inspect encrypted TLS payloads.
  - *VPN Exclusivity:* Android enforces a single active `VpnService` per device; MobiArmour cannot operate simultaneously with third-party enterprise VPN clients.

---

### Module 6: Software-Enforced Network Isolation ("Software Air-Gap Mode")
- **Objective:** Provide an emergency containment mechanism that severs all inbound and outbound network communications across Wi-Fi and Cellular interfaces.
- **Maturity:** `[ADVANCED]`
- **Implementation Status:** `IMPLEMENTED` / `TESTED`
- **Important Distinction:** This is **SOFTWARE-ENFORCED NETWORK ISOLATION**, established via capturing all IPv4 (`0.0.0.0/0`) and IPv6 (`::/0`) routing scopes into a dead-end local `tun0` interface. **It is NOT a physical air gap.**
- **Operational States:**
  - `NORMAL`: Standard selective per-app firewall filtering.
  - `RESTRICTED`: Non-essential background network access constrained.
  - `LOCKDOWN / ISOLATED`: Universal route capture active; zero traffic egress permitted.

---

### Module 7: Security Event Timeline & Telemetry Engine
- **Objective:** Capture, structure, and locally persist security-relevant lifecycle events to provide chronological forensic auditability.
- **Maturity:** `[BUILDABLE NOW]`
- **Implementation Status:** `IMPLEMENTED` / `TESTED`
- **Event Taxonomy:**
  - Package installation, upgrade, or removal events
  - Firewall rule modification or automated threat block events
  - Security posture degradation (e.g., ADB enabled, lockscreen removed)
  - Suspicious package or hidden application detection events
  - Remote lockdown or recovery command dispatches
- **Storage:** Persisted locally in SQLite via Android Room, strictly isolated within the app's sandboxed storage directory.

---

### Module 8: Security Intelligence & Explainable Risk Engine
- **Objective:** Ingest disparate security signals, correlate multi-event relationships, and produce human-understandable risk explanations rather than opaque numerical scores.
- **Maturity:** `[ADVANCED]` (Rule-based correlation) / `[RESEARCH]` (Local on-device ML anomaly detection)
- **Implementation Status:** `PARTIALLY IMPLEMENTED` (Deterministic rules active)
- **Example Correlation:**
  - *Observed:* Package requests `RECEIVE_BOOT_COMPLETED` + `SYSTEM_ALERT_WINDOW` + `BIND_ACCESSIBILITY_SERVICE` + connects to dynamic unlisted domain.
  - *Explanation:* "Assessed risk is HIGH due to background persistence combined with full-screen overlay privileges and accessibility event capture capabilities."

---

### Module 9: Out-of-Band Remote Anti-Theft & Recovery Controller
- **Objective:** Provide a resilient emergency command channel when primary internet connectivity is severed or unavailable.
- **Maturity:** `[PLATFORM-DEPENDENT]`
- **Implementation Status:** `IMPLEMENTED` (SMS command parser & device admin lock) / `TESTED`
- **Supported Commands (Authorized Allowlisted Senders Only):**
  - `#lockdown` / `/lockdown`: Immediate display lock via `DevicePolicyManager.lockNow()`, activates audible siren, queries best cached location fix, and dispatches coordinate response.
  - `#siren` / `/siren`: Triggers high-decibel audible beacon overriding silent modes.
  - `#location` / `/location`: Queries `LocationManager` for best cached/last known geographic fix and transmits SMS telemetry.
  - `#silence`: Deactivates active siren.
- **Platform Limitations:**
  - Subject to Android runtime SMS permissions (`RECEIVE_SMS`, `SEND_SMS`). On Android 10+, background service starts from receivers are restricted by OS battery and background launch limits.
  - GPS acquisition cannot be guaranteed if the device is indoors, shielded, or location hardware is disabled.

---

## 4. Non-Functional Requirements & Engineering Constraints

| Category | Requirement Specification | Validation Method |
|---|---|---|
| **Data Privacy** | 100% Zero Outbound Telemetry. Zero third-party tracker SDKs. All SQLite records stored in private app sandbox. | Network traffic inspection during full-system audit. |
| **Firewall Efficiency** | Selective tunnel routing for blocked apps only. Non-blocked traffic incurs zero VPN userspace copy overhead. | Comparative network throughput and latency benchmarks. |
| **Cold Startup** | Initial interactive frame rendered in under 600ms on standard ARM64 hardware. | Android Jetpack Macrobenchmark / Systrace profiling. |
| **Audit Efficiency** | Incremental scan pipeline processes installed applications without freezing the UI thread (60/120 FPS Compose rendering). | Compose Layout Inspector and FrameMetrics analysis. |
| **Defensive Security** | Non-exported broadcast receivers where applicable; signature/permission enforcement on Device Admin endpoints. | Android Lint & Static AST Security Analysis. |

---

## 5. Explicit Technical Disclaimers

1. **No Antivirus Equivalence Claim:** MobiArmour provides static analysis, heuristic risk correlation, and network-level control. It does not claim equivalence to a cloud-backed antivirus signature scanning network or enterprise EDR kernel hook.
2. **No Guaranteed Compromise Detection:** Sophisticated zero-day exploits, baseband attacks, or bootloader rootkits operating at kernel privilege cannot be guaranteed detectable by an unprivileged userspace Android application.
3. **Software-Only Isolation:** Network isolation mechanisms execute through Android userspace and kernel routing APIs; they do not alter physical RF hardware states.
