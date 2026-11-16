# Bundled "terraform" ruleset: unused variables, missing type/description,
# deprecated syntax, unpinned providers. No plugin download, no token.
plugin "terraform" {
  enabled = true
  preset  = "recommended"
}
