# C6461 Simulator — Design Notes

**George Washington University — CS6461: Computer Architectures**
**Part II Deliverable**

---

## 1. Project Overview

This document describes the design decisions, object model, module interfaces, and architectural rationale for the C6461 Machine Simulator.

The simulator models a 16-bit processor based on the C6461 Instruction Set Architecture. It includes:

- A two-pass assembler that translates `.asm` source files to octal machine code
- A 2048-word single-port memory with a fully associative cache (16 lines, FIFO replacement)
- A CPU with a fetch-decode-execute cycle supporting all ISA instructions
- Machine fault handling (reserved address, illegal opcode, out-of-bounds memory)
- Console I/O via IN/OUT instructions with keyboard and printer panels
- Trace logging of every instruction execution to a timestamped log file
- A Swing-based operator console

---

## 2. System Architecture

### 2.1 High-Level Data Flow

```
 .asm source file
        │
        ▼
 ┌──────────────────┐    listing file (.lst)
 │     Encoder      │──────────────────────────►  (human-readable)
 │   (assembler)    │
 └──────────────────┘
        │ load file (.load)
        ▼
 ┌──────────────────┐
 │      Memory      │◄─── write-through on store
 └──────────────────┘
        ▲
        │
 ┌──────────────────┐
 │      Cache       │◄─── CPU reads/writes (16-line, fully associative, FIFO)
 └──────────────────┘
        ▲
        │ loadProgramToMemory()
        │
 ┌──────────────────┐
 │       CPU        │◄─── executeNextInstruction() / executeAllInstructions()
 └──────────────────┘
        ▲  ▼  (register state)        ▲  ▼  (trace log)
 ┌──────────────────┐          ┌──────────────────┐
 │  RegisterManager │          │   TraceLogger    │──► trace_*.log file
 └──────────────────┘          └──────────────────┘
        ▲  ▼  (UI reads/writes)
 ┌──────────────────┐
 │  UserInterface   │──► OutputManager ──► Messages/Errors panel
 │                  │──► Console Keyboard / Printer panels
 │                  │──► Cache display panel
 └──────────────────┘
```

### 2.2 Package Structure

| Package | Responsibility |
|---|---|
| `(default)` | `Main.java` — bootstraps the Swing event dispatch thread |
| `ui` | `UserInterface` — Swing GUI, all operator controls and register displays |
| `cpu` | `CPU` — fetch-decode-execute loop, instruction dispatch |
| `memory` | `Memory`, `Cache`, `Register`, `RegisterManager` — simulated memory, cache, and register file |
| `encoder` | `Encoder`, `InstructionEncoder`, `EncoderStringUtil` — two-pass assembler |
| `opcode` | `OpcodeLookupTable`, `Opcode`, `OpcodeType`, `InvalidMnemonicException` — ISA table |
| `instruction` | `Instruction` — data object for a parsed assembly line |
| `fileutil` | `FileReader`, `FileWriter` — I/O utilities for source, listing, and load files |
| `outputmanager` | `OutputManager` — routes messages and errors to the UI console |
| `trace` | `TraceLogger` — singleton that records every instruction execution to a timestamped log file |

---

## 3. Module Descriptions

### 3.1 `Main`

Entry point. Launches `UserInterface` on the Swing Event Dispatch Thread using `SwingUtilities.invokeLater`. No business logic resides here.

---

### 3.2 `CPU`

**Key fields:**

| Field | Type | Description |
|---|---|---|
| `programCounter` | `int` | Current instruction address |
| `memory` | `Memory` | Reference to the 2048-word memory |
| `cache` | `Cache` | Fully associative cache sitting between CPU and memory |
| `registerManager` | `RegisterManager` | Holds all register values |
| `outputManager` | `OutputManager` | Error/message output channel |
| `halted` | `boolean` | Execution state flag |
| `consoleInputProvider` | `Supplier<Integer>` | Provides a character from the console keyboard for IN instructions |
| `consolePrinterConsumer` | `Consumer<Integer>` | Accepts a character for the console printer for OUT instructions |

**Key methods:**

| Method | Description |
|---|---|
| `loadProgramToMemory(Path)` | Reads the `.load` file and writes each `(address, value)` pair into memory. Returns the program start address (first non-data, non-RFS instruction address). |
| `executeNextInstruction()` | Fetches the word at `PC`, decodes the 6-bit opcode, updates MAR/MBR/IR, dispatches to the appropriate handler. Increments PC only if the execute method did not modify it (via `pcModified` return flag). |
| `executeAllInstructions()` | Loops `executeNextInstruction()` until `halted == true`. |
| `executeLoadStoreInstruction(int)` | Decodes R, IX, I, and address fields; computes the Effective Address; performs LDR, STR, LDA, LDX, or STX. |
| `executeTransferInstruction(int)` | Handles JZ, JNE, JCC, JMA, JSR, RFS, SOB, and JGE. Returns `true` when PC is modified by a branch. |
| `executeArithmeticInstruction(int)` | Handles AMR, SMR, AIR, and SIR. Updates the Condition Code register on overflow/underflow. |
| `executeMultiplyDivideInstruction(int)` | Handles MLT and DVD. MLT stores the 32-bit result across two register pairs; DVD sets DIVZERO on division by zero. |
| `executeLogicalInstruction(int)` | Handles TRR, AND, ORR, and NOT. TRR sets CC(3) (EQUALORNOT) based on register equality. |
| `executeShiftRotateInstruction(int)` | Handles SRC (shift) and RRC (rotate) with arithmetic/logical and left/right variants. |
| `executeIOInstruction(int)` | Handles IN (read from console keyboard, DEVID 0), OUT (write to console printer, DEVID 1), and CHK (device status). |
| `executeMiscInstruction(int)` | Handles HLT (sets `halted = true`) and TRAP. |
| `raiseFault(int, String)` | Sets the MFR register, logs the fault, and halts the CPU. Used for reserved address access (`0001`), illegal opcode (`0100`), and out-of-bounds memory (`1000`). |
| `readMemory(int)` / `writeMemory(int, int)` | Memory access routed through the cache. `readMemory` checks the cache first (hit → return cached value; miss → read from memory and insert into cache). `writeMemory` uses write-through policy (always writes to memory; updates cached copy if present). |

**Design decision — dispatch table via `OpcodeType` enum:**
Rather than a long chain of if/else on raw opcode integers, the CPU resolves the instruction category through `OpcodeLookupTable.getOpcodeType(opcode)` and switches on the resulting `OpcodeType` enum value. This cleanly separates the format-specific decoding into dedicated methods, and makes adding new instruction categories straightforward.

**Design decision — `pcModified` return flag:**
Each `execute*Instruction` method returns a boolean indicating whether it modified the program counter (e.g., via a branch or jump). `executeNextInstruction()` only auto-increments PC when `pcModified` is `false`. This avoids the previous approach of unconditionally incrementing PC after every instruction, which caused transfer-of-control instructions to skip the target's first instruction.

**Design decision — machine fault handling:**
Rather than crashing or silently ignoring errors, the CPU sets the Machine Fault Register (MFR) with a specific fault code, logs the fault via `OutputManager`, and halts cleanly. Early-exit guards after fault detection prevent post-fault side effects. Fault codes follow the ISA spec: `0001` = illegal reserved address, `0100` = illegal opcode, `1000` = address beyond 2048.

**Design decision — console I/O via callbacks:**
The CPU does not reference UI classes directly. Instead, `Supplier<Integer>` and `Consumer<Integer>` callbacks are injected by the UI at construction time. This keeps the CPU testable independently of Swing and allows the I/O mechanism to be swapped (e.g., for batch testing).

---

### 3.3 `Memory` and `Cache`

`Memory` is a fixed-size 2048-element `int[]` initialized to zero at construction, representing the C6461's 2048-word address space.

**Design decision — single-port, bounds-checked:**
Both `getMemoryAt` and `setMemoryAt` throw `IndexOutOfBoundsException` for any address outside `[0, 2047]`. Callers (CPU and UI) catch this exception and report it via `OutputManager`. This enforces the architectural constraint that the machine has exactly 2048 words.

`Cache` is a fully associative, unified cache sitting between the CPU and main memory. It holds 16 lines, each storing a single 16-bit word (since memory is word-addressable). Replacement uses a FIFO (First-In, First-Out) policy via a circular pointer that tracks which line to evict next.

**Cache API:**

| Method | Description |
|---|---|
| `lookup(int address)` | Returns the cached value on a hit, or `-1` on a miss. |
| `insert(int address, int value)` | Inserts a word into the cache (called on a miss after reading from memory). Evicts via FIFO if all lines are occupied. |
| `writeThrough(int address, int value)` | On a store, updates the cached copy if the address is present. Does not allocate on a miss (no write-allocate). |

**Design decision — write-through, no write-allocate:**
Stores always go to main memory immediately. If the address happens to be cached, the cached copy is updated too, but a store to an uncached address does not bring the line into the cache. This simplifies coherence and matches the ISA spec's requirement for a write-through policy.

**Design decision — FIFO over LRU:**
FIFO is simpler to implement (a single integer pointer) and provides adequate performance for the simulator's small working sets. The circular pointer wraps around the 16 lines, ensuring deterministic and predictable eviction order.

---

### 3.4 `Register` and `RegisterManager`

`Register` is a Java enum listing all architectural registers:

```
GPR0, GPR1, GPR2, GPR3   — General Purpose Registers
IX1,  IX2,  IX3            — Index Registers
PC, MAR, MBR, IR           — Special Registers
CC                          — Condition Code (4 bits)
MFR                         — Machine Fault Register (4 bits)
```

**Condition Code (CC) bits:**

| Bit | Name | Set when |
|---|---|---|
| cc(0) | OVERFLOW | Arithmetic result exceeds 16-bit signed range |
| cc(1) | UNDERFLOW | Arithmetic result falls below 16-bit signed range |
| cc(2) | DIVZERO | Division by zero attempted |
| cc(3) | EQUALORNOT | TRR: registers are equal |

**Machine Fault Register (MFR) codes:**

| Code (binary) | Fault |
|---|---|
| `0001` | Illegal memory address to reserved locations |
| `0100` | Illegal opcode (not found in lookup table) |
| `1000` | Illegal memory address beyond 2048 (out of bounds) |

`RegisterManager` wraps a `Map<Register, Integer>` initialized to zero for all registers. It provides `loadRegister(Register, int)` and `getRegisterValue(Register)`.

**Design decision — enum-keyed map over parallel arrays:**
Using an enum as the map key avoids index arithmetic errors and makes register access self-documenting at call sites (e.g., `registerManager.loadRegister(Register.PC, value)` versus `registers[4] = value`).

---

### 3.5 `Encoder` (Assembler)

The assembler is a **two-pass** design:

**Pass 1 — Tokenization and Label Resolution (`getTokenizedInstructions` + `buildLabelsMap`):**
Each source line is stripped of comments (`;` delimiter), split on whitespace, and parsed into an `Instruction` object containing: label, mnemonic, operands array, comment, and empty `loc`/`octal` fields. A simulated location counter then walks the instruction list: `LOC` directives reset the counter, and each labelled instruction maps its label string to the current counter value in `labelsMap`.

**Pass 2 — Encoding (`encodeLines`):**
Operands that match labels in `labelsMap` are replaced with their numeric address strings. `InstructionEncoder.encodeInstruction` is then called on each instruction to produce the 6-digit octal string stored in `instruction.octal`.

**Design decision — two-pass for forward references:**
A single-pass assembler cannot resolve a label used before it is defined (e.g., `JZ 0,0,End` where `End:` appears later). The two-pass approach first collects all labels, then encodes. This is the classical assembler approach and correctly handles all forward and backward references.

---

### 3.6 `InstructionEncoder`

Encodes a single `Instruction` to a 16-bit binary string (then converted to 6-digit octal). Dispatches to a format-specific encoder based on `OpcodeType`:

| Format method | Instructions |
|---|---|
| `encodeWithLoadStoreFormat` | LDR, STR, LDA, LDX, STX, JZ, JNE, JCC, JMA, JSR, RFS, SOB, JGE, AMR, SMR, AIR, SIR |
| `encodeWithRegisterRegisterFormat` | MLT, DVD, TRR, AND, ORR, NOT |
| `encodeWithShiftRotateFormat` | SRC, RRC |
| `encodeWithIOFormat` | IN, OUT, CHK |
| `encodeWithMiscFormat` | HLT, TRAP |
| `DATA` (special case) | Raw decimal value → zero-padded octal |

**Instruction word layout (16 bits):**

```
Load/Store / Transfer / Arithmetic format:
  [15-10] opcode (6 bits)
  [ 9- 8] R  — register (2 bits)
  [ 7- 6] IX — index register (2 bits)
  [    5] I  — indirect flag (1 bit)
  [ 4- 0] address (5 bits)

Register-Register format (MLT, DVD, TRR, AND, ORR, NOT):
  [15-10] opcode (6 bits)
  [ 9- 8] Rx (2 bits)
  [ 7- 6] Ry (2 bits)
  [ 5- 0] unused (6 bits)

Shift/Rotate format (SRC, RRC):
  [15-10] opcode (6 bits)
  [ 9- 8] R (2 bits)
  [    7] A/L — arithmetic or logical (1 bit)
  [    6] R/L — right or left (1 bit)
  [ 5- 4] unused (2 bits)
  [ 3- 0] count (4 bits)

I/O format (IN, OUT, CHK):
  [15-10] opcode (6 bits)
  [ 9- 8] R (2 bits)
  [ 7- 5] unused (3 bits)
  [ 4- 0] DevID (5 bits)
```

---

### 3.7 `OpcodeLookupTable`

A static `HashMap<String, Opcode>` populated in a `static` initializer, mapping each mnemonic string to its `Opcode` record (octal integer value + `OpcodeType`).

Provides:
- `getOpcodeValue(String mnemonic)` — used by the encoder
- `getOpcodeType(String mnemonic)` / `getOpcodeType(int value)` — used by the CPU decoder
- `getMnemonic(int value)` — reverse lookup used during execution logging

**Design decision — static lookup table:**
All opcode definitions are centralized here. The encoder and CPU both reference the same source of truth, preventing divergence between assembled and executed opcode values.

---

### 3.8 `UserInterface`

A `JFrame` subclass organized into four vertical columns:

| Panel | Contents |
|---|---|
| Left | GPR display rows, IX display rows, Octal Input, Binary Equivalent |
| Center | Special Registers (PC, MAR, MBR, IR, CC, MFR), Load/Store/Run button grid |
| Right | Messages/Errors scrollable text area, Console Keyboard input field, Console Printer output area |
| Cache | Cache line display (16 lines showing Valid/Tag/Data), HIT/MISS indicator, FIFO pointer |

A top-level file-picker bar spans all four columns and includes a **Trace Logging** checkbox.

**Register display:** `registerTextFieldMap` (`Map<Register, JTextField>`) maps every `Register` enum value to its corresponding read-only display field. `syncUIWithCPU()` iterates all registers and updates all fields at once after any state-changing operation. CC and MFR are displayed as 4-digit binary strings rather than octal.

**Console I/O panels:**
- **Console Keyboard** — a text field where the user types input for IN instructions (DEVID 0). Characters are consumed one at a time from left to right. If the field is empty when an IN instruction executes, a dialog prompts the user for input.
- **Console Printer** — a read-only text area that displays characters output by OUT instructions (DEVID 1).

**Cache display panel:**
Shows all 16 cache lines in a table format (Line# | Valid | Tag | Data), a HIT/MISS indicator for the most recent access, and the current FIFO replacement pointer position. Updated after every instruction execution via `syncUIWithCPU()`.

**IPL flow:**
1. Read the `.asm` file via `FileReader`
2. Pass lines to `Encoder.encode()` to get `List<Instruction>`
3. Write `.lst` and `.load` files via `FileWriter`
4. Construct a fresh `CPU` instance (resets all state)
5. Wire console I/O callbacks (`wireConsoleIO()`)
6. Call `cpu.loadProgramToMemory(loadFilePath)` to populate memory
7. Set PC to the returned start address
8. Call `syncUIWithCPU()` to refresh all displays

**Design decision — fresh CPU on each IPL:**
Constructing a new `CPU` on each IPL press resets memory and all registers to zero automatically, matching the architectural specification that powering on clears all state. It avoids the need for explicit reset methods scattered across subsystems.

---

### 3.9 `OutputManager`

Wraps a `JTextArea` reference and provides `writeMessage(String)` and `writeError(String)` methods. Errors are prefixed with `"ERROR: "`. The class also provides the static helper `getPaddedOctalValue(int)` which formats an integer as a zero-padded 6-digit octal string (used consistently across all display updates).

---

### 3.10 `TraceLogger`

A singleton that records every simulator action to a timestamped log file (`trace_YYYYMMDD_HHmmss.log`). Each log line is prefixed with a step counter (e.g., `[STEP 0042]`) that increments with each instruction executed.

The trace file is human-readable and self-contained: someone reading only the trace file should be able to understand exactly what the program did, step by step. Logged events include:

- **Fetch phase** — PC address and fetched instruction word
- **Decode phase** — mnemonic, opcode, and decoded operand fields
- **Execute phase** — effective address computation, register reads/writes, memory reads/writes, branch decisions
- **Register snapshots** — full register state after each instruction (including CC and MFR)
- **Cache activity** — hits, misses, insertions, and evictions

Trace logging is enabled by default and can be toggled at runtime via the **Enable Trace Logging** checkbox in the file-picker bar. The `TraceLogger` creates a new log file on each IPL.

**Design decision — singleton with enable/disable toggle:**
A singleton avoids passing a logger reference through every method call. The enable/disable toggle allows users to suppress logging for long-running programs without restarting the simulator, and the per-IPL file naming ensures each program run has its own isolated trace.

---

## 4. Effective Address Computation

For Load/Store instructions, the CPU computes the Effective Address (EA) as follows:

```
ea = address_field          // Start with the 5-bit address field

if (ix > 0 && not LDX/STX):
    ea = ea + c(IXix)       // Add index register value

if (I == 1):
    ea = c(ea)              // Indirect: dereference EA through memory
```

`LDX` and `STX` use the IX field to select the *target register*, not for indexing, so they skip the index offset step.

---

## 5. Memory Map

| Address Range (octal) | Usage |
|---|---|
| 000000 – 000005 | Reserved / not used by programs |
| 000006 and above | Available for program code and data |
| 001000 – 003777 (0–2047 decimal) | Full addressable memory |

All memory initializes to zero on simulator startup (IPL).

---

## 6. Known Limitations

- There are no floating-point or vector operations (deferred to Part IV).
- TRAP instruction is recognized by the assembler and decoded by the CPU but the trap table mechanism is not yet implemented.
- CHK (Check Device Status) always reports the device as ready.

---

## 7. Testing

Two test programs are provided in `input/`:

### `test_program.asm`

Demonstrates direct addressing, indexed addressing, indirect addressing, and label-based control flow (JZ). Loads data into GPRs and index registers, then conditionally jumps to a HLT.

Expected state after IPL + Run:
- IX2 = 3 (decimal), R3 = 12, R2 = 12, R1 = 18, R0 = 0, IX1 = 1024
- PC halts at address 512 (decimal) / 001000 (octal)

### `test_part1.asm`

The Part I deliverable test program. Covers all Load/Store instructions (LDR, STR, LDA, LDX, STX) and all four addressing modes. Data is pre-placed in memory via `DATA` directives before the code section at `LOC 50`.

| Step | Instruction | Mode | Expected Result |
|---|---|---|---|
| 1 | `LDR 0,0,5` | Direct | R0 = mem[5] = 42 |
| 2 | `LDA 1,0,5` | Load Address | R1 = EA = 5 |
| 3 | `LDX 1,6` | Direct | IX1 = mem[6] = 10 |
| 4 | `LDX 2,7` | Direct | IX2 = mem[7] = 3 |
| 5 | `LDR 2,1,5` | Indexed | EA = IX1+5 = 15, R2 = mem[15] = 88 |
| 6 | `LDR 3,0,8,1` | Indirect | EA = mem[8] = 25, R3 = mem[25] = 99 |
| 7 | `LDR 0,2,8,1` | Indexed+Indirect | EA1 = IX2+8 = 11, EA = mem[11] = 27, R0 = mem[27] = 66 |
| 8 | `STR 0,0,20` | Direct Store | mem[20] = R0 = 66 |
| 9 | `STX 1,21` | Direct Store | mem[21] = IX1 = 10 |

Expected register state after IPL + Run: R0 = 66, R1 = 5, R2 = 88, R3 = 99, IX1 = 10, IX2 = 3.

### `test_faults.asm`

Tests machine fault handling. Executes three normal instructions (two LDRs and an AIR), then attempts to load from reserved address 3, which triggers an MFR fault and halts the CPU before the final HLT is reached.

Expected state after IPL + Run:
- R0 = 47 (42 + 5 from AIR), R1 = 100, R2 = 0 (fault fired before load completed)
- MFR = 0001 (reserved address fault)
- PC = 11 (address of the faulting LDR instruction)

---

## 8. DATA Placement Convention

**Design decision — DATA after HLT, not before code:**

`CPU.loadProgramToMemory` detects the program entry point by scanning the `.load` file for the first word whose top 6 bits (`value >> 10`) form a non-zero, non-RFS opcode. `DATA` values > 1024 have non-zero upper bits that alias valid opcodes (e.g., `DATA 2000` → opcode field = `02` = STR), causing the loader to misidentify a data word as the program start.

The `.load` format (two octal numbers per line, per the ISA spec) carries no metadata to distinguish data from instructions, and the assembler and CPU operate independently — the assembler cannot pass the entry point out-of-band.

Three program layouts were considered:

| Layout | Approach | Verdict |
|---|---|---|
| DATA before code (via `LOC`) | Place data at low addresses, code higher | **Rejected** — DATA > 1024 is encountered first, corrupts entry point detection |
| DATA after `HLT` | Code first, data after halt | **Chosen** — first `.load` entry is always a real instruction; CPU halts before reaching data |
| Jump over inline DATA | `JMA` skips past data block | Viable, but adds an extra instruction and address bookkeeping for no benefit over the HLT approach |

Convention: all `.asm` programs place `DATA` directives after `HLT`.

```asm
LOC 6
LDR 0,0,20       ; entry point
LDR 1,0,21
HLT
DATA 2000         ; unreachable by PC, safe for any value 0–65535
DATA 42
```
