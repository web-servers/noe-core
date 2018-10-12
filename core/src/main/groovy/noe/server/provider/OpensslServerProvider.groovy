package noe.server.provider

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
        File opensslBasedir = new File(basedir, CoreServicesOpenSslHelper.coreOpenSslDirName + File.separator + 'openssl')
        return new OpensslServerBuilder(opensslBasedir)
                .binDir(new File(opensslBasedir, platform.isRHEL() ? 'sbin' : 'bin'))
                .build()
    }

    static OpensslServer 'default'() {
        return new OpensslServerBuilder().build()
    }

}
