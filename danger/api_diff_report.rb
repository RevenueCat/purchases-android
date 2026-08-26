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

  REPO_NAME = "purchases-android".freeze

  MAIN_BRANCH = "main".freeze

  module_function

  def main_branch?(branch)
    branch.to_s.strip == MAIN_BRANCH
  end

  def current_branch(runner:)
    branch = ENV["CIRCLE_BRANCH"].to_s.strip
    return branch unless branch.empty?

    runner.call("git", "rev-parse", "--abbrev-ref", "HEAD").to_s.strip
  end

  def head_commit(runner:)
    sha = runner.call("git", "rev-parse", "HEAD").to_s.strip
    raise "Could not resolve HEAD" if sha.empty?

    sha
  end

  # main is linear squash merges, so HEAD^ holds the surface the merge replaced.
  def resolve_previous_commit(runner:)
    sha = runner.call("git", "rev-parse", "HEAD^").to_s.strip
    raise "Could not resolve the commit before HEAD" if sha.empty?

    sha
  end

  def changed_signature_files(base, head, runner:)
    runner.call("git", "diff", "--name-only", base, head)
          .to_s.each_line.map(&:strip).reject(&:empty?).select { |path| signature_file?(path) }
  end

  def patch_between(base, head, path, runner:)
    runner.call("git", "diff", base, head, "--", path).to_s
  end

  # The commit page already carries the PR link, and its sha is what tells a rerun it announced.
  def commit_link(sha)
    return "" if sha.to_s.empty?

    "<https://github.com/RevenueCat/#{REPO_NAME}/commit/#{sha}|#{sha[0, 7]}>"
  end

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

    lines = [
      "<details><summary>#{summary}</summary>",
      "",
      "```diff",
      body.join("\n"),
      "```",
      "",
      "</details>"
    ]

    lines.join("\n")
  end

  def slack_message(report, source)
    # Metalava renders a changed signature as a removal plus an addition.
    headline = if report[:removed].any?
                 ":warning: *Public API removed or changed on main*"
               else
                 ":sparkles: *New public API landed on main*"
               end
    body = capped_lines(diff_lines(report), limit: SLACK_LIMIT, width: SLACK_WIDTH)

    lines = [[headline, PLATFORM_LABEL, *report[:modules].map { |name| "`#{name}`" }].join(" · ")]
    lines << source unless source.to_s.empty?
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

  SLACK_HISTORY_LIMIT = 100

  # conversations.history takes a channel ID, never a `#name`.
  CHANNEL_ID = /\A[CGD][A-Z0-9]+\z/.freeze

  def history_request(channel, bot_token:, limit: SLACK_HISTORY_LIMIT)
    {
      url: "https://slack.com/api/conversations.history?channel=#{channel}&limit=#{limit}",
      headers: { "Authorization" => "Bearer #{bot_token}" }
    }
  end

  def recent_messages(request, getter: nil)
    getter ||= ->(url, headers) { Net::HTTP.get_response(URI.parse(url), headers) }

    response = getter.call(request[:url], request[:headers])
    raise "Slack returned #{response.code}: #{response.body}" unless (200..299).cover?(response.code.to_i)

    parsed = JSON.parse(response.body.to_s)
    raise "Slack rejected conversations.history: #{parsed['error']}" unless parsed["ok"]

    parsed["messages"].to_a.map { |message| message["text"].to_s }
  end

  # conversations.history answers newest first, so the first match is the channel's last word.
  def last_announcement(texts, source)
    return nil if source.to_s.empty?

    texts.find { |text| text.include?(source) && text.include?(PLATFORM_LABEL) }
  end

  # Each push starts two pipelines, and the auto-canceled one still posts before it dies, so the
  # channel is the only store the sibling run can see in time.
  # Returns [:same | :different | :unknown, why_unknown].
  def announcement_state(message, channel, bot_token, getter, source)
    unless CHANNEL_ID.match?(channel.to_s)
      return [:unknown, "#{channel} is a channel name, and conversations.history needs the channel ID"]
    end

    texts = recent_messages(history_request(channel, bot_token: bot_token), getter: getter)
    last = last_announcement(texts, source)
    return [:unknown, nil] if last.nil?

    [last == message ? :same : :different, nil]
  rescue StandardError => e
    [:unknown, e.message]
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

  # Returns [:posted | :duplicate | :failed, reason_or_nil].
  def announce_to_slack(message, source, credentials, poster, getter)
    return [:failed, "no Slack credentials were reachable"] if credentials.nil?

    bot_token, channel = credentials

    state, history_error = announcement_state(message, channel, bot_token, getter, source)
    return [:duplicate, nil] if state == :same

    post(slack_request(message, bot_token: bot_token, channel: channel), poster: poster)
    [:posted, history_error]
  rescue StandardError => e
    [:failed, e.message]
  end

  def warning(outcome, reason)
    return nil if reason.to_s.empty?

    case outcome
    when :failed
      "The public API changed, but it was not announced in the SDK API feed: #{reason}."
    when :posted
      "Could not read the SDK API feed channel, so this change may be announced twice: #{reason}."
    end
  end

  # Returns nil when the public API did not change, { comment: } for `announce: false`, and
  # { comment:, warning:, outcome: } otherwise. outcome is :posted, :duplicate or :failed.
  def run(changed_files:, patch_for:, source: "", announce: true, credentials: slack_credentials, poster: nil,
          getter: nil)
    signature_files = changed_files.uniq.select { |file| signature_file?(file) }
    report = build(signature_files.to_h { |file| [file, patch_for.call(file)] })
    return nil if empty?(report)

    return { comment: markdown_section(report) } unless announce

    outcome, reason = announce_to_slack(slack_message(report, source), source, credentials, poster, getter)

    {
      comment: markdown_section(report),
      warning: warning(outcome, reason),
      # A warning does not mean the post failed; it also fires on an announced-but-duplicated one.
      outcome: outcome
    }
  end
end
