package polygon.api

fun Problem.toDto() = api.Problem(id, name, owner, api.ProblemAccessType.valueOf(accessType.name), latestPackage)
fun ProblemInfo.toDto() = api.ProblemInfo(inputFile, outputFile, interactive, timeLimit, memoryLimit)
