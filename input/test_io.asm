; Test I/O instructions — IN and OUT
; Type a character in the Console Keyboard field before running.
; The program reads it into R0, then prints it to the Console Printer.
LOC 6
IN 0,0       ; Read one character from keyboard (DEVID 0) into R0
OUT 0,1      ; Print R0 to console printer (DEVID 1)
IN 1,0       ; Read another character into R1
OUT 1,1      ; Print R1 to console printer
HLT
