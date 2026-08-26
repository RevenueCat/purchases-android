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

    assert message.start_with?(":sparkles: *New public API landed on main* · Android :android: · `ui:revenuecatui`")
    assert_includes message, "|#42>"
    assert_includes message, "1 new declaration"
    assert_includes message, "+ method public void apiDiffDemoPong"
  end

  def test_slack_message_warns_when_something_was_removed
    report = ApiDiffReport.build("ui/revenuecatui/api.txt" => SIGNATURE_CHANGE_PATCH)

    message = ApiDiffReport.slack_message(report, "")

    assert message.start_with?(":warning: *Public API removed or changed on main* · Android :android: · `ui:revenuecatui`")
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
      source: "<url|#42>",
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
      source: "<url|#42>",
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
      source: "<url|#42>",
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
      source: "<url|#42>",
      credentials: CHANNEL_CREDENTIALS,
      getter: history_getter([announcement_for("<url|#41>")]),
      poster: collecting_poster(posted)
    )

    assert_equal 1, posted.count
  end

  def test_run_warns_about_a_possible_duplicate_when_history_is_unreadable
    posted = []

    body = ApiDiffReport.run(
      changed_files: PATCHES.keys,
      patch_for: ->(file) { PATCHES[file] },
      source: "<url|#42>",
      credentials: CHANNEL_CREDENTIALS,
      getter: ->(*) { raise "slack is down" },
      poster: collecting_poster(posted)
    )

    assert_equal 1, posted.count
    assert_includes body[:warning], "may be announced twice: slack is down"
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
      source: "<url|#42>",
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
      source: "<url|#42>",
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

  # --- Announcing from main only ---

  def with_circle_branch(value)
    previous = ENV["CIRCLE_BRANCH"]
    ENV["CIRCLE_BRANCH"] = value
    yield
  ensure
    ENV["CIRCLE_BRANCH"] = previous
  end

  def test_current_branch_prefers_the_ci_variable
    with_circle_branch("main") do
      assert_equal "main", ApiDiffReport.current_branch(runner: ->(*_c) { "detached\n" })
    end
  end

  def test_current_branch_falls_back_to_git
    with_circle_branch(nil) do
      runner = ->(*command) { command == ["git", "rev-parse", "--abbrev-ref", "HEAD"] ? "my-branch\n" : "" }

      assert_equal "my-branch", ApiDiffReport.current_branch(runner: runner)
    end
  end

  # release/* runs the same pipeline and must not re-announce what main already did.
  def test_only_main_counts_as_main
    assert ApiDiffReport.main_branch?("main")
    assert ApiDiffReport.main_branch?(" main\n")
    refute ApiDiffReport.main_branch?("release/10.19.0")
    refute ApiDiffReport.main_branch?("facu/my-branch")
    refute ApiDiffReport.main_branch?(nil)
  end

  def test_previous_commit_is_the_commit_the_merge_replaced
    runner = ->(*command) { command == ["git", "rev-parse", "HEAD^"] ? "prevsha\n" : "" }

    assert_equal "prevsha", ApiDiffReport.resolve_previous_commit(runner: runner)
  end

  # A root commit would otherwise diff the whole public API in as new.
  def test_previous_commit_raises_when_empty
    error = assert_raises(RuntimeError) { ApiDiffReport.resolve_previous_commit(runner: ->(*_c) { "\n" }) }

    assert_match(/before HEAD/, error.message)
  end

  def test_head_commit_raises_when_empty
    error = assert_raises(RuntimeError) { ApiDiffReport.head_commit(runner: ->(*_c) { "\n" }) }

    assert_match(/HEAD/, error.message)
  end

  def test_changed_signature_files_keeps_only_the_signature_files
    runner = lambda do |*command|
      next "" unless command == ["git", "diff", "--name-only", "basesha", "headsha"]

      "purchases/api-defauts.txt\npurchases/src/main/kotlin/Purchases.kt\nui/revenuecatui/api.txt\n"
    end

    assert_equal ["purchases/api-defauts.txt", "ui/revenuecatui/api.txt"],
                 ApiDiffReport.changed_signature_files("basesha", "headsha", runner: runner)
  end

  # A `diff.external` in the caller's config silently empties the patch, so the flag is the test.
  def test_patch_between_asks_git_for_that_one_file_ignoring_any_external_differ
    asked = []
    runner = ->(*command) { asked << command; ADDED_METHOD_PATCH }

    patch = ApiDiffReport.patch_between("basesha", "headsha", "purchases/api-defauts.txt", runner: runner)

    assert_equal [["git", "diff", "--no-ext-diff", "basesha", "headsha", "--", "purchases/api-defauts.txt"]], asked
    assert_equal ADDED_METHOD_PATCH, patch
  end

  def test_commit_link_carries_the_short_sha
    assert_equal "<https://github.com/RevenueCat/purchases-android/commit/0123456789abcdef|0123456>",
                 ApiDiffReport.commit_link("0123456789abcdef")
  end

  # last_announcement bails on an empty source, so an empty link would disable the suppression.
  def test_commit_link_is_empty_without_a_sha
    assert_equal "", ApiDiffReport.commit_link("")
    assert_equal "", ApiDiffReport.commit_link(nil)
  end

  def test_run_suppresses_a_rerun_of_the_same_commit
    link = ApiDiffReport.commit_link("0123456789abcdef")

    body = ApiDiffReport.run(
      changed_files: PATCHES.keys,
      patch_for: ->(file) { PATCHES[file] },
      source: link,
      credentials: CHANNEL_CREDENTIALS,
      getter: history_getter([announcement_for(link)]),
      poster: ->(*) { raise "must not post" }
    )

    assert_nil body[:warning]
  end

  def test_run_reports_without_announcing_when_announce_is_false
    body = ApiDiffReport.run(
      changed_files: PATCHES.keys,
      patch_for: ->(file) { PATCHES[file] },
      announce: false,
      credentials: CHANNEL_CREDENTIALS,
      getter: ->(*) { raise "must not read" },
      poster: ->(*) { raise "must not post" }
    )

    assert_includes body[:comment], "+ method public void apiDiffDemoPong"
    # Nothing was attempted, so there is no outcome to report and nothing to warn about.
    assert_equal [:comment], body.keys
  end

  # The runner logged "Announced the public API change" off a failed post.
  def test_run_reports_what_became_of_the_announcement
    link = ApiDiffReport.commit_link("0123456789abcdef")

    posted = ApiDiffReport.run(
      changed_files: PATCHES.keys, patch_for: ->(file) { PATCHES[file] }, source: link,
      credentials: CHANNEL_CREDENTIALS, getter: history_getter([]), poster: collecting_poster([])
    )
    duplicate = ApiDiffReport.run(
      changed_files: PATCHES.keys, patch_for: ->(file) { PATCHES[file] }, source: link,
      credentials: CHANNEL_CREDENTIALS, getter: history_getter([announcement_for(link)]),
      poster: ->(*) { raise "must not post" }
    )
    failed = ApiDiffReport.run(
      changed_files: PATCHES.keys, patch_for: ->(file) { PATCHES[file] }, source: link,
      credentials: nil, poster: ->(*) { raise "must not post" }
    )
    assert_equal :posted, posted[:outcome]
    assert_equal :duplicate, duplicate[:outcome]
    assert_equal :failed, failed[:outcome]
  end

  # --- The wiring ---

  def dangerfile
    File.read(File.expand_path("../Dangerfile", __dir__))
  end

  def announce_script
    File.read(File.expand_path("announce_api_changes.rb", __dir__))
  end

  def circleci_config
    File.read(File.expand_path("../.circleci/config.yml", __dir__))
  end

  def test_danger_reports_the_diff_without_announcing_it
    call = dangerfile[/ApiDiffReport\.run\(.*?\n  \)/m]

    refute_nil call, "the ApiDiffReport.run call in the Dangerfile moved; update this test"
    assert_match(/announce: false/, call, "a PR run must not announce")
  end

  def test_the_announce_script_bails_off_main
    assert_match(/main_branch\?/, announce_script, "the script must bail off main")
    assert_match(/resolve_previous_commit/, announce_script, "main compares against the previous commit")
    assert_match(/commit_link/, announce_script, "the announcement links the commit")
  end

  # It used to print that it had announced even when the post failed.
  def test_the_announce_script_does_not_claim_an_announcement_it_did_not_make
    assert_match(/:outcome\]|\[:outcome/, announce_script, "the script must read the outcome")
    assert_match(/:failed/, announce_script, "a failed post must not read as success")
  end

  # The script is the only thing that announces now, so a workflow that never runs it means silence.
  def test_circleci_runs_the_announce_script_on_main_with_the_slack_context
    workflow = circleci_config[/^  announce-api-changes-on-main:\n(?:.+\n|\n)*?(?=^  \S)/]

    refute_nil workflow, "the announce-api-changes-on-main workflow moved; update this test"
    assert_match(/"main", << pipeline\.git\.branch >>/, workflow, "it must be gated on main")
    assert_match(/- announce-api-changes:/, workflow)
    assert_match(/slack-secrets/, workflow, "the announcement needs the Slack token and channel")
  end

  # The context was there only for the announcement. A PR run has no reason to hold a write token.
  def test_the_danger_job_no_longer_takes_the_slack_context
    workflow = circleci_config[/^  danger:\n(?:.+\n|\n)*?(?=^  \S)/]

    refute_nil workflow, "the danger workflow moved; update this test"
    refute_match(/slack-secrets/, workflow)
  end

  def test_circleci_has_a_job_that_runs_the_announce_script
    job = circleci_config[/^  announce-api-changes:\n(?:.+\n|\n)*?(?=^  \S)/]

    refute_nil job, "the announce-api-changes job moved; update this test"
    assert_match(%r{ruby danger/announce_api_changes\.rb}, job)
  end

  def test_run_is_nil_when_no_signature_file_changed
    assert_nil ApiDiffReport.run(
      changed_files: ["purchases/src/main/kotlin/Purchases.kt"],
      patch_for: ->(_file) { "irrelevant" },
      source: "<url|#42>",
      credentials: ["xoxb-1", "#some-channel"],
      poster: ->(*) { raise "must not post" }
    )
  end

  def test_run_tolerates_a_missing_patch
    assert_nil ApiDiffReport.run(
      changed_files: ["purchases/api-defauts.txt"],
      patch_for: ->(_file) { nil },
      source: "",
      credentials: nil
    )
  end
end
