// key generation
openssl genpkey -algorithm rsa -pkeyopt rsa_keygen_bits:4096 -out server.key

// certificate generation
openssl req -new -x509 -key server.key -out server.crt -days 10950 \
-subj "/C=CZ/ST=Czech Republic/L=Brno/O=Corleone Family/OU=Michael Corleone/CN=localhost/emailAddress=noreplay@corleone.gf"

// keystore in pkcs12 format
openssl pkcs12 -export -in server.crt -passin pass:changeit -inkey server.key  -out server.p12 -passout pass:changeit

// transform keystore into jks format
keytool -importkeystore -srckeystore server.p12 -srcstorepass changeit -srcstoretype pkcs12 -destkeystore server.jks -deststorepass changeit

// list contents
keytool -list -v -keystore server.jks | less
