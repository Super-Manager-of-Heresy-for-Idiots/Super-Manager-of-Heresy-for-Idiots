Как использовать
## Без Argo CD (напрямую kubectl):

- dev: kubectl apply -k k8s/overlays/dev


- prod: kubectl apply -k k8s/overlays/prod


## С Argo CD:

Поменяй repoURL в k8s/argocd/app-dev.yaml и app-prod.yaml на свой репозиторий
Примени:
- kubectl apply -f k8s/argocd/project.yaml
- kubectl apply -f k8s/argocd/app-dev.yaml    # auto-sync
- kubectl apply -f k8s/argocd/app-prod.yaml   # manual sync

Dev — автосинк + selfHeal + prune (пушнул в develop — само раскатилось). Prod — ручной синк через Argo CD UI/CLI, без auto-prune.

Когда появится фронтенд — добавишь k8s/base/frontend/ с deployment + service, добавишь его в base/kustomization.yaml, и раскомментируешь путь / в prod/ingress.yaml.