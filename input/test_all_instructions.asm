; test_all_instructions.asm — Assembler verification for all instruction formats
; Designed for BOTH encoding verification AND safe step-through execution.
; Each section ends with HLT. Use Step to walk through each section,
; then set PC to the next section's start address to continue.

; ============================================================
; Section 1: Load/Store instructions (loc 10-19)
; ============================================================
LOC 10
LDR 0,0,6               ; loc 10: R0 <- mem[6] = 42
LDR 3,0,7               ; loc 11: R3 <- mem[7] = 10
LDR 1,0,8               ; loc 12: R1 <- mem[8] = 20
STR 0,0,9               ; loc 13: mem[9] <- R0 = 42
LDA 1,0,6               ; loc 14: R1 <- EA = 6
LDX 1,6                 ; loc 15: IX1 <- mem[6] = 42
STX 1,9                 ; loc 16: mem[9] <- IX1 = 42
LDR 2,0,7               ; loc 17: R2 <- mem[7] = 10
STR 2,0,9               ; loc 18: mem[9] <- R2 = 10
HLT                     ; loc 19: end section 1

; ============================================================
; Section 2: Arithmetic instructions (loc 20-25)
; ============================================================
LOC 20
LDR 0,0,6               ; loc 20: R0 <- 42
AMR 0,0,7               ; loc 21: R0 <- R0 + mem[7] = 52
SMR 0,0,7               ; loc 22: R0 <- R0 - mem[7] = 42
AIR 0,5                 ; loc 23: R0 <- R0 + 5 = 47
SIR 0,3                 ; loc 24: R0 <- R0 - 3 = 44
HLT                     ; loc 25: end section 2

; ============================================================
; Section 3: Transfer instructions (loc 26-31)
; ============================================================
LOC 26
LDR 0,0,6               ; loc 26: R0 <- 42 (non-zero, positive)
JNE 0,0,29              ; loc 27: R0!=0 -> jump to 29 (skip 28)
HLT                     ; loc 28: skipped if JNE works
JGE 0,0,31              ; loc 29: R0>=0 -> jump to 31 (skip 30)
HLT                     ; loc 30: skipped if JGE works
HLT                     ; loc 31: end section 3

; ============================================================
; Section 4: Multiply/Divide instructions (loc 32-38)
; ============================================================
LOC 32
LDR 0,0,6               ; loc 32: R0 <- 42
LDR 2,0,7               ; loc 33: R2 <- 10
MLT 0,2                 ; loc 34: R0,R1 <- 42*10 = 420
LDR 0,0,6               ; loc 35: R0 <- 42
LDR 2,0,7               ; loc 36: R2 <- 10
DVD 0,2                 ; loc 37: R0 <- 4 (quotient), R1 <- 2 (remainder)
HLT                     ; loc 38: end section 4

; ============================================================
; Section 5: Logical instructions (loc 39-46)
; ============================================================
LOC 39
LDR 0,0,6               ; loc 39: R0 <- 42
LDR 1,0,7               ; loc 40: R1 <- 10
TRR 0,1                 ; loc 41: test R0==R1? (42!=10 -> CC(3)=0)
AND 0,1                 ; loc 42: R0 <- 42 AND 10 = 10
LDR 0,0,6               ; loc 43: R0 <- 42
ORR 0,1                 ; loc 44: R0 <- 42 OR 10 = 42
NOT 0                   ; loc 45: R0 <- NOT R0
HLT                     ; loc 46: end section 5

; ============================================================
; Section 6: Shift/Rotate instructions (loc 47-52)
; ============================================================
LOC 47
LDR 0,0,6               ; loc 47: R0 <- 42
SRC 0,2,1,1             ; loc 48: logical shift left 2: R0=168
SRC 0,1,0,1             ; loc 49: logical shift right 1: R0=84
SRC 0,1,0,0             ; loc 50: arithmetic shift right 1: R0=42
RRC 0,4,1,1             ; loc 51: rotate left 4
HLT                     ; loc 52: end section 6

; ============================================================
; Section 7: I/O instructions (loc 53-55)
; Type a character in Console Keyboard before stepping through
; ============================================================
LOC 53
IN 0,0                  ; loc 53: R0 <- keyboard char
OUT 0,1                 ; loc 54: print R0 to printer
HLT                     ; loc 55: end section 7

; ============================================================
; Section 8: TRAP (loc 56)
; ============================================================
LOC 56
TRAP 6                  ; loc 56: trap code 6 (will fault — illegal in Part II)
HLT                     ; loc 57: end

; ============================================================
; Data area (loc 6-9, placed after HLT per design convention)
; ============================================================
LOC 6
DATA 42                  ; loc 6: value 42
DATA 10                  ; loc 7: value 10
DATA 20                  ; loc 8: value 20
DATA 0                   ; loc 9: scratch space
