docker buildx create --use
docker buildx build --push --platform linux/amd64, linux/arm64 -f ./src/main/docker/Dockerfile.basicPOI -t quay.io/maurycy_krzeminski/dsa_basic .
