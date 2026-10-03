# MobiArmour Platform — Operational Threat Defense & Incident Remediation Guide

**Document Version:** 3.0.0-OpsManual  
**Audience:** Security Analysts, Incident Handlers, Power Users, Enterprise Administrators  
**Classification:** Operational Runbook  

---

## 1. Application Threat Analysis & Guided Remediation

### 1.1 Stealth & Hidden Application Profiles
**Definition:** Non-system packages registered in Android's package database that maintain background services, receivers (`RECEIVE_BOOT_COMPLETED`), or dangerous telemetry permissions (SMS, Camera, Mic, Background Location) while deliberately omitting `Intent.CATEGORY_LAUNCHER` from all declared activities.

**Operational Risk:** These applications do not appear on the user's home screen launcher or standard app grid, allowing background monitoring, covert telemetry, or adware delivery without casual user visibility.

**Heuristic Remediation Workflow:**
1. Navigate to **Application Auditor** (`AppsScreen`).
2. The **Threat Action Center** will aggregate and flag all detected stealth packages.
3. Review the forensic indicators:
   - Package UID and target SDK
   - Granted dangerous permissions
   - Absence of launcher intent filter
   - X.509 certificate signing authority
4. Initiate remediation:
   - **For Third-Party Packages:** Tap **"UNINSTALL"**. MobiArmour dispatches `Intent.ACTION_DELETE` (`package:<target_pkg>`), routing the request to Android's official system package installer. Confirm the prompt to permanently purge the binary.
   - **For System-Resident Packages:** Android's security architecture prohibits third-party unprivileged apps from deleting system partition binaries. Tap **"SETTINGS (SYSTEM)"** to open the OS Application Details screen and select **"Disable"** or **"Force Stop"**.
   - **Immediate Network Quarantine:** Tap **"BLOCK NET"** to immediately sever outbound network routing for the package via MobiArmour's local firewall.

### 1.2 User Whitelist & Trust Management
If an identified package is a custom development build, internal testing tool, or specialized background plugin known to be legitimate:
- Tap **"TRUST"** to register the package in the local `TrustedAppEntity` database.
- Trusted packages are exempted from critical severity scoring and highlighted with a **"TRUSTED DEV"** badge.

---

## 2. Local Network Control & Software-Enforced Isolation

### 2.1 Selective Per-App Firewall Mode
- **Mechanism:** MobiArmour establishes a virtual network interface (`tun0`) through Android's `VpnService`. Using `VpnService.Builder.addAllowedApplication()`, only packages explicitly marked as *Blocked* are assigned to the virtual interface.
- **Traffic Handling:** Outbound packets from blocked packages entering `tun0` are routed into a null sink (`10.0.0.2/32`) and discarded. Non-blocked traffic bypasses the VPN interface entirely, flowing directly through standard OS network sockets to preserve native throughput.
- **Auditing:** Dropped connection attempts (destination IP, port, protocol, timestamp) are recorded in the local Room database for forensic inspection.

### 2.2 Software-Enforced Network Isolation ("Software Air-Gap Mode")
- **Operational Scope:** Emergency containment scenario (e.g., active C2 data exfiltration, unverified malware execution, or untrusted captive portal environment).
- **Technical Behavior:** Captures all IPv4 (`0.0.0.0/0`) and IPv6 (`::/0`) routing scopes into the local null-sink interface, preventing network egress across all installed applications.
- **Important Note:** This is software-enforced routing isolation. It must not be confused with physical RF decoupling. Hardware radios remain powered unless manually placed into Airplane Mode or RF shielding.

---

## 3. Out-of-Band Remote Anti-Theft & Lockdown Procedures

### 3.1 Prerequisite Configuration
1. Open the **Lost Device / Remote Recovery** console.
2. Under **Trusted Contacts**, register the authorized telephone number(s) permitted to dispatch emergency recovery commands.
3. Under **Device Administrator**, grant administrator privileges to allow programmatic display locking via `DevicePolicyManager.lockNow()`.

### 3.2 Command Protocol (Dispatched via SMS)
Authorized senders transmit single-line commands via standard SMS:

| Command | Action Executed | Response / Outcome |
|---|---|---|
| `#lockdown` or `/lockdown` | Calls `DevicePolicyManager.lockNow()`, activates audible emergency siren, queries GPS fix. | Immediate screen lock; SMS reply dispatched with Google Maps coordinate link. |
| `#siren` or `/siren` | Sounds maximum volume acoustic locator beacon overriding system silent/vibrate profiles. | Continuous audible beacon until silenced. |
| `#location` or `/location` | Queries device location providers (`GPS_PROVIDER`, `NETWORK_PROVIDER`). | Dispatches SMS response containing latitude, longitude, and accuracy radius. |
| `#silence` | Deactivates active acoustic siren. | Siren stopped; device remains locked. |

### 3.3 Known Failure Modes & Defensive Boundaries
- **SIM Ejection / Airplane Mode:** If an adversary immediately removes the SIM card or places the device in an RF enclosure, SMS commands cannot be received.
- **Battery Saver / Doze Throttling:** On Android 10+ devices, extreme battery saver modes may delay background broadcast processing until the device is awakened.
- **Indoor Location Shielding:** GPS satellite acquisition may fail inside concrete buildings, falling back to lower-accuracy cell tower triangulation if available.
