# infra: GitHub repository settings as code

Terraform for the GitHub settings of this repo (`ddiling/mydayboard`).
The reusable part is the module in `modules/github_repository`; `main.tf` is the
first instance of it. To manage another repo, add another `module` block.

## What is managed

Always (works on a private Free repo):

- Squash merge only (merge commits and rebase merges off)
- Squash commit title = PR title, body = PR description
- Delete head branch on merge, "Update branch" button
- Issues/projects on, wiki off, description, topics, visibility

Only when `repo_is_public = true` (GitHub Free offers these on public repos only):

- Ruleset on `main`: PR required, status check `CI passed` required,
  force-push and deletion blocked, no code-owner review, no bypass actors
- Auto-merge (GitHub ignores it on a private Free repo, so it follows visibility)
- Secret scanning and push protection
- Dependabot alerts and Dependabot security updates
- GitHub Environment `release` (used by `release.yml`)

While private these resources have `count = 0`: they are absent from the plan,
so plan and apply cannot fail on them.
Note: auto-merge only helps once the ruleset requires checks.

## Not managed (set by hand)

- **Repository secrets** (`GROQ_API_KEY_REVIEW`, `OPENROUTER_API_KEY_REVIEW`,
  `GEMINI_API_KEY_REVIEW`): kept out of Terraform on purpose, secret values
  would end up in the state file.
- **Private vulnerability reporting** and **"require approval for first-time
  contributors"**: the provider (6.13) has no resource for them. After going
  public, enable them in Settings > Code security and Settings > Actions >
  General (or with `gh api`).

## Run it

Requires Terraform >= 1.6. No token is stored anywhere: use your own short-lived
`gh` login (needs admin rights on the repo).

```bash
cd infra
export GITHUB_TOKEN=$(gh auth token)
terraform init
terraform plan
terraform apply
```

### First-time import (once per repo, per machine/state)

The repo already exists, so it is adopted instead of created: the `import {}`
block in `main.tf` does it. On a fresh state, `terraform plan` shows the import
and `terraform apply` performs it. The first apply also writes two
provider-only attributes (`archive_on_destroy`, `ignore_vulnerability_alerts_during_read`)
to the state; nothing changes on GitHub, and afterwards `plan` shows no changes.

### Going public

Do it deliberately, in its own PR: set `repo_is_public = true` (variable default
in `variables.tf`, or `-var repo_is_public=true`). One apply then makes the repo
public and creates the ruleset, security features and environment.
Check `terraform plan` first (expected: 4 to add, 1 to change, and the one change must read `visibility: "private" -> "public"`).

## State

State is local (`terraform.tfstate`, git-ignored). It contains no secrets here,
but it must not be committed. Local state is fine for one maintainer; back it up
or move it to a remote backend (for example a private bucket with locking) if
more people or machines ever apply.

## Versions

The provider is pinned to `~> 6.13.0` and `.terraform.lock.hcl` is committed, so
everyone gets the same build. Upgrade with `terraform init -upgrade` in a PR.

## CI

CI (`.github/workflows/terraform.yml`) runs `fmt -check`, `validate` and `tflint`,
which need no token. There is no `plan` in CI: it needs an admin token, which must
not be stored as a CI secret.
