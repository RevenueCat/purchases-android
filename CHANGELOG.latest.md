## RevenueCat SDK
### 🐞 Bugfixes
* fix: avoid OutOfMemoryError when diagnostics entries are tracked faster than they are written via Álvaro Brey (@AlvaroBrey)
* fix: stop retrying billing requests without limit while the service stays disconnected via Álvaro Brey (@AlvaroBrey)

### 🔄 Other Changes
* test: stop the billing tests failing at random on Duration matchers via Álvaro Brey (@AlvaroBrey)
* build: drop the stale Kotlin version gate on BOM publishing via Álvaro Brey (@AlvaroBrey)
* CI: parallel unit test forks, split Paparazzi and Maestro jobs, fix machine-executor caches via Toni Rico (@tonidero)
* Add diagnostics for stuck billing connections via Rick (@rickvdl)
* Fetch the Play storefront as soon as the billing connection is ready via Toni Rico (@tonidero)
* ci: give the scheduled backend tests a CircleCI read token via Álvaro Brey (@AlvaroBrey)
* ci: name the platform in the backend test Slack report via Álvaro Brey (@AlvaroBrey)
* ci: keep tester version codes above the public project's build numbers via Álvaro Brey (@AlvaroBrey)
