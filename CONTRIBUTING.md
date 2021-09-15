## Contributing

## Environment Setup

We use sdkman to specify the environment that should be used when developing. Install sdkman using  their 
[official instructions](https://sdkman.io/install). Navigate to the root of this project and do `sdk env install`,
which should install the specific Java version we expect you to use.

#### 1. Create an issue to make sure its something that should be done.

Before submitting a Github issue, please make sure to

- Search for [existing Github issues](https://github.com/RevenueCat/purchases-android/issues)
- Review our [Help Center](https://support.revenuecat.com/hc/en-us)
- Read our [docs.revenuecat.com](https://docs.revenuecat.com/)

## Common project specific issues

There are certain project specific issues that are commonly misinterpreted as bugs.

- [Offerings, products, or available packages are empty](https://support.revenuecat.com/hc/en-us/articles/360041793174)
- [Invalid Play Store credentials errors](https://support.revenuecat.com/hc/en-us/articles/360046398913)
- [Unable to connect to the App Store (STORE_PROBLEM) errors](https://support.revenuecat.com/hc/en-us/articles/360046399333)

For support I'd recommend our [online community](https://community.revenuecat.com), 
[StackOverflow](https://stackoverflow.com/tags/revenuecat/) and/or 
[Help Center](https://support.revenuecat.com/hc/en-us) 👍

If you have a clearly defined bug (with a 
[Minimal, Complete, and Reproducible example](https://stackoverflow.com/help/minimal-reproducible-example)) that is not 
specific to your project, follow the steps in the GitHub Issue template to file it with RevenueCat without removing any 
of the steps. For SDK-related bugs, make sure they can be reproduced on a physical device, 
not a simulator (there are simulator-specific problems that prevent purchases from working).

#### 2. Create a fork/branch.

#### 3. Do your work.

Be sure to adhere to the prevailing style of the project.

#### 4. Write tests for your fix/new functionality.

You can use the `purchase-tester` project in the `examples` folder as a tester app for manual tests. Specify your 
RevenueCat API key in the `MainApplication` class. You can also change the package name in the `build.gradle` file
to your own so you can test with real products.  

#### 5. Create a pull request to revenuecat/main and request review

Explain in your pull request the work that was done, the reasoning, and that tests passed.

#### 6. Make changes in response to review

#### 7. Bask in the glory of community maintained software
