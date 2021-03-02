package sybon

import org.koin.core.qualifier.Qualifier

interface ProblemArchive : Qualifier {
    val collectionId: Int
}

object MainProblemArchive : ProblemArchive {
    override val collectionId = 1
    override val value = "Main sybon collection"
}

object TestProblemArchive : ProblemArchive {
    override val collectionId = 10023
    override val value = "Test sybon collection"
}
