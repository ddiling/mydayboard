# Terraform CI: status

## Have

| Area | State |
|---|---|
| Code | `infra/`: reusable module `modules/github_repository`, first instance `mydayboard` |
| Managed (always) | squash-only merge, squash title = PR title, body = PR description, delete head branch, update branch, issues/projects/wiki flags, description, topics, visibility |
| Managed (public only, `repo_is_public = true`) | ruleset on `main` (PR + `CI passed`, no force-push/deletion, no code-owner review, no bypass), auto-merge, secret scanning + push protection, Dependabot alerts + security updates, `release` environment |
| Public-only guard | `count = 0` while private: absent from plan, cannot fail |
| Provider | `integrations/github ~> 6.13.0`; `.terraform.lock.hcl` committed (4 platforms) |
| State | local `infra/terraform.tfstate`, git-ignored |
| Auth | `GITHUB_TOKEN=$(gh auth token)`; no token in repo or CI |
| CI | `.github/workflows/terraform.yml`: `fmt -check`, `init -backend=false -lockfile=readonly`, `validate`, `tflint`. No secrets, runs only on `infra/**` changes |
| Docs | `README.md` (setup, import, going public), `ROADMAP.md` (later steps) |
| Safety | `prevent_destroy`, `archive_on_destroy`; visibility change only via explicit `repo_is_public` |

## Missing

| Item | Why missing | Needs |
|---|---|---|
| First `terraform apply` | writes two provider-only attributes to the state; no GitHub change | run `terraform apply` locally |
| Security/misconfig scan (checkov/trivy) | extra third-party action | SHA-pinned action or container |
| Remote state | state exists only on one PC, no backup/locking | versioned bucket + OIDC, or HCP Terraform |
| Plan on PR | needs a credential for the GitHub API | GitHub App (read-only), private key as secret |
| Drift detection | needs plan credential + remote state | scheduled workflow, `plan -detailed-exitcode` |
| Gated apply | needs write credential, environment approval | write GitHub App, protected environment (public repo) |
| Private vulnerability reporting | no provider resource | manual after going public |
| First-time-contributor approval | no provider resource | manual after going public |
| Repo secrets (`*_API_KEY_REVIEW`) | would land in state | stay manual |

## Order

1. Run the first `terraform apply` locally (state only; then `plan` = no changes).
2. Merge PR #13.
3. Go public: `repo_is_public = true`, then the two manual settings.
4. Roadmap steps in `ROADMAP.md`: scan, remote state, PR plan, drift, gated apply.
