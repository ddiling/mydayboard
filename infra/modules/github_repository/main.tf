# ---------------------------------------------------------------------------
# The repository itself and its merge settings. Always applied.
# ---------------------------------------------------------------------------
resource "github_repository" "this" {
  name        = var.name
  description = var.description
  topics      = var.topics
  visibility  = var.is_public ? "public" : "private"

  # Features that are switched on or off explicitly, so the code (not GitHub's
  # defaults) is the source of truth.
  has_issues   = true
  has_projects = true
  has_wiki     = false

  # Squash merge only: one PR becomes exactly one commit on main.
  allow_squash_merge = true
  allow_merge_commit = false
  allow_rebase_merge = false

  # The squash commit reuses the PR title and the PR description, so the
  # PR text is the commit message (this is why PR titles are conventional).
  squash_merge_commit_title   = "PR_TITLE"
  squash_merge_commit_message = "PR_BODY"

  delete_branch_on_merge = true # remove the head branch once merged
  allow_update_branch    = true # "Update branch" button on a PR that fell behind

  # PRs can merge themselves once required checks pass. GitHub silently ignores
  # this on a private Free repo (the API answers "false"), which would make
  # Terraform report a change forever, so it follows visibility like the other
  # Free-plan-limited settings.
  allow_auto_merge = var.is_public

  # Public-only: secret scanning and push protection. On a private Free repo
  # these need GitHub Advanced Security, so the block is omitted until public.
  dynamic "security_and_analysis" {
    for_each = var.is_public ? [1] : []
    content {
      secret_scanning {
        status = "enabled"
      }
      secret_scanning_push_protection {
        status = "enabled"
      }
    }
  }

  # Safety nets: Terraform must never delete the repo, and if it ever were
  # forced to, it would archive instead of destroying the code.
  archive_on_destroy = true
  lifecycle {
    prevent_destroy = true
  }
}

# ---------------------------------------------------------------------------
# Public-only resources. count = 0 while private, so they do not exist in the
# plan at all and cannot fail.
# ---------------------------------------------------------------------------
locals {
  public_only = var.is_public ? 1 : 0
}

# Rules for the default branch: PRs required, checks must pass, no force-push,
# no deletion. A ruleset (rather than classic branch protection) is the current
# GitHub mechanism and is easier to review as code.
resource "github_repository_ruleset" "default_branch" {
  count = local.public_only

  name        = "protect-${var.default_branch}"
  repository  = github_repository.this.name
  target      = "branch"
  enforcement = "active"

  conditions {
    ref_name {
      include = ["refs/heads/${var.default_branch}"]
      exclude = []
    }
  }

  # No bypass actors: the rules also apply to the admin. (Solo maintainer:
  # open a PR and let the checks pass, like any contributor.)

  rules {
    deletion         = true # block deleting the branch
    non_fast_forward = true # block force-pushes

    pull_request {
      required_approving_review_count = 0     # solo maintainer: no second reviewer exists
      require_code_owner_review       = false # explicitly NOT required
      dismiss_stale_reviews_on_push   = false
      require_last_push_approval      = false
      allowed_merge_methods           = ["squash"]
    }

    dynamic "required_status_checks" {
      for_each = length(var.required_status_checks) > 0 ? [1] : []
      content {
        strict_required_status_checks_policy = false # branch need not be up to date
        dynamic "required_check" {
          for_each = var.required_status_checks
          content {
            context = required_check.value
          }
        }
      }
    }
  }
}

# Dependabot alerts: notifications about vulnerable dependencies.
resource "github_repository_vulnerability_alerts" "this" {
  count = local.public_only

  repository = github_repository.this.name
  enabled    = true
}

# Dependabot security updates: automatic PRs that fix those alerts.
# Needs the alerts above, hence depends_on.
resource "github_repository_dependabot_security_updates" "this" {
  count = local.public_only

  repository = github_repository.this.name
  enabled    = true

  depends_on = [github_repository_vulnerability_alerts.this]
}

# Optional deployment environment (release.yml uses `environment: release`).
resource "github_repository_environment" "release" {
  count = var.is_public && var.release_environment != null ? 1 : 0

  repository  = github_repository.this.name
  environment = var.release_environment
}
