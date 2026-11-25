# Expense Tracker

An offline Android app for tracking personal income and expenses, fuel cycles, EMI instalments and monthly commitments. It can import likely transactions from bank SMS messages and keep an Excel backup that can be restored later.

> **Project status:** A personal testing build. Check imported transactions against your bank statement; SMS formats and device behaviour vary. This app does not connect to a bank or provide live account balances.

## Features

- **Dashboard:** Monthly credit, debit and net totals, a category breakdown and a configurable monthly spending limit. Configured salary is included in the overview for every month; it is not automatically recorded as a bank transaction.
- **Transaction tracker:** Add, edit, review and delete entries; filter by type, category, date or bank; import SMS and export a CSV report. Bank labels are derived from recognised sender IDs or bank names in supported messages and can be edited.
- **SMS import:** With permission, scans messages on the device for likely bank transactions. It checks transaction patterns and rejects examples such as OTPs, promotional messages and failed payments. Rules are conservative and cannot recognise every bank format or authenticate an SMS sender.
- **Fixed commitments:** Set up recurring commitments, then select the appropriate tracker transaction for each month. Linked transactions are excluded from the personal spending-limit calculation.
- **Fuel tracker:** Record refuel date, odometer reading, quantity and amount. Link an existing tracker debit or enter the amount manually; later refuels close prior open cycles and enable mileage calculations.
- **EMI tracker:** Create plans with amount, start date and tenure; view instalments and mark payments with or without linking a tracker debit. Unpaid instalment reminders appear when the app is opened, beginning two days before the due date.
- **Profile and settings:** Name, salary, expected bike mileage, regular payee categories and light/dark appearance.
- **Home-screen widget:** A glance at the monthly overview and an entry point to the app.
- **Excel backup and restore:** Save an `.xlsx` workbook to a folder selected by the user. The first successful setup creates a backup; on the first app opening on subsequent days, it attempts to update the same named workbook. Each month has a readable sheet, and an `App data` sheet holds the records and settings required by restore. Restore merges entries and retains existing data; saved SMS fingerprints help prevent duplicate SMS imports. If a match cannot be established safely, restore stops instead of guessing.

## App flow

1. Open **Profile & settings** to set your name, salary and mileage range, if needed.
2. Add transactions in **Tracker**, or choose **Import SMS** and grant SMS permissions. Review the imported entries and categories.
3. View monthly totals and set a spending limit on **Home**. Link monthly payments under **Fixed commitments** to exclude them from that limit.
4. Add fuel cycles and EMI plans in their respective tabs. When recording a payment, select an existing tracker debit if you want it linked.
5. Choose a folder for **Automatic backup** when prompted. Use **Restore backup** to merge a previously created Expense Tracker workbook into the app.

The app saves transactions and settings in private app storage on the device. The Excel workbook is separate and is not encrypted by the app; anyone with access to that file may be able to read its contents. A backup on the same device can be lost if the device is lost, reset or damaged; keep another copy if recovery matters.

## Privacy and permissions

- `READ_SMS` and `RECEIVE_SMS` support SMS import and incoming bank-message processing. These permissions provide access to SMS beyond bank messages, including potentially sensitive messages; the app filters likely transactions on-device.
- Android may restrict SMS access for APKs installed outside an app store. Manual transactions are available without the permission.
- The manifest does not request `INTERNET`. Excel backup access is granted through Android's folder picker.
- An SMS alert is not proof that a transaction settled. A fingerprint is based on sender, message text and calendar date, so truly identical messages from the same sender on the same day can be treated as one.
- Automatic backup depends on folder access, device state and opening the app; verify the workbook exists before relying on it. Restore uses the app's own workbook format, including its `App data` sheet; a general CSV or arbitrary Excel file is not a full backup.

## Tech stack

| Area | Technology |
| --- | --- |
| Platform | Android, minimum API 26 (Android 8); target / compile API 36 |
| App code and UI | Java, Android platform UI APIs and XML resources |
| Local storage | SQLite and Android SharedPreferences |
| Backups | Android Storage Access Framework and XLSX (OOXML) |
| Build | Android Gradle Plugin 8.9.1 and Gradle wrapper |
| Package | `com.suriya.expensetracker` |

## Build and run

1. Install Android Studio, Android SDK Platform 36, Android SDK Build Tools and JDK 17 or 21 (configure it as the Gradle JDK in Android Studio).
2. Open this folder in Android Studio. Let Gradle sync; install any required SDK components when prompted.
3. Run the `app` configuration on an Android device or emulator. SMS features require a device and permissions capable of accessing SMS.

From a terminal, with the Android SDK configured via Android Studio or `ANDROID_HOME`:

```powershell
.\gradlew.bat :app:assembleDebug
```

On macOS/Linux, use `./gradlew :app:assembleDebug`. The debug APK is generated in `app/build/outputs/apk/debug/`.

This repository does **not** contain the signing key used for previously shared APKs. An Android update must be signed with the same key as the installed app; an independently generated debug key can cause an update to be rejected. Keep signing keys and passwords private and backed up separately. This is not a Play Store release configuration.

## Project structure

```text
Expense-Tracker/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/suriya/expensetracker/
│       │   ├── MainActivity.java        # Dashboard, tracker and settings
│       │   ├── Store.java               # SQLite data and SMS import
│       │   ├── SmsRules.java            # SMS parsing and filtering
│       │   ├── BankNames.java           # Sender and bank identification
│       │   ├── FuelPage.java            # Fuel cycles
│       │   ├── EmiPage.java             # EMI plans and payments
│       │   ├── CommitmentsPage.java     # Monthly commitments
│       │   ├── BackupManager.java       # Folder access and backup writes
│       │   ├── BackupSnapshot.java      # Backup data and restore merge
│       │   ├── WorkbookIO.java          # XLSX reading and writing
│       │   └── ExpenseWidget.java       # Home-screen widget
│       └── res/                         # Layouts, icons and themes
├── gradle/wrapper/                      # Gradle wrapper
├── tests/                               # Standalone Java and Android checks
├── build.gradle
├── settings.gradle
└── README.md
```

## Limitations and testing

The SMS parser is heuristic: different banks and devices need further testing, and some valid transactions can be missed. Bank selection and manual entry are available for corrections. Excel restore and automatic backups should be tested with copies before deleting app data. Monthly sheets are for reading; the `App data` sheet is essential for restore and should not be removed or modified.

The `tests/` directory contains focused standalone checks for SMS parsing, bank labels, EMI dates and workbook reading/writing. Android-dependent checks require an Android runtime; they are not automatically wired into the Gradle test task.
