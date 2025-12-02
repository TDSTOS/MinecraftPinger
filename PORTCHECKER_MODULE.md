# PortChecker Module - Complete Documentation

## Overview

The PortChecker module is a standalone service that scans and tests common Minecraft ports for a given IP address or hostname. It provides comprehensive port scanning capabilities through both CLI and web dashboard interfaces.

## Features

### Core Capabilities

1. **Port Scanning**
   - Test standard Minecraft ports (25565-25569 by default)
   - Scan custom port ranges (1-65535)
   - TCP socket connection testing with configurable timeout
   - Parallel or sequential scanning modes

2. **Result Information**
   - Port number
   - Status (OPEN/CLOSED)
   - Connection latency in milliseconds
   - Detailed error messages for failed connections

3. **Input Validation**
   - IP address validation (IPv4)
   - Hostname validation (DNS format)
   - Port range validation (1-65535)
   - Graceful error handling

## Architecture

### Class Structure

```
PortChecker Module
├── PortChecker.java          (Main service class)
├── PortCheckResult.java      (Result data class)
├── ConfigLoader.java          (Configuration - updated)
├── ConsoleInterface.java      (CLI integration - updated)
├── DashboardServer.java       (Web API - updated)
└── Main.java                  (Initialization - updated)
```

### PortChecker Class

**Main Service**
- Manages port scanning operations
- Handles parallel/sequential execution
- Validates inputs
- Formats results

**Key Methods:**
```java
// Scan default Minecraft ports
List<PortCheckResult> checkDefaultPorts(String ip)

// Scan port range
List<PortCheckResult> checkPorts(String ip, int startPort, int endPort)

// Scan specific port list
List<PortCheckResult> checkPorts(String ip, List<Integer> ports)

// Scan single port
PortCheckResult checkSinglePort(String ip, int port)

// Validation
static boolean isValidIp(String ip)
static boolean isValidHostname(String hostname)
```

### PortCheckResult Class

**Data Container**
- Stores scan results for individual ports
- Provides formatted output methods
- JSON serialization support

**Properties:**
- `String ip` - Target IP/hostname
- `int port` - Port number
- `boolean open` - Port status
- `long latencyMs` - Connection latency
- `String errorMessage` - Error details (if applicable)

**Methods:**
```java
String getStatus()           // Returns "OPEN" or "CLOSED"
String toString()            // Human-readable format
String toTableRow()          // Table-formatted output
String toJson()              // JSON format
```

## Configuration

### config.properties

Add these entries to configure the PortChecker module:

```properties
# Port Checker Configuration
portchecker.defaultPorts=25565,25566,25567,25568,25569
portchecker.scanTimeoutMs=1000
portchecker.parallelChecks=true
```

### Configuration Options

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `portchecker.defaultPorts` | String | 25565,25566,25567,25568,25569 | Comma-separated list of default ports |
| `portchecker.scanTimeoutMs` | Integer | 1000 | Connection timeout in milliseconds |
| `portchecker.parallelChecks` | Boolean | true | Enable parallel port scanning |

## CLI Usage

### Command Syntax

```bash
# Check default Minecraft ports
portcheck <ip|hostname>

# Check custom port range
portcheck <ip|hostname> <startPort> <endPort>
```

### Examples

```bash
# Scan default ports on a server
> portcheck minecraft.example.com

# Scan custom port range
> portcheck 192.168.1.100 25560 25570

# Scan specific range on hostname
> portcheck play.server.net 25500 25600
```

### CLI Output Example

```
Scanning ports 25565-25569 on minecraft.example.com...

Port Scan Results for: minecraft.example.com
============================================================
  PORT     STATUS       LATENCY    MESSAGE
------------------------------------------------------------
  25565    ✓ OPEN       45ms       -
  25566    ✗ CLOSED     1001ms     Connection timeout
  25567    ✗ CLOSED     2ms        Connection refused
  25568    ✗ CLOSED     1001ms     Connection timeout
  25569    ✗ CLOSED     2ms        Connection refused
============================================================
  Total: 5 ports scanned, 1 open, 4 closed
```

## REST API

### Endpoint

```
GET /api/portcheck
```

### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `target` | String | Yes | IP address or hostname to scan |
| `startPort` | Integer | No | Starting port number (1-65535) |
| `endPort` | Integer | No | Ending port number (1-65535) |

**Note:** If `startPort` and `endPort` are omitted, default ports are scanned.

### Request Examples

```bash
# Check default ports
GET /api/portcheck?target=minecraft.example.com

# Check custom range
GET /api/portcheck?target=192.168.1.100&startPort=25560&endPort=25570
```

### Response Format

**Success (200 OK):**
```json
[
  {
    "ip": "minecraft.example.com",
    "port": 25565,
    "open": true,
    "latencyMs": 45,
    "status": "OPEN",
    "errorMessage": ""
  },
  {
    "ip": "minecraft.example.com",
    "port": 25566,
    "open": false,
    "latencyMs": 1001,
    "status": "CLOSED",
    "errorMessage": "Connection timeout"
  }
]
```

**Error Responses:**

```json
// 400 Bad Request - Missing target
{"error": "Missing target parameter"}

// 400 Bad Request - Invalid IP/hostname
{"error": "Invalid IP address or hostname"}

// 400 Bad Request - Invalid port range
{"error": "Port numbers must be between 1 and 65535"}

// 500 Internal Server Error
{"error": "Port scan error: <details>"}
```

## Dashboard Integration

### Access

Navigate to: `http://localhost:8080/portchecker` (planned UI)

### Features

- Input field for target IP/hostname
- Optional port range selection
- Real-time scan progress
- Results table display
- Export results option

### Current Integration

The API endpoint is fully functional. Frontend UI integration can be added to the dashboard HTML.

**Example JavaScript integration:**

```javascript
async function checkPorts(target, startPort, endPort) {
    let url = `/api/portcheck?target=${encodeURIComponent(target)}`;

    if (startPort && endPort) {
        url += `&startPort=${startPort}&endPort=${endPort}`;
    }

    const response = await fetch(url);
    const results = await response.json();

    displayResults(results);
}
```

## Error Handling

### Connection Errors

The module gracefully handles various network errors:

| Error Type | Error Message | Description |
|------------|---------------|-------------|
| Connection Refused | "Connection refused" | Port is closed, service not listening |
| Timeout | "Connection timeout" | No response within timeout period |
| Host Unreachable | "Host unreachable" | Target host cannot be reached |
| Network Unreachable | "Network unreachable" | Network path unavailable |
| No Route | "No route to host" | Routing issue to target |

### Input Validation Errors

- Invalid IP address format
- Invalid hostname format
- Port numbers out of range (1-65535)
- Start port > end port
- Missing required parameters

### Graceful Degradation

- Failed ports don't stop the scan
- Partial results are always returned
- Detailed error messages for debugging
- No crashes on network issues

## Performance Characteristics

### Parallel Mode (Enabled)

- **Speed**: Fast (concurrent connections)
- **Resource Usage**: Higher (multiple threads)
- **Recommended For**: Scanning many ports (>5)
- **Thread Pool**: Max 10 concurrent threads

### Sequential Mode (Disabled)

- **Speed**: Slower (one at a time)
- **Resource Usage**: Lower (single thread)
- **Recommended For**: Scanning few ports (<5)
- **Thread Pool**: None (main thread)

### Timeout Configuration

| Timeout (ms) | Use Case | Trade-off |
|--------------|----------|-----------|
| 500 | Fast networks | May miss slow servers |
| 1000 | Default/Balanced | Good for most cases |
| 1500-2000 | Slow networks | More accurate but slower |
| 5000+ | Very slow/far | Very slow scans |

## Security Considerations

### Built-in Protections

1. **Input Validation**
   - Strict IP/hostname validation
   - Port range enforcement
   - No command injection vectors

2. **Rate Limiting**
   - Maximum 10 parallel threads
   - Configurable timeout prevents resource exhaustion
   - Bounded thread pool

3. **Error Sanitization**
   - Error messages truncated to 40 chars
   - No stack traces exposed to users
   - Safe JSON escaping

### Best Practices

1. **Network Access**
   - PortChecker requires network access
   - May trigger firewall alerts
   - Consider firewall rules for scanned ports

2. **Legal Compliance**
   - Only scan networks you own/control
   - Respect terms of service
   - Port scanning may be restricted in some jurisdictions

3. **Resource Management**
   - Don't scan excessive port ranges
   - Use appropriate timeouts
   - Monitor system resources

## Integration Examples

### Programmatic Usage

```java
// Create PortChecker instance
PortChecker checker = new PortChecker(
    1000,  // timeout ms
    true,  // parallel checks
    Arrays.asList(25565, 25566, 25567)  // default ports
);

// Scan default ports
List<PortCheckResult> results = checker.checkDefaultPorts("minecraft.example.com");

// Scan custom range
List<PortCheckResult> customResults = checker.checkPorts("192.168.1.100", 25560, 25570);

// Check single port
PortCheckResult single = checker.checkSinglePort("play.server.net", 25565);

// Print results
checker.printResults(results);

// Get JSON
String json = checker.resultsToJson(results);
```

### With Configuration

```java
ConfigLoader config = new ConfigLoader("config.properties");

PortChecker checker = new PortChecker(
    config.getPortCheckerScanTimeoutMs(),
    config.isPortCheckerParallelChecks(),
    config.getPortCheckerDefaultPorts()
);

List<PortCheckResult> results = checker.checkDefaultPorts("server.com");
```

## Troubleshooting

### Common Issues

**Issue: All ports show as closed**
- **Cause**: Firewall blocking outbound connections
- **Solution**: Check firewall rules, allow outbound TCP

**Issue: Scans are very slow**
- **Cause**: Timeout too high or parallel mode disabled
- **Solution**: Reduce timeout, enable parallel checks

**Issue: "Invalid IP address" error**
- **Cause**: Hostname requires DNS resolution
- **Solution**: Use hostname validation, check DNS

**Issue: Inconsistent results**
- **Cause**: Network instability
- **Solution**: Increase timeout, run multiple scans

### Debug Tips

1. **Test with known open port**
   ```bash
   portcheck localhost 22  # SSH port if running
   ```

2. **Verify DNS resolution**
   ```bash
   ping minecraft.example.com
   ```

3. **Check firewall logs**
   - Windows: Event Viewer
   - Linux: `/var/log/firewall`

4. **Test with external tools**
   ```bash
   telnet minecraft.example.com 25565
   nc -zv minecraft.example.com 25565
   ```

## Future Enhancements

### Planned Features

1. **UDP Support**
   - Query protocol support
   - Additional Minecraft protocols

2. **Advanced Scanning**
   - Service detection
   - Version detection
   - Banner grabbing

3. **Reporting**
   - Export to CSV/JSON
   - Scan history tracking
   - Trend analysis

4. **UI Improvements**
   - Progress indicators
   - Real-time updates (WebSocket)
   - Scan scheduling

### Not Planned

- Port knocking sequences
- Stealth scanning
- OS fingerprinting
- Vulnerability scanning

## Compatibility

### Requirements

- **Java Version**: Java 8+
- **Network Access**: Outbound TCP required
- **Permissions**: No special permissions needed

### Supported Platforms

- ✅ Windows (all versions)
- ✅ Linux (all distributions)
- ✅ macOS (all versions)
- ✅ Docker containers

### Integration

- ✅ Fully backward compatible
- ✅ No breaking changes
- ✅ Optional feature (can be disabled)
- ✅ Independent module

## Conclusion

The PortChecker module provides robust, efficient port scanning capabilities for Minecraft servers. It integrates seamlessly with the existing application while maintaining clean architecture and full backward compatibility.

For questions or issues, refer to the troubleshooting section or check the inline documentation in the source code.

---

**Module Version**: 1.0.0
**Integration Date**: 2025-11-30
**Maintainer**: Project Team
**Status**: ✅ Production Ready
