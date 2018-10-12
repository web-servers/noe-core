package noe.common.utils

/**
 * @author Jan Stourac <jstourac at redhat.com>
 *
 *  Format of epxected string:
 *
 *         X.Y.Zh
 *         + + ++
 *         | | |+----------+
 *         | | |           |
 *    +----+ | +----+      v
 *    |      v      |   Nano (incremental character)
 *    |    Minor    |
 *    v             v
 *  Major         Micro (incremental version)
 *
 */
public class OpenSslVersion implements Comparable<OpenSslVersion> {

  public static final int MAJOR_VERSION = 0
  public static final int MINOR_VERSION = 1
  public static final int INCREMENTAL_VERSION = 2
  public static final int INCREMENTAL_CHAR = 3

  private List version
  private final String versionString

  private int len() { return version.size() }

  /**
   * @param ver MajorVersion[.MinorVersion][.IncrementalVersion][.IncrementalCharacter]
   */
  public OpenSslVersion(String ver) {
    if (!ver?.trim()) {
      throw new IllegalArgumentException("Attribute version must be defined as non empty string")
    }
    this.versionString = ver.trim()

    List<String> tokenizedVersion = versionString.tokenize(".-")
    // 3rd element might have character, lets split it if it is there
    if (tokenizedVersion.size() > 2) {
        String thirdItem = tokenizedVersion.get(2)
        if (thirdItem.charAt(thirdItem.length() - 1).letter) {
            String incrementalPart = thirdItem.substring(0, thirdItem.length() - 1)
            String incrementalCharPart = thirdItem.charAt(thirdItem.length()-1)
            tokenizedVersion.set(2, incrementalPart)
            tokenizedVersion.add(3, incrementalCharPart)
        }
    }

    version = new Object[tokenizedVersion.size()]
    tokenizedVersion.eachWithIndex { item, i ->
      if (item.isInteger()) {
        version.putAt(i, item.toInteger())
      } else {
        version.putAt(i, item.toString())
      }
    }

    for (int i = version.size(); i <= INCREMENTAL_CHAR; i++) {
      version.add(0)
    }
  }

  public List getVersion() {
    return version
  }

  public int getMajorVersion() {
    return version[MAJOR_VERSION]
  }

  public int getMinorVersion() {
    return version[MINOR_VERSION]
  }

  public int getIncrementalVersion() {
    return version[INCREMENTAL_VERSION]
  }

  public String getIncrementalCharacter() {
    return version[INCREMENTAL_CHAR]
  }

  public String toString() {
    return versionString
  }

  /**
   * Counts version in format <MajorVersion>.<MinorVersion>.<IncrementalVersion><IncrementalChar>
   * If the original version doesn't consist of all parts, there are returned zeroes at its places.
   * Note addition of zeroes is handled as part of constructor.
   */
  public String baseVersionString() {
    String incrementalCharPart = (version[INCREMENTAL_CHAR] instanceof Integer) ? "" : incrementalCharacter
    return "${majorVersion}.${minorVersion}.${incrementalVersion}${incrementalCharPart}"
  }

  @Override
  public boolean equals(Object version) {
    if (version == null) {
      return false
    }

    if (version instanceof OpenSslVersion) {
      OpenSslVersion ver = (OpenSslVersion) version
      return this.version.equals(ver.getVersion()) || this.compareTo(ver) == 0
    } else if (version instanceof String) {
      OpenSslVersion ver = new OpenSslVersion(version)
      return this.version.equals(ver.getVersion()) || this.compareTo(ver) == 0
    } else {
      return false
    }
  }

  @Override
  public int hashCode() {
    if (this.version != null) {
      return this.version.hashCode()
    } else {
      return 0
    }
  }

  @Override
  public int compareTo(OpenSslVersion o) {
    if (o == null) {
      throw new IllegalArgumentException("Attribute version cannot be null")
    }
    if (o.version == null) {
      if (this.version == null) {
        return 0
      } else {
        throw new IllegalArgumentException("Attribute Version.version is null, but this.version isn't")
      }
    }

    if (this.len() >= o.len()) {
      return compareToImpl(o)
    }

    // Multiply by -1
    return -o.compareToImpl(this)
  }

  private int compareToImpl(OpenSslVersion o) {

    if (len() < o.len()) {
      throw new IllegalArgumentException("Length cannot be shorter than input's parameter.")
    }

    for (int i = 0; i < len() && i < o.len(); i++) {
      int diff = version.get(i) <=> o.getVersion().get(i)
      if (diff != 0) {
        return diff
      }
    }

    return len() <=> o.len()
  }
}
