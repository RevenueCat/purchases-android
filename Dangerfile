danger.import_dangerfile(github: 'RevenueCat/Dangerfile')

# Prevent importing pre-built material icons in :ui:revenuecatui.
# The module avoids the material-icons runtime dependency by defining custom
# inline icon vectors using materialIcon/materialPath. Using Icons.Filled.* etc.
# causes NoClassDefFoundError at runtime in apps that don't bundle the artifact.
revenuecatui_kt = (git.modified_files + git.added_files)
  .select { |f| f.start_with?("ui/revenuecatui/") && f.end_with?(".kt") }

revenuecatui_kt.each do |file|
  diff = git.diff_for_file(file)
  next unless diff

  diff.patch.each_line do |line|
    next unless line.start_with?("+") && !line.start_with?("+++")
    if line.match?(/^\+\s*import androidx\.compose\.material\.icons\./) &&
       !line.match?(/^\+\s*import androidx\.compose\.material\.icons\.material(Icon|Path)/)
      warn(
        "`#{file}` adds a direct material-icons import (`#{line.sub(/^\+/, '').strip}`). " \
        "`:ui:revenuecatui` avoids the `material-icons` runtime dependency — " \
        "add a custom inline icon to `icons/` instead (see `CircleOutlined.kt`).",
        file: file,
      )
    end
  end
end

# Fail PRs that change too many lines of production Kotlin/Java code.
# Large PRs are hard to review well, so we cap the churn (insertions + deletions)
# across real .kt/.java files, excluding test sources and generated/build output.
# Authors who legitimately need a large PR can add the bypass label.
PROD_LINES_LIMIT = 300
SKIP_SIZE_LABEL = "skip-pr-lines-changed-check"

prod_code_files = (git.modified_files + git.added_files).uniq.select do |f|
  next false unless f.end_with?(".kt") || f.end_with?(".java")
  next false if f.include?("/src/test/") || f.include?("/src/androidTest/") ||
                f.include?("/src/testFixtures/")
  next false if f.include?("/build/") || f.include?("/generated/")
  next false if f.match?(/Tests?\.(kt|java)$/)
  true
end

total_changed = prod_code_files.sum do |f|
  # Read diff stats directly with a nil guard instead of git.info_for_file, which
  # crashes on renamed files: git keys `diff.stats[:files]` with brace-arrow rename
  # notation rather than the resolved new path, so the lookup returns nil.
  stats = git.diff.stats[:files][f]
  stats ? stats[:insertions] + stats[:deletions] : 0
end

if total_changed > PROD_LINES_LIMIT
  if github.pr_labels.include?(SKIP_SIZE_LABEL)
    message("This PR changes #{total_changed} lines of production Kotlin/Java " \
            "(limit #{PROD_LINES_LIMIT}); skipped via `#{SKIP_SIZE_LABEL}` label.")
  else
    fail("This PR changes #{total_changed} lines of production Kotlin/Java code, " \
         "over the #{PROD_LINES_LIMIT}-line limit. Split it into smaller PRs, or add " \
         "the `#{SKIP_SIZE_LABEL}` label to bypass.")
  end
end

# detektAllBaseline rewrites the whole file, so count <ID> entries rather than diff lines.
# Match by basename: three files share this name and only one is live.
BASELINE_FILENAME = "detekt-baseline.xml"
SKIP_BASELINE_LABEL = "skip-baseline-growth-check"

grown_baselines = (git.modified_files + git.added_files).uniq.select do |file|
  next false unless File.basename(file) == BASELINE_FILENAME
  diff = git.diff_for_file(file)
  next true unless diff # unreadable diff: fail closed rather than wave the file through

  added, removed = diff.patch.each_line.reduce([0, 0]) do |(a, r), line|
    next [a, r] unless line.include?("<ID>")
    next [a + 1, r] if line.start_with?("+") && !line.start_with?("+++")
    next [a, r + 1] if line.start_with?("-") && !line.start_with?("---")
    [a, r]
  end
  added > removed
end

if grown_baselines.any?
  if github.pr_labels.include?(SKIP_BASELINE_LABEL)
    message("This PR adds entries to #{grown_baselines.join(', ')}; " \
            "skipped via `#{SKIP_BASELINE_LABEL}` label.")
  else
    fail("This PR adds entries to #{grown_baselines.join(', ')}. Baselines should only shrink: " \
         "fix the finding, or `@Suppress(\"RuleName\")` it at the smallest possible scope. " \
         "For a bulk change such as a detekt upgrade or a newly added rule, add the " \
         "`#{SKIP_BASELINE_LABEL}` label and re-run this job.")
  end
end

fail_on_generated_edits(["purchases/src/main/kotlin/generated/"])

# Report the public API this PR changes. The SDK API feed is announced from main instead, by
# danger/announce_api_changes.rb.
# Best effort: a raise here would take every other rule in this file down with it.
begin
  require_relative "danger/api_diff_report"

  api_diff = ApiDiffReport.run(
    changed_files: git.modified_files + git.added_files + git.deleted_files,
    patch_for: ->(file) { git.diff_for_file(file)&.patch },
    announce: false,
  )

  markdown(api_diff[:comment]) if api_diff
rescue StandardError => e
  # `warn` is Danger's DSL: surfaces on the PR without failing the run.
  warn("Could not report the public API changes: #{e.message}")
end
