#!/usr/bin/env ruby
# frozen_string_literal: true

# Announces the public API a commit on main changes, into the SDK API feed channel.
#
# Danger reports the same surface on the pull request, but it no longer announces: every PR run
# posted, so one change showed up in the feed once per push. Announcing from main instead means each
# change lands there once, when it is actually about to ship.
#
# The signature files are committed, so the diff is just `HEAD^..HEAD` over them. Nothing is built.

require 'English'

require_relative "api_diff_report"

# Matches the shape ApiDiffReport expects: the whole command, argv-style, no shell in between.
RUNNER = lambda do |*command|
  output = IO.popen(command, &:read)
  raise "#{command.join(' ')} failed with #{$CHILD_STATUS.exitstatus}" unless $CHILD_STATUS.success?

  output
end

branch = ApiDiffReport.current_branch(runner: RUNNER)
unless ApiDiffReport.main_branch?(branch)
  puts "On #{branch}, not main. Nothing to announce."
  exit 0
end

head = ApiDiffReport.head_commit(runner: RUNNER)
base = ApiDiffReport.resolve_previous_commit(runner: RUNNER)
puts "Comparing #{base}..#{head}"

changed_files = ApiDiffReport.changed_signature_files(base, head, runner: RUNNER)
if changed_files.empty?
  puts "No signature file changed."
  exit 0
end

result = ApiDiffReport.run(
  changed_files: changed_files,
  patch_for: ->(file) { ApiDiffReport.patch_between(base, head, file, runner: RUNNER) },
  source: ApiDiffReport.commit_link(head)
)

if result.nil?
  puts "#{changed_files.join(', ')} changed, but no declaration did."
  exit 0
end

# A feed that fails to post is not worth reddening main over, and the PR already reported the
# surface. The message goes to stderr so the job log still carries it.
warn(result[:warning]) if result[:warning]

case result[:outcome]
when :posted     then puts "Announced the public API change on #{head}."
when :duplicate  then puts "#{head} was already announced."
when :failed     then puts "Could not announce #{head}. See the warning above."
else                  puts "Nothing was announced for #{head} (#{result[:outcome]})."
end
