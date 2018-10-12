package noe.common.utils

import java.util.regex.Matcher
import java.util.regex.Pattern


class TextProcessing {

  static final Pattern errorPattern = Pattern.compile('^.*(exception|error|fail|sever|warn|usage|requir|invalid).*', Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)

  private TextProcessing() {
  }

  public static String firstMatchAnyPattern(final String str, Collection<Pattern> patterns) {
    Matcher m
    for (Pattern p : patterns) {
      m = p.matcher(str)
      if (m.find()) return m.group()
    }
    return null
  }

  public static boolean matchesAnyPattern(final String str, Collection<Pattern> patterns) {
    return firstMatchAnyPattern(str, patterns) != null
  }

  // exceptions are patterns matched against the matching portion of the
  // "patterns" patterns, so make sure "patterns" match long enough portion
  public static String firstMatchAnyPattern(final String str, Collection<Pattern> patterns, Collection<Pattern> exceptions) {
    Matcher m
    for (Pattern p : patterns) {
      m = p.matcher(str)
      while (m.find()) {
        if (!matchesAnyPattern(m.group(), exceptions)) {
          return m.group()
        }
      }
    }
    return null
  }

  public static boolean matchesAnyPattern(final String str, Collection<Pattern> patterns, Collection<Pattern> exceptions) {
    return firstMatchAnyPattern(str, patterns, exceptions) != null
  }

}
