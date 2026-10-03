# MobiArmour Platform — Multi-Phase Engineering Roadmap

**Document Version:** 3.0.0-RoadmapSpec  
**Classification:** Long-Term Engineering Strategy  
**Strategic Goal:** Transform MobiArmour from an on-device Android defensive utility into a comprehensive, enterprise-grade mobile security platform.  

---

## 1. Architectural Evolution Path

```
+------------------------------------------------------------------------------------+
| PHASE 1: SYSTEM FOUNDATION & STATIC ANALYSIS BASELINE                              |
| - Device Security Posture Engine (SELinux, Keyguard, ADB, Patch Audit)            |
| - Package Inventory & Permissions Categorization                                   |
| - On-Device Static APK Container Inspection (ZipFile, Multi-DEX, Native .so)       |
| - Local Offline SQLite Persistence (Android Room v3)                               |
| - Base Security Event Timeline                                                     |
+-----------------------------------------+------------------------------------------+
                                          | [STATUS: COMPLETE / IMPLEMENTED]
                                          v
+------------------------------------------------------------------------------------+
| PHASE 2: NETWORK ENFORCEMENT & TRAFFIC ISOLATION                                   |
| - VpnService Kernel-Level Packet Interception Engine                              |
| - Selective Per-Application Routing (Zero Overhead for Unblocked Apps)             |
| - Software-Enforced Network Isolation ("Software Air-Gap Mode")                   |
| - Packet Drop Logging & Local Network Forensics                                    |
| - 1-Tap Threat Quarantine Console                                                  |
+-----------------------------------------+------------------------------------------+
                                          | [STATUS: COMPLETE / IMPLEMENTED]
                                          v
+------------------------------------------------------------------------------------+
| PHASE 3: CORRELATION & SECURITY INTELLIGENCE                                       |
| - Multi-Signal Correlation Matrix (Permission × Background Persistence × Network)  |
| - Incident Generation & Threat Contextualization Engine                            |
| - Human-Readable Explainable Risk Diagnostic Reports                               |
| - Historical Threat Drift & Posture Degradation Tracking                           |
+-----------------------------------------+------------------------------------------+
                                          | [STATUS: IN PROGRESS / PARTIALLY ACTIVE]
                                          v
+------------------------------------------------------------------------------------+
| PHASE 4: RESILIENT RECOVERY & OUT-OF-BAND C2                                       |
| - Authenticated SMS Emergency Command & Control                                    |
| - Device Administrator Display Lockdown & Keystore Isolation                       |
| - High-Decibel Acoustic Locator Beacon (Audio Stream Override)                     |
| - Emergency Real-Time GPS Telemetry Dispatch                                       |
| - Automated Fallback Workflows for Connectivity-Severed Devices                    |
+-----------------------------------------+------------------------------------------+
                                          | [STATUS: COMPLETE / IMPLEMENTED]
                                          v
+------------------------------------------------------------------------------------+
| PHASE 5: ADVANCED BYTECODE FORENSICS & STATIC HEURISTICS                           |
| - Smali/Bytecode Disassembly & AST Representation                                  |
| - Control-Flow Graph (CFG) Construction for High-Risk Method Blocks                |
| - Local YARA-Compatible Pattern Matching Engine for DEX Bytecode                   |
| - Privacy-Preserving Cloud Threat Intelligence Sync (Opt-In k-Anonymity Hashes)    |
| - Controlled On-Device Sandbox Research (Emulated Execution Tracing)               |
+-----------------------------------------+------------------------------------------+
                                          | [STATUS: RESEARCH & ARCHITECTURAL DESIGN]
                                          v
+------------------------------------------------------------------------------------+
| PHASE 6: ENTERPRISE FLEET SECURITY & ZERO-TRUST GOVERNANCE                         |
| - Centralized Policy Management for Android Enterprise / BYOD Fleets               |
| - Hardware-Backed Key Attestation Integration (Android Keystore / StrongBox)       |
| - Mutual TLS (mTLS) Cryptographic Device Trust Scoring                             |
| - Cross-Platform Incident Export (SIEM / Syslog Integration via STIX/TAXII)        |
| - Fleet-Wide Emergency Network Isolation Command Broadcast                        |
+------------------------------------------------------------------------------------+
                                            [STATUS: LONG-TERM ROADMAP]
```

---

## 2. Detailed Milestone Specifications

### Phase 1: System Foundation & Static Analysis Baseline
- **Milestone 1.1:** Device Security Posture Engine auditing OS patch dates, storage encryption, debug flags, developer options, and root presence indicators.
- **Milestone 1.2:** Package auditor indexing user vs. system packages, target SDK thresholds, debuggable build configurations, and exported components.
- **Milestone 1.3:** Static container inspector parsing APK archives via `java.util.zip.ZipFile`, extracting DEX file counts, and cataloging native dynamic libraries (`.so`).
- **Milestone 1.4:** Room database schema v3 persisting scan results, events, and rules offline.

### Phase 2: Network Enforcement & Traffic Isolation
- **Milestone 2.1:** Implement Android `VpnService` with selective routing (`addAllowedApplication`).
- **Milestone 2.2:** Build Software-Enforced Network Isolation ("Software Air-Gap") null-sink capturing `0.0.0.0/0` and `::/0`.
- **Milestone 2.3:** Network traffic logging engine recording dropped egress connections to SQLite.
- **Milestone 2.4:** 1-Tap Threat Action Center providing direct package uninstallation bridges and quarantine toggles.

### Phase 3: Security Intelligence & Incident Generation
- **Milestone 3.1:** Heuristic correlation matrix synthesizing multi-vector risk indicators (e.g., Background Persistence + Sensitive Telemetry + Obfuscated Signature).
- **Milestone 3.2:** Explainable risk reporting delivering human-readable diagnostic summaries to replace opaque single-number ratings.
- **Milestone 3.3:** Event timeline viewer with category-based filtering, severity badges, and audit export capabilities.

### Phase 4: Resilient Recovery & Out-of-Band C2
- **Milestone 4.1:** High-priority SMS broadcast receiver parsing authenticated commands (`#lockdown`, `#siren`, `#location`, `#silence`).
- **Milestone 4.2:** Cryptographic whitelist registry for trusted emergency sender telephone numbers.
- **Milestone 4.3:** Device administrator integration for instant screen locking.
- **Milestone 4.4:** Real-time location fix acquisition and SMS response dispatch.

### Phase 5: Advanced Bytecode Forensics & Research Capabilities
- **Milestone 5.1 (Research):** On-device parsing of DEX header byte arrays to construct Control-Flow Graphs (CFG) for suspicious methods.
- **Milestone 5.2 (Research):** Local YARA-compatible pattern evaluator matching compiled regex rules against DEX string pools.
- **Milestone 5.3 (Roadmap):** Privacy-preserving opt-in hash checking against open-source threat intelligence feeds using k-anonymity prefixes.

### Phase 6: Enterprise Fleet Security & Zero-Trust Governance
- **Milestone 6.1 (Roadmap):** Enterprise policy ingestion allowing organizational IT administrators to push baseline security configurations to managed devices.
- **Milestone 6.2 (Roadmap):** Continuous device trust evaluation integrating hardware-backed Key Attestation (`KeyGenParameterSpec.Builder.setAttestationChallenge`).
- **Milestone 6.3 (Roadmap):** Integration with the broader OmniIntell Labs security suite for synchronized mobile and endpoint threat posture assessment.
