NOE core
================

Description
-----------------
It is a library containing functionality and abstraction to work with servers (start/stop/kill/configuration) in
a unified way.
It also provides workspace management abstraction which is used to prepare the servers for further testing.
It also contains common libraries such as file manipulation (copy, move, remove, unzip,..), web access and many more.


Coding standards
---------------------------------------------
We use same coding standards like wildfly https://github.com/wildfly/wildfly-core/tree/master/ide-configs with these
exceptions:
* indentation size is 2
* continuation indentation is 4
* maximum line length is 160 chars


Versioning
---------------------------------------------
Version numbering is done in accordance to http://semver.org/spec/v2.0.0.html


Release a snapshot
---------------------------------------------
Release is done using maven to internal JBoss QA maven repositories `mvn clean deploy`


Release a new version
---------------------------------------------
Before releasing, don't forget to check, that integration tests are passing on all platforms, e.g. by checking CI job if it is set up.

Signing of the artifacts has been inherently added to the project.
Below you can find the commands you could use to sign the artifacts upon build.
```
echo 'export GPG_TTY=$(tty)' >> ~/.bashrc`

echo "use-agent" >> ~/.gnupg/gpg.conf
echo "pinentry-mode loopback" >> ~/.gnupg/gpg.conf
echo "allow-loopback-pinentry" >> ~/.gnupg/gpg-agent.conf
gpgconf --kill gpg-agent
gpgconf --launch gpg-agent
gpg --full-generate-key # *RSA and RSA *4096 *name *emailAddress *expirationDate
gpg --export -a 'your-generated-keyid' > my-nore-core-pbkey.asc # share or upload
export NEO_CORE_GPG_KEYNAME='your-generated-keyid'
export GPG_PASSPHRASE='your-secure-passphrase'
```

Releasing is done using the maven release plugin to internal JBoss QA maven repositories. To perform the release, use the following commands in a sequence:

`mvn release:prepare -Pcomplete -Darguments="-DskipTests"`

`mvn release:perform -Pcomplete -Darguments="-DskipTests"`

These commands automatically change versions, tags, and push the
changes to the `jbossqe-eap/noe-core` repository.  Note that the commands exclude running tests; run the tests yourself before releasing.

The `release:prepare` command generates temporary files. If you need to re-generate the temporary files without pushing new tags and versions,
run the following command:

`mvn -DdryRun=true release:prepare -Pcomplete -Darguments="-DskipTests"`

To successfully perform the release, you must set username and password in the `settings.xml` file for the `jboss-qa-releases` server.

Running integration testsuite
--------------------------------------------
`mvn clean verify -Pcomplete` where -Pcomplete stands for also building testsuite module

To run single test run `mvn verify -Pcomplete -Dit.test=SomeTest`

Java requirements
---------------------------------------------
At this moment, Noe Core cannot be built with Java greater than 8. However, it
is tested and used in testsuites with Java up to 11. We now support `SERVER_JAVA_HOME`,
which means the testsuite runs with different Java than the server itself.

Simply export `SERVER_JAVA_HOME` to a JAVA_HOME that will be set for servers
(all Tomcats are supported, some EAPs are supported). This enables you to test
a server with various Javas while running the testsuite on JDK 11+.
