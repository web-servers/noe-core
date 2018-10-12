package noe.common.utils

/**
 * Class to abstract out and simplify joining of separate path components.
 *
 * @author Pavel Reichl <preichl@redhat.com>
 *
 */

class PathHelper {
  static final Platform platform = new Platform()
  static final String sep = platform.sep

  static Boolean isAbsolute(final String p) {
    return (new File(p)).isAbsolute()
  }

  /**
   * Returns path string composed from components by adding system specific separator
   *
   * @param p the first component of newly created path
   * @param components other components of newly created path
   * @return the newly created path
   */
  static String join(boolean addExecSuffix = false, String p, String ...components) {
    String res = p

    for (String component : components) {
      if (isAbsolute(component)) {
        res = component
      } else if (res == '' || res[-1] == sep) {
        res += component
      } else {
        res += sep + component
      }
    }

    if (addExecSuffix) {
      res += '.' + platform.getScriptSuffix()
    }

    return res
  }
}
