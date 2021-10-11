#!/usr/bin/env bash

#=======================================================================================================================
#
#          FILE:  generate-trustchain.sh
#
#         USAGE:  ./generate-trustchain.sh
#
#   DESCRIPTION:  Script to generate trustchain. In several steps this script generates CA key and root certificate,
#                 intermediate certificate used to sign user and server certificate and at last user and server
#                 certificates. This script is mentioned to be only run in a preparation phase by user not by any test.
#                 Script creates following structure:
#                    ca
#                    ├── certs
#                    │   └── ca.cert.pem
#                    ├── crlnumber
#                    ├── index.txt
#                    ├── index.txt.attr
#                    ├── index.txt.old
#                    ├── intermediate
#                    │   ├── certs
#                    │   │   ├── ca-chain.cert.pem //trustchain (packed ca and itermediate certs)
#                    │   │   ├── *_client.cert.pem //end user certificate
#                    │   │   ├── intermediate.cert.pem //intermediate certificate
#                    │   │   └── *_server.cert.pem //server certificate
#                    │   ├── crl
#                    │   │   └── intermediate.crl.pem //certificate revocation list referencing revoked certificates
#                    │   ├── crlnumber
#                    │   ├── csr //signing requests - unimportant for future use
#                    │   ├── keystores //PKCS#12 and JKS keystores including trustchain
#                    │   ├── index.txt
#                    │   ├── index.txt.attr
#                    │   ├── index.txt.attr.old
#                    │   ├── index.txt.old
#                    │   ├── newcerts
#                    │   │   ├── 1000.pem
#                    │   │   └── 1001.pem
#                    │   ├── private //certificate's corresponding private keys
#                    │   │   ├── client.key.pem
#                    │   │   ├── intermediate.key.pem
#                    │   │   └── server.key.pem
#                    │   ├── serial
#                    │   └── serial.old
#                    ├── newcerts
#                    ├── keystores //keystores related to CA
#                    ├── private
#                    ├── serial
#                    └── serial.old
#       OPTIONS:  ---
#  REQUIREMENTS:  ---
#          BUGS:  ---
#         NOTES:  ---
#        AUTHOR:  Jan Kasik, jkasik@redhat.com
#       VERSION:  0.05
#       CREATED:  30-01-2018
#      REVISION:  11-03-2019
#=======================================================================================================================

CA_DIR="${PWD}/ca"
PASSPHRASE=testpass
INTERMEDIATE_DIR="${CA_DIR}/intermediate"

SUBJECT_LINE="/C=CZ/ST=Czech Republic/L=Brno/O=Red Hat Czech, s.r.o./OU=EAP QE/emailAddress=jkasik@redhat.com/CN=mod_cluster test certificate"

DEFAULT_EXPIRATION_DAYS=3650 #is 10 years enough?

# configurations
CONF_DIR="${PWD}/conf"
INTERMEDIATE_CONFIG_FILE="${CONF_DIR}/openssl_intermediate.conf"
CA_CONFIG_FILE="${CONF_DIR}/openssl_ca.conf"

function pushd() {
    command pushd "$@" > /dev/null
}

function printlog() {
    printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "${1}"
}

function printerr {
    >&2 printlog "${1}"
}

function ossl() {
    openssl "$@" || exit 1
}

function ktool() {
    /usr/bin/keytool "$@" || exit 1
}

function concat() {
    /usr/bin/cat "$@" || exit 1
}

function initialize_cert_dir() {
    touch index.txt
    echo 1000 > serial
    echo 1000 > crlnumber
}

function refresh_subject_line() {
    local COMMON_NAME="$1"
    export SUBJECT_LINE="/C=CZ/ST=Czech Republic/L=Brno/O=Red Hat Czech, s.r.o./OU=EAP QE/emailAddress=jkasik@redhat.com/CN=${COMMON_NAME}"
}

function generate_intermediate_signed_cert_using_extension() {
    local NAME="${1}"
    local EXTENSION="${2}"
    local COMMON_NAME="${3}"

    # append fresh common name
    refresh_subject_line "${COMMON_NAME}"

    printlog "Using subject '${SUBJECT_LINE}'"

    #private key
    ossl genrsa -aes256 \
        -passout pass:${PASSPHRASE} \
        -out ${INTERMEDIATE_DIR}/private/${NAME}.key.pem 2048 \
        && printlog "Successfully generated private key for certificate '${NAME}'"

    # create certificate signing request
    ossl req -config "${INTERMEDIATE_CONFIG_FILE}" \
        -passin pass:${PASSPHRASE} \
        -subj "${SUBJECT_LINE}" \
        -key ${INTERMEDIATE_DIR}/private/${NAME}.key.pem \
        -new -sha256 -out ${INTERMEDIATE_DIR}/csr/${NAME}.csr.pem \
        && printlog "Successfully generated certificate signing request for '${NAME}'"

    printlog "Signing certificate using '${EXTENSION}' extension"

    # sign the csr to generate certificate using 'batch' option to not be prompted for signing csr
    ossl ca -config "${INTERMEDIATE_CONFIG_FILE}" \
        -batch \
        -subj "${SUBJECT_LINE}" \
        -extensions ${EXTENSION} -days ${DEFAULT_EXPIRATION_DAYS} -notext -md sha256 \
        -passin pass:${PASSPHRASE} \
        -in ${INTERMEDIATE_DIR}/csr/${NAME}.csr.pem \
        -out ${INTERMEDIATE_DIR}/certs/${NAME}.cert.pem \
        && printlog "Signed certificate for '${NAME}' using '${EXTENSION}' extension"

    create_jks_keystore_for_cert "${NAME}" "ca-chain" "${NAME}" "${EXTENSION}"
}

function generate_server_cert() {
    local NAME="${1}"
    local COMMON_NAME="${2}"
    generate_intermediate_signed_cert_using_extension ${NAME} "server_cert" ${COMMON_NAME}
}

function generate_user_cert() {
    local NAME="${1}"
    local COMMON_NAME="${2}"
    generate_intermediate_signed_cert_using_extension ${NAME} "usr_cert" ${COMMON_NAME}
}

function create_certificate_revocation_list() {
    ossl ca -config "$INTERMEDIATE_CONFIG_FILE" \
        -passin pass:${PASSPHRASE} \
        -gencrl -out ${INTERMEDIATE_DIR}/crl/intermediate.crl.pem \
        -crldays ${DEFAULT_EXPIRATION_DAYS} \
        && printlog "Created certificate revocation list (CRL)"
}

function revoke_certificate() {
    local NAME="${1}"

    printlog "Revoking certificate $NAME"
    ossl ca -config "$INTERMEDIATE_CONFIG_FILE" \
        -passin pass:${PASSPHRASE} \
        -revoke "${INTERMEDIATE_DIR}/certs/${NAME}.cert.pem" \
        && printlog "Revoked certificate ${NAME}"
}

function create_jks_keystore_for_cert() {
    local CERT_NAME="${1}"
    local TRUSTCHAIN_NAME="${2}"
    local KEYSTORE_NAME="${3}"
    local KEYSTORE_ALIAS="${4}"

    ossl pkcs12 -export \
        -in  "${INTERMEDIATE_DIR}/certs/${CERT_NAME}.cert.pem" \
        -passin pass:${PASSPHRASE} \
        -passout pass:${PASSPHRASE} \
        -inkey "${INTERMEDIATE_DIR}/private/${CERT_NAME}.key.pem" \
        -certfile "${INTERMEDIATE_DIR}/certs/${TRUSTCHAIN_NAME}.cert.pem" \
        -name "${KEYSTORE_ALIAS}" -out "${INTERMEDIATE_DIR}/keystores/$KEYSTORE_NAME.keystore.p12" \
        && printlog "Created PKCS#12 keystore with ${CERT_NAME}.cert.pem"
    ktool -importkeystore -srckeystore "${INTERMEDIATE_DIR}/keystores/$KEYSTORE_NAME.keystore.p12" -srcstoretype pkcs12 -srcstorepass ${PASSPHRASE} \
        -destkeystore "${INTERMEDIATE_DIR}/keystores/$KEYSTORE_NAME.keystore.jks" -deststoretype JKS -deststorepass ${PASSPHRASE} \
        && printlog "Created JKS keystore with ${CERT_NAME}.cert.pem"

}

function strip_key_passphrase() {
    local NAME="${1}"

    ossl rsa \
        -in ${INTERMEDIATE_DIR}/private/${NAME}.key.pem \
        -out ${INTERMEDIATE_DIR}/private/${NAME}.nopass.key.pem \
        -passin pass:${PASSPHRASE} \
        && printlog "Stripped passphrase from ${INTERMEDIATE_DIR}/private/${NAME}.key.pem to ${INTERMEDIATE_DIR}/private/${NAME}.nopass.key.pem"
}

function combine_key_cert_pem() {
    local NAME="${1}"
    local KEY="${2}"

    concat ${INTERMEDIATE_DIR}/private/${NAME}.${KEY}.pem ${INTERMEDIATE_DIR}/certs/${NAME}.cert.pem > ${INTERMEDIATE_DIR}/keystores/${NAME}.${KEY}.cert.pem \
        && printlog "Concatenated key ${INTERMEDIATE_DIR}/private/${NAME}.${KEY}.pem and cert ${INTERMEDIATE_DIR}/certs/${NAME}.cert.pem to ${INTERMEDIATE_DIR}/keystores/${NAME}.${KEY}.cert.pem"
}
#------------------------------------
# directory structure preparation
#------------------------------------

if test -z "$(ls -A ${CA_DIR})"; then
    if test ! -d "${CA_DIR}"; then
        printlog "'${CA_DIR}' does not exist. Creating..."
        mkdir -p ${CA_DIR} || {
            printerr "Failed to create CA directory '${CA_DIR}'! Exiting..."
            exit 1
        }
    else
        printlog "Directory '${CA_DIR}' exists and it is empty, let's continue!"
    fi
else
    printerr "'${CA_DIR}' already exists and is not empty! Remove previously generated trustchain first! Exiting..."
    exit 1
fi

#prepare general folder structure
pushd ${CA_DIR} || exit 1
mkdir private certs intermediate newcerts keystores
initialize_cert_dir
popd || exit 1

#prepare for intermediate certs generation
pushd ${INTERMEDIATE_DIR} || exit 1
mkdir certs crl csr newcerts private keystores
initialize_cert_dir
popd || exit 1

#------------------------------------
# CA generation
#------------------------------------

printlog "Starting keys generation"

pushd ${CA_DIR} || exit 1

printlog "Starting CA key generation in $PWD "

ossl genrsa -aes256 -out private/ca.key.pem -passout pass:${PASSPHRASE} 4096 && printlog "Generated root key!"

printlog "Starting intermediate key generation"

ossl genrsa -aes256 \
       -out intermediate/private/intermediate.key.pem \
       -passout pass:${PASSPHRASE} \
       4096 && printlog "Generated intermediate key!"

printlog "Starting root cert generation"

refresh_subject_line "ca_mod_cluster_test_certificate"

ossl req -config "$CA_CONFIG_FILE" \
      -subj "${SUBJECT_LINE}" \
      -passin pass:${PASSPHRASE} \
      -key private/ca.key.pem \
      -new -x509 -days ${DEFAULT_EXPIRATION_DAYS} -sha256 -extensions v3_ca \
      -out certs/ca.cert.pem && \
      printlog "Successfully generated root certificate!"

ktool -import -file "certs/ca.cert.pem" \
        -alias "firstCA" -keystore "keystores/ca.keystore.jks" \
        -noprompt \
        -storepass "${PASSPHRASE}" && \
        printlog "Generated CA keystore!"

popd || exit 1

#------------------------------------
# intermediate certificate generation
#------------------------------------

printlog "Starting intermediate cert generation"

refresh_subject_line "intermediate_mod_cluster_test_certificate"

ossl req -config "${INTERMEDIATE_CONFIG_FILE}" -new -sha256 \
      -subj "${SUBJECT_LINE}" \
      -passin pass:${PASSPHRASE} \
      -key ${INTERMEDIATE_DIR}/private/intermediate.key.pem \
      -out ${INTERMEDIATE_DIR}/csr/intermediate.csr.pem && \
      printlog "Successfully generated certificate intermediate certificate signing request!"

ossl ca -config "$CA_CONFIG_FILE" -extensions v3_intermediate_ca \
      -subj "${SUBJECT_LINE}" \
      -batch \
      -passin pass:${PASSPHRASE} \
      -days ${DEFAULT_EXPIRATION_DAYS} -notext -md sha256 \
      -in ${INTERMEDIATE_DIR}/csr/intermediate.csr.pem \
      -out ${INTERMEDIATE_DIR}/certs/intermediate.cert.pem && \
      printlog "Successfully generated intermediate certificate!"

# stuff whole trustchain to one file - client has to verify the intermediate certificate against root too

cat ${INTERMEDIATE_DIR}/certs/intermediate.cert.pem \
    ${CA_DIR}/certs/ca.cert.pem > ${INTERMEDIATE_DIR}/certs/ca-chain.cert.pem && \
    printlog "Created trustchain for intermediate and root certificate"

printlog "Starting creation of trustchain keystore"

# import both certificates as trustedCertEntry to JKS keystore
ktool -import -alias ca -file ${CA_DIR}/certs/ca.cert.pem -keystore ${INTERMEDIATE_DIR}/keystores/ca-chain.keystore.jks -storepass ${PASSPHRASE} -noprompt
ktool -import -alias intermediate -file ${INTERMEDIATE_DIR}/certs/intermediate.cert.pem -keystore ${INTERMEDIATE_DIR}/keystores/ca-chain.keystore.jks -storepass ${PASSPHRASE} -noprompt

#------------------------------------
# signing client and server certs + creating JKSs
#------------------------------------

printlog "Starting user and server certificates generation"
generate_server_cert "node1.server" "node1.javaserver"
generate_user_cert "node1.client" "node1.javaclient"

generate_server_cert "node2.server" "node2.javaserver"
generate_user_cert "node2.client" "node2.javaclient"

generate_server_cert "node3.server.revoked" "node3.javaserver"
generate_user_cert "node3.client" "node3.javaclient"

generate_server_cert "node4.server" "node4.javaserver"
generate_user_cert "node4.client.revoked" "node4.javaclient"

generate_server_cert "node5.server" "node5.javaserver"
generate_user_cert "node5.client" "node5.javaclient"

generate_server_cert "localhost.server" "localhost"

#------------------------------------
# removing passphrase from keys + combining certs with keys
#
# strip password is necessary in cases (e.g. mod_ssl) where encrypted
# private keys are not supported.
# file with concatenated key+cert is also a necessary format for mod_ssl.
#------------------------------------

strip_key_passphrase "localhost.server"
strip_key_passphrase "node1.client"
combine_key_cert_pem "node1.client" "nopass.key"

#------------------------------------
# revoking server certificate
#------------------------------------

create_certificate_revocation_list
revoke_certificate "node3.server.revoked"
revoke_certificate "node4.client.revoked"
# recreate to include revoked certificate
create_certificate_revocation_list
