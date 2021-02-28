package polygon.exception

/**
 * Old built package exception.
 *
 * Thrown if last build package for the problem is not built against the latest problem revision.
 */
class OldBuiltPackageException(message: String) : PackageException(message)
