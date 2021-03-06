# Introduction

### Hey you !

Thank you for wanting to help. Even the smallest things can help this project become better.

Please read the guidelines before contributing, and follow them (or try to) when contributing.

### What you can do to help.

There are many ways to contribute to this project, you could [translate the app](https://crowdin.com/project/readerforselfoss), report bugs, request missing features, suggest enhancements and changes to existing ones. You also can improve the README with useful tips that could help the other users.

You can fork the repository, and [help me solve some issues](https://github.com/aminecmi/ReaderforSelfoss/issues?q=is%3Aissue+is%3Aopen+label%3A%22Up+For+Grabs%22) or [develop new things](https://github.com/aminecmi/ReaderforSelfoss/issues)

### What I can't help you with.

Please, don't use the issue tracker for anything related to [Selfoss itself](https://github.com/SSilence/selfoss). The app calls the api provided by Selfoss, and can't help with solving issues with your Selfoss instance.

Always check if the web version of your instance is working.

# Some rules
### Bug reports/Feature request

* Always search before reporting an issue or asking for a feature to avoid duplicates.
* Include your unique user id. It's displayed on the debug settings page. (You can tap it, it'll be copied to your clipboard)
* Include every other useful details (app version, phone model, Android version and screenshots when possible).
* Avoid bumping non-fatal issues, or feature requests. I'll try to fix them as soon as possible, and try to prioritize the requests. (You may wan to use the [reactions](https://github.com/blog/2119-add-reactions-to-pull-requests-issues-and-comments) for that)

### Pull requests

* Don't create a PR for translations. See [here](https://github.com/aminecmi/ReaderforSelfoss/pull/170#issuecomment-355715654) for an explanation why.
* Please ask before starting to work on an issue. I may be working on it, or someone else could be doing so.
* Each pull request should implement **ONE** feature or bugfix. Keep in mind that you can submit as many PR as you want.
* Your code must be simple and clear enough to avoid using comments to explain what it does.
* Follow the used coding style [the android koding style](https://android.github.io/kotlin-guides/style.html) ([some idoms for reference](http://kotlinlang.org/docs/reference/idioms.html)) with more to come.
* Try as much as possible to write a test for your feature, and if you do so, run it, and make it work.
* Always check your changes and discard the ones that are irrelevant to your feature or bugfix.
* Have meaningful commit messages.
* Always reference the issue you are working on in your PR description.
* Be willing to accept criticism on your PRs (as I am on mine).
* Remember that PR review can take time.


# Install Selfoss (if you don't have an instance)

I won't provide any selfoss instance url. If you want to help, but to not have one, you'll have to install one, and use it.

All the details to need are [here](https://selfoss.aditu.de/).

# Build the project

You can directly import this project into IntellIJ/Android Studio.

You'll have to:

- Define some parameters either in `~/.gradle/gradle.properties` or as gradle parameters (see the examples)

    - appLoginUrl, appLoginUsername and appLoginPassword: url, username and password of a selfoss instance. **These are only used for tests. They can be empty if you don't test API calls.**

### Examples:
#### Inside ~/.gradle/gradle.properties

```
appLoginUrl="URL" # It can be empty.
appLoginUsername="LOGIN" # It can be empty.
appLoginPassword="PASS" # It can be empty.
```

#### As gradle parameters

```
./gradlew .... -P appLoginUrl="URL" -P appLoginUsername="LOGIN" -P appLoginPassword="PASS"
```
