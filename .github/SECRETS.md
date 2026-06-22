# Секреты и переменные для CI/CD

Не коммить реальные значения. Настраиваются в:
`Settings → Secrets and variables → Actions` (Secrets) и
`Settings → Environments → production` (для деплоя с подтверждением).

## Secrets

| Имя | Назначение |
| --- | --- |
| `DOCKERHUB_USERNAME` | Логин Docker Hub. Образ: `DOCKERHUB_USERNAME/dnd-backend`. |
| `DOCKERHUB_TOKEN` | Access Token Docker Hub (Read & Write). |
| `GITOPS_REPO_TOKEN` | Personal Access Token с правом записи (push) в GitOps-репозиторий. |
| `GITOPS_REPO_URL` | URL GitOps-репозитория, напр. `https://github.com/org/dnd-gitops.git`. |

## Variables

| Имя | Назначение |
| --- | --- |
| `GITOPS_REPO_SLUG` | `owner/repo` GitOps-репозитория для `actions/checkout`, напр. `org/dnd-gitops`. |

> Примечание: workflow использует `vars.GITOPS_REPO_SLUG` + `secrets.GITOPS_REPO_TOKEN`
> для checkout. Секрет `GITOPS_REPO_URL` оставлен по ТЗ как альтернатива
> (ручной `git clone` с токеном) — см. закомментированный шаг в `ci-cd.yml`.

## Ручное подтверждение деплоя

Job `deploy` привязан к `environment: production`. Чтобы появилась кнопка
подтверждения в GitHub UI, в настройках окружения `production` включи
**Required reviewers** и добавь апруверов.

## Структура GitOps-репозитория (Kustomize)

Репозиторий: `dnd-gitops`. Деплой обновляет `newTag` у образа `dnd-backend`
в файле `apps/dnd/overlays/prod/kustomization.yaml`
(переменная `GITOPS_KUSTOMIZATION_PATH`):

```yaml
images:
  - name: dnd-backend
    newName: cryingloli/dnd-backend
    newTag: <git-sha>   # обновляется пайплайном
  - name: dnd-frontend
    newName: cryingloli/dnd-frontend
    newTag: bootstrap
```

Если структура изменится — поправь `GITOPS_KUSTOMIZATION_PATH` и `KUSTOMIZE_IMAGE_NAME`
в `.github/workflows/ci-cd.yml`.
