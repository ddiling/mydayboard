variable "name" {
  description = "Repository name (without the owner)."
  type        = string
}

variable "description" {
  description = "One-line repository description shown on GitHub."
  type        = string
  default     = ""
}

variable "topics" {
  description = "Repository topics (lowercase, hyphenated)."
  type        = list(string)
  default     = []
}

variable "default_branch" {
  description = "Branch that the ruleset protects."
  type        = string
  default     = "main"
}

variable "is_public" {
  description = <<-EOT
    The single switch for visibility. false = private, true = public.
    Everything that GitHub Free only offers on public repos (ruleset, secret
    scanning, Dependabot, environments) is created only when this is true, so a
    private repo plans and applies cleanly. Flipping it to true changes the repo
    to PUBLIC, so do that deliberately, in its own reviewed PR.
  EOT
  type        = bool
  default     = false
}

variable "required_status_checks" {
  description = "Names of the status checks that must pass before merging into the default branch."
  type        = list(string)
  default     = []
}

variable "release_environment" {
  description = "Name of a GitHub Environment to create (public repos only). Null = none."
  type        = string
  default     = null
}
