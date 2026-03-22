; test_part1.asm
; Part 1 deliverable test: LDR, STR, LDA, LDX, STX
; Addressing modes covered: direct, indexed, indirect, indexed+indirect
;
; Data layout (placed after HLT per design convention):
;   mem[5]  = 42  direct load value
;   mem[6]  = 10  value to load into IX1
;   mem[7]  = 3   value to load into IX2
;   mem[8]  = 25  indirect pointer (I=1 -> EA = mem[8] = 25)
;   mem[11] = 27  indexed+indirect pointer (IX2+8=11 -> EA = mem[11] = 27)
;   mem[15] = 88  indexed load target (IX1+5 = 10+5 = 15)
;   mem[25] = 99  indirect load target (mem[mem[8]] = mem[25])
;   mem[27] = 66  indexed+indirect target (mem[mem[11]] = mem[27])

LOC 50
LDR 0,0,5       ; [1] direct:           R0 = mem[5] = 42
LDA 1,0,5       ; [2] load address:     R1 = EA = 5
LDX 1,6         ; [3] load IX1:         IX1 = mem[6] = 10
LDX 2,7         ; [4] load IX2:         IX2 = mem[7] = 3
LDR 2,1,5       ; [5] indexed:          EA=IX1+5=15, R2=mem[15]=88
LDR 3,0,8,1     ; [6] indirect:         EA=mem[8]=25, R3=mem[25]=99
LDR 0,2,8,1     ; [7] indexed+indirect: EA1=IX2+8=11, EA=mem[11]=27, R0=mem[27]=66
STR 0,0,20      ; [8] store direct:     mem[20] = R0 = 66
STX 1,21        ; [9] store IX1:        mem[21] = IX1 = 10
HLT

; === DATA SECTION (after HLT per design convention) ===
LOC 5
DATA 42     ; mem[5]  = 42
DATA 10     ; mem[6]  = 10  (IX1 value)
DATA 3      ; mem[7]  = 3   (IX2 value)
DATA 25     ; mem[8]  = 25  (indirect pointer)
LOC 11
DATA 27     ; mem[11] = 27  (indexed+indirect pointer)
LOC 15
DATA 88     ; mem[15] = 88  (indexed target: IX1+5=15)
LOC 25
DATA 99     ; mem[25] = 99  (indirect target)
LOC 27
DATA 66     ; mem[27] = 66  (indexed+indirect target)
