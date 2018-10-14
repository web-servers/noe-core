package noe.ews.utils.selinux

class SELinuxException extends RuntimeException {

  SELinuxException(String s) {
    super(s)
  }

  SELinuxException(String s, Throwable cause) {
    super(s, cause)
  }
}
