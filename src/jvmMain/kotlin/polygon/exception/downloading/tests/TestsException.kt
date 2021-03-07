package polygon.exception.downloading.tests

import polygon.exception.downloading.ProblemDownloadingException

/**
 * Tests exception.
 *
 * Thrown if something bad happened to tests of the problem.
 */
abstract class TestsException(message: String) : ProblemDownloadingException(message)
