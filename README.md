# IBM App Connect Enterprise — IBM i (AS/400) Nodes

IBM App Connect Enterprise (ACE) v12 / v13 integration library for IBM i (AS/400, iSeries) systems. Provides native, high-performance message flow nodes for interacting directly with IBM i programs (`*PGM`) and Data Queues (`*DTAQ`) using IBM Toolbox for Java (JTOpen).

---

## Overview

This library provides four custom nodes that appear under the **iSeries** drawer in the ACE Toolkit palette:

| Node | Type | Description | Terminals |
| :--- | :--- | :--- | :--- |
| **DataQueueInput** | Input | Event-driven input node that waits for and retrieves messages from an IBM i FIFO or Keyed Data Queue (`*DTAQ`). Starts a message flow when an entry arrives. | **Out**: `out`, `failure`<br>*(No in terminal)* |
| **DataQueueGet** | Mid-Flow | Polls or retrieves a message from a Data Queue mid-flow using either FIFO order or key-based lookup. | **In**: `in`<br>**Out**: `out`, `noMessage`, `failure` |
| **DataQueueOutput** | Mid-Flow | Serializes and writes a message payload to an IBM i FIFO or Keyed Data Queue. | **In**: `in`<br>**Out**: `out`, `failure` |
| **ISeriesPcmlCall** | Mid-Flow | Invokes native IBM i programs (`*PGM`) using Program Call Markup Language (PCML / XPCML), transforming XML/JSON inputs to program parameters and mapping results back. | **In**: `in`<br>**Out**: `out`, `failure` |

All nodes communicate through managed connection pools to minimize login and handshake overhead.

---

## Installation

### 1. ACE Runtime Extension

Deploy the runtime plugin archive (`ISeriesImpl.par` or `ace-iseries-13.par`) to your ACE integration server:

1. Place the PAR file into a dedicated directory on the ACE server host (e.g. `/var/mqsi/extensions/iseries/` or an external extensions directory).
2. Register the directory in your integration server's `server.conf.yaml`:

```yaml
Defaults:
  lilPath: '/var/mqsi/extensions/iseries'
```

3. Restart the integration server. The runtime automatically discovers the extension and loads its bundled JTOpen drivers.

> **Note:** The PAR archive is completely self-contained with its required JTOpen dependencies. Do not install duplicate JTOpen jars into server `shared-classes` or global classpath.

### 2. ACE Toolkit Plug-in

To design message flows in the ACE Toolkit:

1. Copy `ISeries_13.0.0.jar` into your Toolkit `dropins/` folder:
   - Linux: `$ACE_HOME/tools/dropins/`
   - Windows: `<install_dir>\tools\dropins\`
2. Restart Toolkit with a clean cache:
   - Linux: `$ACE_HOME/tools/eclipse -clean -initialize`
   - Windows: `<install_dir>\tools\eclipse.exe -clean -initialize`
3. Launch Toolkit. The **iSeries** category will appear in the node palette.

### 3. PCML Stylesheets (For `ISeriesPcmlCall`)

If using `ISeriesPcmlCall`, place the required XSL transformation stylesheets (`transformToPcml.xsl`, `transformToXPcml.xsl`, `transformFromXPcml.xsl`) into the server's working directory or configure an absolute path in the node properties.

---

## Connection Policy Configuration

Connection pooling and credentials are configured via an ACE **UserDefined Policy** inside a Policy Project.

### Policy Project & Descriptor

Create a Policy Project (e.g., `CommonPP` or `ISeriesPolicies`) containing a policy file with type `UserDefined`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<policies>
  <policy policyType="UserDefined" policyName="myISeries" policyTemplate="UserDefined">
    <iSeriesAddress>iseries.example.com</iSeriesAddress>
    <iSeriesUser>MYUSER</iSeriesUser>
    <iSeriesPassword>MYPASSWORD</iSeriesPassword>
    <maxConnections>10</maxConnections>
    <minConnections>1</minConnections>
    <CCSID>37</CCSID>
    <maxUseTimeout>-1</maxUseTimeout>
  </policy>
</policies>
```

### Policy Properties

| Property | Description | Recommended Default |
| :--- | :--- | :--- |
| `iSeriesAddress` | Hostname or IP address of the IBM i system. | *Required* |
| `iSeriesUser` | IBM i user profile with authority to the target library, program, and data queue. | *Required* |
| `iSeriesPassword` | User password. *(Keep policies secure or restrict access).* | *Required* |
| `CCSID` | Character Set Identifier for IBM i data exchange (e.g., `37` for US/Canada EBCDIC, `273` for Germany/Austria, `1208` for UTF-8, or `0` for system autodetect). | `37` |
| `maxConnections` | Maximum number of simultaneous connections maintained in the pool. | `10` |
| `minConnections` | Minimum warm connections kept open in the pool (`0` avoids eager connection on start). | `0` or `1` |
| `maxUseTimeout` | Lifetime limit in milliseconds before recycling an active connection (`-1` = unlimited). | `-1` |

### Referencing Policies in Nodes

In the node property view (**Connection** tab), enter the policy reference in the format:
```text
{PolicyProjectName}:PolicyName
```
Example: `{CommonPP}:pub400` or `{ISeriesPolicies}:myISeries`.

---

## IBM i Data Queue CCSID & Encoding

IBM i Data Queues (`*DTAQ`) store arbitrary binary byte streams without storing native CCSID metadata. Native IBM i applications (RPG, COBOL, CL) write and read records in the host job's single-byte EBCDIC CCSID:

- **United States & International Standard:** **CCSID 37** (`IBM-37`, EBCDIC).
- **Germany / Austria:** **CCSID 273**.
- **Web / XML / Modern Open:** **CCSID 1208** (UTF-8).
- **Auto-Detect:** Set `CCSID: 0` in the policy to inherit the default CCSID directly from the connected IBM i system.

### How the Nodes Handle CCSID

1. **`DataQueueOutput`**: Serializes the outgoing message tree using the policy's configured CCSID. If sending characters or XML, ACE encodes them to the specified CCSID (e.g., EBCDIC 37).
2. **`DataQueueInput`**: Emits received raw bytes under `OutputRoot.BLOB.BLOB` and sets `OutputRoot.Properties.CodedCharSetId` to the policy CCSID. Downstream parsers (JSON, XMLNSC, MRM) automatically decode the payload using this CCSID.
3. **`KeyedDataQueue`**: String search keys are automatically encoded and space-padded to the required key length using the connection CCSID.

---

## Node Reference

### 1. DataQueueInput

Event listener node. Continuously polls an IBM i data queue and emits arriving records into the flow.

- **Palette**: iSeries -> DataQueueInput
- **Input Terminals**: None (Input node)
- **Output Terminals**:
  - `out`: Successfully read message payload (`BLOB`).
  - `failure`: Routed if downstream processing fails or record cannot be propagated.
- **Key Properties**:
  - `Connection Policy`: Policy reference (`{Project}:Name`).
  - `Queue Name`: Target `*DTAQ` object name.
  - `Library`: Library containing the data queue.
  - `Wait Interval`: Read timeout in seconds (`0` or `1` for periodic poll; `-1` for bounded wait).
  - `Keyed`: Check if the target is a Keyed Data Queue (`*KEYED`).
  - `Key`: Key value filter (when `Keyed` is enabled).
  - `Search Type`: `EQ`, `NE`, `LT`, `LE`, `GT`, `GE`.

---

### 2. DataQueueGet

Mid-flow node for retrieving or polling a record on demand.

- **Palette**: iSeries -> DataQueueGet
- **Input Terminals**: `in`
- **Output Terminals**:
  - `out`: Message retrieved successfully.
  - `noMessage`: Queue was empty or wait interval expired without a message.
  - `failure`: Error during retrieval.
- **Result Location**: The retrieved byte payload is placed in:
  ```text
  LocalEnvironment.DataQueueResult.Data
  ```
  Original incoming message body and headers are preserved and propagated to `out`.

---

### 3. DataQueueOutput

Writes a message to a Data Queue.

- **Palette**: iSeries -> DataQueueOutput
- **Input Terminals**: `in`
- **Output Terminals**:
  - `out`: Message written successfully.
  - `failure`: Error writing to data queue.
- **Payload Handling**: Serializes the message body (e.g. `XMLNSC`, `JSON`, `BLOB`) into the CCSID specified in the connection policy before writing to the queue.

---

### 4. ISeriesPcmlCall

Invokes an IBM i program (`*PGM`) synchronously.

- **Palette**: iSeries -> ISeriesPcmlCall
- **Input Terminals**: `in`
- **Output Terminals**: `out`, `failure`
- **Key Properties**:
  - `Connection Policy`: Policy reference (`{Project}:Name`).
  - `Program Call / PCML`: PCML definition file or program specifier.
  - `Library List`: Comma-separated list of libraries added to `*LIBL` for the call.
  - `CCSID`: Encoding for PCML parameters (`1208` for UTF-8 default).
  - `To PCML XSL`: Stylesheet to convert input XML to PCML.
  - `From PCML XSL`: Stylesheet to convert program output PCML back to flow XML/JSON.

---

## Message Flow Patterns & ESQL Examples

### Pattern 1: Asynchronous Event Consumer (`DataQueueInput`)

A flow triggered whenever an IBM i background job writes to `ARSON55011/ACE_EVT`:

```
[DataQueueInput] ---> [Compute: Parse Payload] ---> [Process Event]
```

**Compute Node ESQL:**

```sql
CREATE COMPUTE MODULE ParseEvent_Compute
	CREATE FUNCTION Main() RETURNS BOOLEAN
	BEGIN
		SET OutputRoot.Properties = InputProperties;
		
		-- DataQueueInput puts raw queue bytes in BLOB.BLOB
		-- Properties.CodedCharSetId is set to policy CCSID (e.g. 37)
		DECLARE payloadBlob BLOB InputRoot.BLOB.BLOB;
		
		-- Parse as character string using the inherited CCSID
		DECLARE payloadStr CHAR CAST(payloadBlob AS CHAR CCSID InputProperties.CodedCharSetId);
		
		-- Alternatively parse directly into JSON domain
		CREATE LASTCHILD OF OutputRoot DOMAIN('JSON')
			PARSE(payloadBlob,
			      InputProperties.Encoding,
			      InputProperties.CodedCharSetId);
			      
		RETURN TRUE;
	END;
END MODULE;
```

---

### Pattern 2: Correlated Request-Reply over Keyed Data Queue (`Inquiry`)

Sends an inquiry request to a backend queue, waits for an RPG server program to process it, and retrieves the response using a unique correlation key:

```
[HTTP Input] ---> [Compute: Build Request] ---> [DataQueueOutput]
                                                     |
                                                (writes req)
                                                     |
                                              [DataQueueGet]
                                           (reads keyed resp)
                                                     |
                                          [Compute: Format JSON] ---> [HTTP Reply]
```

**1. Prepare Request & Set Correlation Key:**

```sql
CREATE COMPUTE MODULE PrepareInquiry_Compute
	CREATE FUNCTION Main() RETURNS BOOLEAN
	BEGIN
		SET OutputRoot.Properties = InputProperties;
		
		-- Generate unique 32-character correlation ID
		DECLARE corrId CHAR UUIDASCHAR;
		
		-- Build binary request (e.g. 32-byte CorrID + Account Number)
		DECLARE corrBytes BLOB CAST(corrId AS BLOB CCSID 37);
		DECLARE acctBytes BLOB CAST(InputRoot.JSON.Data.AccountNo AS BLOB CCSID 37);
		SET OutputRoot.BLOB.BLOB = corrBytes || acctBytes;
		
		-- Set key on DataQueueGet dynamically via LocalEnvironment
		SET OutputLocalEnvironment.Destination.ISeries.key = corrId;
		SET OutputLocalEnvironment.Destination.ISeries.waitInterval = 5; -- seconds
		
		RETURN TRUE;
	END;
END MODULE;
```

**2. Parse Retrieved Keyed Response (`DataQueueGet` output):**

```sql
CREATE COMPUTE MODULE ParseInquiryResponse_Compute
	CREATE FUNCTION Main() RETURNS BOOLEAN
	BEGIN
		SET OutputRoot.Properties = InputProperties;
		
		-- Response payload is placed in LocalEnvironment.DataQueueResult.Data
		DECLARE resBlob BLOB InputLocalEnvironment.DataQueueResult.Data;
		
		-- Extract response fields by position (EBCDIC CCSID 37)
		DECLARE responseCode CHAR CAST(SUBSTRING(resBlob FROM 33 FOR 2) AS CHAR CCSID 37);
		DECLARE balance CHAR CAST(SUBSTRING(resBlob FROM 35 FOR 15) AS CHAR CCSID 37);
		DECLARE customerName CHAR TRIM(CAST(SUBSTRING(resBlob FROM 50 FOR 50) AS CHAR CCSID 37));
		
		SET OutputRoot.JSON.Data.Status = responseCode;
		SET OutputRoot.JSON.Data.Balance = balance;
		SET OutputRoot.JSON.Data.Customer = customerName;
		
		RETURN TRUE;
	END;
END MODULE;
```

---

## Dynamic Overrides via `LocalEnvironment`

Message flows can dynamically override node properties at runtime per-message using `LocalEnvironment.Destination.ISeries`:

| Variable Path | Type | Applies To | Description |
| :--- | :--- | :--- | :--- |
| `LocalEnvironment.Destination.ISeries.queueName` | String | Output, Get | Target Data Queue name. |
| `LocalEnvironment.Destination.ISeries.library` | String | Output, Get | Target library name. |
| `LocalEnvironment.Destination.ISeries.key` | String | Output, Get | Lookup key for Keyed Data Queue. |
| `LocalEnvironment.Destination.ISeries.searchType` | String | Get | Key search operator (`EQ`, `NE`, `LT`, `LE`, `GT`, `GE`). |
| `LocalEnvironment.Destination.ISeries.waitInterval` | Integer | Get | Read timeout in seconds (`0` = no wait, `-1` = bounded wait). |
| `LocalEnvironment.Destination.ISeries.pcmlCall` | String | PcmlCall | Override PCML program name. |
| `LocalEnvironment.Destination.ISeries.libraryList` | String | PcmlCall | Dynamic library list (`*LIBL`). |
| `LocalEnvironment.Destination.ISeries.ccsid` | Integer | PcmlCall | Override CCSID for the call. |

---

## Operational Best Practices

1. **Connection Pooling**: Use a sensible `maxConnections` (default 10) in your policy to prevent saturating IBM i job table resources.
2. **Destructive Reads**: Data Queue reads are destructive. Connect the `failure` terminal on `DataQueueInput` to route messages to a dead-letter queue or error handler in case downstream parsing fails.
3. **Correlation ID Uniqueness**: When using Keyed Data Queues for request-reply, always generate high-entropy unique keys (such as `UUIDASCHAR`) to prevent cross-talk between concurrent transactions.
4. **CCSID Matching**: Verify the target AS/400 job and data queue character set. For standard North American AS/400 environments, use CCSID `37`. For European systems, use the appropriate country CCSID (e.g. `273` for German, `285` for UK).
