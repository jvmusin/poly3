package polygon

import ir.IRProblem

interface PolygonProblemDownloader {
    suspend fun download(problemId: Int, onlyEssentials: Boolean = false): IRProblem
}