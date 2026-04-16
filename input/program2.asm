; ============================================================================
; Program 2: Paragraph Word Search
; CSCI 6461 - C6461 Machine Simulator
; ============================================================================
;
; REQUIREMENT:
; Read a paragraph of 6 sentences from a file (card reader). Print them
; on the console printer. Ask the user for a word. Search the paragraph
; for the word. If found, print sentence number and word number.
; If not found, print "No".
;
; ALGORITHM:
; 1. Read all characters from card reader into buffer at 500+
; 2. Print the buffer to console printer
; 3. Prompt user, read search word into buffer at 820+
; 4. Walk through paragraph. At each word start (non-delimiter preceded
;    by delimiter), call CompareWord to check for a match.
; 5. On match: count sentences (periods) and words (spaces) from buffer
;    start to match position, then report results.
;
; A character is treated as a "delimiter" if its ASCII code < 48.
; This covers space (32), LF (10), CR (13), and period (46).
; All word characters (letters, digits) have ASCII >= 48.
;
; MEMORY MAP:
;   6-19:    Constants and jump target addresses
;   20-31:   Variables
;   50-59:   Phase 1 - Read file into buffer
;   60-73:   Phase 2 - Print buffer to printer
;   74-89:   Phase 3a - Print prompt
;   90-104:  Phase 3b - Read search word from keyboard
;   108-119: Phase 4 setup - Initialize search state
;   123-164: Phase 4 - Search loop + match handler
;   165-197: Phase 4 - Count sentence/word at match position
;   200-242: Phase 5 - Report results + HLT
;   250-285: PrintNum subroutine (print integer in R0 as decimal)
;   290-321: CompareWord subroutine (compare search word at SCANPTR)
;   500-799: Paragraph character buffer (runtime)
;   820-849: Search word buffer (runtime)
;   860-879: Digit stack for PrintNum (runtime)
;
; REGISTER CONVENTIONS:
;   R0: accumulator, number passing, general purpose
;   R1: I/O character, comparison operand
;   R2: counter (GPR2, separate from IX2)
;   R3: return address (set by JSR), temp
;   IX1: pointer into paragraph buffer
;   IX2: code base for IX-offset jumps
;   IX3: temp pointer
;
; ENTRY POINT: PC = 50 (auto-detected by loader)
; ============================================================================

; ============================================================================
; PHASE 1: Read file from card reader into buffer (Loc 50-59)
; ============================================================================

LOC 50
LDR 0,0,10        ; [50] R0 = c(10) = 500 (buffer base)
STR 0,0,20        ; [51] CURPTR = 500
IN 1,2            ; [52] ReadLoop: R1 = char from card reader (DEVID 2)
JZ 1,0,6,1        ; [53] If R1=0 (EOF) -> PC = c(6) = 60 (DoneRead)
LDX 3,20          ; [54] IX3 = c(20) = CURPTR
STR 1,3,0         ; [55] Memory[CURPTR] = R1
LDR 0,0,20        ; [56] R0 = CURPTR
AIR 0,1           ; [57] R0++
STR 0,0,20        ; [58] CURPTR = R0
JMA 0,7,1         ; [59] PC = c(7) = 52 (ReadLoop)

; ============================================================================
; PHASE 2: Print paragraph buffer to printer (Loc 60-73)
; ============================================================================

LOC 60
LDR 0,0,20        ; [60] DoneRead: R0 = CURPTR (one past last char)
STR 0,0,25        ; [61] ENDPTR = R0
LDR 0,0,10        ; [62] R0 = 500
STR 0,0,24        ; [63] SCANPTR = 500 (reused as print pointer)
LDR 0,0,24        ; [64] PrintLoop: R0 = SCANPTR
SMR 0,0,25        ; [65] R0 = SCANPTR - ENDPTR
JGE 0,0,8,1       ; [66] If past end -> PC = c(8) = 74 (PrintDone)
LDX 3,24          ; [67] IX3 = c(24) = SCANPTR
LDR 1,3,0         ; [68] R1 = Memory[SCANPTR]
OUT 1,1           ; [69] Print char to console printer (DEVID 1)
LDR 0,0,24        ; [70] R0 = SCANPTR
AIR 0,1           ; [71] R0++
STR 0,0,24        ; [72] SCANPTR = R0
JMA 0,9,1         ; [73] PC = c(9) = 64 (PrintLoop)

; ============================================================================
; PHASE 3a: Print prompt (Loc 74-89)
; ============================================================================

LOC 74
LDR 1,0,18        ; [74] PrintDone: R1 = c(18) = 10 (LF)
OUT 1,1           ; [75] Print newline
LDA 1,0,0         ; [76] R1 = 0
AIR 1,31          ; [77] R1 = 31
AIR 1,31          ; [78] R1 = 62
AIR 1,1           ; [79] R1 = 63 = '?'
OUT 1,1           ; [80] Print '?'
LDA 1,0,0         ; [81] R1 = 0
AIR 1,31          ; [82] R1 = 31
AIR 1,1           ; [83] R1 = 32 = ' '
OUT 1,1           ; [84] Print ' '
LDR 0,0,11        ; [85] R0 = c(11) = 820 (word buffer base)
STR 0,0,20        ; [86] CURPTR = 820
LDA 0,0,0         ; [87] R0 = 0
STR 0,0,26        ; [88] WORDLEN = 0
JMA 0,13,1      ; [89] PC = c(13) = 90 (ReadWordLoop)

; ============================================================================
; PHASE 3b: Read search word from keyboard (Loc 90-104)
; ============================================================================

LOC 90
IN 1,0            ; [90] ReadWordLoop: R1 = char from keyboard (DEVID 0)
OUT 1,1           ; [91] Echo char to printer
STR 1,0,23        ; [92] TEMP = R1 (save char)
SIR 1,13          ; [93] R1 = char - 13
JZ 1,0,12,1       ; [94] If CR (char=13) -> PC = c(12) = 108 (WordDone)
LDR 1,0,23        ; [95] R1 = char (restore from TEMP)
LDX 3,20          ; [96] IX3 = c(20) = CURPTR
STR 1,3,0         ; [97] Memory[CURPTR] = char
LDR 0,0,20        ; [98] R0 = CURPTR
AIR 0,1           ; [99] R0++
STR 0,0,20        ; [100] CURPTR = R0
LDR 0,0,26        ; [101] R0 = WORDLEN
AIR 0,1           ; [102] R0++
STR 0,0,26        ; [103] WORDLEN = R0
JMA 0,13,1      ; [104] PC = c(13) = 90 (ReadWordLoop)

; ============================================================================
; PHASE 4 SETUP: Initialize search state (Loc 108-119)
; ============================================================================

LOC 108
LDR 1,0,18        ; [108] WordDone: R1 = 10 (LF)
OUT 1,1           ; [109] Print newline after word input
LDA 0,0,0         ; [110] R0 = 0
STR 0,0,27        ; [111] FOUND = 0
LDA 0,0,1         ; [112] R0 = 1
STR 0,0,21        ; [113] ATSTART = 1 (beginning of buffer = word start)
LDR 0,0,10        ; [114] R0 = c(10) = 500
STR 0,0,24        ; [115] SCANPTR = 500
; Rewrite c(6) with AdvanceScan address (149):
LDR 0,0,16        ; [116] R0 = c(16) = 123
AIR 0,26          ; [117] R0 = 149
STR 0,0,6         ; [118] c(6) = 149 (AdvanceScan addr for search phase)
JMA 0,16,1      ; [119] PC = c(16) = 123 (SearchLoop)

; ============================================================================
; PHASE 4: Search loop (Loc 123-164)
; ============================================================================
; Walk through paragraph one character at a time.
; Use ATSTART flag (loc 21): 1 = previous char was delimiter or at start.
; When ATSTART=1 and current char is non-delimiter: word start, try match.
; When delimiter: set ATSTART=1 and advance.
; When non-delimiter and ATSTART=0: just advance.
;
; IX2 = 123 for IX-offset jumps within 123-154.
; All indirect jumps use c(N) where N is a DATA/variable location.

LOC 123
LDR 0,0,24        ; [123] SearchLoop: R0 = SCANPTR
SMR 0,0,25        ; [124] R0 = SCANPTR - ENDPTR
JGE 0,0,17,1      ; [125] If past end -> PC = c(17) = 200 (Results)
LDX 2,16          ; [126] IX2 = c(16) = 123 (search base)
LDX 1,24          ; [127] IX1 = c(24) = SCANPTR
LDR 0,1,0         ; [128] R0 = Memory[SCANPTR] (current char)
STR 0,0,30        ; [129] PCHAR = R0
; Check if delimiter (char < 48):
SIR 0,31          ; [130] R0 -= 31
SIR 0,17          ; [131] R0 = PCHAR - 48
JGE 0,2,20        ; [132] If char >= 48 (non-delim) -> NonDelim at 123+20=143
; Delimiter found:
LDA 0,0,1         ; [133] R0 = 1
STR 0,0,21        ; [134] ATSTART = 1
JMA 2,26        ; [135] -> AdvanceScan at 123+26 = 149

; (136-142: unused, between handlers)

LOC 143
; --- NonDelim (143): current char is a word character ---
LDR 0,0,21        ; [143] R0 = ATSTART
JNE 0,2,30        ; [144] If ATSTART=1 -> TryMatch at 123+30 = 153
JMA 2,26        ; [145] ATSTART=0: not word start -> AdvanceScan (149)

; (146-148: unused)

LOC 149
; --- AdvanceScan (149): move to next character ---
LDR 0,0,24        ; [149] R0 = SCANPTR
AIR 0,1           ; [150] R0++
STR 0,0,24        ; [151] SCANPTR = R0
JMA 0,16,1      ; [152] PC = c(16) = 123 (SearchLoop)

; --- TryMatch (153): word start, attempt to match search word ---
LOC 153
LDA 0,0,0         ; [153] R0 = 0
STR 0,0,21        ; [154] ATSTART = 0 (clear flag)
JSR 0,19,1      ; [155] Call CompareWord: R3=156, PC = c(19) = 290
JZ 0,0,6,1        ; [156] If R0=0 (no match) -> PC = c(6) = 149 (AdvanceScan)
; Match found!
LDA 0,0,1         ; [157] R0 = 1
STR 0,0,27        ; [158] FOUND = 1
; Compute CountSection base (164) and jump there:
LDR 0,0,16        ; [159] R0 = c(16) = 123
AIR 0,31          ; [160] R0 = 154
AIR 0,10          ; [161] R0 = 164
STR 0,0,23        ; [162] TEMP = 164
LDX 2,23          ; [163] IX2 = c(23) = 164
JMA 2,1           ; [164] PC = 164 + 1 = 165

; ============================================================================
; PHASE 4: Count sentence and word numbers at match position (Loc 165-197)
; ============================================================================
; Scan from buffer start (500) to SCANPTR, counting '.' for sentence
; number and spaces for word number within the current sentence.
; IX2 = 164 for IX-offset jumps.

LOC 165
LDA 0,0,1         ; [165] R0 = 1
STR 0,0,28        ; [166] MATCHSENT = 1
LDA 0,0,0         ; [167] R0 = 0
STR 0,0,29        ; [168] spaceCount = 0
LDR 0,0,10        ; [169] R0 = c(10) = 500
STR 0,0,20        ; [170] k = 500 (counting pointer)
; --- CountLoop (171): check if k reached match position ---
LDR 0,0,20        ; [171] R0 = k
SMR 0,0,24        ; [172] R0 = k - SCANPTR (match position)
JGE 0,0,17,1      ; [173] If k >= matchPos -> PC = c(17) = 200 (Results)
LDX 3,20          ; [174] IX3 = c(20) = k
LDR 1,3,0         ; [175] R1 = Memory[k] (paragraph char)
STR 1,0,30        ; [176] PCHAR = R1
; Check if '.' (ASCII 46):
SIR 1,31          ; [177] R1 -= 31
SIR 1,15          ; [178] R1 = char - 46
JNE 1,2,22        ; [179] If not '.' -> CheckSpace at 164+22 = 186
; --- DotHandler (180): period found, increment sentence ---
LDR 0,0,28        ; [180] R0 = MATCHSENT
AIR 0,1           ; [181] R0++
STR 0,0,28        ; [182] MATCHSENT = R0
LDA 0,0,0         ; [183] R0 = 0
STR 0,0,29        ; [184] spaceCount = 0 (reset for new sentence)
JMA 2,29          ; [185] -> AdvanceK at 164+29 = 193
; --- CheckSpace (186): check if space (ASCII 32) ---
LDR 1,0,30        ; [186] R1 = PCHAR (reload original char)
SIR 1,31          ; [187] R1 -= 31
SIR 1,1           ; [188] R1 = char - 32
JNE 1,2,29        ; [189] If not space -> AdvanceK at 193
; Space found: increment space count
LDR 0,0,29        ; [190] R0 = spaceCount
AIR 0,1           ; [191] R0++
STR 0,0,29        ; [192] spaceCount = R0
; --- AdvanceK (193): advance counting pointer ---
LDR 0,0,20        ; [193] R0 = k
AIR 0,1           ; [194] R0++
STR 0,0,20        ; [195] k = R0
JMA 2,7           ; [196] -> CountLoop at 164+7 = 171

; ============================================================================
; PHASE 5: Report results (Loc 200-242)
; ============================================================================
; If FOUND=1: MATCHWRD = spaceCount+1, print "S<N> W<M>" + newline
; If FOUND=0: print "No" + newline

LOC 200
; Compute MATCHWRD = spaceCount + 1:
LDR 0,0,29        ; [200] R0 = spaceCount
AIR 0,1           ; [201] R0 = spaceCount + 1
STR 0,0,29        ; [202] MATCHWRD = R0
; Build NotFound address (229) and store in c(7):
LDR 0,0,15        ; [203] R0 = c(15) = 250
SIR 0,21          ; [204] R0 = 229
STR 0,0,7         ; [205] c(7) = 229 (NotFound address)
; Check FOUND flag:
LDR 0,0,27        ; [206] R0 = FOUND
JZ 0,0,7,1        ; [207] If FOUND=0 -> PC = c(7) = 229 (NotFound)

; --- Found path (208-228): print "S<N> W<M>" ---
; Print 'S' (ASCII 83 = 31+31+21):
LDA 1,0,0         ; [208] R1 = 0
AIR 1,31          ; [209] R1 = 31
AIR 1,31          ; [210] R1 = 62
AIR 1,21          ; [211] R1 = 83 = 'S'
OUT 1,1           ; [212] Print 'S'
; Print sentence number:
LDR 0,0,28        ; [213] R0 = MATCHSENT
JSR 0,15,1      ; [214] PrintNum(MATCHSENT): R3=215, PC = c(15) = 250
; Print ' ' (ASCII 32 = 31+1):
LDA 1,0,0         ; [215] R1 = 0
AIR 1,31          ; [216] R1 = 31
AIR 1,1           ; [217] R1 = 32 = ' '
OUT 1,1           ; [218] Print ' '
; Print 'W' (ASCII 87 = 31+31+25):
LDA 1,0,0         ; [219] R1 = 0
AIR 1,31          ; [220] R1 = 31
AIR 1,31          ; [221] R1 = 62
AIR 1,25          ; [222] R1 = 87 = 'W'
OUT 1,1           ; [223] Print 'W'
; Print word number:
LDR 0,0,29        ; [224] R0 = MATCHWRD
JSR 0,15,1      ; [225] PrintNum(MATCHWRD): R3=226, PC = c(15) = 250
; Print newline:
LDR 1,0,18        ; [226] R1 = c(18) = 10 (LF)
OUT 1,1           ; [227] Print newline
HLT              ; [228] Stop

; --- NotFound path (229-242): print "No" ---
LOC 229
; Print 'N' (ASCII 78 = 31+31+16):
LDA 1,0,0         ; [229] R1 = 0
AIR 1,31          ; [230] R1 = 31
AIR 1,31          ; [231] R1 = 62
AIR 1,16          ; [232] R1 = 78 = 'N'
OUT 1,1           ; [233] Print 'N'
; Print 'o' (ASCII 111 = 31+31+31+18):
LDA 1,0,0         ; [234] R1 = 0
AIR 1,31          ; [235] R1 = 31
AIR 1,31          ; [236] R1 = 62
AIR 1,31          ; [237] R1 = 93
AIR 1,18          ; [238] R1 = 111 = 'o'
OUT 1,1           ; [239] Print 'o'
; Print newline:
LDR 1,0,18        ; [240] R1 = c(18) = 10 (LF)
OUT 1,1           ; [241] Print newline
HLT              ; [242] Stop

; ============================================================================
; PrintNum SUBROUTINE (Loc 250-285)
; ============================================================================
; Prints an unsigned integer in R0 to the console printer as decimal.
; Numbers in this program are always small positive (sentence/word numbers).
;
; Entry: JSR 0,15,1 (R3 = return addr, R0 = number to print)
; Exit:  returns to caller via JMA 0,31,1 (PC = c(31) = saved R3)
; Clobbers: R0, R1, R2, R3, IX2, IX3
; Uses: TEMP (loc 23) as digit count, PCHAR (loc 30) as temp addr,
;       SAVR3 (loc 31) for return address, digit stack at c(14)=860

LOC 250
STR 3,0,31        ; [250] SAVR3 = R3 (save return address)
LDX 2,15          ; [251] IX2 = c(15) = 250 (PrintNum base)
JNE 0,2,9         ; [252] If R0 != 0 -> DivStart at 250+9 = 259
; Zero case: print '0' and return
LDA 1,0,0         ; [253] R1 = 0
AIR 1,31          ; [254] R1 = 31
AIR 1,17          ; [255] R1 = 48 = '0'
OUT 1,1           ; [256] Print '0'
JMA 0,31,1      ; [257] Return: PC = c(31) = saved return addr
LDA 0,0,0         ; [258] (unused padding)
; --- DivStart (259): extract digits by repeated division by 10 ---
LDA 3,0,0         ; [259] R3 = 0 (preserves R0 which has the number)
STR 3,0,23        ; [260] TEMP = 0 (digit count)
; --- DivLoop (261, offset 11): ---
LDR 2,0,18        ; [261] R2 = c(18) = 10 (divisor)
DVD 0,2           ; [262] R0 = quotient, R1 = remainder (digit)
; Store digit at stack[DIGCNT]:
LDR 3,0,14        ; [263] R3 = c(14) = 860 (digit stack base)
AMR 3,0,23        ; [264] R3 = 860 + DIGCNT
STR 3,0,30        ; [265] PCHAR = computed stack addr
LDX 3,30          ; [266] IX3 = stack addr
STR 1,3,0         ; [267] Memory[stack addr] = digit (R1)
LDR 3,0,23        ; [268] R3 = DIGCNT
AIR 3,1           ; [269] R3++
STR 3,0,23        ; [270] DIGCNT = R3
JNE 0,2,11        ; [271] If quotient != 0 -> DivLoop at 250+11 = 261
; --- PrintDigits (272): print digits in reverse order ---
LDR 3,0,23        ; [272] R3 = DIGCNT
; --- PrintDigLoop (273, offset 23): ---
SIR 3,1           ; [273] R3-- (index = DIGCNT-1 down to 0)
STR 3,0,23        ; [274] save current index
LDR 1,0,14        ; [275] R1 = 860 (stack base)
AMR 1,0,23        ; [276] R1 = 860 + index
STR 1,0,30        ; [277] PCHAR = digit address
LDX 3,30          ; [278] IX3 = digit address
LDR 1,3,0         ; [279] R1 = digit value
AIR 1,31          ; [280] R1 = digit + 31
AIR 1,17          ; [281] R1 = digit + 48 (ASCII digit character)
OUT 1,1           ; [282] Print digit
LDR 3,0,23        ; [283] R3 = current index
JNE 3,2,23        ; [284] If index != 0 -> PrintDigLoop at 250+23 = 273
; Return to caller:
JMA 0,31,1      ; [285] PC = c(31) = saved return addr

; ============================================================================
; CompareWord SUBROUTINE (Loc 290-321)
; ============================================================================
; Compares WORDLEN characters of the paragraph starting at SCANPTR with
; the search word buffer starting at 820. Also checks that the character
; AFTER the matched region is a delimiter (ASCII < 48) or end of buffer.
;
; Entry: JSR 0,19,1 (R3 = return addr, SCANPTR at loc 24, WORDLEN at loc 26)
; Exit:  RFS 1 if full word match, RFS 0 if no match
;        (RFS sets R0 = immed and PC = c(R3))
; Clobbers: R0, R1, R2 (GPR2), IX1, IX2, IX3
; Does NOT modify R3 (GPR3) or SCANPTR (loc 24)
; Uses: CURPTR (loc 20) as working paragraph pointer,
;       WPTR (loc 22) as working word pointer

LOC 290
LDR 0,0,24        ; [290] R0 = SCANPTR
STR 0,0,20        ; [291] CURPTR = SCANPTR (working copy)
LDR 0,0,11        ; [292] R0 = c(11) = 820 (word buffer base)
STR 0,0,22        ; [293] WPTR = 820 (working copy)
LDR 2,0,26        ; [294] R2 = WORDLEN (GPR2, loop counter)
LDX 2,19          ; [295] IX2 = c(19) = 290 (CompareWord base for offsets)
; --- CmpLoop (296, offset 6): compare one character pair ---
LDX 1,20          ; [296] IX1 = c(20) = CURPTR (paragraph pointer)
LDR 0,1,0         ; [297] R0 = Memory[CURPTR] (paragraph char)
LDX 3,22          ; [298] IX3 = c(22) = WPTR (word pointer)
LDR 1,3,0         ; [299] R1 = Memory[WPTR] (search word char)
TRR 0,1           ; [300] Compare: sets CC bit 3 if equal
JCC 3,2,13        ; [301] If equal -> CharsEqual at 290+13 = 303
RFS 0             ; [302] Not equal: return 0 (no match)
; --- CharsEqual (303, offset 13): advance both pointers ---
LDR 0,0,20        ; [303] R0 = CURPTR
AIR 0,1           ; [304] R0++
STR 0,0,20        ; [305] CURPTR = R0
LDR 0,0,22        ; [306] R0 = WPTR
AIR 0,1           ; [307] R0++
STR 0,0,22        ; [308] WPTR = R0
SIR 2,1           ; [309] R2-- (GPR2: remaining char count)
JNE 2,2,6         ; [310] If count != 0 -> CmpLoop at 296 (290+6)
; --- All WORDLEN characters matched! Check right boundary ---
LDR 0,0,20        ; [311] R0 = CURPTR (points to char after last match)
SMR 0,0,25        ; [312] R0 = CURPTR - ENDPTR
JGE 0,2,29        ; [313] If at/past end of buffer -> match! RFS 1 at 319
; Not at end: check if next char is a delimiter (ASCII < 48)
LDX 3,20          ; [314] IX3 = CURPTR
LDR 1,3,0         ; [315] R1 = Memory[CURPTR] (next char after match)
SIR 1,31          ; [316] R1 -= 31
SIR 1,17          ; [317] R1 = char - 48
JGE 1,2,31        ; [318] If char >= 48 (non-delim) -> RFS 0 at 321 (no match)
RFS 1             ; [319] char < 48 (delimiter) -> word boundary match!
RFS 0             ; [320] (dead code, never reached)
RFS 0             ; [321] Not a word boundary: return 0

; ============================================================================
; DATA SECTION (after all HLT/RFS instructions, per design convention)
; ============================================================================
; DATA values > 1024 have non-zero upper bits that alias valid opcodes
; in the loader's entry point detection. Placing DATA after all code
; ensures the loader finds the first real instruction at address 50.

LOC 6
DATA 60           ; [6]  DoneRead address (rewritten to 149 during search)
DATA 52           ; [7]  ReadLoop address (rewritten to NotFound during results)
DATA 74           ; [8]  PrintDone address
DATA 64           ; [9]  PrintLoop address
DATA 500          ; [10] BUFBASE: paragraph buffer base address
DATA 820          ; [11] WORDBUF: search word buffer base address
DATA 108          ; [12] WordDone address
DATA 90           ; [13] ReadWordLoop address
DATA 860          ; [14] DIGSTK: digit stack base for PrintNum
DATA 250          ; [15] PrintNum subroutine address
DATA 123          ; [16] SearchLoop address
DATA 200          ; [17] ResultsSection address
DATA 10           ; [18] C10: constant 10 (LF char / division base)
DATA 290          ; [19] CompareWord subroutine address

LOC 20
DATA 0            ; [20] CURPTR: current write pointer / working pointer
DATA 0            ; [21] ATSTART: word-start flag (1=prev was delimiter)
DATA 0            ; [22] WPTR: word buffer comparison pointer
DATA 0            ; [23] TEMP: general purpose temp variable
DATA 0            ; [24] SCANPTR: scan/print pointer
DATA 0            ; [25] ENDPTR: end of paragraph buffer
DATA 0            ; [26] WORDLEN: length of search word
DATA 0            ; [27] FOUND: match found flag (0=no, 1=yes)
DATA 0            ; [28] MATCHSENT: sentence number of match
DATA 0            ; [29] MATCHWRD: word number of match / space count
DATA 0            ; [30] PCHAR: current character being examined
DATA 0            ; [31] SAVR3: saved return address for PrintNum
