# MobiArmour Platform — System Boundaries, Technical Limitations & Architectural Blind Spots

**Document Version:** 3.0.0-LimitationsSpec  
**Classification:** Defensive Engineering Governance Document  
**Audience:** Security Auditors, Enterprise Risk Officers, Technical Evaluators  

---

## 1. The Principle of Architectural Transparency

A hallmark of a credible security engineering project is an uncompromising, exhaustive disclosure of its operational limitations. MobiArmour explicitly rejects the deceptive marketing prevalent in consumer mobile utilities that promise "100% complete antivirus protection" or "guaranteed unhackable devices."

Operating within the standard, unprivileged Android userspace sandbox imposes concrete security boundaries governed by the Linux kernel and the Android Open Source Project (AOSP) security architecture.

---

## 2. Explicit System Blind Spots & Boundaries

### 2.1 Kernel-Level Rootkits & Privilege Escalation Exploits
- **Limitation:** MobiArmour runs as an unprivileged userspace process (`UID >= 10000`). If an adversary exploits a Linux kernel vulnerability or a vendor driver flaw to achieve root execution (`UID 0`), the malicious code can hook system calls, falsify `/proc` and `/sys` filesystem representations, and conceal processes from `PackageManager`.
- **Architectural Reality:** MobiArmour can identify common, unhidden SU binaries (`/system/bin/su`, `/system/xbin/su`) and known root management packages, but it **cannot guarantee detection or removal of advanced kernel-level rootkits or bootloader implants**.

### 2.2 Private Application Sandboxes
- **Limitation:** Linux kernel UID separation prevents MobiArmour from reading, scanning, or modifying files residing in other applications' private directories (`/data/user/0/<other_pkg>/`).
- **Architectural Reality:** If a malicious application stores an encrypted payload, stolen session tokens, or cached keystrokes inside its private sandbox, MobiArmour cannot inspect that data. Inspection is strictly confined to public APK container files (`/data/app/*/*.apk`) and manifest declarations.

### 2.3 Plaintext HTTPS / Encrypted TLS Inspection
- **Limitation:** As a layer 3/4 packet filter built on `VpnService`, MobiArmour observes network packet headers: source and destination IP addresses, transport protocols (TCP/UDP), and destination port numbers.
- **Architectural Reality:** MobiArmour **does not and cannot decrypt TLS 1.2 / TLS 1.3 encrypted payloads** without performing active Man-in-the-Middle (MITM) proxying and requiring users to install a custom root Certificate Authority (CA) certificate. Even with a custom CA, modern Android applications enforce Network Security Config pins that reject user-installed CAs.

### 2.4 In-Memory Dynamic Code Loading (DCL)
- **Limitation:** Advanced malware can download an encrypted blob from a C2 server, decrypt it purely in memory, and load it dynamically using `InMemoryDexClassLoader` without writing a `.dex` file to disk.
- **Architectural Reality:** Static analysis of the installed APK will reveal only the initial dropper code or generic loader framework. Without an active, isolated runtime instrumentation sandbox (which requires root or emulation), memory-only execution paths cannot be fully mapped.

### 2.5 Software-Enforced Isolation vs. Physical Air Gaps
- **Limitation:** MobiArmour's "Software Air-Gap Mode" is a software-enforced routing barrier created by assigning universal IP routes (`0.0.0.0/0`, `::/0`) to a local null-sink virtual network interface (`tun0`).
- **Architectural Reality:** It is **NOT a physical air gap**. Physical transceivers (Wi-Fi chipsets, Cellular basebands, Bluetooth controllers, NFC coils, GPS antennas) remain powered and active at the hardware layer. Physical isolation requires powering off device radios or placing the hardware into a verified RF Faraday enclosure.

### 2.6 Out-of-Band Remote Anti-Theft & GPS Guarantees
- **Limitation:** The SMS emergency C2 recovery channel relies on cellular carrier infrastructure, functional SIM card connectivity, and device power.
- **Architectural Reality:**
  - If a thief immediately ejects the SIM card, places the device in an RF shielded container, or turns off the device, SMS commands cannot be received.
  - GPS coordinate acquisition relies on line-of-sight satellite reception and device location hardware enablement. If the device is inside a deep subterranean structure or location hardware is disabled, coordinates cannot be acquired.
  - On Android 10+, aggressive background battery management policies can delay broadcast receiver invocation.

### 2.7 Silent Application Uninstallation
- **Limitation:** In the Android security architecture, an unprivileged third-party app cannot silently remove other applications from the device without user interaction.
- **Architectural Reality:** MobiArmour initiates uninstallation via `Intent.ACTION_DELETE`, which presents the user with the operating system's native confirmation dialog. The user must manually confirm uninstallation.

### 2.8 Single Active VpnService Limitation
- **Limitation:** The Android OS permits only **one** application to hold an active `VpnService` interface at any given time.
- **Architectural Reality:** Enabling MobiArmour's firewall will immediately terminate any existing corporate VPN (e.g., AnyConnect, WireGuard, OpenVPN). Users cannot run a commercial anonymity VPN and MobiArmour's firewall concurrently.
