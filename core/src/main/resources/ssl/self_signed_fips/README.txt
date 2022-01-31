This README is the counterpart of the existing README.txt 
for the non-fips directory, with the relevant information
to recreate the certificates in the FIPS case.

//generate a NSS keystore (note that key is generated on the fly)
mkdir -p nssdb
modutil -create -dbdir sql:nssdb
chmod a+r nssdb/*.db
modutil -fips true -dbdir sql:nssdb
// java compatibility needs an empty secmod.db
touch nssdb/secmod.db
//store a password for the key
echo 'changeit' >nwpwfile
//don't set a password for the keystore
//modutil -changepw "NSS FIPS 140-2 Certificate DB" -dbdir sql:nssdb -newpwfile nwpwfile
//generate a key
certutil -S -k rsa -n tomcat  -t "CTu,Cu,Cu" -x -7 noreplay@corleone.gf \
-s 'C=CZ, ST=Czech Republic, L=Brno, O=Corleone Family, OU=Michael Corleone, CN=localhost' -d sql:nssdb -f nwpwfile

// keystore in pkcs12 format (for use in non-FIPS mode)
pk12util -o server.p12 -d nssdb -n tomcat

// certificate
openssl pkcs12 -in server.p12 -out server.crt -clcerts -nokeys  -nodes -passin pass:changeit

// key
openssl pkcs12 -in server.p12 -out server.key -nocerts -nodes -password pass:changeit
// set a password for the extracted key???

// transform keystore into jks format (Note it needs to be done in non-FIPS mode)
/usr/lib/jvm/java-11/bin/keytool -J-Dcom.redhat.fips=false -importkeystore -srckeystore server.p12 -srcstorepass changeit -srcstoretype pkcs12 -destkeystore server.jks -deststoretype jks -deststorepass changeit


// list contents (both list the same result)
keytool -list -v -keystore server.jks -storetype jks -protected
keytool -list -v -keystore server.jks -storetype jks -storepass changeit
// list contents of the nss
keytool -J-Djava.security.properties==/dev/null -list -storetype pkcs11
