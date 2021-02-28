@file:Suppress("MemberVisibilityCanBePrivate")

package sybon

import sybon.api.SybonCompiler

/**
 * Sybon compilers.
 *
 * Contains all known Sybon compilers in one place.
 *
 * Call [SybonCompilers.list] to get the list of these compilers.
 */
object SybonCompilers {
    val C = SybonCompiler(
        id = 1,
        type = SybonCompiler.Type.gcc,
        name = "C",
        description = "C11",
        args = "lang=c,optimize=2,std=c11,fno-stack-limit,lm",
        timeLimitMillis = 60000,
        memoryLimitBytes = 1610612736,
        numberOfProcesses = 1000,
        outputLimitBytes = 536870912,
        realTimeLimitMillis = 120000
    )
    val CPP = SybonCompiler(
        id = 2,
        type = SybonCompiler.Type.gcc,
        name = "C++",
        description = "C++11",
        args = "lang=c++,optimize=2,std=c++11,fno-stack-limit",
        timeLimitMillis = 60000,
        memoryLimitBytes = 1610612736,
        numberOfProcesses = 1000,
        outputLimitBytes = 536870912,
        realTimeLimitMillis = 120000
    )
    val CSHARP = SybonCompiler(
        id = 3,
        type = SybonCompiler.Type.mono,
        name = "C#",
        description = "mono",
        args = "lang=d",
        timeLimitMillis = 60000,
        memoryLimitBytes = 1610612736,
        numberOfProcesses = 1000,
        outputLimitBytes = 536870912,
        realTimeLimitMillis = 120000
    )
    val DELPHI = SybonCompiler(
        id = 4,
        type = SybonCompiler.Type.fpc,
        name = "Delphi",
        description = "FreePascal PascalABC",
        args = "lang=delphi,optimize=2",
        timeLimitMillis = 60000,
        memoryLimitBytes = 1610612736,
        numberOfProcesses = 1000,
        outputLimitBytes = 536870912,
        realTimeLimitMillis = 120000
    )
    val PASCAL = SybonCompiler(
        id = 5,
        type = SybonCompiler.Type.fpc,
        name = "Pascal",
        description = "FreePascal",
        args = "lang=fpc,optimize=2",
        timeLimitMillis = 60000,
        memoryLimitBytes = 1610612736,
        numberOfProcesses = 1000,
        outputLimitBytes = 536870912,
        realTimeLimitMillis = 120000
    )
    val PYTHON2 = SybonCompiler(
        id = 6,
        type = SybonCompiler.Type.python,
        name = "Python 2",
        description = "Python 2",
        args = "lang=2",
        timeLimitMillis = 60000,
        memoryLimitBytes = 1610612736,
        numberOfProcesses = 1000,
        outputLimitBytes = 536870912,
        realTimeLimitMillis = 120000
    )
    val PYTHON3 = SybonCompiler(
        id = 7,
        type = SybonCompiler.Type.python,
        name = "Python 3",
        description = "Python 3",
        args = "lang=3",
        timeLimitMillis = 60000,
        memoryLimitBytes = 1610612736,
        numberOfProcesses = 1000,
        outputLimitBytes = 536870912,
        realTimeLimitMillis = 120000
    )
    val JAVA = SybonCompiler(
        id = 8,
        type = SybonCompiler.Type.java,
        name = "Java",
        description = "Java",
        args = "",
        timeLimitMillis = 60000,
        memoryLimitBytes = 1610612736,
        numberOfProcesses = 1000,
        outputLimitBytes = 536870912,
        realTimeLimitMillis = 120000
    )

    /**
     * List of all the sybon compilers.
     */
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