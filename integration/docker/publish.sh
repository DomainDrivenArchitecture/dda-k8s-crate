#! /bin/bash

docker_repo=$1

docker login -u "${DOCKER_USERNAME}" -p "${DOCKER_PASSWORD}"
docker tag ${docker_repo} ${DOCKER_USERNAME}/${docker_repo}:latest
docker push ${DOCKER_USERNAME}/${docker_repo}
