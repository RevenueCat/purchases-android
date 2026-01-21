This directory contains copies of the logging utilities from the purchases module. They are copied here instead
of imported to prevent the `@InternalRevenueCatAPI` annotation from leaking to the vast majority of files/
functions in the purchases module.

These copies can be removed in the future if we extract our logging code to a common module.
