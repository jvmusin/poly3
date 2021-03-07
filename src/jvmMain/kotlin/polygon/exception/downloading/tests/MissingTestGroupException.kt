package polygon.exception.downloading.tests

/**
 * Missing test group exception.
 *
 * Thrown if some test doesn't have a test group but it should.
 */
class MissingTestGroupException(message: String) : TestsException(message)
