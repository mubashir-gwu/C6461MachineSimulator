# C6461 Simulator — Design Notes

**George Washington University — CS6461: Computer Architectures**
**Part I Deliverable**

---

## 1. Project Overview

This document describes the design decisions, object model, module interfaces, and architectural rationale for the C6461 Machine Simulator, Part I.

The simulator models a 16-bit CISC processor based on the C6461 Instruction Set Architecture. Part I covers:

- A two-pass assembler that translates `.asm` source files to octal machine code
- A 2048-word single-port memory
- A CPU with a fetch-decode-execute cycle supporting Load/Store instructions and HLT
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
 │      Memory      │◄─── CPU reads/writes
 └──────────────────┘
        ▲
        │ loadProgramToMemory()
        │
 ┌──────────────────┐
 │       CPU        │◄─── executeNextInstruction() / executeAllInstructions()
 └──────────────────┘
        ▲  ▼  (register state)
 ┌──────────────────┐
 │  RegisterManager │
 └──────────────────┘
        ▲  ▼  (UI reads/writes)
 ┌──────────────────┐
 │  UserInterface   │──► OutputManager ──► Messages/Errors panel
 └──────────────────┘
```

### 2.2 Package Structure

| Package | Responsibility |
|---|---|
| `(default)` | `Main.java` — bootstraps the Swing event dispatch thread |
| `ui` | `UserInterface` — Swing GUI, all operator controls and register displays |
| `cpu` | `CPU` — fetch-decode-execute loop, instruction dispatch |
| `memory` | `Memory`, `Register`, `RegisterManager` — simulated memory and register file |
| `encoder` | `Encoder`, `InstructionEncoder`, `EncoderStringUtil` — two-pass assembler |
| `opcode` | `OpcodeLookupTable`, `Opcode`, `OpcodeType`, `InvalidMnemonicException` — ISA table |
| `instruction` | `Instruction` — data object for a parsed assembly line |
| `fileutil` | `FileReader`, `FileWriter` — I/O utilities for source, listing, and load files |
| `outputmanager` | `OutputManager` — routes messages and errors to the UI console |

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
| `registerManager` | `RegisterManager` | Holds all register values |
| `outputManager` | `OutputManager` | Error/message output channel |
| `halted` | `boolean` | Execution state flag |

**Key methods:**

| Method | Description |
|---|---|
| `loadProgramToMemory(Path)` | Reads the `.load` file and writes each `(address, value)` pair into memory. Returns the program start address (first non-data instruction address). |
| `executeNextInstruction()` | Fetches the word at `PC`, decodes the 6-bit opcode, updates MAR/MBR/IR, dispatches to the appropriate handler, increments PC. |
| `executeAllInstructions()` | Loops `executeNextInstruction()` until `halted == true`. |
| `executeLoadStoreInstruction(int)` | Decodes R, IX, I, and address fields; computes the Effective Address; performs LDR, STR, LDA, LDX, or STX. |
| `executeMiscInstruction(int)` | Handles HLT by setting `halted = true`. |

**Design decision — dispatch table via `OpcodeType` enum:**
Rather than a long chain of if/else on raw opcode integers, the CPU resolves the instruction category through `OpcodeLookupTable.getOpcodeType(opcode)` and switches on the resulting `OpcodeType` enum value. This cleanly separates the format-specific decoding into dedicated methods, and makes adding new instruction categories straightforward.

**Design decision — error reporting without exceptions:**
Out-of-bounds memory accesses throw `IndexOutOfBoundsException` internally (checked at the `Memory` boundary), which `CPU` catches and routes to `OutputManager`. Execution stops at that instruction but the application does not crash. This mirrors the spec requirement that errors be reported to the console printer.

---

### 3.3 `Memory`

A fixed-size 2048-element `int[]` initialized to zero at construction, representing the C6461's 2048-word address space.

**Design decision — single-port, bounds-checked:**
Both `getMemoryAt` and `setMemoryAt` throw `IndexOutOfBoundsException` for any address outside `[0, 2047]`. Callers (CPU and UI) catch this exception and report it via `OutputManager`. This enforces the architectural constraint that the machine has exactly 2048 words.

---

### 3.4 `Register` and `RegisterManager`

`Register` is a Java enum listing all architectural registers:

```
GPR0, GPR1, GPR2, GPR3   — General Purpose Registers
IX1,  IX2,  IX3            — Index Registers
PC, MAR, MBR, IR           — Special Registers
```

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

A `JFrame` subclass organized into three vertical columns:

| Panel | Contents |
|---|---|
| Left | GPR display rows, IX display rows, Octal Input, Binary Equivalent |
| Center | Special Registers (PC, MAR, MBR, IR), Load/Store/Run button grid |
| Right | Messages/Errors scrollable text area |

A top-level file-picker bar spans all three columns.

**Register display:** `registerTextFieldMap` (`Map<Register, JTextField>`) maps every `Register` enum value to its corresponding read-only display field. `syncUIWithCPU()` iterates all registers and updates all fields at once after any state-changing operation.

**IPL flow:**
1. Read the `.asm` file via `FileReader`
2. Pass lines to `Encoder.encode()` to get `List<Instruction>`
3. Write `.lst` and `.load` files via `FileWriter`
4. Construct a fresh `CPU` instance (resets all state)
5. Call `cpu.loadProgramToMemory(loadFilePath)` to populate memory
6. Set PC to the returned start address
7. Call `syncUIWithCPU()` to refresh all displays

**Design decision — fresh CPU on each IPL:**
Constructing a new `CPU` on each IPL press resets memory and all registers to zero automatically, matching the architectural specification that powering on clears all state. It avoids the need for explicit reset methods scattered across subsystems.

---

### 3.9 `OutputManager`

Wraps a `JTextArea` reference and provides `writeMessage(String)` and `writeError(String)` methods. Errors are prefixed with `"ERROR: "`. The class also provides the static helper `getPaddedOctalValue(int)` which formats an integer as a zero-padded 6-digit octal string (used consistently across all display updates).

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

## 6. Known Limitations (Part I Scope)

- **Transfer, Arithmetic, Multiply/Divide, Logical, Shift/Rotate, and I/O instruction execution** are not yet implemented in the CPU (method bodies are empty stubs). The assembler correctly encodes all of these.
- Only **LDR, STR, LDA, LDX, STX, and HLT** execute during simulation in Part I.
- There is no cache (deferred to Part II).
- There are no floating-point or vector operations (deferred to Part IV).

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
