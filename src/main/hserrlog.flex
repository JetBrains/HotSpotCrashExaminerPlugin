// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.language;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import com.github.mkartashev.hserr.language.psi.HsErrTypes;
%%

%class _HsErrLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType

EOL = \r|\n|\r\n
CHAR = [^\r\n]
WHITE_SPACE = {EOL} | [ \t\f]
STRING = \' ([^\'\r\n]|\\\')* \' | \" ([^\"\r\n]|\\\")* \"
NUMBER = [0-9]+ | 0x[0-9a-fA-F]+ | [0-9a-fA-F][0-9a-fA-F]+
JIDENTIFIER = [[:jletter:]\$] ([:jletterdigit:] | \$ | _)*
PUNCT_IMPORTANT = \( | \) | \[ | \] | \{ | \} | = | \| | < | > | ,
PUNCT = : | ; | , | \. | \? | \! | @ | \$ | % | \^ |
        & | \* | # | \- | \+ |  \\ | \~

SPECIAL = [$-_.+!*'(),/] | [:jletterdigit:]

SIGNAL = SIGHUP | SIGINT | SIGQUIT | SIGILL | SIGTRAP | SIGABRT | SIGEMT | SIGFPE |
         SIGKILL | SIGBUS | SIGSEGV | SIGSYS | SIGPIPE | SIGALRM | SIGTERM | SIGURG |
         SIGSTOP | SIGTSTP | SIGCONT | SIGCHLD | SIGTTIN | SIGTTOU | SIGIO |
         SIGXCPU | SIGXFSZ | SIGVTALRM | SIGPROF | SIGWINCH | SIGINFO | SIGUSR1 | SIGUSR2 |
         EXCEPTION_ACCESS_VIOLATION | EXCEPTION_IN_PAGE_ERROR

ENV_VARS = JAVA_HOME | JAVA_TOOL_OPTIONS | _JAVA_OPTIONS | CLASSPATH |
           PATH | USERNAME | LD_LIBRARY_PATH | LD_PRELOAD | SHELL | DISPLAY |
           WAYLAND_DISPLAY | HOSTTYPE | OSTYPE | ARCH | MACHTYPE | LANG |
           LC_ALL | LC_CTYPE | LC_NUMERIC | LC_TIME | LC_MESSAGES | LC_COLLATE |
           LC_MONETARY | TERM | TMPDIR | TZ | LIBPATH |
           LDR_PRELOAD | LDR_PRELOAD64 | _JAVA_SR_SIGNUM | DYLD_LIBRARY_PATH |
           DYLD_FALLBACK_LIBRARY_PATH | DYLD_FRAMEWORK_PATH | DYLD_FALLBACK_FRAMEWORK_PATH |
           DYLD_INSERT_LIBRARIES | OS | PROCESSOR_IDENTIFIER | _ALT_JAVA_HOME_DIR | TMP | TEMP

REGISTERS = pc | sp | x[0-9] | x[1-2][0-9] | cpsr | RIP | EFLAGS | RAX | RBX | RCX | RDX | RSI | RDI |
            RSP | RBP | R8 | R9 | R1[0-5] | ERR | TRAPNO | fp | lr | CSGSFS

KEYWORDS = JRE\ version | Java\ VM | Problematic\ frame | Current\ thread | current\ thread |
           id | stack | si_signo | si_code | si_addr |
           _thread_in_[:jletter:]+ | _thread_blocked | C1 | C2 | c1 | c2 | Thread | Exception | NULL |
           JavaThread | VMThread | WatcherThread | GCTaskThread | ConcurrentGCThread | safepoint |
           reserved\ size | Narrow\ klass\ base | Narrow\ klass\ shift | Narrow\ klass\ range |
           Possible\ reasons | Possible\ solutions | Out\ of\ Memory\ Error | daemon |
           thread | unknown\ value | unknown\ readable\ memory | nmethod | total | used |
           committed | reserved | OutOfMemoryError | Halt | SafepointALot | Cleanup |
           ForceSafepoint | ICBufferFull | ClearICs | CleanClassLoaderDataMetaspaces |
           DeoptimizeFrame | DeoptimizeAll | ZombieAll | PrintThreads | PrintMetadata |
           FindDeadlocks | ThreadDump | Exit | PrintCompileQueue | PrintClassHierarchy |
           HandshakeAllThreads | size | free | max_used | before | after | class | Class |
           true | false | stdout | stderr | Event |
           {ENV_VARS}

SECTIONS = Current\ CompileTask | Instructions | Register\ to\ memory\ mapping |
              Registers | Top\ of\ Stack | Stack\ slot\ to\ memory\ mapping |
              Threads\ class\ SMR\ info | Java\ Threads | Other\ Threads |
              Threads\ with\ active\ compile\ tasks | GC\ Precious\ Log |
              Heap\ Regions | Metaspace | Usage | Virtual\ space | Chunk\ freelists |
              Internal\ statistics | Dynamic\ libraries |
              VM\ Arguments | Logging | Log\ output\ configuration |
              Environment\ Variables | Signal\ Handlers | Process\ Memory |
              Time | Host | Command\ Line | Stack | Native\ frames | Java\ frames |
              VM\ state | VM\ Mutex\/Monitor\ currently\ owned\ by\ a\ thread |
              Heap\ address | Compressed\ class\ space\ mapped\ at | Card\ table\ byte_map |
              Narrow\ klass\ base |
              Polling\ page | Marking\ Bits | jvm_args | java_command | java_class_path |
              uname | OS uptime | rlimit | CPU | Memory | vm_info | libc |
              Launcher\ Type | dbghelp | symbol\ engine| siginfo | Current\ thread |
              load\ average | libc | OS\ uptime | MaxMetaspaceSize | CompressedClassSpaceSize |
              Initial\ GC\ threshold | Current\ GC\ threshold | CDS | MetaspaceReclaimPolicy |
              CodeHeap

HEADER = END\. | Heap | Dynamic\ libraries | VM\ Arguments | Logging | Environment\ Variables |
         Signal\ Handlers | Active\ Locale | Process\ memory\ usage | Native\ Memory\ Tracking

EVENT = Compilation\ events | GC\ Heap\ History | Dll\ operation\ events | Deoptimization\ events |
        Classes\ loaded | ZGC\ Phase\ Switch |
        Classes\ unloaded | Classes\ redefined | Internal\ exceptions | VM\ Operations | Events

SCOPE_DELIM = \/ | \. | ::

%%

<YYINITIAL> {
    "---------------" {CHAR}* $ { return HsErrTypes.SECTION_HDR; }
    ^{HEADER}          { return HsErrTypes.SECTION_HDR; }
    ^{EVENT}           { return HsErrTypes.SECTION_HDR; }
    ^{SECTIONS}        { return HsErrTypes.SUBTITLE; }
    {SIGNAL}           { return HsErrTypes.SIGNAL; }
    {KEYWORDS}         { return HsErrTypes.KEYWORD; }
    {PUNCT_IMPORTANT}  { return HsErrTypes.PUNCT; }
    {STRING}           { return HsErrTypes.STRING; }
    {NUMBER}           { return HsErrTypes.NUMBER; }
    {REGISTERS}        { return HsErrTypes.REGISTER; }
    {JIDENTIFIER}:\/\/[:jletterdigit:]+{SPECIAL}*        { return HsErrTypes.URL; }
    _Z[A-Za-z0-9_]+    { return HsErrTypes.IDENTIFIER; }
    {JIDENTIFIER}({SCOPE_DELIM}{JIDENTIFIER})+  { return HsErrTypes.IDENTIFIER; }
    {JIDENTIFIER}\+0                 { return HsErrTypes.IDENTIFIER; }
    {JIDENTIFIER}\+0x[0-9a-fA-F]+    { return HsErrTypes.IDENTIFIER; }
    {WHITE_SPACE}+     { return HsErrTypes.WHITE_SPACE; }
    {PUNCT}+           { return HsErrTypes.WHITE_SPACE; }
    (\w)+              { return HsErrTypes.WORD; }
    [^]                { return HsErrTypes.WORD; }
}