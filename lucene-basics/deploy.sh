#!/bin/bash

PROJECT_ID="gd-gcp-em-search-re-training"
REGION="europe-west6"
REPO="trainingartifacts"
SERVICE_NAME="wczetyrbok-lucene-mini-search"
IMAGE_TAG="0.1"
IMAGE_URI="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO}/${SERVICE_NAME}:${IMAGE_TAG}"

echo "Building Quarkus app..."
./mvnw clean package -DskipTests

echo "Building Docker image..."
DOCKER_DEFAULT_PLATFORM="linux/amd64" docker build \
  -f src/main/Docker/Dockerfile.jvm \
  -t ${IMAGE_URI} .

echo "Pushing image to Artifact Registry..."
docker push ${IMAGE_URI}

echo "Deploying to Cloud Run..."
gcloud run deploy ${SERVICE_NAME} \
  --image ${IMAGE_URI} \
  --platform managed \
  --region ${REGION} \
  --allow-unauthenticated \
  --port 8080 \
  --memory 512Mi \
  --quiet
