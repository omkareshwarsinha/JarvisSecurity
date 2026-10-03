# MobiArmour Platform — Performance Targets, Engineering Budgets & Benchmarking Methodology

**Document Version:** 3.0.0-BenchmarkSpec  
**Classification:** Performance Engineering Reference  
**Audience:** Performance Engineers, Systems Developers, QA Engineers  

---

## 1. Engineering Philosophy: Measurable Targets over Absolute Claims

Conventional utility applications routinely market fictitious performance claims such as "zero-latency firewall," "100% bandwidth preservation," and "zero battery consumption." From an operating system and networking perspective, every instruction executed, every context switch, and every network interface transition consumes non-zero CPU cycles and electrical energy.

MobiArmour replaces unsupported absolute claims with:
1. **Explicit Engineering Targets & Budgets:** Quantified operational envelopes for memory, CPU, latency, and battery.
2. **Standardized Benchmarking Methodologies:** Repeatable test procedures using Android Jetpack Macrobenchmark, Linux `iperf3`, and battery historian tooling.
3. **Architectural Optimizations:** Designing subsystems (e.g., Selective Per-App Routing) specifically to minimize userspace copy overhead on unblocked traffic.

---

## 2. Quantitative Performance Targets & Budgets

| Metric Dimension | Engineering Target Budget | Measurement Method | Failure Threshold (Regression) |
|---|---|---|---|
| **Cold Startup Time** | `< 600 ms` to first interactive frame | `MacrobenchmarkRule.measureRepeated(StartupTimingMetric())` | `> 1000 ms` on mid-tier ARM64 |
| **Warm Startup Time** | `< 250 ms` to restored state | Activity lifecycle resume measurement | `> 500 ms` |
| **Resident Memory (RSS) - Idle** | `< 45 MB` | Android Studio Memory Profiler / `ActivityManager.MemoryInfo` | `> 75 MB` |
| **Resident Memory (RSS) - Active Audit** | `< 85 MB` during full APK multi-DEX parse | `Debug.getNativeHeapAllocatedSize()` + JVM heap metrics | `> 140 MB` |
| **UI Rendering Frame Rate** | Consistent 60 / 120 FPS (`0` janky frames in `LazyColumn`) | Android FrameMetrics API / Jetpack Compose Layout Inspector | `> 5%` dropped frames |
| **Firewall Throughput (Unblocked Apps)** | `>= 98%` of baseline direct socket throughput | `iperf3` TCP/UDP throughput against local 10 GbE test server | `< 90%` of baseline |
| **Firewall Latency Overhead (Unblocked)** | `< 2 ms` additional round-trip time (RTT) | ICMP / TCP ping latency against baseline router | `> 5 ms` added latency |
| **Firewall Packet-Processing (Blocked Apps)** | Immediate kernel drop (`< 0.1 ms` packet drop time) | VpnService file descriptor read-and-discard loop profiling | Buffer overflow / queue stall |
| **Battery Consumption (Background Idle)** | `< 0.8%` battery discharge per 24-hour cycle | Android Battery Historian v2.1 analysis | `> 2.5%` per 24 hours |
| **Battery Consumption (Firewall Active)** | `< 1.8%` additional discharge per 24 hours under normal use | Battery Historian foreground service power calculation | `> 4.0%` per 24 hours |
| **VPN Reconnect / Rebuild Time** | `< 120 ms` on rule update without interface restart | `JarvisFirewallService.rebuildVpn()` execution timer | `> 350 ms` |

---

## 3. Benchmarking Methodology & Test Protocols

### 3.1 Network Throughput & Latency Protocol (Selective Routing Benchmark)
- **Objective:** Measure the impact of `JarvisFirewallService` on both unblocked applications and blocked applications.
- **Test Topology:**
  ```
  [Device Under Test (DUT)] <---- Wi-Fi 6 (802.11ax) ----> [Controlled LAN Server running iperf3]
  ```
- **Test Steps:**
  1. *Baseline Run:* Execute 60-second `iperf3 -c <server_ip> -t 60 -i 1` with MobiArmour Firewall disabled. Record average throughput (Mbps), jitter (ms), and packet loss (%).
  2. *Selective Firewall Run (Unblocked):* Enable MobiArmour Firewall with 10 unrelated applications blocked. Execute identical `iperf3` test from an unblocked terminal app. Compare metrics against baseline.
  3. *Selective Firewall Run (Blocked):* Add the benchmark application to the blocked list. Attempt connection. Assert 100% packet drop at zero external packet egress.
  4. *Software Air-Gap Run:* Activate emergency isolation mode. Attempt connection from any package. Assert 100% packet capture into null sink.

### 3.2 Full-System Static Audit Protocol
- **Objective:** Quantify CPU and memory consumption while scanning devices with 150+ installed applications.
- **Test Protocol:**
  1. Populate test device with a standardized corpus of 150 installed APKs (comprising system apps, multi-DEX packages, large native libraries, and synthetic trojans).
  2. Trigger `PackageAuditor.auditInstalledApplications()`.
  3. Monitor CPU core frequencies and thermal throttling metrics via `dumpsys cpuinfo` and `/sys/class/thermal/`.
  4. Measure total audit completion duration (Target: `< 4.5 seconds` on Snapdragon 7-series / Dimensity 8000 class hardware).

---

## 4. Empirical Validation Notice

Numerical performance metrics published in release changelogs are populated exclusively after controlled execution on certified physical hardware test benches. Results vary across device manufacturers, Linux kernel versions, and storage controller bus architectures (UFS 3.1 vs. eMMC 5.1).
