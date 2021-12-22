Adds initial Amazon store support. This won't work right away as it requires special RevenueCat setup that's not available for all customers.

Add a new dependency to the build.gradle apart from the regular `purchases` dependency. This new dependency has the classes needed to use Amazon IAP:

```
dependencies {
    implementation 'com.revenuecat.purchases:purchases:5.0.0-amazon.alpha.6'
    implementation 'com.revenuecat.purchases:purchases-store-amazon:5.0.0-amazon.alpha.6'
```

Configure the SDK using your **RevenueCat API key specific for Amazon**:

```kotlin
Purchases.configure(AmazonConfiguration.Builder(this, "api_key").build())
```

Modify your `app/build.gradle` to add a dependency to the official `in-app-purchasing-2.0.76.jar`

```
dependencies {
    implementation files('libs/in-app-purchasing-2.0.76.jar')
```

The next step would be to add the `jar` to your project. For that you can use the following gradle task that can be added to `app/build.gradle` and run via `./gradlew getAmazonLibrary` or via Android Studio :

```
// Gradle task to download Amazon library
ext {
    iapVersion = "2.0.76"
}

task getAmazonLibrary {
    ext {
        downloadURL = "https://amzndevresources.com/iap/sdk/AmazonInAppPurchasing_Android.zip"
        fileToExtract = "in-app-purchasing-${iapVersion}.jar"
        destFile = new File( projectDir, "libs/$fileToExtract" )
    }

    inputs.property( 'downloadURL', downloadURL )
    inputs.property( 'fileToExtract', fileToExtract )
    outputs.file( destFile )

    doLast {
        File destDir = destFile.parentFile
        destDir.mkdirs()

        File downloadFile = new File( temporaryDir, 'download.zip' )
        new URL( downloadURL ).withInputStream { is ->
            downloadFile.withOutputStream { it << is }
        }

        project.copy {
            from {
                zipTree(downloadFile).matching { include "**/$fileToExtract" }.singleFile
            }

            into( destDir )
        }
    }
}
```

That gradle task will add the jar to the `libs` folder inside `app`:

<img width="265" alt="Screen Shot 2021-10-05 at 2 13 41 PM" src="https://user-images.githubusercontent.com/664544/136103548-bb58f01d-12ad-4053-8ae1-6523ffd8e84e.png">

Alternatively, you can do this manually by downloading the .zip from [Amazon](https://amzndevresources.com/iap/sdk/AmazonInAppPurchasing_Android.zip) and then unzipping and moving the `in-app-purchasing-2.0.76.jar` into your projects `android/app/libs/` folder like in the screenshot above.

Due to some limitations, RevenueCat will only validate purchases made in production or in Live App Testing and won't validate purchases made with the Amazon App Tester.
