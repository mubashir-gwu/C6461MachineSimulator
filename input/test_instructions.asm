; ================================================================
; GROUP 2 Comprehensive Test Program
; Tests: Arithmetic, Logical, MLT/DVD, Shift/Rotate, Transfer
;
; Memory layout:
;   Addresses  6-13 (octal 6-15):   Data constants
;   Address   47    (octal 57):     Subroutine for JSR/RFS test
;   Addresses 14-46 (octal 16-56):  Sequential tests
;   Addresses 50-74 (octal 62-112): Transfer tests
;
; All expected values documented in comments.
; On success: HLT at address 74 (octal 112), R3=74, R0=0.
; On failure: HLT at an unexpected address (52,56,59,63,70).
; ================================================================

; === SECTION 1: ARITHMETIC TESTS (starts at address 14) ===
LOC 14

; --- AIR: add immediate, special case c(r)=0 ---
AIR 0,7         ; addr 14: R0 was 0 -> R0 <- 7.          Expected: R0=7

; --- AIR: add immediate, normal ---
AIR 0,3         ; addr 15: R0 = 7 + 3 = 10.              Expected: R0=10

; --- SIR: subtract immediate, normal ---
SIR 0,4         ; addr 16: R0 = 10 - 4 = 6.              Expected: R0=6

; --- SIR: special case c(r)=0, loads -(Immed) ---
SIR 1,5         ; addr 17: R1 was 0 -> R1 <- -(5)=65531. Expected: R1=65531 (0xFFFB)

; --- AMR: add memory to register ---
LDR 2,0,6      ; addr 18: R2 = mem[6] = 10.              Expected: R2=10
AMR 2,0,7      ; addr 19: R2 = 10 + mem[7] = 30.         Expected: R2=30

; --- SMR: subtract memory from register ---
SMR 2,0,8      ; addr 20: R2 = 30 - mem[8] = 27.         Expected: R2=27

; === SECTION 2: MULTIPLY/DIVIDE TESTS ===

; --- MLT: 10 * 3 = 30 ---
LDR 0,0,6      ; addr 21: R0 = 10
LDR 2,0,8      ; addr 22: R2 = 3
MLT 0,2         ; addr 23: 10*3=30. High=0, Low=30.      Expected: R0=0, R1=30

; --- DVD: 20 / 3 = 6 remainder 2 ---
LDR 0,0,7      ; addr 24: R0 = 20
LDR 2,0,8      ; addr 25: R2 = 3
DVD 0,2         ; addr 26: 20/3=6 rem 2.                  Expected: R0=6, R1=2

; === SECTION 3: LOGICAL TESTS ===

; --- AND: 15 AND 6 = 6 ---
LDR 0,0,11     ; addr 27: R0 = 15 (0x000F)
LDR 2,0,12     ; addr 28: R2 = 6  (0x0006)
AND 0,2         ; addr 29: 0x000F & 0x0006 = 0x0006.      Expected: R0=6

; --- ORR: 15 OR 6 = 15 ---
LDR 0,0,11     ; addr 30: R0 = 15
ORR 0,2         ; addr 31: 0x000F | 0x0006 = 0x000F.      Expected: R0=15

; --- NOT: NOT 15 = 65520 ---
NOT 0           ; addr 32: ~0x000F = 0xFFF0 = 65520.      Expected: R0=65520

; --- TRR: test equality, sets cc(3) ---
LDR 0,0,6      ; addr 33: R0 = 10
LDR 2,0,6      ; addr 34: R2 = 10
TRR 0,2         ; addr 35: R0==R2, cc(3)=1.               Expected: CC=0b1000

; === SECTION 4: SHIFT/ROTATE TESTS ===

; --- SRC logical left shift by 2 ---
LDR 0,0,12     ; addr 36: R0 = 6   (0b110)
SRC 0,2,1,1    ; addr 37: logical left 2.                 Expected: R0=24 (0b11000)

; --- SRC logical right shift by 1 ---
SRC 0,1,0,1    ; addr 38: logical right 1.                Expected: R0=12 (0b1100)

; --- SRC arithmetic right shift by 1 ---
LDR 0,0,12     ; addr 39: R0 = 6   (0b110)
SRC 0,1,0,0    ; addr 40: arith right 1, sign=0.          Expected: R0=3 (0b011)

; --- SRC arithmetic left shift by 2 ---
LDR 0,0,12     ; addr 41: R0 = 6   (0b110)
SRC 0,2,1,0    ; addr 42: arith left 2, sign stays 0.     Expected: R0=24 (0b11000)

; --- RRC rotate left by 4 ---
LDR 0,0,11     ; addr 43: R0 = 15  (0b0000000000001111)
RRC 0,4,1,1    ; addr 44: rotate left 4.                  Expected: R0=240 (0b0000000011110000)

; === LOAD IX1 AND JUMP TO TRANSFER TESTS ===
LDX 1,13        ; addr 45: IX1 = mem[13] = 40
JMA 1,10        ; addr 46: jump to EA=10+40=50 (transfer test section)

; === SECTION 5: TRANSFER TESTS (starts at address 50) ===
; Uses IX1=40 for all jump targets: EA = addr_field + 40
LOC 50

; --- JNE: R0 != 0, should jump ---
LDR 0,0,6      ; addr 50: R0 = 10 (non-zero)
JNE 0,1,13     ; addr 51: R0!=0, jump to 13+40=53
HLT             ; addr 52: FAIL (should not reach)
AIR 3,1         ; addr 53: PASS, R3=1

; --- JZ: R0 == 0, should jump ---
LDR 0,0,9      ; addr 54: R0 = mem[9] = 0
JZ 0,1,17      ; addr 55: R0==0, jump to 17+40=57
HLT             ; addr 56: FAIL
AIR 3,1         ; addr 57: PASS, R3=2

; --- JMA: unconditional jump ---
JMA 1,20        ; addr 58: jump to 20+40=60
HLT             ; addr 59: FAIL
AIR 3,1         ; addr 60: PASS, R3=3

; --- JGE: R0 >= 0, should jump (R0=0 from JZ test) ---
LDR 0,0,6      ; addr 61: R0 = 10 (positive)
JGE 0,1,24     ; addr 62: R0>=0, jump to 24+40=64
HLT             ; addr 63: FAIL
AIR 3,1         ; addr 64: PASS, R3=4

; --- SOB: loop 3 times ---
SIR 1,2         ; addr 65: R1=2-2=0 (clear R1, was 2 from DVD)
AIR 1,3         ; addr 66: R1=0 -> R1 <- 3
SOB 1,1,27     ; addr 67: R1--; if R1>0 jump to 27+40=67 (itself)
                ;   Iteration 1: R1=3->2, 2>0, jump to 67
                ;   Iteration 2: R1=2->1, 1>0, jump to 67
                ;   Iteration 3: R1=1->0, 0 not >0, fall through
AIR 3,1         ; addr 68: PASS, R3=5

; --- JCC: test cc(3)=1 (set by TRR at addr 35) ---
JCC 3,1,31     ; addr 69: cc(3)==1, jump to 31+40=71
HLT             ; addr 70: FAIL
AIR 3,1         ; addr 71: PASS, R3=6

; --- JSR/RFS: subroutine call and return ---
STR 3,0,10     ; addr 72: save R3=6 to mem[10] (pass count)
JSR 1,7         ; addr 73: R3 <- 74, jump to 7+40=47 (subroutine)
                ;   Subroutine at 47: RFS 0 -> R0=0, PC=c(R3)=74
HLT             ; addr 74: SUCCESS - all tests passed!

; === DATA SECTION (after HLT per design convention) ===
LOC 6
DATA 10         ; addr 6:  value 10
DATA 20         ; addr 7:  value 20
DATA 3          ; addr 8:  value 3
DATA 0          ; addr 9:  value 0
DATA 5          ; addr 10: value 5
DATA 15         ; addr 11: value 15  (0b0000000000001111)
DATA 6          ; addr 12: value 6   (0b0000000000000110)
DATA 40         ; addr 13: value 40  (IX1 base for transfer jumps)

; === SUBROUTINE (address 47, for JSR/RFS test) ===
; NOTE: RFS 0 is manually encoded because the assembler does not handle
; the single-operand RFS format. Opcode 015, Immed=0 → octal 032000 = decimal 13312.
LOC 47
DATA 13312      ; addr 47: RFS 0 (manually encoded: R0 <- 0, PC <- c(R3))

; ================================================================
; EXPECTED FINAL STATE:
;   Program halts at address 74 (octal 112)
;   R0 = 0     (set by RFS)
;   R1 = 0     (from SOB loop)
;   R2 = 10    (from last LDR in TRR test)
;   R3 = 74    (return address saved by JSR)
;   IX1 = 40
;   mem[10] = 6 (pass count saved before JSR)
;   CC bit 3 = 1 (EQUALORNOT, from TRR)
;
; KEY CHECKPOINTS (verify in trace log):
;   Step at addr 14: R0=7      (AIR load from zero)
;   Step at addr 17: R1=65531  (SIR negative from zero)
;   Step at addr 19: R2=30     (AMR add memory)
;   Step at addr 23: R0=0,R1=30 (MLT result)
;   Step at addr 26: R0=6,R1=2 (DVD quotient/remainder)
;   Step at addr 29: R0=6      (AND result)
;   Step at addr 32: R0=65520  (NOT result)
;   Step at addr 35: CC=1000   (TRR equality)
;   Step at addr 37: R0=24     (logical left shift)
;   Step at addr 40: R0=3      (arithmetic right shift)
;   Step at addr 44: R0=240    (rotate left)
;   Steps at 53,57,60,64,68,71: R3 increments 1-6 (transfer passes)
;   Step at addr 72: mem[10]=6 (all 6 transfers passed)
;   Step at addr 74: HLT (success)
; ================================================================
