; test_all_ls.asm
; Comprehensive test for Load/Store instructions and addressing modes
; Addresses in instructions are <= 31.
; Comments include octal values for addresses and data.

; --- Data Section ---
LOC 10
Data 42       ; Direct Addressing: Data at 10 (octal 12) is 42 (octal 52)

LOC 15
Data 100      ; Indexed: Base address at 15 (octal 17) is 100 (octal 144)
LOC 110
Data 88       ; Data at 110 (octal 156) is 88 (octal 130)

LOC 20
Data 150      ; Indirect: Pointer at 20 (octal 24) is 150 (octal 226)
LOC 150
Data 99       ; Data at 150 (octal 226) is 99 (octal 143)

LOC 25
Data 200      ; Indexed-Indirect: Base address for pointer at 25 (octal 31)
LOC 26
Data 20       ; Index value at 26 (octal 32) is 20 (octal 24)
LOC 45
Data 300      ; Pointer at 45 (octal 55) is 300 (octal 454)
LOC 300
Data 77       ; Data at 300 (octal 454) is 77 (octal 115)


; --- Code Section ---
LOC 500		; Location 500 (octal 764)
; Direct Addressing
LDR 0,0,10      ; R0 <- c(10). R0 should be 42 (octal 52).
STR 0,0,11      ; c(11) <- R0. Addr 11 (octal 13) gets 42 (octal 52).

; Indexed Addressing
LDX 1,15        ; X1 <- c(15). X1 should be 100 (octal 144).
LDR 1,1,10      ; EA = c(X1) + 10 = 100 + 10 = 110 (octal 156). R1 <- c(110). R1 should be 88 (octal 130).
STR 1,1,11      ; EA = c(X1) + 11 = 100 + 11 = 111 (octal 157). c(111) <- R1.

; Indirect Addressing
LDR 2,0,20,1    ; EA = c(20) = 150 (octal 226). R2 <- c(150). R2 should be 99 (octal 143).
STR 2,0,20,1    ; EA = c(20) = 150 (octal 226). c(150) <- R2.

; Indexed-Indirect Addressing
LDX 2,26        ; X2 <- c(26). X2 should be 20 (octal 24).
LDR 3,2,25,1    ; Pointer address = c(X2) + 25 = 20 + 25 = 45 (octal 55).
                ; EA = c(45) = 300 (octal 454).
                ; R3 <- c(300). R3 should be 77 (octal 115).
HLT
