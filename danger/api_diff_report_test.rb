require 'minitest/autorun'
require_relative 'api_diff_report'

class ApiDiffReportTest < Minitest::Test
  # Synthetic declarations: a plausible unreleased API name in a public repo reads as a roadmap hint.
  ADDED_METHOD_PATCH = <<~PATCH.freeze
    --- a/purchases/api-defauts.txt
    +++ b/purchases/api-defauts.txt
    @@ -10,6 +10,7 @@ package com.revenuecat.purchases {

       public final class ApiDiffDemo {
         method public void apiDiffDemoPing();
    +    method public void apiDiffDemoPong();
       }

     }
  PATCH

  SIGNATURE_CHANGE_PATCH = <<~PATCH.freeze
    --- a/ui/revenuecatui/api.txt
    +++ b/ui/revenuecatui/api.txt
    @@ -3,5 +3,5 @@ package com.revenuecat.purchases.ui.revenuecatui {

       public final class ApiDiffDemoKt {
    -    method public static void apiDiffDemoPing(String value);
    +    method public static void apiDiffDemoPing(String value, optional int count);
       }
  PATCH

  NEW_TYPE_PATCH = <<~PATCH.freeze
    --- a/feature/amazon/api.txt
    +++ b/feature/amazon/api.txt
    @@ -1,3 +1,9 @@
     // Signature format: 4.0
    +package com.revenuecat.purchases.apidiffdemo {
    +
    +  public final class ApiDiffDemo {
    +    method public void apiDiffDemoPing();
    +  }
    +
    +}
     package com.revenuecat.purchases.amazon {
  PATCH

  # Must accept everything the `api.txt` / `api-*.txt` pathspecs in scripts/api-check.sh gate on.
  def test_signature_file_matches_the_api_check_globs
    assert ApiDiffReport.signature_file?("ui/revenuecatui/api.txt")
    assert ApiDiffReport.signature_file?("purchases/api-defauts.txt")
    assert ApiDiffReport.signature_file?("purchases/api-entitlement.txt")
    assert ApiDiffReport.signature_file?("purchases/api-release.debug.txt")
    refute ApiDiffReport.signature_file?("purchases/src/main/kotlin/Api.kt")
    refute ApiDiffReport.signature_file?("docs/api.txt.md")
    refute ApiDiffReport.signature_file?("docs/notapi.txt")
  end

  def test_module_name_uses_gradle_path_notation
    assert_equal "purchases", ApiDiffReport.module_name("purchases/api-defauts.txt")
    assert_equal "ui:revenuecatui", ApiDiffReport.module_name("ui/revenuecatui/api.txt")
  end

  def test_build_collects_added_declarations_in_file_order
    report = ApiDiffReport.build("feature/amazon/api.txt" => NEW_TYPE_PATCH)

    assert_equal ["public final class ApiDiffDemo", "method public void apiDiffDemoPing();"], report[:added]
    assert_empty report[:removed]
    assert_equal ["feature:amazon"], report[:modules]
  end

  def test_build_reports_a_signature_change_as_a_removal_and_an_addition
    report = ApiDiffReport.build("ui/revenuecatui/api.txt" => SIGNATURE_CHANGE_PATCH)

    assert_equal 1, report[:added].count
    assert_equal 1, report[:removed].count
    assert_includes report[:added].first, "optional int count"
  end

  def test_build_ignores_braces_package_lines_and_the_banner
    report = ApiDiffReport.build("purchases/api-defauts.txt" => ADDED_METHOD_PATCH)

    assert_equal ["method public void apiDiffDemoPong();"], report[:added]
  end

  def test_build_deduplicates_across_flavour_files
    report = ApiDiffReport.build(
      "purchases/api-defauts.txt" => ADDED_METHOD_PATCH,
      "purchases/api-entitlement.txt" => ADDED_METHOD_PATCH.gsub("api-defauts", "api-entitlement")
    )

    assert_equal 1, report[:added].count
    assert_equal ["purchases"], report[:modules]
  end

  # Member lines omit the owning type, so two types can add the same text in one file.
  def test_build_keeps_identical_declarations_on_different_types
    patch = <<~PATCH
      --- a/purchases/api-defauts.txt
      +++ b/purchases/api-defauts.txt
      @@ -10,6 +10,8 @@ package com.revenuecat.purchases {
      +  public final class Foo {
      +    method public String getName();
      +  }
      +  public final class Bar {
      +    method public String getName();
      +  }
    PATCH

    report = ApiDiffReport.build("purchases/api-defauts.txt" => patch)

    assert_equal 2, report[:added].count { |d| d == "method public String getName();" }
  end

  def test_build_is_empty_when_no_declaration_changed
    report = ApiDiffReport.build("purchases/api-defauts.txt" => "--- a/x\n+++ b/x\n@@ -1 +1 @@\n-}\n+}\n")

    assert ApiDiffReport.empty?(report)
  end

  def test_capped_lines_truncates_by_count_and_width
    lines = (1..15).map { |index| "line#{index} #{'a' * 400}" }

    shown = ApiDiffReport.capped_lines(lines, limit: 10, width: 50)

    assert_equal 11, shown.count
    assert_equal "…and 5 more", shown.last
    assert shown.first(10).all? { |line| line.length <= 50 }
  end

  def test_capped_lines_leaves_short_lists_alone
    shown = ApiDiffReport.capped_lines(["a", "b"], limit: 10, width: 50)

    assert_equal ["a", "b"], shown
  end

  def test_slack_message_labels_the_platform_and_modules
    report = ApiDiffReport.build("ui/revenuecatui/api.txt" => ADDED_METHOD_PATCH)

    message = ApiDiffReport.slack_message(report, "<https://github.com/RevenueCat/purchases-android/pull/42|#42>")

    assert message.start_with?(":sparkles: *New public API* · Android :android: · `ui:revenuecatui`")
    assert_includes message, "|#42>"
    assert_includes message, "1 new declaration"
    assert_includes message, "+ method public void apiDiffDemoPong"
  end

  def test_slack_message_warns_when_something_was_removed
    report = ApiDiffReport.build("ui/revenuecatui/api.txt" => SIGNATURE_CHANGE_PATCH)

    message = ApiDiffReport.slack_message(report, "")

    assert message.start_with?(":warning: *Public API removed or changed* · Android :android: · `ui:revenuecatui`")
    assert_includes message, "1 new declaration, 1 removed"
    refute_includes message, "|#"
  end

  def test_markdown_section_is_a_collapsible_diff_block
    report = ApiDiffReport.build("purchases/api-defauts.txt" => ADDED_METHOD_PATCH)

    section = ApiDiffReport.markdown_section(report)

    assert_includes section, "<details><summary>Public API changes in purchases (1 new declaration)</summary>"
    assert_includes section, "```diff"
    assert_includes section, "+ method public void apiDiffDemoPong"
    refute_match(/…and \d+ more/, section)
  end

  # A metalava format bump rewrites whole signature files.
  def test_markdown_section_stays_under_the_github_comment_limit
    wholesale_rewrite = { added: (1..4000).map { |index| "method public void f#{index}(#{'a' * 800});" }, removed: [], modules: ["purchases"] }

    section = ApiDiffReport.markdown_section(wholesale_rewrite)

    assert section.length < 65_536, "section was #{section.length} characters"
    assert_match(/…and \d+ more/, section)
  end

  def test_slack_request_targets_chat_post_message
    request = ApiDiffReport.slack_request("hi", bot_token: "xoxb-1", channel: "#some-channel")

    assert_equal "https://slack.com/api/chat.postMessage", request[:url]
    assert_equal "Bearer xoxb-1", request[:headers]["Authorization"]
    assert_equal({ channel: "#some-channel", text: "hi" }, request[:body])
  end

  def test_post_raises_when_slack_answers_ok_false
    response = Struct.new(:code, :body).new("200", '{"ok":false,"error":"channel_not_found"}')
    request = ApiDiffReport.slack_request("hi", bot_token: "xoxb-1", channel: "#some-channel")

    error = assert_raises(RuntimeError) { ApiDiffReport.post(request, poster: ->(*) { response }) }
    assert_includes error.message, "channel_not_found"
  end

  def test_post_raises_on_a_non_success_status
    response = Struct.new(:code, :body).new("500", "boom")
    request = ApiDiffReport.slack_request("hi", bot_token: "xoxb-1", channel: "#some-channel")

    assert_raises(RuntimeError) { ApiDiffReport.post(request, poster: ->(*) { response }) }
  end

  def test_post_accepts_a_successful_response
    response = Struct.new(:code, :body).new("200", '{"ok":true}')
    request = ApiDiffReport.slack_request("hi", bot_token: "xoxb-1", channel: "#some-channel")

    assert_nil ApiDiffReport.post(request, poster: ->(*) { response })
  end

  PATCHES = {
    "purchases/api-defauts.txt" => ADDED_METHOD_PATCH,
    "purchases/src/main/kotlin/Purchases.kt" => "irrelevant"
  }.freeze

  CHANNEL_CREDENTIALS = ["xoxb-1", "C0BLWE7VBS4"].freeze

  def history_getter(texts)
    lambda do |_url, _headers|
      Struct.new(:code, :body).new("200", { ok: true, messages: texts.map { |text| { "text" => text } } }.to_json)
    end
  end

  def collecting_poster(posted)
    lambda do |_url, request_body, _headers|
      posted << JSON.parse(request_body)
      Struct.new(:code, :body).new("200", '{"ok":true}')
    end
  end

  def announcement_for(link)
    ApiDiffReport.slack_message(ApiDiffReport.build("purchases/api-defauts.txt" => ADDED_METHOD_PATCH), link)
  end

  def test_run_returns_the_comment_body_and_posts_to_slack
    posted = []

    body = ApiDiffReport.run(
      changed_files: PATCHES.keys,
      patch_for: ->(file) { PATCHES[file] },
      pull_request_link: "<url|#42>",
      credentials: CHANNEL_CREDENTIALS,
      getter: history_getter([]),
      poster: collecting_poster(posted)
    )

    assert_includes body[:comment], "+ method public void apiDiffDemoPong"
    assert_nil body[:warning]
    assert_equal 1, posted.count
    assert_includes posted.first["text"], "<url|#42>"
  end

  def test_run_skips_slack_when_the_last_word_on_this_pull_request_is_this_summary
    body = ApiDiffReport.run(
      changed_files: PATCHES.keys,
      patch_for: ->(file) { PATCHES[file] },
      pull_request_link: "<url|#42>",
      credentials: CHANNEL_CREDENTIALS,
      getter: history_getter([announcement_for("<url|#42>"), "unrelated"]),
      poster: ->(*) { raise "must not post" }
    )

    assert_includes body[:comment], "+ method public void apiDiffDemoPong"
    assert_nil body[:warning]
  end

  def test_run_reannounces_a_surface_the_pull_request_had_already_left_behind
    posted = []
    superseded = ApiDiffReport.slack_message(
      ApiDiffReport.build("purchases/api-defauts.txt" => ADDED_METHOD_PATCH.gsub("Pong", "Pang")), "<url|#42>"
    )

    ApiDiffReport.run(
      changed_files: PATCHES.keys,
      patch_for: ->(file) { PATCHES[file] },
      pull_request_link: "<url|#42>",
      credentials: CHANNEL_CREDENTIALS,
      getter: history_getter([superseded, announcement_for("<url|#42>")]),
      poster: collecting_poster(posted)
    )

    assert_equal 1, posted.count
  end

  # What the PR touches changes the module list, so the modules cannot be part of the identity.
  def test_last_announcement_matches_a_previous_announcement_of_other_modules
    previous = ApiDiffReport.slack_message(
      ApiDiffReport.build("ui/revenuecatui/api.txt" => SIGNATURE_CHANGE_PATCH), "<url|#42>"
    )

    assert_equal previous, ApiDiffReport.last_announcement([previous], "<url|#42>")
  end

  def test_last_announcement_ignores_other_pull_requests_and_platforms
    ours = announcement_for("<url|#42>")
    texts = ["#{ours.sub(ApiDiffReport::PLATFORM_LABEL, 'iOS :ios:')}", announcement_for("<url|#41>"), ours]

    assert_equal ours, ApiDiffReport.last_announcement(texts, "<url|#42>")
  end

  def test_run_posts_when_the_channel_holds_another_pull_requests_summary
    posted = []

    ApiDiffReport.run(
      changed_files: PATCHES.keys,
      patch_for: ->(file) { PATCHES[file] },
      pull_request_link: "<url|#42>",
      credentials: CHANNEL_CREDENTIALS,
      getter: history_getter([announcement_for("<url|#41>")]),
      poster: collecting_poster(posted)
    )

    assert_equal 1, posted.count
  end

  def test_run_falls_back_to_the_marker_on_the_pull_request_when_history_is_unreadable
    markers = []

    body = ApiDiffReport.run(
      changed_files: PATCHES.keys,
      patch_for: ->(file) { PATCHES[file] },
      pull_request_link: "<url|#42>",
      credentials: CHANNEL_CREDENTIALS,
      getter: ->(*) { Struct.new(:code, :body).new("200", '{"ok":false,"error":"missing_scope"}') },
      announced_in_comment: ->(marker) { markers << marker; true },
      poster: ->(*) { raise "must not post" }
    )

    assert_match(/\A<!-- api-diff:[0-9a-f]{12} -->\z/, markers.first)
    assert_includes body[:comment], markers.first
    assert_nil body[:warning]
  end

  def test_run_warns_about_a_possible_duplicate_when_neither_store_is_readable
    posted = []

    body = ApiDiffReport.run(
      changed_files: PATCHES.keys,
      patch_for: ->(file) { PATCHES[file] },
      pull_request_link: "<url|#42>",
      credentials: CHANNEL_CREDENTIALS,
      getter: ->(*) { raise "slack is down" },
      announced_in_comment: ->(_marker) { false },
      poster: collecting_poster(posted)
    )

    assert_equal 1, posted.count
    assert_includes body[:warning], "may be announced twice: slack is down"
  end

  def test_run_records_the_fingerprint_only_once_announced
    announced = ApiDiffReport.run(
      changed_files: PATCHES.keys,
      patch_for: ->(file) { PATCHES[file] },
      pull_request_link: "<url|#42>",
      credentials: CHANNEL_CREDENTIALS,
      getter: history_getter([]),
      poster: ->(*) { Struct.new(:code, :body).new("200", '{"ok":true}') }
    )
    failed = ApiDiffReport.run(
      changed_files: PATCHES.keys,
      patch_for: ->(file) { PATCHES[file] },
      pull_request_link: "<url|#42>",
      credentials: CHANNEL_CREDENTIALS,
      getter: history_getter([]),
      poster: ->(*) { raise "slack is down" }
    )

    assert_match(/<!-- api-diff:[0-9a-f]{12} -->/, announced[:comment])
    refute_match(/<!-- api-diff:/, failed[:comment])
  end

  def test_fingerprint_survives_a_rerun_and_moves_with_the_report
    unchanged = ApiDiffReport.build("purchases/api-defauts.txt" => ADDED_METHOD_PATCH)
    changed = ApiDiffReport.build("purchases/api-defauts.txt" => ADDED_METHOD_PATCH.gsub("Pong", "Pang"))

    assert_equal ApiDiffReport.fingerprint(unchanged), ApiDiffReport.fingerprint(unchanged)
    refute_equal ApiDiffReport.fingerprint(unchanged), ApiDiffReport.fingerprint(changed)
  end

  # chat.postMessage takes a `#name`, conversations.history does not.
  def test_announcement_state_needs_the_channel_id
    state, reason = ApiDiffReport.announcement_state("hi", "#feed", "xoxb-1", ->(*) { raise "must not read" }, "<url|#42>")

    assert_equal :unknown, state
    assert_includes reason, "channel ID"
  end

  def test_announcement_state_is_unknown_without_a_reason_when_the_pull_request_is_not_in_the_window
    state, reason = ApiDiffReport.announcement_state("hi", "C1", "xoxb-1", history_getter(["unrelated"]), "<url|#42>")

    assert_equal :unknown, state
    assert_nil reason
  end

  def test_recent_messages_returns_the_texts_newest_first
    request = ApiDiffReport.history_request("C1", bot_token: "xoxb-1")

    assert_includes request[:url], "channel=C1"
    assert_equal "Bearer xoxb-1", request[:headers]["Authorization"]
    assert_equal ["new", "old"], ApiDiffReport.recent_messages(request, getter: history_getter(["new", "old"]))
  end

  def test_recent_messages_raises_when_the_token_cannot_read_the_channel
    response = Struct.new(:code, :body).new("200", '{"ok":false,"error":"missing_scope"}')
    request = ApiDiffReport.history_request("C1", bot_token: "xoxb-1")

    error = assert_raises(RuntimeError) { ApiDiffReport.recent_messages(request, getter: ->(*) { response }) }
    assert_includes error.message, "missing_scope"
  end

  def test_run_reports_a_skipped_announcement_without_credentials
    body = ApiDiffReport.run(
      changed_files: PATCHES.keys,
      patch_for: ->(file) { PATCHES[file] },
      pull_request_link: "<url|#42>",
      credentials: nil,
      poster: ->(*) { raise "must not post" }
    )

    assert_includes body[:comment], "+ method public void apiDiffDemoPong"
    assert_includes body[:warning], "no Slack credentials were reachable"
  end

  def test_run_still_returns_the_comment_when_slack_fails
    body = ApiDiffReport.run(
      changed_files: PATCHES.keys,
      patch_for: ->(file) { PATCHES[file] },
      pull_request_link: "<url|#42>",
      credentials: ["xoxb-1", "#some-channel"],
      poster: ->(*) { raise "slack is down" }
    )

    assert_includes body[:comment], "+ method public void apiDiffDemoPong"
    assert_includes body[:warning], "not announced in the SDK API feed: slack is down"
  end

  def test_slack_credentials_accepts_the_legacy_ios_token_name
    ENV["SLACK_CHANNEL_SDK_NEW_API"] = "#some-channel"
    ENV["SLACK_ACCESS_TOKEN_CIRCLE_CI_NOTIFY_ORB_IOS"] = "xoxb-legacy"

    assert_equal ["xoxb-legacy", "#some-channel"], ApiDiffReport.slack_credentials
  ensure
    ENV.delete("SLACK_CHANNEL_SDK_NEW_API")
    ENV.delete("SLACK_ACCESS_TOKEN_CIRCLE_CI_NOTIFY_ORB_IOS")
  end

  def test_slack_credentials_is_nil_without_a_channel
    ENV["SLACK_ACCESS_TOKEN_CIRCLE_CI_NOTIFY_ORB"] = "xoxb-1"
    ENV.delete("SLACK_CHANNEL_SDK_NEW_API")

    assert_nil ApiDiffReport.slack_credentials
  ensure
    ENV.delete("SLACK_ACCESS_TOKEN_CIRCLE_CI_NOTIFY_ORB")
  end

  def test_run_is_nil_when_no_signature_file_changed
    assert_nil ApiDiffReport.run(
      changed_files: ["purchases/src/main/kotlin/Purchases.kt"],
      patch_for: ->(_file) { "irrelevant" },
      pull_request_link: "<url|#42>",
      credentials: ["xoxb-1", "#some-channel"],
      poster: ->(*) { raise "must not post" }
    )
  end

  def test_run_tolerates_a_missing_patch
    assert_nil ApiDiffReport.run(
      changed_files: ["purchases/api-defauts.txt"],
      patch_for: ->(_file) { nil },
      pull_request_link: "",
      credentials: nil
    )
  end
end
