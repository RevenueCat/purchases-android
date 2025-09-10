## RevenueCat SDK
> [!NOTE]  
> This release brings all non-breaking changes between SDK versions 9.4.0 and 9.6.0 to major 8, so developers that don't/can't use major 9, can still get the latest updates.

### ✨ New Features
* Add `RevenueCatBackupAgent` (#2625) via Toni Rico (@tonidero)
* Add preferred UI locale override for RevenueCat UI components (#2620) via Josh Holtz (@joshdholtz)
* Add option to disable automatic ID collection when setting attribution network IDs at configuration time (#2643) via Toni Rico (@tonidero)
### 🐞 Bugfixes
* Handle payment pending errors when restoring properly (#2635) via Toni Rico (@tonidero)

## RevenueCatUI SDK
### Paywallv2
#### ✨ New Features
* MON-1193 Support delayed close button (Component Transitions) (#2623) via Jacob Rakidzich (@JZDesign)
#### 🐞 Bugfixes
* Fix PaywallDialog going over screen size on Android 35+ (#2642) via Toni Rico (@tonidero)
### Customer Center
#### ✨ New Features
* Add button_text to ScreenOffering (#2638) via Facundo Menzella (@facumenzella)

### 🔄 Other Changes
* Fix CoroutineCreationDuringComposition lint error on AGP 8.13.0 (#2659) via Cesar de la Vega (@vegaro)
* Support setting null offering id on PaywallView (#2658) via Toni Rico (@tonidero)
* Improve thread safety of setting paywalls preferred locale (#2655) via Josh Holtz (@joshdholtz)
* Remove validation for no packages on paywalls (#2653) via Josh Holtz (@joshdholtz)
* MON-1193 flatten Transition JSON structure after chatting more thoroughly with team (#2641) via Jacob Rakidzich (@JZDesign)
