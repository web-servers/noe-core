package noe.server.provider

import noe.common.DefaultProperties
import noe.common.utils.OpenSslVersion
import noe.common.utils.Platform
import noe.jbcs.utils.CoreServicesOpenSslHelper
import noe.server.OpensslServer

/**
 * This class is meant to install the server and provide its Java representation
 *
 * Currently is just providing the Java representation and expects its installation on provided location
 *
 * TODO: both physical installation and Java init should be done from single method
 */
class OpensslServerProvider {
    private static final Platform platform = new Platform()

    static OpensslServer coreServices(File basedir) {
        File openSSLBaseDir, openSSLBinDir


        if (new OpenSslVersion("1.0.2n").equals(DefaultProperties.jbcsOpenSslVersion())
                && (new Platform().isWindows() || new Platform().isSolaris())) {
            // WORKAROUND of the issue https://issues.jboss.org/browse/JBCS-637
            openSSLBaseDir = basedir
            openSSLBinDir = new File(openSSLBaseDir, 'bin')
        } else {
            openSSLBaseDir = new File(basedir, CoreServicesOpenSslHelper.coreOpenSslDirName + File.separator + 'openssl')
            openSSLBinDir = new File(openSSLBaseDir, platform.isRHEL() ? 'sbin' : 'bin')
        }

        return new OpensslServerBuilder(openSSLBaseDir).binDir(openSSLBinDir).build()
    }

    static OpensslServer 'default'() {
        return new OpensslServerBuilder().build()
    }

}
