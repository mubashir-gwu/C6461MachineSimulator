; test_faults.asm — Test machine fault handling
;
; EXPECTED BEHAVIOR:
;   Steps 0-2: Normal operations succeed (LDR, LDR, AIR)
;   Step 3: LDR from reserved address 3 triggers MFR=0001, machine halts
;   The HLT instruction at the end should NEVER execute.
;
; EXPECTED REGISTER STATE AFTER FAULT:
;   R0 = 47 (42 + 5 from AIR)
;   R1 = 100
;   R2 = 0 (fault fired before load completed)
;   MFR = 0001 (reserved address fault)
;   PC = 11 (address of the faulting LDR instruction)

LOC 6
DATA 42
DATA 100

LDR 0,0,6
LDR 1,0,7
AIR 0,5
LDR 2,0,3
HLT
