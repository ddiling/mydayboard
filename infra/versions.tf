terraform {
  required_version = ">= 1.6"

  required_providers {
    github = {
      source = "integrations/github"
      # Pessimistic constraint: accept 6.13.x patch releases, never 6.14+ or 7.x
      # without a deliberate change. The exact build is pinned by .terraform.lock.hcl.
      version = "~> 6.13.0"
    }
  }
}
