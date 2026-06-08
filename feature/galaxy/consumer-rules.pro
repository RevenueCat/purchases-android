# GalaxyBillingWrapper is instantiated reflectively from the purchases module
# (GalaxyBillingWrapperFactory) via Class.forName + getDeclaredConstructor(...). Keep all its
# constructors so the reflective lookup resolves under minification, regardless of which
# constructor signature the factory uses (so changing it doesn't require updating this rule).
-keep class com.revenuecat.purchases.galaxy.GalaxyBillingWrapper {
    <init>(...);
}
