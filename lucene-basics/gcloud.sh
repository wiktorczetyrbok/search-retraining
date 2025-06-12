mvn clean package
build docker image: note: replace zlj-lucene-mini_search with your image name !!!
DOCKER_DEFAULT_PLATFORM="linux/amd64"
docker build -f src/main/Docker/Dockerfile.jvm -t europe-west6-docker.pkg.dev/gd-gcp-em-search-re-training/trainingartifacts/zlj-lucene-mini_search:0.1 .

gcloud auth login
gcloud auth configure-docker europe-west6-docker.pkg.dev
gcloud config set project gd-gcp-em-search-re-training
