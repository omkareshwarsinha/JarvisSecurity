# MobiArmour — Comprehensive Security Checks Specification & Detection Catalog

**Document Version:** 3.1.0-SecurityChecks  
**Classification:** Enterprise Security Architecture Specification  
**System Target:** Android 8.0 (API 26) through Android 15 (API 35+)  
**Design Paradigm:** Offline-First, Zero-Telemetry, Heuristic and Deterministic On-Device Inspection  

---

## 1. Architectural Overview & Inspection Pipeline

MobiArmour employs a multi-tiered, asynchronous security evaluation pipeline designed to evaluate the physical device, operating system integrity, application sandbox isolation, network traffic egress, and external link threats without transmitting any telemetry off-device.

```
+-------------------------------------------------------------------------+
|                       MobiArmour Auditing Engine                        |
+-------------------------------------------------------------------------+
       |                         |                        |
       v                         v                        v
+---------------+       +------------------+     +------------------+
| DeviceAuditor |       | AppSecurityAudit |     | PhishingDetector |
| (Host & OS)   |       | (APKs & Sandbox) |     | (URL & Pre-Click)|
+---------------+       +------------------+     +------------------+
       |                         |                        |
       +-------------------------+------------------------+
                                 |
                                 v
                 +-------------------------------+
                 |  MobiArmour Posture Evaluator |
                 |  - CVSS / OWASP Severity Map  |
                 |  - Risk Score Aggregator      |
                 |  - Remediation Router         |
                 +-------------------------------+
```

The engine classifies all findings into four explicit operational severity levels:
- **`CRITICAL`**: Immediate active threat, compromised root boundaries, or active exploit vector.
- **`WARNING`**: Severe misconfiguration or excessive attack surface that degrades platform defense-in-depth.
- **`INFORMATIONAL`**: Hardening opportunity or device configuration state that deviates from zero-trust baseline.
- **`VERIFIED_SECURE`**: Explicit verification of an active hardware or operating system defense mechanism.

---

## 2. Host Operating System & Hardware Integrity Checks

| Check ID | Name | Category | Severity Baseline | Threat Vector Mitigated | Detection Mechanism |
|---|---|---|---|---|---|
| `root_detection` | Root & SU Binaries Check | Integrity | `CRITICAL` / `VERIFIED_SECURE` | Unrestricted privilege escalation, kernel compromise, evasion of Android application sandboxing. | Traverses high-risk filesystem paths (`/system/bin/su`, `/system/xbin/su`, `/sbin/su`, `/system/app/Superuser.apk`, Magisk sockets) and validates `Build.TAGS` for unverified `"test-keys"`. |
| `selinux_enforcement` | SELinux Enforcement Status | Integrity | `CRITICAL` / `VERIFIED_SECURE` | Mandatory Access Control (MAC) bypass, unconfined domain execution, DAC-only privilege leaks. | Queries `/sys/fs/selinux/enforce` and executes reflection or property checks on `os.selinux`. Permissive or disabled status is flagged as critical. |
| `screen_lock` | Screen Lock & Keystore Hardware Binding | Physical Security | `CRITICAL` / `VERIFIED_SECURE` | Unauthorized physical access, cryptographic key decapsulation without physical user factor. | Evaluates `KeyguardManager.isDeviceSecure()`. Ensures Android Keystore keys cannot be unlocked without hardware-backed biometrics or passphrases. |
| `adb_debugging` | USB Debugging (ADB) Check | Surface Exposure | `WARNING` / `VERIFIED_SECURE` | Juice-jacking at untrusted charging kiosks, physical forensic extraction via ADB shell and backup commands. | Inspects `Settings.Global.ADB_ENABLED` via `ContentResolver`. Flags active ADB states when disconnected from development environments. |
| `developer_options` | Developer Options State | Surface Exposure | `INFORMATIONAL` / `VERIFIED_SECURE` | Tampering with background process limits, unverified app sideloading flags, and execution profiling hooks. | Evaluates `Settings.Global.DEVELOPMENT_SETTINGS_ENABLED`. Alerts user if developer instrumentation remains enabled on personal devices. |
| `mock_locations` | Mock Location Provider Check | Geolocation Privacy | `WARNING` / `VERIFIED_SECURE` | GPS spoofing, fake location telemetry injection into emergency or tracking modules. | On Android 12+ (API 31+), inspects `Location.isMock()`. On legacy APIs, inspects `Settings.Secure.ALLOW_MOCK_LOCATION`. |
| `os_security_patch` | Android OS Security Patch Cadence | Firmware Integrity | `WARNING` / `VERIFIED_SECURE` | Exploitation of weaponized Linux kernel, Qualcomm/MediaTek SoC baseband, or Android framework CVEs. | Parses `Build.VERSION.SECURITY_PATCH` against real-time baseline date (flagging firmware outdated by > 90 days). |
| `encryption_status` | Device Storage Encryption | Data-at-Rest | `CRITICAL` / `VERIFIED_SECURE` | Direct storage chip extraction, NAND chip-off forensic scraping. | Validates `DevicePolicyManager.STORAGE_ENCRYPTION_STATUS_ACTIVE` on API < 33, or enforces default File-Based Encryption (FBE) on Android 10+. |

---

## 3. Application Sandbox & APK Bytecode Inspection Checks

Every installed application on the device is audited by `AppSecurityAuditor` across multiple static analysis dimensions:

### 3.1 Dangerous & Over-Privileged Permissions Matrix
Applications are inspected for combinations of high-risk permissions that provide surveillance or financial fraud capabilities:

- **Accessibility Service Weaponization (`BIND_ACCESSIBILITY_SERVICE`):**
  - Identifies non-system applications requesting UI tree inspection, keylogging, or automated touch injection.
  - Mitigates banking trojans (e.g., SharkBot, TeaBot) that abuse accessibility to bypass multi-factor authentication.
- **System Alert Window (`SYSTEM_ALERT_WINDOW`):**
  - Audits overlay permissions used in overlay clickjacking attacks and fake credential entry dialogues.
- **SMS Exfiltration (`RECEIVE_SMS`, `READ_SMS`, `SEND_SMS`):**
  - Audits background access to incoming one-time SMS verification codes and premium SMS fraud.
- **Call & Audio Telemetry (`RECORD_AUDIO`, `PROCESS_OUTGOING_CALLS`):**
  - Identifies persistent background acoustic surveillance threats.
- **Device Administration (`BIND_DEVICE_ADMIN`):**
  - Audits third-party applications holding hardware-level wipe, screen lock, or camera disabling authorities.

### 3.2 APK Archive & Manifest Hardening
- **Plaintext HTTP Traffic (`usesCleartextTraffic`):**
  - Scans `ApplicationInfo.flags` for `FLAG_USES_CLEARTEXT_TRAFFIC`. Identifies apps that expose session cookies, passwords, and API tokens to local network Wi-Fi MITM eavesdropping.
- **Debuggable APK Flag (`FLAG_DEBUGGABLE`):**
  - Verifies that production apps do not ship with active JDWP debug hooks that allow local process memory injection and runtime manipulation.
- **Shared Linux UID (`sharedUserId`):**
  - Flags non-system applications utilizing `sharedUserId` to merge Linux process sandboxes with other installed packages.
- **Target SDK Deprecation:**
  - Audits apps targeting legacy Android API levels (e.g., targetSdk < 29) that circumvent scoped storage, runtime permissions, and background location boundaries.

### 3.3 Adware & Tracking SDK Signatures
The package scanner inspects embedded activities, services, receivers, and meta-data keys against heuristic fingerprints of aggressive advertising networks and tracking SDKs:
- Unity Ads, IronSource, AppLovin, Mintegral, Vungle, InMobi, Chartboost, AdColony.
- Flags apps containing > 3 integrated ad networks as severe privacy risk profiles.

### 3.4 Stealth / Hidden App Detection
- Identifies installed applications with zero launchable `Intent.CATEGORY_LAUNCHER` activity entries that also do not declare `ACTION_MAIN` or input-method/wallpaper system roles.
- Flags potential stalkerware, droppers, and hidden background tracking agents.

---

## 4. Network & Egress Firewall Inspection Checks

MobiArmour integrates a local, zero-trust loopback VPN architecture (`JarvisFirewallService`):

| Inspection Layer | Technical Mechanism | Protective Outcome |
|---|---|---|
| **Air-Gap Kill Switch** | Drops all outbound IPv4 and IPv6 traffic at the virtual TUN interface (`fd`). | Immediate total network severance in the event of suspected malware intrusion or active exfiltration. |
| **Selective App Blocker** | Maps Linux socket UIDs via Android VpnService routing tables to selectively isolate blocked application packages. | Blocks background apps from transmitting user telemetry, phone identifiers, or location data. |
| **Tracker & Ad Domain Filter** | Intercepts DNS query packets and compares requested hostnames against local regex blocklists. | Blocks surveillance trackers, telemetry beacons, and unwanted ad delivery domains at zero battery cost. |
| **Outbound Flow Logging** | Logs destination IP, transport protocol (TCP/UDP), port, and byte counts into Room database table `network_traffic`. | Provides complete visibility into all outbound destinations contacted by installed apps. |

---

## 5. URL Pre-Click & Phishing Heuristic Checks

The `PhishingDetector` evaluates links prior to execution in the default browser:

1. **High-Entropy / Random Domain Scoring:**
   - Detects algorithmic domain generation (DGA) used by botnets and transient phishing hosts.
2. **Homograph & Punycode Detection:**
   - Identifies non-ASCII Unicode (Cyrillic, Greek) lookalike characters designed to spoof authentic brand domains (e.g., `раураl.com` vs `paypal.com`).
3. **Suspicious TLD Classification:**
   - Heuristically scores domain extensions frequently associated with disposable phishing campaigns (`.xyz`, `.top`, `.tk`, `.buzz`, `.club`, `.work`).
4. **Direct IP Host Address Detection:**
   - Flags links containing raw IPv4/IPv6 addresses in place of registered DNS hostnames, a common pattern in credential harvester staging servers.
5. **Credential Keyword Heuristics:**
   - Inspects URL paths and query parameters for security-sensitive terms (`login`, `verify`, `banking`, `update-account`, `secure-recovery`) hosted on untrusted domains.

---

## 6. Remote Lockdown & C2 Hardware Defense Checks

- **Cryptographic Phone Number Whitelisting:**
  - SMS commands (`#siren`, `#lockdown`, `#locate`, `#unlock`) are evaluated against `TrustedDeviceEntity` using SHA-256 phone number hash comparison.
- **Hardware Lockdown Authority:**
  - Integrates with Android `DevicePolicyManager` to lock the physical display immediately upon reception of authenticated emergency signals.
- **Acoustic Emergency Siren:**
  - Forces audio stream `AudioManager.STREAM_ALARM` to maximum hardware volume, looping high-frequency emergency siren tones to locate misplaced or stolen hardware.

---

## 7. Operational Verification Checklist

To verify that all security checks function deterministically on a test device:
- [x] Run Complete Device Audit from Dashboard (`viewModel.triggerScan()`).
- [x] Verify Root Detection reports `VERIFIED_SECURE` on standard commercial devices or `CRITICAL` on Magisk/KernelSU devices.
- [x] Verify Screen Lock detection reflects PIN/Biometric state.
- [x] Toggle USB Debugging in Developer Options and verify `adb_debugging` state updates.
- [x] Verify Mock Location check flags developer mock locations when enabled.
- [x] Validate App Auditor lists all non-system applications with correct permission risk badges.
- [x] Test Phishing Link evaluation on sample suspicious URLs (`http://192.168.1.1/login`, `http://paypаl.com`).
- [x] Verify Firewall Master Switch and Per-App Blocker properly drop packets for targeted packages.
