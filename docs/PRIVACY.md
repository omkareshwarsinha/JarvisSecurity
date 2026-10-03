# MobiArmour Platform — Privacy Architecture & Data Governance Policy

**Document Version:** 3.0.0-PrivacySpec  
**Classification:** Privacy Architecture & Data Governance Charter  
**Audience:** End Users, Privacy Regulators, Security Auditors  

---

## 1. Core Privacy Architecture & Philosophy

MobiArmour operates on an **Offline-First, Zero-Telemetry** data governance charter. Unlike typical consumer security and utility applications that harvest device fingerprints, installed app lists, and location traces to monetize user behavior, MobiArmour treats privacy as an immutable system constraint.

### Architectural Commitments:
1. **Zero Telemetry by Default:** The core application contains zero background network transmitters, analytics collectors, crash-reporting SDKs, or advertising libraries.
2. **Local Data Sovereignity:** All audit findings, security posture metrics, packet drop logs, and timeline events reside strictly in local SQLite tables within the application's private filesystem.
3. **No Centralized Device Fingerprinting:** MobiArmour never collects or transmits the Android Advertising ID (AAID), IMEI, IMSI, hardware serial numbers, or persistent Android IDs (`Settings.Secure.ANDROID_ID`).

---

## 2. On-Device Processing vs. Cloud Services

### 2.1 Current Implementation: Complete Local Processing
In its current architecture:
- Static APK inspection, DEX extraction, and certificate hashing execute purely on the physical device CPU.
- Risk heuristics and threat score calculations are computed synchronously or asynchronously via local coroutines.
- Network firewall rules and packet drop decisions execute inside the local Linux kernel and `VpnService` virtual driver on-device.

### 2.2 Distinction: "Offline-First" vs. "Guaranteed Offline"
- **Offline-First Definition:** MobiArmour does not require network connectivity to perform full system posture auditing, static bytecode inspection, per-app network firewalling, or local recovery actions.
- **Future Threat Intelligence (Roadmap Consideration):** If future iterations introduce cloud-based threat intelligence feeds (such as querying SHA-256 hashes against community threat databases or updating signature catalogs), such services will be:
  - Strictly **Opt-In** with explicit user consent.
  - Architected using privacy-preserving techniques (e.g., k-anonymity prefix hashing) to prevent leaking the user's complete installed package inventory.
  - Fully decoupled from core functionality so that complete offline functionality remains intact.

---

## 3. Data Ingestion & Purpose Specification

| Data Element | Collection Scope | Local Retention | Purpose Specification |
|---|---|---|---|
| **Installed Package Metadata** | Package names, version codes, requested permissions, target SDKs | Stored in `ScannedAppEntity` (local SQLite) | Auditing application risk profiles, identifying stealth/hidden apps, evaluating permission hygiene. |
| **System Posture State** | Patch level, encryption status, ADB state, lockscreen status | Stored in `ScanResultEntity` (local SQLite) | Computing device risk score and displaying actionable hardening guidance. |
| **Firewall Rules & Traffic Logs** | Blocked package names, destination IP, port, protocol | Stored in `FirewallRuleEntity` & `NetworkTrafficEntity` | Enforcing per-app network blocking policies and providing local forensic visibility. |
| **Trusted Emergency Contacts** | Phone numbers specified by user | Stored in `TrustedDeviceEntity` (local SQLite) | Authenticating incoming out-of-band SMS lockdown and locator commands. |
| **Device Location Telemetry** | Latitude, longitude, accuracy radius | **Transient only** (never saved to database) | Acquired temporarily during authorized emergency `#location` or `#lockdown` SMS commands to reply to trusted contacts. |

---

## 4. User Data Ownership & Sanitization Controls

MobiArmour ensures complete user sovereignty over all generated operational data:
1. **Instant Audit Purge:** The Settings console provides a 1-tap **"Purge Audit History"** command that instantly clears all historical scan records, timeline events, and network traffic entries from the Room database.
2. **Standard Application Uninstallation:** Because all state is confined to the application's private sandbox directory (`/data/user/0/com.example/`), uninstalling MobiArmour causes the Android operating system to completely delete all local database tables, shared preferences, and configuration caches.
