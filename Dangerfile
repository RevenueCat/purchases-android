#### HELPER METHODS
def fail_if_no_supported_label_found
  supported_types = ["breaking", "build", "ci", "docs", "feat", "fix", "perf", "refactor", "style", "test", "next_release"]

  supported_labels_in_pr = supported_types & github.pr_labels
  no_supported_label = supported_labels_in_pr.empty?
  if no_supported_label
    fail("Label the PR using one of the change type labels: #{supported_types}")
    markdown <<-MARKDOWN
    | Label | Description |
    |-------|-------------|
    | breaking | Changes that are breaking |
    | build | Changes that affect the build system |
    | ci | Changes to our CI configuration files and scripts |
    | docs | Documentation only changes |
    | feat | A new feature |
    | fix | A bug fix |
    | perf | A code change that improves performance |
    | refactor | A code change that neither fixes a bug nor adds a feature |
    | style | Changes that don't affect the meaning of the code (white-space, formatting, missing semi-colons, etc |
    | test | Adding missing tests or correcting existing tests |
    | next_release | Preparing a new release |
    MARKDOWN
  end
end

# Fail when GitHub PR label is missing
fail_if_no_supported_label_found
