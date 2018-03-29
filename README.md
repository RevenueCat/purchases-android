# android-template [![Build Status](https://travis-ci.org/uber/android-template.svg?branch=master)](https://travis-ci.org/uber/android-template)

This template provides a starting point for open source Android projects at Uber.

## Scaffolding Provided

This repository provides the following components that are common to our open source projects:

- CHANGELOG.md
- CONTRIBUTING.md
- LICENSE.txt
- README.md
- RELEASING.md
- Issue & Pull Request Templates
- Git Ignore
- Checkstyle
- Links to Codestyle
- Lint
- Dependency Management
- Travis CI Support
- Maven Central Release Script
- Proguard Config

## Checklist

- [ ] Clone this repository into a folder of your project's name `git clone git@github.com:uber/android-template.git MY_PROJECT`. Or if you're copying the folder, don't forget hidden files!
- [ ] Reinitialize git
    - [ ] Delete the `.git` folder
    - [ ] Start a git repo with `git init`
    - [ ] Make initial git commit with all files
- [ ] Update the `PROJECT_NAME` inside the files `./github/ISSUE_TEMPLATE.md` and `./github/PULL_REQUEST_TEMPLATE.md` to your project’s name.
- [ ] Move your project's modules into the sample project
    - [ ] Update `settings.gradle` to point to the modules you added
    - [ ] Update `dependencies.gradle` and respective `build.gradle` files to make sure dependencies are hooked up and compiling properly
- [ ] Remove the sample modules `app` and `lib`.
- [ ] Modify `CHANGELOG.md` to reflect the version of your initial release.
- [ ] Update this `README.md` file to reflect your project.
    - [ ] Update the Travis Build Status badge to reflect your project
    - [ ] Delete everything above including these checkboxes

# Project Name [![Build Status](https://travis-ci.org/uber/your-project.svg?branch=master)](https://travis-ci.org/uber/your-project)

Replace this text with a synopsis of the library.

## Motivation

Explain why this library exists and what problems it solves.

## Download

Include instructions on how to integrate the library into your projects. For instance install in your build.gradle:

```
dependencies {
  annotationProcessor 'com.uber:annotation-processor:0.0.1'
  compile 'com.uber:library:0.0.1'
}
```

## Usage

Provide instructions on how to use and integrate the library into a project.

If there's some special peices for testing (ie Mocks) explain those here as well.

## License

    Copyright (C) 2017 Uber Technologies

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

