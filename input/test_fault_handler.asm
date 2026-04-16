; test_fault_handler.asm — Test machine fault handler redirection (Part III)
;
; SETUP:
;   Memory location 1 = 20 (fault handler address)
;   Location 20 contains a simple fault handler that just halts.
;
; TEST:
;   1. Normal operations: load values into R0 and R1
;   2. Trigger a fault: write to reserved address (causes MFR=0001)
;   3. Verify: fault handler at address 20 executes (HLT)
;      - PC saved to location 4 = address of faulting instruction
;      - MFR = 0001
;
; EXPECTED FINAL STATE:
;   R0 = 42
;   R1 = 100
;   MFR = 0001 (reserved address fault)
;   Memory[4] = 11 (PC at time of fault, saved by raiseFault)
;   PC = 20 (fault handler address where HLT executed)
;
; NOTE: The fault handler at address 20 simply halts. A real handler
;       could inspect MFR, print an error, and resume via memory[4].

; === FAULT HANDLER (at address 20) ===
LOC 20
HLT              ; [20] Fault handler: just halt so we can inspect state

; === MAIN PROGRAM ===
LOC 8
LDR 0,0,6       ; [8]  R0 = mem[6] = 42
LDR 1,0,7       ; [9]  R1 = mem[7] = 100
AIR 0,5         ; [10] R0 = 42 + 5 = 47
STR 2,0,3       ; [11] Fault: write to reserved address 3 -> MFR=0001
HLT              ; [12] Should NOT execute — fault redirects to handler

; === DATA SECTION (after HLT per design convention) ===
LOC 1
DATA 20          ; mem[1] = 20: fault handler address
LOC 6
DATA 42          ; mem[6] = 42
DATA 100         ; mem[7] = 100
