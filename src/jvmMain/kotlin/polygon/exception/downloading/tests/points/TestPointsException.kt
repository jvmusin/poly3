package polygon.exception.downloading.tests.points

import polygon.exception.downloading.tests.TestsException

/**
 * Test points exception.
 *
 * Thrown if something bad happened to test points.
 */
abstract class TestPointsException(message: String) : TestsException(message)
