package noe.common.utils
/**
 * Helper class for filtering available IP addresses on host network interfaces based on provided criteria
 */
public class IPAddressFilter {

  private final Collection<NetworkInterface> networkInterfaces;
  private final Set<InetAddress> addressesToIgnore;

  private IPAddressFilter(Collection<NetworkInterface> interfaces, Set<InetAddress> addressesToIgnore) {
    this.networkInterfaces = interfaces;
    this.addressesToIgnore = addressesToIgnore;
  }

  public enum IPVersion {
    IPv6,
    IPv4,
    ALL
  }

  /**
   * returns IP addresses filtered based on IP version
   */
  public Collection<InetAddress> find(IPVersion ipAddressVersion) throws SocketException {
    Set<InetAddress> addresses = new HashSet<InetAddress>();
    for (NetworkInterface networkInterface : networkInterfaces) {
      Enumeration<InetAddress> ee = networkInterface.getInetAddresses();
      while (ee.hasMoreElements()) {
        InetAddress inetAddress = ee.nextElement();
        if (addressesToIgnore.contains(inetAddress)) continue;
        // address which should be ignored, continuing with next one

        if (ipAddressVersion == IPVersion.ALL) {
          addresses.add(inetAddress);
        } else if (ipAddressVersion == IPVersion.IPv4 && inetAddress instanceof Inet4Address) {
          addresses.add(inetAddress);
        } else if (ipAddressVersion == IPVersion.IPv6 && inetAddress instanceof Inet6Address) {
          addresses.add(inetAddress);
        }
      }
    }
    return addresses;
  }

  /**
   * Builder for defining criteria used filtering based on network interfaces
   */
  public static class Builder {
    private boolean excludeLoopbackInterfaces = false;
    private boolean excludeVirtualAddressesAndInterfaces = false;
    private boolean excludePointToPointInterfaces = false;
    private boolean excludeNotRunningInterfaces = false
    private String excludeIfaceNameRegexp = null;


    public Builder excludeLoopbackInterfaces() {
      this.excludeLoopbackInterfaces = true;
      return this;
    }

    public Builder excludeVirtualAddressesAndInterfaces() {
      this.excludeVirtualAddressesAndInterfaces = true;
      return this;
    }

    /**
     * Excludes interfaces with name matching given regexp
     */
    public Builder excludeInterfacesBasedOnRegexp(String ifaceNameRegexp) {
      this.excludeIfaceNameRegexp = ifaceNameRegexp;
      return this;
    }

    public Builder excludeNotRunningInterfaces() {
      this.excludeNotRunningInterfaces = true;
      return this;

    }

    public Builder excludePointToPointInterfaces() {
      this.excludePointToPointInterfaces = true;
      return this;
    }


    private boolean shouldBeExcluded(NetworkInterface networkInterface) throws SocketException {
      boolean shouldBeExcluded = false;
      if (networkInterface.isLoopback() && excludeLoopbackInterfaces) return true;
      if (networkInterface.isVirtual() && excludeVirtualAddressesAndInterfaces) return true;
      if (networkInterface.isPointToPoint() && excludePointToPointInterfaces) return true;
      if (!networkInterface.isUp() && excludeNotRunningInterfaces) return true;
      if (excludeIfaceNameRegexp != null && networkInterface.getName().matches(excludeIfaceNameRegexp)) return true;

      return shouldBeExcluded;
    }

    public IPAddressFilter build() throws SocketException {
      Enumeration e = NetworkInterface.getNetworkInterfaces();
      Set<InetAddress> virtualAddresses = new HashSet<InetAddress>();
      Set<NetworkInterface> interfaces = new HashSet<NetworkInterface>();
      while (e.hasMoreElements()) {
        NetworkInterface networkInterface = (NetworkInterface) e.nextElement();
        if (shouldBeExcluded(networkInterface))
          continue; // lets process next one as this one should be excluded
        interfaces.add(networkInterface);
        if (excludeVirtualAddressesAndInterfaces) {
          Enumeration<NetworkInterface> subInterfaces = networkInterface.getSubInterfaces();
          while (subInterfaces.hasMoreElements()) {
            NetworkInterface virtualInterface = subInterfaces.nextElement();
            Enumeration<InetAddress> virtAddrEnum = virtualInterface.getInetAddresses();
            while (virtAddrEnum.hasMoreElements()) {
              virtualAddresses.add(virtAddrEnum.nextElement());
            }
          }
        }
      }
      return new IPAddressFilter(interfaces, virtualAddresses);
    }
  }

  /**
   * Example method showing how to use the IPAddressFilter class
   * @param args
   */
  public static void main(String[] args) {
    IPAddressFilter ipAddressFilter = new IPAddressFilter.Builder()
        .excludeLoopbackInterfaces()
        .excludeVirtualAddressesAndInterfaces()
        .excludePointToPointInterfaces()
        .excludeNotRunningInterfaces()
        .excludeInterfacesBasedOnRegexp('virbr.*')
        .build()

    ipAddressFilter.find(IPAddressFilter.IPVersion.IPv4).each {println(it.hostAddress)}

  }
}
