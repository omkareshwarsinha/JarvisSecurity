# MobiArmour Platform — Comprehensive Threat Model & Security Boundaries

**Document Version:** 3.0.0-ThreatModel  
**Classification:** Defensive Engineering Specification  
**Scope:** Android Device Security, Application Heuristics, Network Enforcement, and Anti-Theft  
**Audience:** Security Architects, Penetration Testers, Systems Engineers  

---

## 1. Executive Summary & Attacker Model

MobiArmour operates as a standard, unprivileged Android application (non-root) running within the Android Application Sandbox (UID isolation). It optionally requests `Device Administrator` privileges to facilitate display locking and recovery actions.

This threat model outlines the explicit threat vectors, defensive controls, detection capabilities, failure modes, and residual risks of MobiArmour.

### 1.1 Attacker Classification
- **Adversary Tier 1 (Commercial Adware / PUPs):** Aggressive monetization SDKs bundling tracking libraries, aggressive display overlays, and out-of-context notifications.
- **Adversary Tier 2 (Consumer Stalkerware / Spyware):** Commercial spyware deployed with physical or social access, seeking to silently capture keystrokes, SMS, location, and calls while hiding from the app drawer.
- **Adversary Tier 3 (Trojanized APKs / Banking Malware):** Deceptive applications masquerading as utilities or financial tools, seeking to abuse Android Accessibility Services to automate clicks, steal OTPs, and harvest credentials.
- **Adversary Tier 4 (Physical Device Thief):** Malicious individual possessing physical custody of the hardware, attempting to bypass lockscreen protections or exfiltrate data before remote lock or wipe can be asserted.
- **Adversary Tier 5 (Advanced Persistent Threat / State Actor):** Actors deploying kernel exploits, zero-click baseband vulnerabilities, or firmware-level implants.

---

## 2. Threat Vector Analysis & Control Matrix

| Threat Category | Attack Surface | Security Control | Detection Capability | Limitations | Residual Risk |
|---|---|---|---|---|---|
| **Malicious Applications** | Sideloaded APKs, rogue app store packages, repackaged utilities | Static DEX inspection, permission profile scoring, signature checking | **High (Heuristic)**: Flags dangerous permission clusters and debug signatures | Cannot inspect dynamically fetched DEX payloads executing purely in memory | Zero-day malware with benign initial static profile |
| **Stalkerware & Stealth Apps** | Background services running without launcher activities | Stealth app heuristic engine (`CATEGORY_LAUNCHER` omission + sensor permissions) | **High**: Detects iconless packages holding sensitive telemetry privileges | Legitimate headless background services may trigger alerts (handled by user trust whitelist) | Stalkerware installed as a system app with OEM pre-signing |
| **Phishing & Deceptive URLs** | Inbound SMS, messaging apps, rogue links | URL heuristic analyzer & `PhishingGateActivity` link interceptor | **Medium**: Detects homograph attacks, IP-based domains, excessive subdomains | Cannot inspect encrypted HTTPS page content or dynamic redirects executed in-browser | Cloaked phishing URLs routing through legitimate CDNs |
| **Malicious Network Destinations** | Outbound application traffic, C2 beacons, telemetry endpoints | Per-app `VpnService` firewall & domain/IP drop rules | **High (at L3/L4)**: Drops packets matching blocked package rules | Does not inspect encrypted TLS payload content; limited to IP/port/package level | Data exfiltration through shared system services or DNS covert channels |
| **Unauthorized Device Access** | Physical access, unattended device, shoulder surfing | Device Posture Engine auditing Keyguard lock status | **High**: Detects missing screen lock, weak lockscreen, insecure Smart Lock | Cannot enforce biometric enrollment complexity without MDM Device Owner enrollment | Attacker accessing device while already unlocked by user |
| **Configuration Tampering** | USB Debugging, ADB shell, Developer Options enabled | Continuous security monitor checking `Settings.Global.ADB_ENABLED` | **High**: Flags active ADB and developer mode exposure | Cannot programmatically disable ADB (Android requires user action in Settings) | User ignoring warning and leaving ADB active on untrusted PC |
| **Credential & OTP Theft** | Accessibility Service abuse, SMS interception | Sensor permission auditing, notification listener inspection | **Medium**: Flags non-standard apps requesting Accessibility and SMS access | Android userspace apps cannot block an already-granted Accessibility Service from running | User explicitly granting Accessibility permissions to a malicious app |
| **Unsafe / Over-Privileged Apps** | Applications requesting broad, unneeded permissions | Permission risk engine cross-referencing package category vs. permissions | **High**: Identifies flashlight or calculator apps holding camera, SMS, or contacts access | Cannot dynamically revoke permissions programmatically; requires user intent in OS Settings | User knowingly accepting excessive permissions |
| **Lost / Stolen Device** | Physical device theft, offline device scenarios | Out-of-band SMS lockdown controller, high-decibel acoustic beacon, GPS fix | **Medium-High**: Triggers lock and location reply via SMS | Dependent on active cellular reception, SIM card retention, and Android SMS permissions | Thief immediately removing SIM card or placing device in RF Faraday enclosure |
| **Supply-Chain / Build Tampering** | Malicious third-party dependencies in installed apps | Static package manifest analysis, native `.so` library audit | **Medium**: Flags suspicious native dynamic libraries and known intrusive SDKs | Cannot perform full binary decompilation and AST reconstruction on-device | Backdoored native library compiled into an otherwise benign enterprise application |

---

## 3. Explicit Architectural Blind Spots (What MobiArmour CANNOT Detect)

A rigorous security specification must clearly delineate its detection boundaries:

1. **Kernel-Level Exploitation & Rootkits:**
   - MobiArmour runs in unprivileged userspace. If the underlying Linux kernel or SELinux policies have been compromised by a privilege escalation exploit, malicious kernel code can intercept system calls and falsify package information. MobiArmour cannot detect or remove kernel-resident rootkits.
2. **Private Application Sandboxes:**
   - Android UID isolation strictly prohibits an app from reading `/data/data/<target_package>/`. MobiArmour cannot inspect databases, files, or cached tokens inside another application's private sandbox.
3. **Plaintext HTTPS / TLS Traffic Contents:**
   - MobiArmour functions as a layer 3/4 packet filter using `VpnService`. It does not perform Man-in-the-Middle (MITM) TLS proxying or install custom root CA certificates. It cannot inspect the encrypted payloads of HTTPS requests.
4. **Baseband & Hardware Implants:**
   - MobiArmour cannot detect or mitigate vulnerabilities in the cellular baseband processor, Secure Processing Unit (SPU), or proprietary hardware microcode.
5. **Memory-Only Dynamically Decrypted Payloads:**
   - Payloads downloaded over encrypted channels and loaded via memory buffers into `InMemoryDexClassLoader` without touching the filesystem cannot be inspected via static APK archive analysis.
6. **Physical Air-Gap Security:**
   - MobiArmour's network isolation feature operates via software routing tables (`tun0`). It cannot physically cut power to device radios or protect against physical proximity RF eavesdropping.

---

## 4. False Positives & False Negatives Governance

- **Transparent Risk Classifications:** Risk is categorized into `CRITICAL`, `HIGH RISK`, `WARNING`, `SAFE`, and `CANNOT_VERIFY`.
- **Explainable Reasoning:** Every risk penalty is presented with its contributing heuristic factors (e.g., "Score reduced by 25 points due to Background Service + SMS Read Permission in an unverified app").
- **User Trust / Whitelist Registry:** Developers and power users can explicitly whitelist personal projects, developer builds, or internal tools (`TrustedAppEntity`), immediately neutralizing false alarms without compromising global detection rules.
- **Fail-Safe Design:** If an OEM firmware strips or modifies an Android API, MobiArmour logs an informational finding rather than claiming a false clean bill of health.
