**1.5.4.08**

- Thanks @jrafaelsantana for translating the whole app in Brazilian Portuguese.

**1.5.4.07**

- Loading more items on swipe too.

- Fixed popup menu style. User may need to reselect the theme.

- Disabled reporting marking items as read if there isn't an issue.

**1.5.4.05/06**

- Translation fix.

**1.5.4.04**

- Fixing an issue with marking items as read (something related to an old version of selfoss).

**1.5.4.03**

- Trying to fix some issue with pre-launch reports. Reverted because it seems to be related to the dev console side.

**1.5.4.02**

- Fixing full height cards issue.

**1.5.4.01**

- Removed the "apk downloaded from outside of playstore" message.

- Versions update.

- HTML viewer version update. It should fix an issue with images.

- Some code cleaning.

**1.5.4.00**

- Added issue reporting from within the app.

**1.5.3.06**

- Fixed infinite scroll not working.

- Fixed logs not working.

- Temporary workaround handling opening invalid urls. Waiting to solve #83.

**1.5.3.05**

- Fixed an issue on older versions of Android.

- Libs update.

**1.5.3.04**

- Crowdin translations

**1.5.3.03**

- Libs updates.

- Translation fix.

**1.5.3.01/02**

- Added translation link to the settings page.

- Added the translation link to the README.

**1.5.3.00**

- (BETA) Added pull from bottom to load more pages of results. May be buggy.

**1.5.2.18/19**

- APK minification finally working. That means less space taken !
- Added an option to log every API call.

**1.5.2.17**

- Source code and tracker links weren't being set, and updated the contributing doc.

**1.5.2.15/16**

- Adding an account header on the lateral drawer.

- The account header is only displayed when the setting is enabled.

**1.5.2.13/14**

- Updated glide.

- Loading images from self signed certificate now working.

**1.5.2.12**

- Self signed certificates are now working for loading data. Image are not loading yet.

**1.5.2.11**

- Added a random unique identifier to be used in the logs.

**1.5.2.08/09/10**

- Added settable logs for reading articles problems.

**1.5.2.07**

- Added the ability to choose the number of items loaded (the maximum value is 200 and is imposed by the selfoss api)

**1.5.2.06**

- Fix problem introduced in 1.5.2.04. SVG file not working on older versions of android.

**1.5.2.05**

- Versions updates

**1.5.2.04**

- Reverted to the old icon.

- Better icon for the intro activity.

- Updated gradle version.

**1.5.2.03**

- Added the ability to accept self signed certificates. (Needs more testing)

**1.5.2.02**

- Added optional login option.

**1.5.2.01**

- New (Better) Icon !

**1.5.2.0**

- New Icon !

**1.5.1.9/10/11**

- Hiding the unread badge when marking all items as read.

**1.5.1.8**

- Fixes and libs updates.

**1.5.1.7**

- Bug fixes.

- Code cleaning

**1.5.1.6**

- Added back the badges after it was fixed on the library side.

**1.5.1.5**

- THEMES !!!! For now, the app has predefined themes. You can ask for new ones until I make them dynamic.

**1.5.1.3/4**

- Fixes introduces by the previous alpha (1.5.1.2)

**1.5.1.2**

- Added testing to the CI.

- Code cleaning

- Display the pull to refresh loader on api call

- Fixes :

  - Can't pull down to refresh on first launch

  - Recurring crash because of the url

  - Couldn't open some urls because of missing "http"

  - Adding a source with invalid url would crash


**1.5.1.1**

- Fixed an issue when trying to add a source without being logged in.

- Reloading drawer tags badges on slide to refresh.

**1.5.1**

- Added a drawer for filtering sources and tags.

- You can now search for items from the toolbar.

**1.5.0.2**

- If the content in the article viewer is empty, the article will open in a custom tab.

- Added a share button, and an "open in browser" button to the bottom of the article viewer.

- Updated custom tab code.

**1.5.0.1**

- The release APK wasn't working at all.

**1.5.0.0**

_New_

- The app is now open source ! And rewritten in Kotlin !

**1.4.0.9**

_Fixes_

- Fixes and missing translations.

**1.4.0.8**

_New_

- Added setting for full height and fixed height cards size.

_Fixed_

- Action Bar color now matches the primary color on the recent apps screen.

- Added a bottom margin to de article viewer content

- Multiple fixes for the new article viewer.

**1.4.0.7**

_Fixed_

- Disable swipe to hide from other "tabs" and avoid badges problems

- Fixed a bug with the new Article viewer with some displaying fixes


**1.4.0.6**

_New_

- Added the ability to use http authentication (Basic and Digest)

_Fixed_

- Fixed gitter link

- Change the article viewer because the other was causing crashes

**1.4.0.5**

_New_

- Added an intro to the app.

- Added the ability to test the app without a Selfoss instance.

**1.4.0.4**

_New_

- Added the ability to have a github build. If the apk is a Github build, check for update and ask the user to download it (directly from the github page).

_Changes_

- The apk stating that the app wasn't installed from the store is only displayed on start.

**1.4.0.3**

_Fixed_

- Fixed boolean problem.

**1.4.0.2**

_New_

- The app is available in Dutch !

_Fixed_

- Fixed a bug with the articles states.

**1.4.0.1**

_New_

- You can now help me translate the app ! There will be a dialog displayed the first time you open the app, and the link will still be available from the settings page.

_Changes_

- Changed the custom tabs color to dark orange to fix the wrong title color.

_Fixes_

- The badges now are shown even if the tab is selected.

- Fixed feeds not reloading on app resume (caused by 1.4.0.0 changes).

**1.4.0.0**

_New_

- Added a setting to enable/disable the article viewer when the internal browser is enabled.

- Added peek to the card view.

- Text drawable if no icon.


_Changes_

- Changed the external browser setting to internal browser and handled the change on first open.

- Some text changes.

- Better animations handling on slow networks.

...

**1.3.3.5**

_New_

- Added tab bar badges with settings to display them.

- Added invites.

_Fixes_

- Fixed a typo.

_Updates_

- Updated support library to 10.2.0.

- Updated firebase to 10.2.0.

- Updated article_viewer to 0.20.1.

- Updated bottom-bar to 2.1.1.


**1.3.3.4**

...
