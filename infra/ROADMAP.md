# Infra roadmap: from "good for one maintainer" to production grade

Current state: Terraform runs locally, state is a local file, and CI only runs
token-free checks (`.github/workflows/terraform.yml`: fmt, validate, tflint).
Each step below is independent; do them in this order. Stop whenever the
effort is no longer worth it for a one-maintainer repo.

## Step 1: token-free checks (done)

fmt, `validate`, tflint on every PR that touches `infra/`. No secrets.

## Step 2: security / misconfiguration scan (token-free)

- Add `checkov` or `trivy config` as another step in the same workflow.
- Decide first whether the extra third-party action is worth its supply-chain
  surface; pin it by commit SHA. Running the scanner from a pinned container
  or `pip install` with a hash is an alternative.
- Optional policy as code (OPA/conftest), e.g. "squash only must stay true".
  Probably overkill at this size.

## Step 3: remote state

- A versioned bucket with locking (S3 with a lockfile, or GCS), or HCP Terraform.
- CI authenticates to the cloud with OIDC (short-lived, no stored key); local
  runs use the maintainer's own cloud login.
- Add a `backend` block, then `terraform init -migrate-state`. Delete the local
  state file only after verifying the remote copy.
- Do this after the repo is public; state may hold sensitive values, so secure
  the bucket (no public access, encryption, least-privilege access).

## Step 4: plan on every PR

- OIDC does not cover the GitHub API, so create a **GitHub App** installed on
  the repo with read-only permissions (Administration: read, Metadata: read).
  A step mints a short-lived token per run (`actions/create-github-app-token`);
  only the app's private key is stored as a secret.
- Run `terraform plan` on PRs touching `infra/` and post it as a PR comment.
- Never use `pull_request_target`; fork PRs must not receive the secret.

## Step 5: drift detection

- Scheduled workflow (nightly): `terraform plan -detailed-exitcode`.
  Exit code 2 = someone changed a setting by hand -> open an issue / notify.
- Needs the read-only credential from step 4 and remote state from step 3.

## Step 6: gated apply (only if wanted)

- Runs on merge to `main`, in a protected GitHub Environment (e.g. `infra-prod`)
  with a required reviewer (environments need a public repo on Free).
- Uses a separate write-capable GitHub App that only that environment can see.
- Apply the saved plan file, so exactly what was reviewed is what runs.
- For a solo maintainer the approval is a formality, but it leaves an audit trail.

## Out of scope on purpose

Repository secrets stay manual (they would land in state). Private vulnerability
reporting and first-time-contributor approval have no provider resource yet;
re-check provider releases before each step.
