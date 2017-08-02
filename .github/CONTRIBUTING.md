# Introduction

### Hey you !

Thank you for wanting to help. Even the smallest things can help this project become better.

Please read the guidelines before contributing, and follow them (or try to) when contributing.

### What you can do to help.

There are many ways to contribute to this project, you could report bugs, request missing features, suggest enhancements and changes to existing ones. You also can improve the README with useful tips that could help the other users.

You can fork the repository, and [help me solve some issues](https://github.com/aminecmi/ReaderforSelfoss/labels/help%20wanted) or [develop new things](https://github.com/aminecmi/ReaderforSelfoss/issues)

### What I can't help you with.

Please, don't use the issue tracker for anything related to [Selfoss itself](https://github.com/SSilence/selfoss). The app calls the api provided by Selfoss, and can't help with solving issues with your Selfoss instance.

Always check if the web version of your instance is working.

# Some rules
### Bug reports/Feature request

* Always search before reporting an issue or asking for a feature to avoid duplicates.
* Include every usefull details (app version, phone model, Android version and screenshots when possible)
* Avoid bumping non-fatal issues, or feature requests. I'll try to fix them as soon as possible, and try to prioritize the requests. (You may wan to use the [reactions](https://github.com/blog/2119-add-reactions-to-pull-requests-issues-and-comments) for that)

### Pull requests

* Please ask before starting to work on an issue. I may be working on it, or someone else could be doing so.
* Each pull request should implement **ONE** feature or bugfix. Keep in mind that you can submit as many PR as you want.
* Your code must be simple and clear enough to avoid using comments to explain what it does.
* Follow the used coding style [the official one](https://kotlinlang.org/docs/reference/coding-conventions.html) ([some idoms for reference](http://kotlinlang.org/docs/reference/idioms.html)) with more to come.
* Try as much as possible to write a test for your feature, and if you do so, run it, and make it work.
* Always check your changes and discard the ones that are irrelevant to your feature or bugfix.
* Have meaningful commit messages.
* Always reference the issue you are working on in your PR description.
* Be willing to accept criticism on your PRs (as I am on mine).
* Remember that PR review can take time.


# Build the project

You can directly import this project into IntellIJ/Android Studio.

You'll have to:

- Configure Fabric, or [remove it](https://docs.fabric.io/android/fabric/settings/removing.html#).
- Create a firebase project and add the `google-services.json` to the `app/` folder.
- Define the following in `res/values/strings.xml` or create `res/values/secrets.xml`

    - mercury: A [Mercury](https://mercury.postlight.com/web-parser/) web parser api key for the internal browser
    - feedback_email: An email to receive users  feedback.
    - source_url: an url to the source code, used in the settings
    - tracker_url: an url to the tracker, used in the settings
- To run your app, you'll have to add `-P appLoginUrl="your.selfoss-instance.url" -P appLoginUsername="Username" -P appLoginPassword="password"`. (These are only used to run the espresso tests. You should be able to use empty strings or fake values to build the app)
