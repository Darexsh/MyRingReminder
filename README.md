* * *

<div align="center">

📱 Veri Aristo App
============================

**An Android app for tracking and managing contraceptive ring cycles**  
📅⏰📝🎨📊

![Projekt-Status](https://img.shields.io/badge/Status-Aktiv-brightgreen) ![License](https://img.shields.io/badge/License-NonCommercial-blue) ![Version](https://img.shields.io/badge/Version-1.3.0-orange)

[![Telegram Bot](https://img.shields.io/badge/Telegram-Bot-2AABEE?logo=telegram&logoColor=white)](https://t.me/darexsh_bot) [![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-yellow?logo=buy-me-a-coffee)](https://buymeacoffee.com/darexsh)  
<sub>Get release updates on Telegram.<br>If you want to support more apps, you can leave a small donation for a coffee.</sub>

</div>


* * *

✨ Authors
---------

| Name | GitHub | Role | Contact | Contributions |
| --- | --- | --- | --- | --- |
| **[Darexsh by Daniel Sichler](https://github.com/Darexsh)** | [Link](https://github.com/Darexsh?tab=repositories) | Android App Development 📱🛠️, UI/UX Design 🎨 | 📧 [E-Mail](mailto:sichler.daniel@gmail.com) | Concept, Feature Implementation, Calendar & Reminder Logic, Notes Management, UI Design |

* * *

🚀 About the Project
==============

**Veri Aristo** is an Android application designed to help users track and manage contraceptive ring cycles. The app provides a calendar-based visualization of insertion, removal, and ring-free days, along with reminders, personal notes, and customizable settings.

* * *

✨ Features
----------

* 📅 **Cycle Tracking**: Visualize insertion, removal, ring-free, and active days with color-coded calendar highlights; tap legend dots to recolor.
    
* 🔔 **Reminders**: Custom reminder lead times for insertion/removal, with automatic rescheduling.
    
* 📝 **Notes**: Notebook-style notes with autosave, character counter, and quick delete.
    
* 🎨 **Customization**: Set cycle length, insertion date/time, calendar range, language, and background image. Customize button color, home ring color, and widget colors.

* 🎞️ **App Animations**: Choose a global navigation animation style in Settings (for tab switches and screen openings), including options such as slide, fade, zoom, rotate, pop, or none.

* ⚙️ **Animated Advanced Settings**: The **Settings → Advanced** section opens and closes with a subtle transition for smoother interaction.

* 🗓️ **Enhanced Calendar Navigation**: Navigate months with left/right arrows, tap the month header to jump directly to a selected month and year, and scroll through months without a fixed limit.

* 🌐 **Language Mode**: Select German, English, or **System Default**. System Default follows the device language and falls back to English if unsupported.

* 🌙 **Dark Mode (Default)**: App runs in dark mode by default, independent of system theme.

* 🔒 **Portrait Only**: Interface stays in vertical orientation for a consistent experience.
    
* 🧭 **Welcome Tour**: First-run guided tour across all screens, with a restart option in Settings → Advanced.

* 📊 **Cycle History**: Review past and upcoming cycles to track patterns and durations.

* ⏩ **Wear Ring Longer**: Extend the current cycle once and keep history intact, with built‑in info guidance.

* 🧩 **Home Screen Widgets**: Small widget shows days left; large widget shows days left plus next dates, both color‑synced with app settings and resizable.

* 💾 **Backup / Restore**: One‑tap export and import of all settings and notes, with import validation and a user-friendly expandable review before confirming restore.

* 🛠️ **Debug View**: Long‑press settings title for detailed diagnostic information.
    

* * *

📸 Screenshots
--------------

<table>
  <tr>
    <td align="center"><b>Home Screen</b><br><img src="Screenshots/Home.png" width="200" height="450"></td>
    <td align="center"><b>Calendar</b><br><img src="Screenshots/Calendar.png" width="200" height="450"></td>
    <td align="center"><b>Notes</b><br><img src="Screenshots/Notes.png" width="200" height="450"></td>
    <td align="center"><b>Cycles</b><br><img src="Screenshots/Cycles.png" width="200" height="450"></td>
  </tr>
</table>

<table>
  <tr>
    <td align="center"><b>Settings</b><br><img src="Screenshots/Settings.png" width="200" height="450"></td>
    <td align="center"><b>Widgets</b><br><img src="Screenshots/Widgets.png" width="200" height="450"></td>
    <td align="center"><b>About</b><br><img src="Screenshots/About.png" width="200" height="450"></td>
  </tr>
</table>

* * *

📥 Installation
---------------

1. **Build from source**:
    
    * Clone or download the repository from GitHub:
        
        ```bash
        git clone https://github.com/Darexsh/Veri_Aristo_App.git
        ```
        
    * Open the project in **Android Studio**.
        
    * Sync Gradle and build the project.
        
    * Run the app on an Android device or emulator (Android 8+ recommended).
        
2. **Install via the provided APK**:
    
    * Download the APK from the repository (`veri_aristo_app.apk`).
        
    * 🔒 Enable installation from unknown sources if prompted (required on Android 8+).
        
    * 📂 Open the APK on your device and follow the installation steps.
    

* * *

📝 Usage
--------

1. **Setup Cycle**:
    
    * Go to **Settings**.
        
    * Select the insertion date, cycle length, and reminder lead times.
        
    * Optionally choose a background image.

    * Switch the app language (German/English/System Default).

    * (Optional) Restart the welcome tour in **Settings → Advanced**.
        
2. **View Calendar**:
    
    * Open the **Calendar** tab to see color-coded days:
        
        * 🟦 Cyan: Ring insertion
            
        * 🟨 Yellow: Ring removal
            
        * 🔴 Red: Ring-free days
            
        * 🟩 Green: Active cycle days

    * Tap legend dots to change the colors.

    * Use the left/right arrows to move between months.

    * Tap the month label to choose a specific month and year.
            
3. **Get Notifications**:
    
    * Receive reminders for insertion and removal at your selected times.
        
4. **Use Widgets**:
    
    * Add the small or large widget to your home screen for quick status.
    * Widgets are resizable from the launcher.
        
5. **Take Notes**:
    
    * Use the **Notes** tab to store private notes, automatically saved locally.
        
6. **Track History**:
    
    * Check the **Cycles** tab for past and upcoming cycles.

7. **Backup / Restore**:

    * Use **Settings → Advanced** to export or import all settings and notes.

    * Before restore, review validated import content in an expandable, readable preview and confirm with **Yes/No**.

8. **Wear Ring Longer Info**:

    * Tap the **info** icon next to “Wear ring longer” for guidance.

9. **Animation Style**:

    * Open **Settings** and choose your preferred global app transition animation.
        

* * *

🔑 Permissions
--------------

* 💾 **Storage / Media Access**: Required to select a custom background image.
    
* 🔔 **Notifications**: Required to receive cycle reminders.

* ⏰ **Exact Alarms**: Used to schedule precise reminder notifications.
    

* * *

⚙️ Technical Details
--------------------

* 📦 Built with **Java** and **Android MVVM** architecture.
    
* 🗓️ Calendar rendering and cycle logic are computed locally (no backend).
    
* 🛠️ Stores user settings and notes in **SharedPreferences**.
    
* 🔔 Notifications implemented via **BroadcastReceiver** and **NotificationManagerCompat**.
    
* 📊 State sharing between fragments is managed via **SharedViewModel** and **LiveData**.

* 🧩 Home‑screen widgets implemented via **AppWidgetProvider**.
    

* * *

📜 License
----------

This project is licensed under the **Non-Commercial Software License (MIT-style) v1.0** and was developed as an educational project. You are free to use, modify, and distribute the code for **non-commercial purposes only**, and must credit the author:

**Copyright (c) 2025 Darexsh by Daniel Sichler**

Please include the following notice with any use or distribution:

> Developed by Daniel Sichler aka Darexsh. Licensed under the Non-Commercial Software License (MIT-style) v1.0. See `LICENSE` for details.

The full license is available in the [LICENSE](LICENSE) file.

* * *

<div align="center"> <sub>Created with ❤️ by Daniel Sichler</sub> </div>
