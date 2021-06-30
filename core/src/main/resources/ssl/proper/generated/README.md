This directory contains script to generate whole trustchain for testing purposes.
Complete description of generated structere may be found in [generate-trustchain.sh](generate-trustchain.sh).

## How to run

For quick, OS-independent generation, run following commands to perform build using docker: 

```bash
IMAGE_NAME="my_cool_docker_image"
docker build . -t ${IMAGE_NAME}
docker run --rm -v $PWD/ca:/trustchain/ca:z ${IMAGE_NAME} bash generate-trustchain.sh

```

See [Dockerfile](Dockerfile) for further information on which version of Java (keytool) and OpenSSL is being used.

You can also run the script manually, if you have all dependencies installed (see [Dockerfile](Dockerfile)):

```bash
./generate-trustchain.sh
```
