package polygon.exception.downloading.tests

/**
 * Test points disabled exception.
 *
 * Thrown if test groups are enabled, but points are disabled.
 */
class TestPointsDisabledException(message: String) : TestsException(message)
