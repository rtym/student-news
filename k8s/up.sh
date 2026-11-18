#!/usr/bin/env bash
# Build the images, load them into minikube, apply the manifests and wait
# until every pod is ready. The same steps as "Run on minikube" in the README,
# in one command:
#
#   ./k8s/up.sh
#
# Before the first run: `minikube start`, then create the Secret (README).
set -euo pipefail

# Work from the repository root, wherever the script was started from.
cd "$(dirname "$0")/.."

# Must match the image tags in k8s/03-backend.yaml and k8s/04-frontend.yaml.
TAG=1.0

# Safety net: only ever deploy to the local minikube cluster.
if [ "$(kubectl config current-context)" != "minikube" ]; then
  echo "kubectl is not pointing at minikube." >&2
  echo "Run: kubectl config use-context minikube" >&2
  exit 1
fi

# The namespace first: the Secret lives in it. The Secret is not in Git, so
# stop early with a hint if it has not been created yet.
kubectl apply -f k8s/00-namespace.yaml
if ! kubectl get secret db-credentials -n student-news >/dev/null 2>&1; then
  echo "Secret db-credentials is missing. Create it first:" >&2
  echo "  kubectl create secret generic db-credentials -n student-news \\" >&2
  echo "    --from-literal=POSTGRES_DB=student_news \\" >&2
  echo "    --from-literal=POSTGRES_USER=student \\" >&2
  echo "    --from-literal=POSTGRES_PASSWORD='choose-your-own'" >&2
  exit 1
fi

echo "==> Building images"
docker build -t "student-news-backend:$TAG" ./backend
docker build -t "student-news-frontend:$TAG" ./frontend

echo "==> Loading images into minikube"
minikube image load "student-news-backend:$TAG"
minikube image load "student-news-frontend:$TAG"

echo "==> Applying manifests"
kubectl apply -f k8s/

echo "==> Waiting for rollouts"
for deployment in db backend frontend; do
  kubectl rollout status "deployment/$deployment" -n student-news --timeout=180s
done

kubectl get pods,svc,pvc -n student-news

echo
echo "Ready. Open the app with two port-forwards (one terminal each):"
echo "  kubectl port-forward -n student-news service/backend 8080:8080"
echo "  kubectl port-forward -n student-news service/frontend 5173:8080"
echo "Then browse to http://localhost:5173"
