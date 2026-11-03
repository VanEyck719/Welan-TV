# What each app on the list does

These are the 31 apps in [`scripts/apps-to-remove.txt`](../scripts/apps-to-remove.txt). The descriptions are a guide: some were worked out from the package name or from what showed on the TV.

To keep an app, delete its line from the list.

## Advertising and statistics (8)

| Package | What it is |
|---|---|
| `com.tcl.bi` | Usage statistics |
| `com.tcl.overseasappshow` | App promotions |
| `com.tcl.waterfall.overseas` | TCL content and recommendations |
| `com.tcl.channelplus` | TCL Channel, free channels with ads |
| `com.tcl.messagebox` | Notifications and promotions |
| `com.tcl.esticker` | In-store demo banners |
| `com.tcl.logkit` | Background diagnostic logs |
| `android.autoinstalls.config.google.gtvpai` | Installs promoted apps automatically |

## TCL apps (13)

| Package | What it is | Keep it if... |
|---|---|---|
| `com.tcl.ocean.instructions` | User manual | you use the on-screen manual |
| `com.tcl.usercenter` | TCL account | you use a TCL account |
| `com.tcl.browser` | Web browser | you have no other browser |
| `com.tcl.hearaid` | Hearing aids and accessibility | **you use hearing accessibility features** |
| `com.tcl.gamebar` | Game bar | you use that bar |
| `com.tcl.magiconnectfree` | MagiConnect, for connecting a phone | you control the TV with that app |
| `com.tcl.smartalexa` | Appears to be the Alexa integration | **you want to control the TV with Alexa** |
| `com.tcl.guard` | TCL tool, purpose not confirmed | |
| `com.tcl.gallery` | Photo gallery | you view photos with it |
| `com.tcl.ui_mediaCenter` | Media center | you play files with it |
| `com.tcl.t_solo` | Uses the TV as a Bluetooth speaker | you use that feature |
| `com.tcl.dashboard` | TCL Home portal | you use that portal |
| `com.tcl.partnercustomizer` | TCL configuration that leaves a TCL Channel icon | |

## Google, Android and other apps (10)

| Package | What it is | Keep it if... |
|---|---|---|
| `com.google.android.play.games` | Google Play Games | you play with that account |
| `com.google.android.feedback` | Sends feedback to Google | |
| `com.google.android.marvin.talkback` | Screen reader | **someone at home needs it** |
| `com.google.android.youtube.tvmusic` | YouTube Music | you listen to music there |
| `com.android.camera2` | Camera | |
| `com.android.messaging` | SMS | |
| `com.android.printspooler` | Printing | |
| `org.chromium.webview_shell` | WebView test tool | |
| `au.com.stan.and` | Stan, an Australian streaming service | you subscribe to it |
| `tv.wuaki.apptv` | Rakuten TV | you subscribe to it |

## What stays

Google's launcher, the Assistant, the settings, the HDMI inputs, the remote, the keyboard, screen casting and the Google TV app are left alone.

## Tips

- **Remove a few at a time.** If something stops working, it's easier to tell what caused it.
- **You can bring back a single app:**

```powershell
adb shell cmd package install-existing PACKAGE_NAME
```

- **Other TVs:** to see which apps a TV has, run `adb shell pm list packages`, and find out what each one does before removing it.
