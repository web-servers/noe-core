package noe.common.utils


/**
 * @author jstefl , rhatlapa
 * @author Pavel Reichl <preichl@redhat.com>
 *
 *  Some examples of version comparison logic:
 *
 *         X.Y.Z == X.Y.Z.GA
 *         + + +           +
 *         | | |           |
 *         | | |           |
 *    +----+ | +----+      v
 *    |      v      |   Specially tagged release - General Availability.
 *    |    Minor    |
 *    v             v
 *  Major         Micro
 *
 *   'GA' should only be at the forth or fifth (following 'SP') field of the version.
 *
 *   X == X.0 == X.0.0 == X.0.0.GA // Missing minor and incremental version is substituted for 0.
 *
 *   X.Y.Z.DRX|ERX|CRX < X.Y.Z
 *          +   +   +
 *          |   |   |
 *          +---+---+
 *              v
 *         Prereleases - Specially tagged releases that precede GA build
 *         DR1 < DR2 ... < DRN
 *         ER1 < ER2 ... < ERN
 *         CR1 < CR2 ... < CRN
 *
 *    DRX < ERY < CRZ  // for any X,Y,Z from {1,2,...}
 *
 *    X.Y.Z.19700101 > X.Y.Z // Dated release is greater than non-dated
 *    X.Y.Z.SpecialRelease > X.Y.Z // Text tagged release is greater than non-text-tagged release
 *    X.Y.Z.19700101 > X.Y.Z.SpecialRelease // Dated release is greater than text-tagged release
 *
 *    No special check for date format is implemented, if field can be interpreted as Integer then it's considered as a
 *    date and compared as anInteger.
 *
 *    X.Y.Z.19700101 != X.Y.Z.197011 AND X.Y.Z.19700101 > X.Y.Z.197011
 *
 *    X.Y.Z.SP1 > X.Y.Z  // SP (Service Pack) tagged releases are released after 'GA' releases and can be followed by
 *                       // prerelease tags.
 *    X.Y.Z.SP1 > X.Y.Z.SP1.DR1
 *    X.Y.Z.SP1 == X.Y.Z.SP1.GA
 *
 */
public class Version implements Comparable<Version> {

  public static final int MAJOR_VERSION = 0
  public static final int MINOR_VERSION = 1
  public static final int INCREMENTAL_VERSION = 2
  public static final int BUILD_NUMBER = 3
  public static final int QUALIFIER = 4

  private List version
  private final String versionString
  private Vector<Node> l = []

  public Node at(int index) { return l[index] }
  public int len() { return l.size() }

  /**
   * @param ver MajorVersion[.MinorVersion][.IncrementalVersion][.BuildNumber].Qualifier
   */
  public Version(String ver) {
    this.versionString = ver
    if (!ver?.trim()) {
      throw new IllegalArgumentException("Attribute version must be defined as non empty string")
    }

    List<String> tokenizedVersion = ver.tokenize(".-")
    version = new Object[tokenizedVersion.size()]
    tokenizedVersion.eachWithIndex { item, i ->
      if (item.isInteger()) {
        version.putAt(i, item.toInteger())
      } else {
        version.putAt(i, item)
      }
      l << Node.build(item.trim())
    }

    for (int i = l.size(); i <= INCREMENTAL_VERSION; i++) {
      version.add(0)
      l << Node.build('0')
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

  public String getBuildNumber() {
    return version.size() > BUILD_NUMBER ? version[BUILD_NUMBER] : ""
  }

  public String getQualifier() {
    return version.size() > QUALIFIER ? version[QUALIFIER] : ""
  }

  public boolean isMajorVersion(int expVersion) {
    return getMajorVersion() == expVersion
  }

  public String toString() {
    return versionString
  }

  /**
   * Counts version in format MajorVersion.MinorVersion.IncrementalVersion
   * If the original version doesn't consist of all parts, there are returned zeroes at its places.
   * Note addition of zeroes is handled as part of constructor.
   */
  public String baseVersionString() {
    return "${majorVersion}.${minorVersion}.${incrementalVersion}"
  }

  @Override
  public boolean equals(Object version) {
    if (version == null) {
      return false
    }

    if (version instanceof Version) {
      Version ver = (Version) version
      return this.version.equals(ver.getVersion()) || this.compareTo(ver) == 0
    } else if (version instanceof String) {
      Version ver = new Version(version)
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
  public int compareTo(Version o) {
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

  private int compareToImpl(Version o) {

    if (len() < o.len()) {
      throw new IllegalArgumentException("Length cannot be shorter than input's parameter.")
    }

    for (int i = 0; i < l.size(); i++) {
      // One version is shorter
      if (o.len() == i) {
        if (l[i].type == Node.NodeType.GA &&
                (i == 3 || (i == 4 && l[3].type == Node.NodeType.SP))) {
          return 0
        }
        return l[i].type.isPrerelease() ? -1 : 1
      }

      int diff = l[i] <=> o.at(i)
      if (diff != 0) {
        return diff
      }
    }
    return 0
  }

  private static class Node<T> implements Comparable<Node> {
    static enum NodeType {
      DR, ER, CR, GA, TEXT, INT, SP /*ServicePack*/
      boolean isNumberedRelase() { return this in [DR, ER, CR, SP] }
      boolean isPrerelease() { return this in [DR, ER, CR] }
    }

    T value
    NodeType type

    private Node(NodeType type, T value) {
      this.value = value
      this.type = type
    }

    @Override
    int compareTo(Node o) {
      // Same types
      if (type == o.type) {
        if (type == NodeType.INT || type == NodeType.TEXT || type.isNumberedRelase()) {
          return value <=> o.value
        } else if (type == NodeType.GA) {
          return 0
        }
        throw new IllegalArgumentException("Missing implementation for: " + type)
      }

      // Different types
      return type <=> o.type
    }

    static Node build(String s) {
      if (s.isInteger()) {
        return new Node(NodeType.INT, Integer.valueOf(s))
      }

      int size = s.size()

      if (size < 2) {
        return new Node(NodeType.TEXT, s)
      }

      /* Release types are deemed to be Case Insensitive */
      String prefix = s[0..1].toUpperCase()
      if (size == 2 && prefix == 'GA') {
        return new Node(NodeType.GA, null)
      }

      NodeType t = ["DR":NodeType.DR, "ER":NodeType.ER, "CR":NodeType.CR, "SP":NodeType.SP].get(prefix, NodeType.TEXT)

      if (t.isNumberedRelase()) {
        int value

        try {
          value = s[2..size - 1].toInteger()
        } catch(NumberFormatException) {
          return new Node(NodeType.TEXT, s)
        }
        return new Node(t, value)
      }
      return new Node(t, s)
    }
  }
}
