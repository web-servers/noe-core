# Increase the maximum file descriptors if we can
# Use the maximum available, or set MAX_FD != -1 to use that
MAX_FD="maximum"
MAX_FD_LIMIT=`ulimit -H -n`
if [ "$?" -eq 0 ]; then
  # Solaris hasn't sysctl
  if [ "`uname`" != "SunOS" -a "$MAX_FD_LIMIT" = "unlimited" ]; then
    MAX_FD_LIMIT=`/usr/sbin/sysctl -n kern.maxfilesperproc`
  fi

  if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ]; then
    # use the system max
    MAX_FD="$MAX_FD_LIMIT"
  fi

  ulimit -n $MAX_FD
  if [ "$?" -ne 0 ]; then
    echo "[WARN] Could not set maximum file descriptor limit: $MAX_FD"
  fi
else
  echo "[WARN] Could not query system maximum file descriptor limit: $MAX_FD_LIMIT"
fi
