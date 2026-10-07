# MobiArmour Platform — Zero-Trust Security Architecture & Threat Defense

**Document Version:** 3.0.0-ZeroTrustSpec  
**Classification:** Information Security Architecture  
**Scope:** Device-Level Micro-Segmentation, Identity Verification, and Network Containment  

---

## 1. Zero-Trust Security Philosophy

MobiArmour treats every subsystem, application, process, and network connection as inherently untrusted. The platform enforces the three foundational pillars of Zero Trust across mobile endpoints:

1. **Verify Explicitly:** Always authenticate and authorize based on all available data points (app signature, certificate hash, permissions, target SDK, and behavior).
2. **Use Least Privilege Access:** Constrain app capabilities through permission auditing, background monitoring, and per-app firewall sandboxing.
3. **Assume Breach:** Design defensive components (such as the VPN drop sink and out-of-band SMS emergency lockdown) assuming that local networks or untrusted applications are already compromised.

```
       +-------------------------------------------------------------+
       |               Zero-Trust Verification Pipeline              |
       +-------------------------------------------------------------+
                                      |
            +-------------------------+-------------------------+
            |                                                   |
            v                                                   v
+-----------------------+                           +-----------------------+
|  Static Verification  |                           |  Runtime Sandboxing   |
| - Certificate Signing |                           | - Local VpnService    |
| - DEX Architecture    |                           | - Per-App Drop Sink   |
| - Manifest Cleartext  |                           | - Zero Inbound/Out    |
+-----------------------+                           +-----------------------+
            |                                                   |
            +-------------------------+-------------------------+
                                      |
                                      v
                        +---------------------------+
                        |  Out-of-Band Verification |
                        | - Allowlisted SMS C2      |
                        | - SHA-256 Auth Hashing    |
                        | - Anti-Spoofing Parser    |
                        +---------------------------+
```

---

## 2. Micro-Segmentation & Application Containment

### 2.1 The Virtual VPN Drop Sink
Instead of relying on remote corporate firewalls or cloud proxy tunnels, MobiArmour instantiates an on-device Linux `tun0` virtual network interface.
- Untrusted or telemetry-heavy applications are assigned to the drop sink via `VpnService.Builder.addAllowedApplication()`.
- Network packets emitted by quarantined applications are routed directly into the local TUN file descriptor where the background reader discards them without forwarding to physical network interfaces (Wi-Fi or Cellular).
- Unquarantined applications bypass the virtual tunnel, preserving line-rate speeds and low latency for legitimate software.

### 2.2 Device Air-Gap Protocol
When severe threat conditions or physical theft are detected:
- The user or remote security officer can engage the **Device Air-Gap**.
- All IPv4 (`0.0.0.0/0`) and IPv6 (`::/0`) routing is bound to the local sink, isolating all device software from external telemetry and command-and-control exfiltration channels.

---

## 3. Cryptographic Identity & Out-of-Band Command Defense

### 3.1 SMS Emergency Channel Authentication
Because cellular SMS operates outside internet TCP/IP stacks, it serves as an emergency channel when data connectivity is compromised. To prevent unauthorized attackers from spoofing emergency commands:
- **Allowlist Verification:** Sender phone numbers are normalized (removing formatting artifacts, hyphens, and whitespace) and verified against the local encrypted SQLite database table `trusted_devices`.
- **Command Token Syntax:** Commands require strict prefix matching (`#`, `/`, `!`).
- **PIN/Nonce Authentication:** The parser supports token arguments (`#lockdown <pin_or_nonce>`) ensuring that even caller-ID spoofed SMS cannot trigger lockdown unless the sender possesses the pre-shared secret.
- **Conversational Rejection:** Natural language sentences containing words like "lockdown" or "siren" are strictly discarded to eliminate false positives.

---

## 4. Anti-Tamper & Integrity Protection Controls

| Layer | Threat Vector | Implemented Protective Control |
|---|---|---|
| **OS Integrity** | Root escalation & su binaries | Probes known su binary paths, test-keys build tags, and SELinux status. |
| **Settings** | Hostile ADB / Developer debugging | Detects enabled USB debugging and alerts the user to close remote debug shells. |
| **Storage** | Forensic data extraction | Zero sensitive data on external storage; all data stored in internal app sandbox `/data/user/0/`. |
| **Process** | Receiver kill during execution | BroadcastReceiver utilizes `goAsync()` ensuring task completion before thread termination. |
| **App Impersonation** | Phishing & Lookalike domains | PhishingGate heuristic engine evaluates homographs, high subdomain counts, and suspect ports. |
