package polygon.exception.response

/**
 * Test groups disabled exception.
 *
 * Thrown if test groups are disabled for the problem.
 */
class TestGroupsDisabledException(message: String) : PolygonResponseException(message)
