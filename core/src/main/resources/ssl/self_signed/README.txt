// key generation
openssl genrsa -des3 -out server.key 1024

// certificate generation
openssl req -new -x509 -key server.key -out server.crt -days 10950

// keystore in pkcs12 format
openssl pkcs12 -export -in server.crt -inkey server.key  -out server.p12

// transform keystore into jks format
keytool -importkeystore -srckeystore server.p12 -destkeystore server.jks -srcstoretype pkcs12
