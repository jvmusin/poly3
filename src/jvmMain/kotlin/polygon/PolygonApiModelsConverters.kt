package polygon

fun Problem.toDto() = api.Problem(id, name, owner, api.Problem.AccessType.valueOf(accessType.name), latestPackage)
fun ProblemInfo.toDto() = api.ProblemInfo(inputFile, outputFile, interactive, timeLimit, memoryLimit)
