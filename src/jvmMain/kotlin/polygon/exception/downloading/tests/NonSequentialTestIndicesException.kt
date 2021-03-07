package polygon.exception.downloading.tests

/**
 * Non sequential test indices exception.
 *
 * Thrown if test indices don't go in order.
 */
class NonSequentialTestIndicesException(message: String) : TestsException(message)
