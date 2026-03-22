; ============================================================================
; Program 1: Read 20 integers, print them, find closest to target
; CSCI 6461 - C6461 Machine Simulator
; ============================================================================
;
; REQUIREMENT:
; A program that reads 20 numbers (integers) from the keyboard, prints the
; numbers to the console printer, requests a number from the user, and searches
; the 20 numbers read in for the number closest to the number entered by the
; user. Print the number entered by the user and the number closest to that
; number. Your numbers should not be 1…10, but distributed over the range of
; -32768 … 32767. Therefore, as you read a character in, you need to check it
; is a digit, convert it to a number, and assemble the integer. You should be
; able to handle negative numbers.
; ============================================================================
;
; MEMORY MAP:
;   6-27:    Constants, variables, address pointers (directly addressable, DATA after HLT)
;   100-136: ReadNum subroutine
;   200-240: PrintNum subroutine
;   300-359: Main program (entry point)
;   500-519: Array of 20 numbers (runtime)
;   560-579: Digit stack for printing (runtime)
;
; REGISTER CONVENTIONS:
;   R0: accumulator, multiply/divide operand, number passing
;   R1: I/O char, temp
;   R2: counter, multiply/divide operand
;   R3: temp, return address (set by JSR)
;   X2: code section base for intra-section branch targets
;   X3: temp address pointer
;
; ENTRY POINT: PC = 300 (auto-detected by loader)
; ============================================================================

; ============================================================================
; MAIN PROGRAM (Loc 300-359)
; ============================================================================
; Phase 1: Read 20 integers from keyboard (stored at 500-519)
; Phase 2: Print all 20 integers to the console printer
; Phase 3: Read a target number from the user
; Phase 4: Search for the closest number to the target
; Phase 5: Print the target and the closest number
;
; NOTE: Main must appear first in source so the loader's entry point
; detection finds address 300 before the subroutines at 100/200.
; ============================================================================

LOC 300

; ==================== PHASE 1: Read 20 numbers =============================
LDX 2,24         ; [300] X2 = 300 (Main Part1 base)
LDR 2,0,12       ; [301] R2 = 20 (loop counter)
LDR 3,0,10       ; [302] R3 = 500 (array base address)
STR 3,0,14       ; [303] T1 = 500 (current storage address)

; --- ReadLoop (304): Read one number per iteration ---
STR 2,0,20       ; [304] Save counter to SAVR2
JSR 0,22,1       ; [305] Call ReadNum -> R0 = integer from keyboard
LDX 2,24         ; [306] Restore X2 = 300 (ReadNum clobbered it)
STR 0,0,14,1     ; [307] Memory[c(T1)] = R0 (store number at array address)
; Advance array address:
LDR 3,0,14       ; [308] R3 = current address
AIR 3,1          ; [309] R3++
STR 3,0,14       ; [310] T1 = next address
; Decrement counter and loop:
LDR 2,0,20       ; [311] R2 = saved counter
SIR 2,1          ; [312] R2--
JNE 2,2,4        ; [313] If R2 != 0 -> ReadLoop at 304

; ==================== PHASE 2: Print 20 numbers ============================
LDR 2,0,12       ; [314] R2 = 20
LDR 3,0,10       ; [315] R3 = 500
STR 3,0,14       ; [316] T1 = 500

; --- PrintLoop (317): Print one number per iteration ---
STR 2,0,20       ; [317] Save counter
LDR 0,0,14,1     ; [318] R0 = Memory[c(T1)] = array[i]
JSR 0,23,1       ; [319] Call PrintNum (prints R0 with newline)
LDX 2,24         ; [320] Restore X2 = 300
; Advance address:
LDR 3,0,14       ; [321] R3 = current address
AIR 3,1          ; [322] R3++
STR 3,0,14       ; [323] T1 = next address
; Decrement counter and loop:
LDR 2,0,20       ; [324] R2 = saved counter
SIR 2,1          ; [325] R2--
JNE 2,2,17       ; [326] If R2 != 0 -> PrintLoop at 317

; ==================== PHASE 3: Read target number ==========================
JSR 0,22,1       ; [327] Call ReadNum -> R0 = target number
STR 0,0,17       ; [328] TARGET = R0

; ==================== PHASE 4: Search for closest ==========================
; Set X2 to Part2 base for search loop branch targets
LDX 2,25         ; [329] X2 = 332
LDR 3,0,11       ; [330] R3 = 32767 (max int)
STR 3,0,19       ; [331] MINDIFF = 32767 (initial minimum difference)
LDR 2,0,12       ; [332] R2 = 20 (counter)
LDR 3,0,10       ; [333] R3 = 500
STR 3,0,14       ; [334] T1 = 500 (current array address)

; --- SearchLoop (335): Compare one element per iteration ---
; X2 = 332, so offset 3 = address 335
STR 2,0,20       ; [335] Save counter
LDR 0,0,14,1     ; [336] R0 = array[i] (indirect through T1)
STR 0,0,15       ; [337] T2 = array[i] (save original value)
SMR 0,0,17       ; [338] R0 = array[i] - TARGET (signed difference)
; Compute absolute value of difference:
JGE 0,2,10       ; [339] If R0 >= 0 -> AbsOK at 342 (offset 10 from 332)
NOT 0            ; [340] Negate step 1
AIR 0,1          ; [341] R0 = |difference|
; --- AbsOK (342): R0 = |array[i] - TARGET| ---
STR 0,0,16       ; [342] T3 = |diff|
SMR 0,0,19       ; [343] R0 = |diff| - MINDIFF
JGE 0,2,17       ; [344] If |diff| >= MINDIFF -> NotCloser at 349
; New closest number found:
LDR 0,0,16       ; [345] R0 = |diff|
STR 0,0,19       ; [346] MINDIFF = |diff|
LDR 0,0,15       ; [347] R0 = array[i] (original value from T2)
STR 0,0,18       ; [348] CLOSEST = array[i]
; --- NotCloser (349): Advance to next array element ---
; offset 17 from 332 = 349
LDR 3,0,14       ; [349] R3 = current address
AIR 3,1          ; [350] R3++
STR 3,0,14       ; [351] T1 = next address
LDR 2,0,20       ; [352] R2 = saved counter
SIR 2,1          ; [353] R2--
JNE 2,2,3        ; [354] If R2 != 0 -> SearchLoop at 335 (offset 3 from 332)

; ==================== PHASE 5: Print results ===============================
LDR 0,0,17       ; [355] R0 = TARGET
JSR 0,23,1       ; [356] PrintNum(TARGET)
LDR 0,0,18       ; [357] R0 = CLOSEST
JSR 0,23,1       ; [358] PrintNum(CLOSEST)
HLT              ; [359] Stop the machine

; ============================================================================
; ReadNum SUBROUTINE (Loc 100-136)
; ============================================================================
; Reads a signed integer from the console keyboard, one character at a time.
; Characters are echoed to the printer. Enter (CR) terminates input.
; Handles optional leading '-' for negative numbers.
; Validates that each character is a digit (0-9) before accumulating.
;
; Entry: JSR 0,22,1   (jumps here via indirect through loc 22)
; Exit:  R0 = the signed integer read
; Clobbers: R1, R2, R3, X2, X3
; ============================================================================

LOC 100
STR 3,0,21       ; [100] Save return address to SAVR3
LDA 0,0,0        ; [101] R0 = 0 (number accumulator)
STR 0,0,13       ; [102] SIGN = 0 (positive)
LDX 2,22         ; [103] X2 = 100 (ReadNum base for branch offsets)
JMA 2,15         ; [104] Jump to ReadChar at 115

; --- SetNeg (105): User typed '-', set sign flag ---
LDA 1,0,1        ; [105] R1 = 1
STR 1,0,13       ; [106] SIGN = 1
JMA 2,15         ; [107] -> ReadChar at 115

; --- DoneNum (108): Enter was pressed, finalize number ---
LDR 1,0,7        ; [108] R1 = 10 (LF)
OUT 1,1          ; [109] Print newline after echoed CR
LDR 1,0,13       ; [110] R1 = SIGN
JZ 1,2,14        ; [111] If SIGN=0 (positive) -> Return at 114
NOT 0            ; [112] Two's complement negate step 1: R0 = ~R0
AIR 0,1          ; [113] Step 2: R0 = ~R0 + 1 = -R0

; --- Return (114): Jump back to caller, R0 has the number ---
JMA 0,21,1       ; [114] PC = c(21) = saved return address (preserves R0)

; --- ReadChar (115): Read one character from keyboard ---
IN 1,0           ; [115] R1 = character from keyboard (DEVID 0)
OUT 1,1          ; [116] Echo character to printer (DEVID 1)
STR 1,0,27       ; [117] RNTMP = char (save for later checks)

; Check if Enter (CR = 13):
SMR 1,0,9        ; [118] R1 = char - 13
JZ 1,2,8         ; [119] If zero (Enter pressed) -> DoneNum at 108

; Check if minus sign (ASCII 45):
LDR 1,0,27       ; [120] Restore char from RNTMP
SMR 1,0,8        ; [121] R1 = char - 45
JZ 1,2,5         ; [122] If zero (minus sign) -> SetNeg at 105

; Check if digit (ASCII 48-57):
LDR 1,0,27       ; [123] Restore char from RNTMP
SMR 1,0,6        ; [124] R1 = char - 48 (potential digit value)
JGE 1,2,27       ; [125] If >= 0 -> ChkUpper at 127 (might be digit)
JMA 2,15         ; [126] Not a digit (< '0') -> ReadChar at 115

; --- ChkUpper (127): Check digit <= 9 ---
SIR 1,10         ; [127] R1 = digit - 10
JGE 1,2,15       ; [128] If >= 0 (digit >= 10) -> invalid -> ReadChar at 115

; --- Valid digit: accumulate R0 = R0 * 10 + digit ---
AIR 1,10         ; [129] R1 = digit value (restore: was digit-10, now digit)
STR 1,0,15       ; [130] T2 = digit value
LDR 2,0,7        ; [131] R2 = 10
MLT 0,2          ; [132] R0(hi):R1(lo) = R0 * 10
AMR 1,0,15       ; [133] R1 = (R0*10) + digit
STR 1,0,27       ; [134] RNTMP = new accumulated value
LDR 0,0,27       ; [135] R0 = new accumulated value
JMA 2,15         ; [136] -> ReadChar at 115

; ============================================================================
; PrintNum SUBROUTINE (Loc 200-240)
; ============================================================================
; Prints a signed 16-bit integer in R0 to the console printer as decimal.
; Prints a newline (LF) after the number.
;
; Entry: JSR 0,23,1   (jumps here via indirect through loc 23)
;        R0 = number to print
; Exit:  returns to caller
; Clobbers: R0, R1, R2, R3, X2, X3
; ============================================================================

LOC 200
STR 3,0,21       ; [200] Save return address to SAVR3
LDX 2,23         ; [201] X2 = 200 (PrintNum base for branch offsets)

; Check if negative:
JGE 0,2,7        ; [202] If R0 >= 0 -> PrintPos at 207
LDR 1,0,8        ; [203] R1 = 45 (ASCII '-')
OUT 1,1          ; [204] Print minus sign
NOT 0            ; [205] Negate step 1: R0 = ~R0
AIR 0,1          ; [206] Negate step 2: R0 = |R0|

; --- PrintPos (207): R0 is now non-negative ---
LDA 3,0,0        ; [207] R3 = 0
STR 3,0,16       ; [208] DIGCNT = 0
JNE 0,2,15       ; [209] If R0 != 0 -> DivLoop at 215

; --- Zero special case: number is 0 ---
LDA 3,0,1        ; [210] R3 = 1
STR 3,0,16       ; [211] DIGCNT = 1
LDX 3,26         ; [212] X3 = 560 (digit stack base)
STR 0,3,0        ; [213] digit_stack[0] = 0 (R0 is already 0)
JMA 2,26         ; [214] -> PrintDigits at 226

; --- DivLoop (215): Extract digits by repeated division by 10 ---
LDR 2,0,7        ; [215] R2 = 10
DVD 0,2          ; [216] R0 = quotient, R1 = remainder (digit)
; Store remainder (digit) at digit_stack[DIGCNT]:
LDR 3,0,26       ; [217] R3 = 560 (stack base)
AMR 3,0,16       ; [218] R3 = 560 + DIGCNT
STR 3,0,15       ; [219] T2 = computed address
LDX 3,15         ; [220] X3 = computed address
STR 1,3,0        ; [221] digit_stack[DIGCNT] = digit (remainder)
; Increment digit count:
LDR 3,0,16       ; [222] R3 = DIGCNT
AIR 3,1          ; [223] R3++
STR 3,0,16       ; [224] DIGCNT = R3
JNE 0,2,15       ; [225] If quotient != 0 -> DivLoop at 215

; --- PrintDigits (226): Print digits in reverse order (MSB first) ---
LDR 3,0,16       ; [226] R3 = DIGCNT

; --- PrintDigLoop (227): Print one digit ---
SIR 3,1          ; [227] R3-- (index = DIGCNT-1 down to 0)
STR 3,0,16       ; [228] Save current index
LDR 1,0,26       ; [229] R1 = 560 (stack base)
AMR 1,0,16       ; [230] R1 = 560 + index
STR 1,0,15       ; [231] T2 = digit address
LDX 3,15         ; [232] X3 = digit address
LDR 1,3,0        ; [233] R1 = digit at that address
AMR 1,0,6        ; [234] R1 = digit + 48 (convert to ASCII)
OUT 1,1          ; [235] Print digit character
LDR 3,0,16       ; [236] R3 = current index
JNE 3,2,27       ; [237] If index != 0 -> PrintDigLoop at 227

; --- Newline and return ---
LDR 1,0,7        ; [238] R1 = 10 (LF)
OUT 1,1          ; [239] Print newline
JMA 0,21,1       ; [240] Return to caller (PC = c(21))

; ======================== DATA SECTION (Loc 6-27) ===========================
; Placed after HLT per design convention: DATA values > 1024 (e.g., 32767)
; have non-zero upper bits that alias valid opcodes, causing the loader's
; entry point detection to misidentify them as instructions.
; These locations are directly addressable (address field is 5 bits: 0-31).

LOC 6
Data 48          ; [6]  C48:     ASCII '0'
Data 10          ; [7]  C10:     constant 10, also ASCII LF
Data 45          ; [8]  C45:     ASCII '-'
Data 13          ; [9]  C13:     ASCII CR (Enter key)
Data 500         ; [10] PARR:    array base address
Data 32767       ; [11] CMAXINT: max positive 16-bit int
Data 20          ; [12] C20:     constant 20 (number of elements)
Data 0           ; [13] SIGN:    sign flag (0=positive, 1=negative)
Data 0           ; [14] T1:      temp variable 1 / current array address
Data 0           ; [15] T2:      temp variable 2 / digit value
Data 0           ; [16] T3:      temp variable 3 / digit count
Data 0           ; [17] TARGET:  user's search target number
Data 0           ; [18] CLOSEST: closest number found
Data 0           ; [19] MINDIFF: minimum absolute difference
Data 0           ; [20] SAVR2:   saved R2 (loop counter)
Data 0           ; [21] SAVR3:   saved R3 (return address for subroutines)
Data 100         ; [22] ARDNUM:  address of ReadNum subroutine
Data 200         ; [23] APRNUM:  address of PrintNum subroutine
Data 300         ; [24] AMAIN:   address of Main program (Part 1 base)
Data 332         ; [25] AMAIN2:  address of Main Part 2 (search base)
Data 560         ; [26] PDSTK:   digit stack base address
Data 0           ; [27] RNTMP:   ReadNum temp (char / accumulator save)
