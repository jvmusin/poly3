package polygon.exception.downloading.tests

/**
 * Non sequential tests in test group exception.
 *
 * Thrown if tests within a test group don't form a continuous sequence of it's indices.
 */
class NonSequentialTestsInTestGroupException(message: String) : TestsException(message)
