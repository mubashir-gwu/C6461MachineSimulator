# C6461 Machine Simulator

**George Washington University - CS6461: Computer Architectures**
**Part III Deliverable**

## Team Members

- Mubashir Chandle
- Mukendi Patrick Tshiswala
- Miguel Martinez

---

A Java-based simulator for the C6461 16-bit computer architecture. The simulator includes a built-in assembler that translates assembly source files into machine code, loads programs into simulated memory, and executes them through a graphical operator console. All ISA instructions are fully implemented, including a 16-line fully associative cache, console I/O (keyboard, printer, card reader), machine fault handling with handler redirection, TRAP instruction with trap table dispatch, and instruction-level trace logging.

---

## Requirements

- Java JDK 17 or later (JDK 1.8 minimum)

---

## Building the Project

The project is structured as a plain Java module (IntelliJ IDEA). To compile from source:

```bash
# From the project root directory
mkdir -p out
javac -d out src/Main.java src/cpu/*.java src/encoder/*.java src/fileutil/*.java src/instruction/*.java src/memory/*.java src/opcode/*.java src/outputmanager/*.java src/trace/*.java src/ui/*.java
```

To package as a JAR:

```bash
mkdir -p out
jar --create --file C6461MachineSimulator.jar --main-class Main -C out .
```

### Running with IntelliJ IDEA

Open the project in IntelliJ IDEA. The `Main` class in `src/Main.java` is the entry point. Run it directly from the IDE or use **Build > Build Artifacts** to produce a JAR.

---

## Running the Simulator

### From a JAR file

```bash
java -jar C6461MachineSimulator.jar
```

### From compiled classes

```bash
java -cp out Main
```

The simulator window will open immediately upon launch. No command-line arguments are required.

### Sample Programs

Ready-to-run sample programs are provided in the `input/` directory:

- `input/test_program.asm`: Demonstrates load/store, indexed, and indirect addressing with label-based control flow
- `input/test_part1.asm`: Part I deliverable test covering all Load/Store instructions and all four addressing modes
- `input/test_faults.asm`: Tests machine fault handling (reserved address access triggers MFR and halts)
- `input/test_fault_handler.asm`: Tests fault handler redirection (Part III: fault saves PC, jumps to handler)
- `input/test_trap.asm`: Tests TRAP instruction dispatch and return via trap table (Part III)
- `input/program1.asm`: Program 1: reads 20 integers, prints them, finds closest to target
- `input/program2.asm`: Program 2: reads paragraph from file, searches for a word (Part III, requires `paragraph.txt` via Card Reader)

### Loading and Running a Program

1. Click the **Browse** button at the top of the simulator window. A file picker dialog will open. Navigate to any `.asm` file (for example, one of the samples in `input/`) and select it. The file path appears in the program field.
2. Press **IPL** (the red button). The assembler compiles the `.asm` source into a `.load` machine-code file and a `.lst` listing file (written alongside the source), then loads the program into simulated memory and sets the PC to the program's start address.
3. Press **Step** to execute one instruction at a time and observe register changes, or press **Run** to execute the full program until a `HLT` instruction.

---

## Preparing an Assembly Program

Write your assembly source in a `.asm` text file. The assembler supports the following syntax:

```
; This is a comment
LOC 10          ; Set the location counter to 10 (decimal)
DATA 42         ; Place the value 42 at the current location

LABEL: LDR 0,0,10   ; Load R0 from address 10
       STR 0,0,11   ; Store R0 to address 11
       HLT          ; Halt execution
```

**Syntax rules:**

| Element            | Format                     | Example            |
| ------------------ | -------------------------- | ------------------ |
| Location directive | `LOC <decimal address>`    | `LOC 500`          |
| Data word          | `DATA <decimal value>`     | `DATA 42`          |
| Label definition   | `LABEL: MNEMONIC operands` | `Start: LDR 0,0,5` |
| Label reference    | Used as an operand         | `JZ 0,0,Start`     |
| Comment            | `;` to end of line         | `; my comment`     |
| Instruction        | `MNEMONIC r,ix,addr[,i]`   | `LDR 1,2,15,1`     |

Sample programs are provided in the `input/` directory (see "Sample Programs" section above).

---

## Using the Simulator

### Console Layout

![C6461 Machine Simulator](assets/C6461%20Machine%20Simulator.png)

The simulator window is divided into five regions:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              C6461 Machine Simulator                                    │
├────────────────────────────────────────────────────────────────────────────────────────-┤
│  Load Program: [ file path field            ]  [☑ Enable Trace Logging]  [ Browse ]     │
├──────────────────┬──────────────────┬────────────────────┬─────────────────────────────-┤
│  Left Panel      │  Center Panel    │  Right Panel       │  Cache Panel                  │
│  (Registers)     │  (Special Regs/  │  (Messages/Errors, │  (16-line cache display,      │
│                  │   Controls)      │   Console I/O)     │   HIT/MISS, FIFO pointer)     │
└──────────────────┴──────────────────┴────────────────────┴─────────────────────────────-┘
```

#### Load Program Bar (top)

- **Program field**: displays the path of the currently selected assembly file.
- **Enable Trace Logging**: checkbox to toggle instruction-level trace logging. When enabled, every instruction execution is recorded to a timestamped `trace_*.log` file. Enabled by default.
- **Browse**: opens a file chooser to select a `.asm` source file. After selection, the path is shown and the file is ready to be loaded via the **IPL** button.

#### Left Panel: Registers

**General Purpose Registers (GPR)**

| Register | Description                |
| -------- | -------------------------- |
| GPR0     | General purpose register 0 |
| GPR1     | General purpose register 1 |
| GPR2     | General purpose register 2 |
| GPR3     | General purpose register 3 |

**Index Registers (IX)**

| Register | Description      |
| -------- | ---------------- |
| IX1      | Index register 1 |
| IX2      | Index register 2 |
| IX3      | Index register 3 |

Each register row shows its current value in **octal** and has a **Load** button. To manually load a value into a register:

1. Type an octal value into the **Octal Input** field.
2. Click the **Load** button next to the target register.

**Octal Input**: text field for entering an octal value to load into any register.

**Binary Equivalent**: read-only field that automatically shows the 16-bit binary representation of whatever is typed in the Octal Input field.

#### Center Panel: Special Registers & Controls

**Special Registers/Counters**

| Register | Description                                                                                |
| -------- | ------------------------------------------------------------------------------------------ |
| PC       | Program Counter: address of the next instruction to execute                               |
| MAR      | Memory Address Register: address used by Load/Store operations                            |
| MBR      | Memory Buffer Register: data read from or written to memory                               |
| IR       | Instruction Register: binary encoding of the last fetched instruction (read-only display) |
| CC       | Condition Code: 4 bits: OVERFLOW, UNDERFLOW, DIVZERO, EQUALORNOT (displayed as binary)    |
| MFR      | Machine Fault Register: 4 bits: reserved addr, illegal trap, illegal opcode, out-of-bounds (displayed as binary) |

PC and MAR have **Load** buttons; MBR has a **Load** button; IR, CC, and MFR are display-only.

**Load/Store/Run Controls**

| Button        | Action                                                                                                                                                                   |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Load**      | Reads the value at address `MAR` from memory and places it in `MBR`                                                                                                      |
| **Load+**     | Same as Load, then auto-increments `MAR` by 1                                                                                                                            |
| **Store**     | Writes the value in `MBR` to memory at address `MAR`                                                                                                                     |
| **Store+**    | Same as Store, then auto-increments `MAR` by 1                                                                                                                           |
| **Run**       | Runs the program from the current `PC` until a `HLT` instruction                                                                                                         |
| **Step**      | Executes a single instruction at the current `PC`, then pauses                                                                                                           |
| **Halt**      | Immediately halts execution                                                                                                                                              |
| **IPL** (red) | Initial Program Load: assembles the selected `.asm` file, writes listing and load files, loads the machine code into memory, and sets `PC` to the program start address |

#### Right Panel: Messages/Errors & Console I/O

**Messages/Errors**: displays runtime messages, confirmations, and error reports. Examples:

- `Loaded program test_program into memory.`
- `Loaded value 000052 from address 000012`
- `ERROR: address out of bounds: 9999 for memory of size 2048`

**Console Keyboard**: text field for typing input consumed by IN instructions (DEVID 0). Characters are read one at a time from left to right. If the field is empty when an IN instruction executes, a dialog prompts the user.

**Console Printer**: read-only text area displaying characters output by OUT instructions (DEVID 1).

**Card Reader (IN DEVID 2)**: file loader for card reader input. Click "Load File" to select a text file; its content is fed character-by-character to IN instructions using DEVID 2. Returns 0 (EOF) when all characters are consumed. The read position resets on each IPL.

#### Cache Panel

Displays the state of the 16-line fully associative cache:

- **Cache lines**: each line shows Line number, Valid bit, Tag (memory address), and Data (stored word)
- **HIT/MISS**: indicator showing whether the most recent cache access was a hit or miss
- **FIFO Pointer**: shows which cache line will be evicted next

The cache uses a write-through, no write-allocate policy: stores always go to main memory, and the cached copy is updated only if the address is already present.

---

## Typical Workflow

1. **Write** an assembly program and save it as a `.asm` file.
2. **Browse** to select the file using the Browse button.
3. Press **IPL** (red button). The assembler runs, the listing file (`.lst`) and load file (`.load`) are written to disk next to the source, and the program is loaded into simulated memory.
4. Optionally, manually set **PC** to a specific address using Octal Input + the PC Load button.
5. Press **Step** to execute one instruction at a time and observe register changes, or press **Run** to execute the entire program.
6. Use **Halt** at any time to stop execution.
7. Check the **Messages/Errors** panel for runtime output and error diagnostics.
8. Review the **Cache** panel to observe cache behavior (hits, misses, FIFO pointer).
9. If trace logging is enabled, inspect the generated `trace_*.log` file for a step-by-step execution record.

---

## Assembler Output Files

When IPL is pressed, two files are generated in the same directory as the source:

| File          | Description                                                                             |
| ------------- | --------------------------------------------------------------------------------------- |
| `<name>.lst`  | Listing file: each line shows octal address, octal encoding, and original source       |
| `<name>.load` | Load file: pairs of `<octal address> <octal value>` consumed by the simulator's loader |

---

## Project Structure

```
C6461MachineSimulator/
├── src/
│   ├── Main.java                        # Application entry point
│   ├── cpu/
│   │   └── CPU.java                     # CPU simulation, fetch-decode-execute cycle
│   ├── memory/
│   │   ├── Memory.java                  # 2048-word single-port memory
│   │   ├── Cache.java                   # 16-line fully associative cache (FIFO, write-through)
│   │   ├── Register.java                # Register enum (GPR0-3, IX1-3, PC, MAR, MBR, IR, CC, MFR)
│   │   └── RegisterManager.java         # Manages register read/write state
│   ├── encoder/
│   │   ├── Encoder.java                 # Two-pass assembler (tokenize → label map → encode)
│   │   ├── InstructionEncoder.java      # Per-format binary encoding logic
│   │   └── EncoderStringUtil.java       # Binary/octal string helpers
│   ├── opcode/
│   │   ├── OpcodeLookupTable.java       # Mnemonic ↔ opcode value/type mapping
│   │   ├── Opcode.java                  # Opcode record (value + type)
│   │   ├── OpcodeType.java              # Enum of instruction format categories
│   │   └── InvalidMnemonicException.java
│   ├── instruction/
│   │   └── Instruction.java             # Parsed instruction (label, mnemonic, operands, loc, octal)
│   ├── fileutil/
│   │   ├── FileReader.java              # Reads .asm / .load files line by line
│   │   └── FileWriter.java              # Writes .lst and .load output files
│   ├── outputmanager/
│   │   └── OutputManager.java           # Routes messages/errors to the UI text area
│   ├── trace/
│   │   └── TraceLogger.java             # Singleton trace logger, records every instruction to a timestamped log file
│   └── ui/
│       └── UserInterface.java           # Swing GUI, console layout and all button wiring
├── input/
│   ├── test_program.asm                 # Sample program (addressing modes + control flow demo)
│   ├── test_part1.asm                   # Part I deliverable: all Load/Store instructions and addressing modes
│   ├── test_faults.asm                  # Machine fault handling test (reserved address triggers MFR)
│   ├── test_fault_handler.asm           # Fault handler redirection test (Part III)
│   ├── test_trap.asm                    # TRAP instruction dispatch and return test (Part III)
│   ├── program1.asm                     # Program 1: 20-number search
│   ├── program2.asm                     # Program 2: paragraph word search (Part III)
│   └── paragraph.txt                    # Sample paragraph file for Program 2 (6 sentences)
└── docs/
    ├── CSCI6461ProjectDescription2024.pdf
    └── C6461 Class Project Instruction Set Architecture.pdf
```

---

## Supported Instructions

All instructions listed below are fully implemented in both the assembler and the CPU.

### Load/Store

| Mnemonic | Opcode (octal) | Description                          |
| -------- | -------------- | ------------------------------------ |
| LDR      | 001            | Load Register from memory            |
| STR      | 002            | Store Register to memory             |
| LDA      | 003            | Load Register with Effective Address |
| LDX      | 041            | Load Index Register from memory      |
| STX      | 042            | Store Index Register to memory       |

### Transfer of Control

| Mnemonic | Opcode (octal) | Description                |
| -------- | -------------- | -------------------------- |
| JZ       | 010            | Jump if Zero               |
| JNE      | 011            | Jump if Not Equal to Zero  |
| JCC      | 012            | Jump if Condition Code set |
| JMA      | 013            | Unconditional Jump         |
| JSR      | 014            | Jump to Subroutine         |
| RFS      | 015            | Return from Subroutine     |
| SOB      | 016            | Subtract One and Branch    |
| JGE      | 017            | Jump if Greater or Equal   |

### Arithmetic

| Mnemonic | Opcode (octal) | Description                      |
| -------- | -------------- | -------------------------------- |
| AMR      | 004            | Add Memory to Register           |
| SMR      | 005            | Subtract Memory from Register    |
| AIR      | 006            | Add Immediate to Register        |
| SIR      | 007            | Subtract Immediate from Register |

### Multiply/Divide

| Mnemonic | Opcode (octal) | Description                   |
| -------- | -------------- | ----------------------------- |
| MLT      | 070            | Multiply Register by Register |
| DVD      | 071            | Divide Register by Register   |

### Logical

| Mnemonic | Opcode (octal) | Description                        |
| -------- | -------------- | ---------------------------------- |
| TRR      | 072            | Test the Equality of Two Registers |
| AND      | 073            | Logical AND                        |
| ORR      | 074            | Logical OR                         |
| NOT      | 075            | Logical NOT                        |

### Shift/Rotate

| Mnemonic | Opcode (octal) | Description              |
| -------- | -------------- | ------------------------ |
| SRC      | 031            | Shift Register by Count  |
| RRC      | 032            | Rotate Register by Count |

### I/O

| Mnemonic | Opcode (octal) | Description                    |
| -------- | -------------- | ------------------------------ |
| IN       | 061            | Input Character to Register    |
| OUT      | 062            | Output Character from Register |
| CHK      | 063            | Check Device Status            |

### Miscellaneous

| Mnemonic | Opcode (octal) | Description    |
| -------- | -------------- | -------------- |
| HLT      | 000            | Halt execution |
| TRAP     | 030            | Software trap  |

---

## Addressing Modes

| Mode             | Format           | Effective Address        |
| ---------------- | ---------------- | ------------------------ |
| Direct           | `r, 0, addr`     | `EA = addr`              |
| Indexed          | `r, ix, addr`    | `EA = addr + c(IXix)`    |
| Indirect         | `r, 0, addr, 1`  | `EA = c(addr)`           |
| Indexed+Indirect | `r, ix, addr, 1` | `EA = c(addr + c(IXix))` |

All addresses in source files are **decimal**; the assembler converts them to octal encoding. Most register displays in the simulator show values in **octal**; CC and MFR are displayed as 4-digit **binary** strings.
