provider "github" {
  owner = var.owner
  # No token here. The provider reads GITHUB_TOKEN from the environment.
}

# First instance of the module. To manage another repo, add another block.
module "mydayboard" {
  source = "./modules/github_repository"

  name        = "mydayboard"
  description = "Modular Android app (Kotlin, Compose): alarms, timers, trackers and recurring tasks behind one dashboard"

  # Private for now. Set to true ONLY when you decide to go public: that flips
  # the visibility and creates the ruleset, security features and environment.
  is_public = var.repo_is_public

  required_status_checks = ["CI passed"]
  release_environment    = "release"
}
