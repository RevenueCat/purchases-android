# Reports the public API a PR changes, from the metalava signature diffs. Loaded by the Dangerfile.

require 'json'
require 'net/http'
require 'uri'

module ApiDiffReport
  PLATFORM_LABEL = "Android :android:".freeze

  # The same two patterns scripts/api-check.sh gates on.
  SIGNATURE_GLOBS = ["api.txt", "api-*.txt"].freeze

  # A metalava 4.0 file holds nothing but the banner, `package … {`, `}`, types and members.
  NOISE = %r{\A(\}|package\s|//)}.freeze

  SLACK_LIMIT = 10

  # Slack stops wrapping code blocks past this; the full list is in the PR comment.
  SLACK_WIDTH = 160

  # GitHub answers 422 over 65536 characters, and Danger shares one comment across every rule.
  MARKDOWN_LIMIT = 200
  MARKDOWN_WIDTH = 200

  module_function

  def signature_file?(path)
    basename = File.basename(path.to_s)

    SIGNATURE_GLOBS.any? { |glob| File.fnmatch?(glob, basename) }
  end

  def module_name(path)
    File.dirname(path.to_s).tr("/", ":")
  end

  def declarations(patch)
    added = []
    removed = []

    patch.to_s.each_line do |raw_line|
      sign = raw_line[0]
      next unless sign == "+" || sign == "-"
      next if raw_line.start_with?("+++", "---")

      line = raw_line[1..].to_s.strip
      next if line.empty? || NOISE.match?(line)

      # Members end in `;`; only a type header carries the opening brace of its body.
      (sign == "+" ? added : removed) << line.sub(/\s*\{\z/, "")
    end

    [added, removed]
  end

  # A member line omits its owning type, so two types in one file can add the same text. Identity is
  # therefore the text plus how many times it has already appeared in that file.
  def occurrences(declarations)
    seen = Hash.new(0)

    declarations.map do |declaration|
      seen[declaration] += 1
      [declaration, seen[declaration]]
    end
  end

  def build(patches_by_path)
    by_module = {}

    patches_by_path.each do |path, patch|
      path_added, path_removed = declarations(patch)
      next if path_added.empty? && path_removed.empty?

      # :purchases keeps two flavour files of largely the same surface, so a shared declaration
      # lands in both. Union per module, which keeps repeats inside one file.
      bucket = (by_module[module_name(path)] ||= { added: [], removed: [] })
      bucket[:added] |= occurrences(path_added)
      bucket[:removed] |= occurrences(path_removed)
    end

    modules = by_module.keys.sort

    {
      added: modules.flat_map { |name| by_module[name][:added].map(&:first) },
      removed: modules.flat_map { |name| by_module[name][:removed].map(&:first) },
      modules: modules
    }
  end

  def empty?(report)
    report[:added].empty? && report[:removed].empty?
  end

  def counts(report)
    parts = []
    parts << "#{report[:added].count} new declaration#{'s' if report[:added].count != 1}" if report[:added].any?
    parts << "#{report[:removed].count} removed" if report[:removed].any?
    parts.join(", ")
  end

  def diff_lines(report)
    report[:removed].map { |declaration| "- #{declaration}" } +
      report[:added].map { |declaration| "+ #{declaration}" }
  end

  def capped_lines(lines, limit:, width:)
    shown = lines.first(limit).map do |line|
      line.length > width ? "#{line[0, width - 1]}…" : line
    end
    remaining = lines.count - shown.count
    shown << "…and #{remaining} more" if remaining.positive?

    shown
  end

  def markdown_section(report)
    summary = "Public API changes in #{report[:modules].join(', ')} (#{counts(report)})"
    body = capped_lines(diff_lines(report), limit: MARKDOWN_LIMIT, width: MARKDOWN_WIDTH)

    [
      "<details><summary>#{summary}</summary>",
      "",
      "```diff",
      body.join("\n"),
      "```",
      "",
      "</details>"
    ].join("\n")
  end

  def slack_message(report, pull_request_link)
    # Metalava renders a changed signature as a removal plus an addition.
    headline = report[:removed].any? ? ":warning: *Public API removed or changed*" : ":sparkles: *New public API*"
    body = capped_lines(diff_lines(report), limit: SLACK_LIMIT, width: SLACK_WIDTH)

    lines = [[headline, PLATFORM_LABEL, *report[:modules].map { |name| "`#{name}`" }].join(" · ")]
    lines << pull_request_link unless pull_request_link.to_s.empty?
    lines << counts(report)
    lines << "```\n#{body.join("\n")}\n```"

    lines.join("\n")
  end

  # The shared slack-secrets context still carries this token under its original iOS-only name.
  TOKEN_VARIABLES = ["SLACK_ACCESS_TOKEN_CIRCLE_CI_NOTIFY_ORB", "SLACK_ACCESS_TOKEN_CIRCLE_CI_NOTIFY_ORB_IOS"].freeze

  def slack_credentials
    token = TOKEN_VARIABLES.map { |name| ENV[name].to_s }.find { |value| !value.empty? }
    channel = ENV["SLACK_CHANNEL_SDK_NEW_API"].to_s
    return nil if token.nil? || channel.empty?

    [token, channel]
  end

  def slack_request(message, bot_token:, channel:)
    {
      url: "https://slack.com/api/chat.postMessage",
      headers: { "Content-Type" => "application/json", "Authorization" => "Bearer #{bot_token}" },
      body: { channel: channel, text: message }
    }
  end

  def post(request, poster: nil)
    poster ||= ->(url, body, headers) { Net::HTTP.post(URI.parse(url), body, headers) }

    response = poster.call(request[:url], request[:body].to_json, request[:headers])
    raise "Slack returned #{response.code}: #{response.body}" unless (200..299).cover?(response.code.to_i)

    # chat.postMessage answers 200 with ok:false.
    parsed = begin
      JSON.parse(response.body.to_s)
    rescue JSON::ParserError
      nil
    end
    raise "Slack rejected the message: #{parsed['error']}" if parsed.is_a?(Hash) && parsed["ok"] == false

    nil
  end

  # The PR comment is the report and Slack only mirrors it, so a failed announcement must not cost
  # us the comment. Returns the reason it was not announced, or nil.
  def announce(report, pull_request_link, credentials, poster)
    return "no Slack credentials were reachable" if credentials.nil?

    bot_token, channel = credentials
    post(slack_request(slack_message(report, pull_request_link), bot_token: bot_token, channel: channel), poster: poster)
    nil
  rescue StandardError => e
    e.message
  end

  # Returns { comment:, slack_error: }, or nil when the public API did not change.
  def run(changed_files:, patch_for:, pull_request_link:, credentials: slack_credentials, poster: nil)
    signature_files = changed_files.uniq.select { |file| signature_file?(file) }
    report = build(signature_files.to_h { |file| [file, patch_for.call(file)] })
    return nil if empty?(report)

    {
      comment: markdown_section(report),
      slack_error: announce(report, pull_request_link, credentials, poster)
    }
  end
end
