# Hubitat-QolSysIQPanel

This is a set of Hubitat Elevation drivers for the QolSys IQ Alarm Panel.  

See https://qolsys.com/products/ for more information on this panel.

Using these drivers, you can monitor the state of all the devices connected
to your panel, such as door/window contacts, motion detectors, smoke and CO
detectors, water sensors, etc., as well as the state of the system itself 
(disarmed, armed, alarm, etc.).  You can also arm and disarm the system, and initiate alarm events.

Note that this is not alarm.com integration.  These drivers use a local interface provided by the panel itself, and work the same regardless of whether you have an
alarm.com account associated with the panel or not.

These drivers have only been tested with the IQ2+ panel, although they probably work
with the IQ2 and the new IQ4 panels.  

Please note that although the IQ panels have z-wave controllers, the interface these
drivers use does not provide access to any z-wave devices.  If you want HE to control
z-wave devices controlled by the alarm panel, you could add HE as a secondary z-wave
controller.  As of 2.3.129, this seems to work although it is not really supported
by HE.  In any case, controlling z-wave devices paired to your alarm panel is outside
the scope of these drivers.

Although these drivers use a supported interface, the interface itself is not publicly
documented or supported, and these drivers rely on reverse engineering to do what they do.
Use at your own risk.  


**WARNING**
=======

These drivers provide the ability to arm and disarm your alarm system, and to initiate
alarm conditions from HE.  If you enable either these commands, especially disarm,
take appropriate measures to secure your Hubutat hub and any other connected systems
(e.g., NodeRed, WebCore etc.) where you can create rules that manipulate your alarm
system.


Prerequisites
=============

1. You must have the **dealer code** for your panel in order to configure the "3rd
Party Connection" options.  The default dealer code is 2222.  If your panel was
installed by an alarm installer, they may have changed the dealer code and you
will need to obtain it from them.  Please note that the "dealer" code is **NOT** the
same as the "installer" code, nor is it the same as any user codes.

2. Your IQ2+ alarm panel must be running system firmware 2.4.0 or later.  Instructions
on how to check your firmware version and install updates may be found here:
https://www.alarmgrid.com/downloads/qolsys-iqpanel2plus-firmware-updates
I have no affiliation with AlarmGrid, but they are a good source of information, are
very DIY friendly, and generally have very competitive pricing, so give them a look
if you need any alarm system devices.

3. You must have Wifi enabled on your alarm panel, and the IP address of your alarm
panel must be reachable from your HE hub.  If Wifi isn't enabled, on your alarm panel,
touch the small gray bar at the top of the screen, then choose Settings ->
Advanced Settings -> Wifi and configure the Wifi settings.  

    If your alarm panel is configured to use DHCP (the default), it is strongly suggested that you go into your router or DHCP server and reserve the IP address assigned to your alarm panel, so that it cannot change in the future.  Alternatively, you can assign a
static IP address to the alarm panel.  Go into the Wifi setup screen, add a new Wifi connection and click the Advanced Settings checkbox in the Wifi parameters dialog, then you can configure a static IP address.

4. If your alarm system is monitored, you may want to call the monitoring station and put your system in test mode for some period of time.  That way you can play with the various
commands without having the police or fire department show up at your door.

Installation Instructions
=========================

1. These drivers use the alarm panel's Control4 (C4) interface.  As of firmware 2.6,
you must enable 6 digit user codes before you can enable the C4 interface (by default,
the IQ panel uses 4 digit user codes).  However, once the C4 interface is enabled, 
you can go back to 4 digit user codes.  Your existing codes will remain intact.

    If you are already using 6 digit user codes, skip to step 2.  Otherwise, on your alarm panel,
touch the small gray bar at the top of the screen, then choose Settings -> Advanced Settings -> 
Enter Dealer Code -> Installation -> Dealer Settings.  Scroll down to 6 Digit User Codes and check the box.
Your existing codes will have two zeros added to the end.  Touch Ok.  Your panel may reboot.

2. Touch the small gray bar at the top of the screen, then choose Settings -> Advanced Settings
 -> Enter Dealer Code -> Installation -> Devices -> WiFi Devices -> 3rd Party Connections.
 Check the box for **Control4**. Confirm Ok, and wait for the panel to reboot.

3. If you were using 6 digit user codes, skip to step 4.  Otherwise, touch the small gray bar
at the top of the screen, then choose Settings -> Advanced Settings -> Enter Dealer Code -> Installation -> Dealer Settings.
Scroll down to 6 Digit User Codes and uncheck the box.  The two zeros added to the end of your original 
codes will be removed.  This may require your alarm panel to reboot.

4. Touch the small gray bar at the top of the screen, then choose Settings -> Advanced Settings
 -> Enter Dealer Code -> Installation -> Devices -> WiFi Devices -> 3rd Party Connections.
Touch "Reveal Secure Token".  Write down the 6 character token, then touch the icon of a house
at the bottom of the screen.

5. At the alarm panel home screen, swipe left or right until you see the Wifi / Software Update / Bluetooth screen.  
Touch the "Details" button under Wifi and make a note of the panel's IP address.  Note that if the
Wi-Fi icon is not green, your panel is not connected.  You must resolve this before proceeding any further.

6. Install the HE drivers.  The easiest way to do this and keep up to date is to use
the Hubitat Package Manager app.  In HPM click Install, then Search by Keywords and search for "qolsys".

7. On the HE Devices page, click on the Add Device button in the upper right corner, then
click the "Virtual" button.  In the Device Name field enter something like "Alarm System".
In the Type field select "QolSys IQ Alarm Panel".  Click "Save Device".

8. Under preferences, enter the IP address and the access token you obtained in steps 4 and 5.  

9. There are three settings that will cause HE's mode to be changed when the alarm
panel's arming status changes.  For example, when the alarm system is disarmed you may choose
to have HE's mode set to "Home", armed-stay to "Night" and armed-away to "Away".  If you have 
created or modified HE's default modes, they will appear in the list.  You may leave any or all
of these settings to "No selection", in which case no change to HE's current mode will occur
when the alarm system's arming mode chages to that particular state.

10. If you want to allow the alarm system to be armed or disarmed from HE, enable the
"Allow HE to send arming and disarming commands" setting.  If this setting is turned off,
the ArmStay, ArmAway and Disarm commands will have no effect.

11. If you want to allow an alarm condition to be initiated from HE, enable the "Allow HE
to trigger an alarm condition" setting.  If this setting is turned off, the Alarm command will
have no effect.

12. Click Save Preferences, then go back to the HE Devices page.  If everything is configured
correctly, you should see an entry for your alarm system, plus a child device for every sensor
connected to the system.  The child devices will be given the same name as the corresponding
physical devices in the alarm system, but you can go into the device settings and change the
name if you want.

QolSys IQ Alarm Panel Device
============================

This virtual device represents the physical alarm panel.

The QolSys IQ panel has a feature called "partitions".  When enabled, up to four partitions may
be configured.  Every alarm sensor belongs to a partition, and each parition is armed and diarmed
individually.  A house with a deteched garage for example could use partition 0 for the house and
partition 1 for the garage.  Most residential systems will not have partitions enabled so all devices
are in partition 0.

Attributes
----------

If partitions are not enabled, the only the attributes ending in "_0" will be present.

- **Alarm_Mode_Partition_0**  (and if partitions enabled, _1, _2, _3)

    Arming, disarming or tripping the alarm system will cause one of these attributes to change to one of the following enum values:

    - DISARM
    - ARM_STAY
    - ARM_AWAY
    - EXIT_DELAY
    - ENTRY_DELAY
    - ALARM_POLICE
    - ALARM_FIRE
    - ALARM_AUXILIARY

- **Entry_Delay_Partition_0**  (and if partitions enabled, _1, _2, _3)

    When the Alarm_Mode_Partition_x attribute changes to ENTRY_DELAY, this attribute will be updated with the number of seconds
    the panel will wait for a valid user code to disarm the system.  If a valid user code was entered, Alarm_Mode_Partition_x
    changes to DISARM and this attribute changes to 0.

    Note that if you have a rule in alarm.com that disarms your panel when a valid code is entered on a connected z-wave door
    lock, Alarm_Mode_Partition_x will immediately be set to DISARM and this attribute will remain 0.

- **Exit_Delay_Partition_0** (and if partitions enabled, _1, _2, _3)

    When the Alarm_Mode_Partition_x attribute changes to EXIT_DELAY, this attribute will be updated with the number of seconds
    the panel will wait before arming the system, allowing you to open any doors configured as entry/exit doors and trip any
    interior motion sensors while exiting.  

    If the exit delay period is extended at the alarm panel, or by closing and then reopening an exit door before the delay expires,
    this attribute will be updated to reflect the new exit delay period.

    When the exit delay period has expired, Alarm_Mode_Partition_x changes to ARM_AWAY and this attribute changes to 0.

- **Error_Partition_0**  (and if partitions enabled, _1, _2, _3)

   If an error occurs and is reported by the panel, this attribute will contain the error message.  This will occur for example
   if an invalid user code was used to arm or disarm the system.  This attribute is cleared when the next command is sent to the
   panel.

Commands
--------

- **alarm**

    This command will initiate an alarm condition.  Select the partition and type of alarm you want to initiate.
To prevent the police or fire department from showing up at your door, this command will have no effect
unless the "Allow HE to trigger an alarm condition" setting is turned on.  If you have no need to initiate an
alarm from HE, leave that setting off.

- **armAway**

    This command will arm the system in arm-away mode.  If the bypass parameter is true, any open or faulted zones
will automatically be bypassed.  If your alarm panel is configured for secure arming, you will need to supply
a valid user code in order to arm the system.  If your panel is not configured for secure arming, you can omit
this parameter or pass zero. 

    To prevent the system being armed accidentally, this command will have no effect unless the "Allow HE to send
arming and disarming commands" setting is turned on.  If you have no need to arm or disarm your system from HE,
leave that setting off.
 
- **armStay**

    This command will arm the system in arm-stay mode.  If the bypass parameter is true, any open or faulted zones
will automatically be bypassed.  If your alarm panel is configured for secure arming, you will need to supply
a valid user code in order to arm the system.  If your panel is not configured for secure arming, you can omit
this parameter or pass zero.   As with armAway, the "Allow HE to send arming and disarming commands" needs to be on
for this command to have any effect.
 
- **disarm**

    This command disarms the alarm system.  A valid user code must be provided.  As with armStay and armAway, 
the "Allow HE to send arming and disarming commands" needs to be on for this command to have any effect.

- **initialize**

    This command will close the connection to your alarm panel and attempt to reconnect.  There should really be no
reason to call this command.

- **refresh**

    This command will request a list of devices from the alarm panel, and add, delete or update the corresponding
virtual drivers in HE.  If you add, remove or edit any devices on your alarm panel, you should use this command
to update HE.


Child Devices
=============

Each physical sensor connected to the alarm panel is represented by a virtual device of the approprate type.
These virtual devices are created as child devices of the alarm panel virtual device.

The child devices do not have any commands, they simply mirror the state of their respective physical devices.
Generally there will be a single attribute such as "contact" or "motion" that mirrors the state of the physical device.


Known issues
============

   1. Although there is a "tamper" value that is transmitted by the alarm panel, this value does not seem to change even when
   the physical device is in a tampered state.  Therefore the "TamperAlert" capability has not been implemented in any
   of these drivers.  Perhaps a future alarm panel firmware update will enable tamper reporting.

   2. Likewise, the alarm panel does not appear to send any low battery or other fault conditions for any alarm sensors,
   nor does it appear to send any indication that the panel's AC power has been lost.

   3. I do not have every possible type of device that could be connected to the IQ panel, so there may
   be devices that are not properly detected.  If a device on your alarm system does not have a corresponding virtual
   device in HE, please turn on all the logging options for the parent driver, click Refresh, email or post the relevant
   log entries in the HE community forum, and I will add support for the device.



Release History
===============

v 1.0.0    12/11/21

   Initial public version

v 1.0.1    12/16/21

    Better handling and retry logic if panel is offline or unreachable by HE
