package sybon

import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.QualifierValue

object MainProblemArchive : Qualifier {
    override val value: QualifierValue get() = "Main sybon collection"
    const val collectionId = 1
}

object TestProblemArchive : Qualifier {
    override val value: QualifierValue get() = "Test sybon collection"
    const val collectionId = 10023
}