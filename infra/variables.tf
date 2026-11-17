variable "owner" {
  description = "GitHub user or organization that owns the repositories."
  type        = string
  default     = "ddiling"
}

variable "repo_is_public" {
  description = "Public-visibility switch for mydayboard. Leave false until the go-public decision."
  type        = bool
  default     = false
}
