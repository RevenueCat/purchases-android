## RevenueCat SDK
### 🐞 Bugfixes
* fix: handle platform MalformedURLException as a network error instead of crashing (#38) via Álvaro Brey (@AlvaroBrey)
* fix: include paywall offering context in observer mode receipts (#29) via Rick (@rickvdl)
* Fetch the Play storefront as soon as the billing connection is ready (#5) via Toni Rico (@tonidero)

### 🔄 Other Changes
* Add diagnostics for stuck billing connections (#11) via Rick (@rickvdl)
* Track how long a fresh install waits for the Play storefront (#4578) via Álvaro Marco (@amarco90)

## RevenueCatUI SDK
### Paywallsv2
#### 🐞 Bugfixes
* fix(paywalls): hidden paywall components appearing after a text or style override (#41) via Cesar de la Vega (@vegaro)
* fix(paywalls): honor numbering system from `preferredUILocaleOverride` when formatting prices (#17) via Cesar de la Vega (@vegaro)
* fix(paywalls): size fit-height sticky footers to their content (#34) via Cesar de la Vega (@vegaro)
* fix(paywalls): show "---" lines, indented text and HTML blocks as typed instead of hiding them (#55) via Leti Esperón (@letiesperon)
* Fix(Paywalls ) Conditionally apply nested scrollability to prevent crashes (#4510) via Jacob Rakidzich (@JZDesign)
* fix(paywalls): Fix scrolling for max-bounded nested paywall stacks (#4531) via Cesar de la Vega (@vegaro)
