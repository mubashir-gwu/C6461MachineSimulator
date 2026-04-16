; test_trap.asm — Test TRAP instruction (Part III)
;
; SETUP:
;   Memory[0] = 100 (trap table base address)
;   Memory[100] = 30 (handler address for TRAP 0)
;   Memory[101] = 40 (handler address for TRAP 1)
;
; TRAP 0 handler (at address 30): loads R1 = 99 via AIR, returns via mem[2]
; TRAP 1 handler (at address 40): loads R2 = 77 via AIR, returns via mem[2]
;
; Return mechanism: TRAP saves PC+1 to location 2.
;   Handler returns with JMA 0,0,2,1 (jump indirect through address 2).
;
; EXPECTED FINAL STATE:
;   R0 = 42
;   R1 = 99 (set by TRAP 0 handler)
;   R2 = 77 (set by TRAP 1 handler)
;   PC = 13 (address of HLT)

; === TRAP 0 HANDLER ===
LOC 30
AIR 1,31         ; [30] R1 = 0 + 31
AIR 1,31         ; [31] R1 = 31 + 31 = 62
AIR 1,31         ; [32] R1 = 62 + 31 = 93
AIR 1,6          ; [33] R1 = 93 + 6 = 99
JMA 0,0,2,1      ; [34] Return: PC = mem[2] (indirect through location 2)

; === TRAP 1 HANDLER ===
LOC 40
AIR 2,31         ; [40] R2 = 0 + 31
AIR 2,31         ; [41] R2 = 31 + 31 = 62
AIR 2,15         ; [42] R2 = 62 + 15 = 77
JMA 0,0,2,1      ; [43] Return: PC = mem[2] (indirect through location 2)

; === MAIN PROGRAM ===
LOC 10
LDR 0,0,8        ; [10] R0 = mem[8] = 42
TRAP 0            ; [11] -> handler at 30, sets R1=99, returns to [12]
TRAP 1            ; [12] -> handler at 40, sets R2=77, returns to [13]
HLT               ; [13]

; === DATA SECTION (after all code, per design convention) ===
LOC 0
DATA 100          ; mem[0] = 100: trap table base address
LOC 8
DATA 42           ; mem[8] = 42
LOC 100
DATA 30           ; trap_table[0] = 30: handler for TRAP 0
DATA 40           ; trap_table[1] = 40: handler for TRAP 1
