@file:Suppress("MemberVisibilityCanBePrivate")

package sybon

object SybonCompilers {
    val C = Compiler(
        id = 1,
        type = Compiler.Type.gcc,
        name = "C",
        description = "C11",
        args = "lang=c,optimize=2,std=c11,fno-stack-limit,lm",
        timeLimitMillis = 60000,
        memoryLimitBytes = 1610612736,
        numberOfProcesses = 1000,
        outputLimitBytes = 536870912,
        realTimeLimitMillis = 120000
    )
    val CPP = Compiler(
        id = 2,
        type = Compiler.Type.gcc,
        name = "C++",
        description = "C++11",
        args = "lang=c++,optimize=2,std=c++11,fno-stack-limit",
        timeLimitMillis = 60000,
        memoryLimitBytes = 1610612736,
        numberOfProcesses = 1000,
        outputLimitBytes = 536870912,
        realTimeLimitMillis = 120000
    )
    val CSHARP = Compiler(
        id = 3,
        type = Compiler.Type.mono,
        name = "C#",
        description = "mono",
        args = "lang=d",
        timeLimitMillis = 60000,
        memoryLimitBytes = 1610612736,
        numberOfProcesses = 1000,
        outputLimitBytes = 536870912,
        realTimeLimitMillis = 120000
    )
    val DELPHI = Compiler(
        id = 4,
        type = Compiler.Type.fpc,
        name = "Delphi",
        description = "FreePascal PascalABC",
        args = "lang=delphi,optimize=2",
        timeLimitMillis = 60000,
        memoryLimitBytes = 1610612736,
        numberOfProcesses = 1000,
        outputLimitBytes = 536870912,
        realTimeLimitMillis = 120000
    )
    val PASCAL = Compiler(
        id = 5,
        type = Compiler.Type.fpc,
        name = "Pascal",
        description = "FreePascal",
        args = "lang=fpc,optimize=2",
        timeLimitMillis = 60000,
        memoryLimitBytes = 1610612736,
        numberOfProcesses = 1000,
        outputLimitBytes = 536870912,
        realTimeLimitMillis = 120000
    )
    val PYTHON2 = Compiler(
        id = 6,
        type = Compiler.Type.python,
        name = "Python 2",
        description = "Python 2",
        args = "lang=2",
        timeLimitMillis = 60000,
        memoryLimitBytes = 1610612736,
        numberOfProcesses = 1000,
        outputLimitBytes = 536870912,
        realTimeLimitMillis = 120000
    )
    val PYTHON3 = Compiler(
        id = 7,
        type = Compiler.Type.python,
        name = "Python 3",
        description = "Python 3",
        args = "lang=3",
        timeLimitMillis = 60000,
        memoryLimitBytes = 1610612736,
        numberOfProcesses = 1000,
        outputLimitBytes = 536870912,
        realTimeLimitMillis = 120000
    )
    val JAVA = Compiler(
        id = 8,
        type = Compiler.Type.java,
        name = "Java",
        description = "Java",
        args = "",
        timeLimitMillis = 60000,
        memoryLimitBytes = 1610612736,
        numberOfProcesses = 1000,
        outputLimitBytes = 536870912,
        realTimeLimitMillis = 120000
    )

    val list = listOf(
        C,
        CPP,
        CSHARP,
        DELPHI,
        PASCAL,
        PYTHON2,
        PYTHON3,
        JAVA
    )
}