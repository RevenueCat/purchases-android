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

  def test_run_returns_the_comment_body_and_posts_to_slack
    posted = []

    body = ApiDiffReport.run(
      changed_files: PATCHES.keys,
      patch_for: ->(file) { PATCHES[file] },
      pull_request_link: "<url|#42>",
      credentials: ["xoxb-1", "#some-channel"],
      poster: lambda do |_url, request_body, _headers|
        posted << JSON.parse(request_body)
        Struct.new(:code, :body).new("200", '{"ok":true}')
      end
    )

    assert_includes body[:comment], "+ method public void apiDiffDemoPong"
    assert_nil body[:slack_error]
    assert_equal 1, posted.count
    assert_includes posted.first["text"], "<url|#42>"
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
    assert_equal "no Slack credentials were reachable", body[:slack_error]
  end

  # The comment is the report; Slack only mirrors it.
  def test_run_still_returns_the_comment_when_slack_fails
    body = ApiDiffReport.run(
      changed_files: PATCHES.keys,
      patch_for: ->(file) { PATCHES[file] },
      pull_request_link: "<url|#42>",
      credentials: ["xoxb-1", "#some-channel"],
      poster: ->(*) { raise "slack is down" }
    )

    assert_includes body[:comment], "+ method public void apiDiffDemoPong"
    assert_equal "slack is down", body[:slack_error]
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
