// key generation
openssl genrsa -des3 -out server.key 4096
passwd: changeit

// certificate generation
openssl req -new -x509 -key server.key -out server.crt -days 10950

// keystore in pkcs12 format
openssl pkcs12 -export -in server.crt -inkey server.key  -out server.p12
passwd: changeit

// transform keystore into jks format
keytool -importkeystore -srckeystore server.p12 -destkeystore server.jks -srcstoretype pkcs12
passwd: changeit
