package polygon.exception.downloading.packages

/**
 * No built packages exception.
 *
 * Thrown if there are no build packages for the problem.
 */
class NoPackagesBuiltException(message: String) : PackageException(message)
