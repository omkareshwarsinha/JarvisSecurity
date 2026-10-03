# MobiArmour Platform — Application Security Architecture & Hardening Specification

**Document Version:** 3.0.0-SecuritySpec  
**Classification:** Internal & Public Security Design Document  
**Audience:** Security Auditors, Code Reviewers, Android Security Engineers  

---

## 1. Application Security Architecture Principles

MobiArmour is engineered as a zero-trust, offline-first security application. As a tool designed to inspect and protect host systems, its internal codebase must maintain the highest standards of software integrity, least privilege, and defensive programming.

### Core Principles:
1. **Zero Outbound Telemetry:** MobiArmour transmits zero network payloads to external servers. All processing is strictly on-device.
2. **Deterministic Data Flow:** Security states are managed via immutable data classes and unidirectional data flow (Kotlin `StateFlow`).
3. **Defense-in-Depth IPC:** Inter-Process Communication (IPC) interfaces and Broadcast Receivers enforce strict validation and signature/permission checks.
4. **Resilient Error Handling:** All interactions with Android framework APIs and OEM settings intent resolvers are defensively wrapped to prevent denial-of-service crashes on modified Android distributions.

---

## 2. Permission Model & Least Privilege Justification

MobiArmour declares only permissions strictly required for its stated security and operational roles:

| Permission | Category | Architectural Justification |
|---|---|---|
| `QUERY_ALL_PACKAGES` | Core Auditing | Required on Android 11+ (API 30+) to enumerate installed applications, inspect package manifests, identify stealth apps, and calculate device risk scores. |
| `BIND_VPN_SERVICE` | Network Enforcement | System-level permission required to bind `JarvisFirewallService`, enabling per-application selective routing and software-enforced network isolation. |
| `ACCESS_NETWORK_STATE` | Network Telemetry | Inspects default network connectivity states, active network transport types, and link properties. |
| `RECEIVE_SMS` & `SEND_SMS` | Emergency Recovery | Utilized exclusively by `RemoteLockdownReceiver` to parse authorized C2 commands from whitelisted contacts and dispatch emergency telemetry replies when the device is lost or stolen. |
| `ACCESS_FINE_LOCATION` | Emergency Recovery | Required solely for querying GPS telemetry when an authorized `#location` or `#lockdown` SMS command is authenticated. Not accessed during routine audits. |
| `POST_NOTIFICATIONS` | User Alerting | Required on Android 13+ (API 33+) to deliver critical threat alerts and firewall status indicators. |
| `RECEIVE_BOOT_COMPLETED` | Service Resilience | Restores active firewall routing policies after system reboot without requiring manual user application launch. |

---

## 3. Storage & Cryptographic Protections

- **Application-Private Sandbox:** All databases, preferences, and cached structures reside exclusively within `/data/user/0/com.example/` (subject to kernel-level Linux UID permissions).
- **Room Database Storage:** The SQLite database is never placed on shared external storage (`/sdcard/`), preventing cross-application data scraping.
- **Certificate Verification:** Cryptographic signatures of installed packages are verified using standard Android X.509 certificate parsers (`SigningInfo` / `PackageInfo.signatures`), inspecting public keys and Subject DNs to identify untrusted debug signatures.
- **Data Retention & Sanitization:** Users can purge the entire operational event log, traffic records, and posture history in a single tap via the Application Settings menu.

---

## 4. IPC & Component Hardening

### 4.1 Component Exposure Controls
- **Activities:** All internal screens and sub-components are declared with `android:exported="false"`. The only exported activity is `MainActivity` (acting as the system launcher entry point) and `PhishingGateActivity` (which filters standard `http`/`https` scheme intents for phishing evaluation).
- **Broadcast Receivers:**
  - `MobiArmourAdminReceiver`: Bound exclusively via `android.permission.BIND_DEVICE_ADMIN`, preventing arbitrary third-party callers from triggering device administration callbacks.
  - `RemoteLockdownReceiver`: Defensively parses incoming SMS PDUs, validates sender phone numbers against the local cryptographic hash table in `TrustedDeviceEntity`, and discards unauthorized commands without execution.
- **Services:**
  - `JarvisFirewallService`: Protected by the platform-enforced `android.permission.BIND_VPN_SERVICE`, ensuring only the Android OS can bind to the VPN service lifecycle.

### 4.2 Safe Intent Resolution
- Every explicit or implicit system intent dispatch (such as navigating to `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` or `Settings.ACTION_SECURITY_SETTINGS`) is wrapped in defensive exception handling to prevent uncaught `ActivityNotFoundException` on non-standard vendor ROMs (e.g., modified versions of ColorOS, MIUI, or Flyme).

---

## 5. WebViews & Dynamic Code Execution

- **Zero WebView Footprint:** MobiArmour contains **zero WebViews**. The entire UI hierarchy is constructed in pure native Jetpack Compose. This completely eliminates attack surfaces related to Cross-Site Scripting (XSS), malicious JavaScript bridges, and local file URL exfiltration.
- **Zero Dynamic Code Loading:** MobiArmour does not utilize `DexClassLoader`, `PathClassLoader`, or dynamic reflection on hidden Android internal APIs (`@hide`). All logic is compiled directly into the release binary.
- **Zero Third-Party Ad or Analytics SDKs:** The application does not bundle any third-party tracking, advertising, crash reporting, or telemetry SDKs.

---

## 6. Vulnerability Disclosure & Reporting

Security researchers and developers who discover potential vulnerabilities in MobiArmour are encouraged to report findings directly to the maintainers via encrypted communication channels. All reported issues are audited, remediated, and documented in public changelogs.
