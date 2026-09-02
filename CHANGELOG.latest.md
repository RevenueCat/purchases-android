## RevenueCat SDK
### 🐞 Bugfixes
* Narrow consumer R8 rules (#3557) via Toni Rico (@tonidero)
* Surface attribute sync errors when fetching offerings (#4129) via Rick (@rickvdl)
* Fall back to "en" when a paywall screen omits `default_locale` (#4111) via Monika Mateska (@MonikaMateska)
* Cache web purchase redemption CustomerInfo for the initiating App User ID (#4114) via Rick (@rickvdl)

## RevenueCatUI SDK
### Paywallsv2
#### ✨ New Features
* feat(paywalls): send workflow and step identity to custom components (#4122) via Álvaro Brey (@AlvaroBrey)
* feat(paywalls): send offering and package details to custom components (#4121) via Álvaro Brey (@AlvaroBrey)
* feat(paywalls): send custom variables to custom components (#4091) via Álvaro Brey (@AlvaroBrey)
* feat(paywalls): send device context to custom components (#4084) via Álvaro Brey (@AlvaroBrey)
#### 🐞 Bugfixes
* fix(paywalls): traverse headers before body content (#3948) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Centralize remote-config staleness handling in shared primitives (#4154) via Toni Rico (@tonidero)
* Share one arity check across the strict operators (#4157) via Antonio Pallares (@ajpallares)
* Add rc.regexReplace custom operator (#4146) via Antonio Pallares (@ajpallares)
* Add rc.regexExtract custom operator (#4145)

### Motivation

Android port of https://github.com/RevenueCat/purchases-ios/pull/7557.
Stacked on #4144.

### Description

- Shape is `{"rc.regexExtract": [input, pattern]}` or `[input, pattern,
group]`, returning the text of the first match or of one of its capture
groups. Group `0`, the default, is the whole match.
- No match returns `null`, and so does a group that exists but took no
part in the match — group 1 of `(a)|(b)` against `"b"`, which all three
engines agree on. An *empty* capture stays a distinct answer from a
non-match.
- Asking for a group the pattern does not have throws instead, since
that can only be a lowering bug.
- `rc_regex_extract.json` is bytewise identical to the iOS copy.

<!-- CURSOR_SUMMARY -->
---

> [!NOTE]
> **Low Risk**
> Additive rules-engine operator with conformance fixtures only; no
changes to purchase, auth, or networking paths.
> 
> **Overview**
> Adds **`rc.regexExtract`** to the JSON Logic rules engine so
predicates can pull the first regex match (or a capture group) from a
string—e.g. locale language from `es-419`.
> 
> Shape is `[input, pattern]` or `[input, pattern, group]` (group `0` =
whole match). **No match** or a **non-participating capture** returns
`null`; invalid arity, types, or an out-of-range group index throw
`TypeMismatch`, matching the existing `rc.regexMatch` style.
> 
> Wires the operator through `CustomOperators` and `RegexOperators`,
adds shared predicate fixtures (`rc_regex_extract.json`, iOS-identical),
and bumps the fixture count guard to **658**.
> 
> <sup>Reviewed by [Cursor Bugbot](https://cursor.com/bugbot) for commit
31d5074e80870b0983b09fca35bb8950c5f57a32. Bugbot is set up for automated
code reviews on this repo. Configure
[here](https://www.cursor.com/dashboard/bugbot).</sup>
<!-- /CURSOR_SUMMARY -->

Co-authored-by: Cursor <cursoragent@cursor.com> via Antonio Pallares
* Add rc.regexMatch custom operator (#4144) via Antonio Pallares (@ajpallares)
* Remove the rc.length and rc.indexOf custom operators (#4153) via Antonio Pallares (@ajpallares)
* Add rc.indexOf custom operator (#4142) via Antonio Pallares (@ajpallares)
* Allow editing custom variables in paywall-tester (#4143) via Álvaro Brey (@AlvaroBrey)
* Add rc.let custom operator (#4138) via Antonio Pallares (@ajpallares)
* Index substr by UTF-16 code unit (#4139) via Antonio Pallares (@ajpallares)
* Match the separator by code unit in rc.split (#4141) via Antonio Pallares (@ajpallares)
* Add rc.slice custom operator (#4089) via Antonio Pallares (@ajpallares)
* Unify projects used by Maestro tests (#3912) via Cesar de la Vega (@vegaro)
* Add rc.sortBy custom operator (#4088) via Antonio Pallares (@ajpallares)
* Pin UTF-16 string comparison in the predicate fixtures (#4106) via Antonio Pallares (@ajpallares)
* Update baseline profiles (#4137) via RevenueCat Git Bot (@RCGitBot)
* Clear paywall web view storage when the user logs out or switches (#4075) via Álvaro Brey (@AlvaroBrey)
* build(deps): bump fastlane-plugin-revenuecat_internal from `7dd9ab9` to `6db1da0` (#4132) via dependabot[bot] (@dependabot[bot])
* Exclude nested agent worktrees from detekt (#3865) via Cesar de la Vega (@vegaro)
* Remove the remote config session kill switch (#4112) via Toni Rico (@tonidero)
* Skip test suite on main branch pushes (#4119) via Toni Rico (@tonidero)
