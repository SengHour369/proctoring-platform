# Kubernetes deployment

Manifests for the whole stack: Postgres, Zipkin, Eureka, Config Server, the API Gateway
(with an Ingress), and all 9 business services. Mirrors [`docker-compose.yml`](../docker-compose.yml)'s
topology, aimed at an actual cluster instead of a single host.

## 1. Build and push images

Each module builds from the shared [`docker/Dockerfile`](../docker/Dockerfile). Build and push
every one under whatever registry you use — the manifests here reference
`proctoring-platform/<module>:latest`, so either push under that name or update the `image:`
field in each YAML file (or layer a Kustomize image transformer on top):

```bash
for module in eureka-server config-server api-gateway identity-service exam-service \
              attempt-service proctoring-service review-service result-service \
              payment-service notification-service config-service; do
  docker build --build-arg MODULE=$module -t your-registry/proctoring-platform/$module:latest \
    -f docker/Dockerfile .
  docker push your-registry/proctoring-platform/$module:latest
done
```

For a local cluster (kind/minikube/k3d) you can skip the registry and load images directly:
`kind load docker-image proctoring-platform/<module>:latest`, then leave `imagePullPolicy:
IfNotPresent` as-is.

## 2. Deploy

```bash
kubectl apply -k k8s/
```

This creates the `proctoring-platform` namespace and everything in it. Watch it come up with:

```bash
kubectl -n proctoring-platform get pods -w
```

Startup order matters loosely (Postgres → Eureka/Config Server → everything else), but nothing
here hard-fails on a missing dependency at boot — `spring.config.import` is `optional:configserver:...`
and Eureka registration retries — so a rolling `apply` that brings things up in any order will
self-heal within a minute or two as pods restart on failed health checks.

## 3. Reach it

The gateway is exposed via an `Ingress` (`api-gateway.local` by default — edit
[`12-api-gateway.yaml`](12-api-gateway.yaml) for your ingress controller and host). Without an
ingress controller installed, port-forward instead:

```bash
kubectl -n proctoring-platform port-forward svc/api-gateway 8080:8080
```

## What's deliberately minimal here

- **Secrets** ([`01-secrets.yaml`](01-secrets.yaml)) are placeholder values in plain YAML — fine
  for a local cluster, not for anything real. Swap for Sealed Secrets / External Secrets /
  your cloud's KMS-backed secret store before this touches real data, and stop committing the
  file once it holds anything real.
- **Single shared Postgres StatefulSet** for all 9 databases — matches `docker-compose.yml`'s
  dev convenience, but is a shared blast radius. Split to one managed instance per service (or
  at least per bounded-context group) for a real deployment.
- **No HorizontalPodAutoscaler, NetworkPolicy, or PodDisruptionBudget** — add these once you
  know real traffic/failure patterns; guessing at thresholds now would just be noise.
- **No Helm chart** — plain manifests + Kustomize, since there's no templating need yet (no
  multi-environment overlays exist). Add a `base`/`overlays` split, or migrate to Helm, once
  there's a second environment (staging vs prod) that actually needs different values.
