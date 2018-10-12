package noe.server.provider

import noe.common.utils.Platform
import noe.server.OpensslServer

/**
 * Simple builder class helping to produce OpensslServer instance
 */
class OpensslServerBuilder {
    private Platform platform = new Platform()
    private String openssl
    private String serverCert = null
    private String serverKey = null
    private Integer port = 4433
    private String keyPassword = null
    private File libDir = null
    private File binDir = null
    private File baseDir = null

    /**
     * Means using system openssl which must be on PATH
     */
    OpensslServerBuilder() {
        this('openssl')
    }

    /**
     * Either full path to openssl or just the name of openssl binary if it is available on PATH
     */
    OpensslServerBuilder(String openssl) {
        this.openssl = openssl
    }

    /**
     * Path to base directory of openssl, which contains in subdirectories lib and bin dirs
     */
    OpensslServerBuilder(File basedir) {
        this.baseDir = basedir
        this.binDir = new File(baseDir, "bin")
        this.libDir = new File(baseDir, (platform.isX64() ? 'lib64' : 'lib'))
    }

    OpensslServerBuilder serverCert(File serverCert) {
        this.serverCert = serverCert.absolutePath
        return this
    }

    OpensslServerBuilder serverKey(File serverKey) {
        this.serverKey = serverKey.absolutePath
        return this
    }

    OpensslServerBuilder keyPassword(String password) {
        this.keyPassword = password
        return this
    }

    OpensslServerBuilder port(Integer port) {
        this.port = port
        return this
    }

    OpensslServerBuilder libDir(File libDir) {
        this.libDir = libDir
        return this
    }

    OpensslServerBuilder binDir(File binDir) {
        this.binDir = binDir
        return this
    }

    private String opensslBinaryName() {
        return (platform.isWindows() ? 'openssl.exe' : 'openssl')
    }

    OpensslServer build() {
        if (openssl == null) {
            if (binDir) {
                this.openssl = new File(binDir, opensslBinaryName()).absolutePath
            } else {
                openssl = opensslBinaryName()
            }
        }
        return new OpensslServer(this)
    }
}
