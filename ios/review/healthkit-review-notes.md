# Guideline 2.5.1 — HealthKit identification

## App Review notes for the updated build

Leyla uses HealthKit for optional, read-only menstrual flow access from Apple Health. It does not use CareKit.

After signing in and completing onboarding, open **Settings → Apple Health**. The top of this screen identifies HealthKit and explains that menstrual flow data powers cycle phase, cycle day and next-period estimates. This explanation is visible without granting Health access or enabling personal cycle tracking.

Tap **Cycle & health** on that screen. The same explanation appears at the top, before the personal-cycle or partner-support content. Users who choose personal cycle tracking can connect Apple Health from the cycle screen. Leyla requests read access to menstrual flow only and does not request write access.

The explanation also describes optional partner sharing: period dates are kept on the device, while the selected cycle summary and optional note are uploaded when the user enables sharing.

## Before resubmission

- Install the updated build on a physical device and verify Settings → Apple Health → Cycle & health, including with Health access denied and with the partner-support role selected.
- Check the disclosure on iPad and with larger text enabled.
- Record the navigation above on a physical device, and attach the recording in App Review Information → Notes as requested by the reviewer.
- Submit the updated build with the review notes above. Do not describe these changes as present in version 1.1 (13) unless that exact submitted binary has been verified.
