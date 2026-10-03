# MobiArmour Platform — Capability Maturity & Implementation Matrix

**Document Version:** 3.0.0-Matrix  
**Classification:** Engineering Traceability & Capability Baseline  
**Audience:** System Architects, Product Managers, Security Engineers  

---

## 1. Feature Maturity Classification System

Every functional capability is assessed against two strict dimensions:

1. **Maturity Class:**
   - `[BUILDABLE NOW]`: Utilizes standard, documented Android SDK APIs without special entitlements.
   - `[ADVANCED]`: Requires complex Android system integrations (e.g., `VpnService`, Device Admin, low-level bytecode extraction).
   - `[PLATFORM-DEPENDENT]`: Subject to OEM ROM variations, battery management restrictions, or API version gates.
   - `[RESEARCH]`: Experimental heuristics, Control-Flow Graph analysis, or on-device statistical anomaly detection.
   - `[ROADMAP]`: Future architectural milestones planned across multi-phase engineering.
   - `[NOT SUPPORTED BY NORMAL ANDROID]`: Prohibited by Android security sandbox for non-root, third-party apps.

2. **Implementation Reality State:**
   - `NOT IMPLEMENTED`: Architectural concept only; zero code footprint.
   - `PROTOTYPE`: Initial prototype or experimental stub.
   - `PARTIALLY IMPLEMENTED`: Core functional path active; secondary edge cases in development.
   - `IMPLEMENTED`: Code complete, integrated within ViewModel/Repository architecture.
   - `TESTED`: Verified via automated JVM/Robolectric unit tests and manual execution harnesses.
   - `PRODUCTION VALIDATED`: Empirically validated across diverse physical OEM test benches.

---

## 2. Comprehensive Capability Matrix

| Feature / Subsystem | Maturity Class | Implementation Reality | Android API / Dependency | In Current MVP? | Advanced Scope | Research Scope | Primary Limitations & Architectural Caveats |
|---|---|---|---|---|---|---|---|
| **Device Posture Engine** | `[BUILDABLE NOW]` | `IMPLEMENTED` / `TESTED` | `Build.VERSION`, `KeyguardManager`, `DevicePolicyManager`, `Settings.Global` | Yes | Extended hardware attestation signals | Anomaly detection across configuration history | Scoring model is a heuristic risk assessment, not a mathematical compromise proof. |
| **Installed Package Inventory** | `[BUILDABLE NOW]` | `IMPLEMENTED` / `TESTED` | `PackageManager.getInstalledPackages()`, `QUERY_ALL_PACKAGES` | Yes | Dynamic component state tracking | Real-time package install behavior profiling | Requires `QUERY_ALL_PACKAGES` permission on Android 11+ (API 30+). |
| **System vs User App Partitioning** | `[BUILDABLE NOW]` | `IMPLEMENTED` / `TESTED` | `ApplicationInfo.flags & FLAG_SYSTEM` | Yes | OEM pre-install bloatware categorization | System app privilege escalation detection | Cannot distinguish between necessary OS binaries and carrier-bundled adware without catalog. |
| **Static APK Container Inspection** | `[ADVANCED]` | `IMPLEMENTED` / `TESTED` | `java.util.zip.ZipFile`, `appInfo.sourceDir` | Yes | Multi-DEX header extraction, native `.so` ABI audit | Bytecode entropy and packing detection | Cannot inspect dynamically downloaded DEX payloads loaded strictly in-memory. |
| **X.509 Certificate Verification** | `[BUILDABLE NOW]` | `IMPLEMENTED` / `TESTED` | `PackageInfo.signingInfo`, `Signature`, `CertificateFactory` | Yes | Certificate chain and Subject DN analysis | Shared signing key correlation | Distinguishes `CN=Android Debug` from production certificates; does not verify revoked CA status. |
| **Stealth / Iconless App Detection** | `[ADVANCED]` | `IMPLEMENTED` / `TESTED` | `Intent.CATEGORY_LAUNCHER`, `PackageManager.queryIntentActivities` | Yes | Background persistence + sensor privilege cross-referencing | Behavioral masquerade detection | Headless background utility plugins may require user whitelisting to avoid false alarms. |
| **Direct Threat Uninstallation** | `[BUILDABLE NOW]` | `IMPLEMENTED` / `TESTED` | `Intent.ACTION_DELETE`, `Uri: package:$pkg` | Yes | Automatic fallback to application details settings | Bulk package removal automation | Non-root third-party apps cannot silently delete packages; requires OS user confirmation prompt. |
| **Silent Package Uninstallation** | `[NOT SUPPORTED BY NORMAL ANDROID]` | `NOT IMPLEMENTED` | `PackageInstaller` (System / Device Owner signature) | No | No | No | Android OS sandboxing strictly forbids unprivileged apps from silently uninstalling other apps. |
| **Selective Per-App Firewall** | `[ADVANCED]` | `IMPLEMENTED` / `TESTED` | `android.net.VpnService.Builder.addAllowedApplication` | Yes | Real-time packet drop logging to Room DB | Protocol-level anomaly detection | VpnService exclusivity: cannot coexist with other active third-party VPN apps. |
| **Software-Enforced Network Isolation** | `[ADVANCED]` | `IMPLEMENTED` / `TESTED` | `VpnService.Builder.addRoute("0.0.0.0", 0)` | Yes | Instant emergency global kill switch | Dynamic DNS capture | Software routing isolation only; does NOT alter physical RF transceiver state. |
| **Plaintext HTTPS Decryption** | `[NOT SUPPORTED BY NORMAL ANDROID]` | `NOT IMPLEMENTED` | Custom CA Certificate Store Injection + TLS MITM Proxy | No | No | No | Cannot inspect encrypted TLS payloads of other apps without installing custom user CAs (breaks Network Security Config). |
| **Security Event Timeline** | `[BUILDABLE NOW]` | `IMPLEMENTED` / `TESTED` | Room Database (`SecurityEventEntity`), Kotlin Flow | Yes | Filterable timeline categorized by event severity | Chronological incident chaining | Event logs are bounded by local SQLite retention limits to prevent unbounded storage growth. |
| **Explainable Risk Engine** | `[ADVANCED]` | `PARTIALLY IMPLEMENTED` | Heuristic correlation matrix, Kotlin `ScoringEngine` | Yes | Multi-vector permission + persistence + network rule correlation | Local SLM (Small Language Model) diagnostic summaries | Currently rule-driven; does not employ stochastic AI for root threat determination. |
| **Out-of-Band Remote Lockdown (SMS)** | `[PLATFORM-DEPENDENT]` | `IMPLEMENTED` / `TESTED` | `BroadcastReceiver` (`goAsync()`), `Telephony.Sms.Intents`, `DevicePolicyManager` | Yes | Allowlisted phone-number validation with prefix enforcement and optional PIN tokens | Fallback encrypted mesh communication | Subject to SMS permissions, carrier SMS delivery latency, and Android background execution limits. |
| **Emergency Location Dispatch** | `[PLATFORM-DEPENDENT]` | `IMPLEMENTED` / `TESTED` | `LocationManager` (GPS/Network/Passive cached fix) | Yes | Best cached / last-known coordinate fix transmitted via SMS | Geofencing anomaly alerts | Reports cached fix; fresh satellite lock requires foreground location acquisition and active hardware. |
| **Acoustic Locator Siren** | `[BUILDABLE NOW]` | `IMPLEMENTED` / `TESTED` | `MediaPlayer`, `AudioManager.STREAM_ALARM` | Yes | Overrides silent and vibrate system audio profiles | Frequency-hopping acoustic beacon | Does not operate if device battery is depleted or speaker hardware is physically damaged. |
| **Phishing URL Heuristic Interceptor** | `[BUILDABLE NOW]` | `IMPLEMENTED` / `TESTED` | `PhishingGateActivity`, URL string heuristics | Yes | Homograph, non-standard port, and subdomain depth analysis | Local bloom filter of known malicious domain hashes | Cannot evaluate server-side dynamic redirects or dynamic payload execution inside the browser. |
| **Control-Flow Graph (CFG) Analysis** | `[RESEARCH]` | `ROADMAP` | Smali/Baksmali bytecode decompiler, graph traversal algorithms | No | No | Yes | High memory and CPU overhead on mobile devices; requires selective function targeting. |
| **Kernel Rootkit Eradication** | `[NOT SUPPORTED BY NORMAL ANDROID]` | `NOT IMPLEMENTED` | Direct kernel memory manipulation (`/dev/kmem`) | No | No | No | Unprivileged Android applications have zero access to kernel memory or low-level SELinux policies. |
| **Multi-Device Enterprise Fleet Trust** | `[ROADMAP]` | `NOT IMPLEMENTED` | Remote Attestation, mTLS Gateway, Zero-Trust Architecture | No | Yes | No | Planned for Phase 6; requires backend verification infrastructure. |
