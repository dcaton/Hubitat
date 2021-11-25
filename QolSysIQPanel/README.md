# Hubitat-QolSysIQPanel

This is a Hubitat Elevation driver for the QolSys IQ Alarm Panel.  

It has only been tested in the IQ2+, but probably also works on the
IQ2 as well as the new IQ4.  

See https://qolsys.com/products/ for more information on this panel.

WARNING !!!
===========

These drivers provide the ability to arm and disarm your alarm system.
Although you will need a valid user code and accessToken for the alarm
system, there is nothing stopping you from hard-coding user codes into
a rule that invokes the driver's disarm command.

If you do so, take appropriate measures to secure your Hubutat hub, and any
other systems (Node-Red, webcore, etc.) where you create rules that contain
any alarm system user codes.  Likewise for any dashboards or other interfaces
that can invoke this driver's Disarm command.

Although these drivers use a supported alarm system interface, the interface
itself is not publicly documented, and these drivers heavily rely on reverse
engineering to do what they do.  Use at your own risk.

Prerequisites
=============

1. You must have the dealer code for your panel in order to enable the 3rd
Party Connection option.  The default dealer code is 2222.  If your panel was
installed by an alarm installer, they may have changed the dealer code and you
will need to obtain it from them.  Please note that the "dealer" code is NOT the
same as the "installer" code, nor it is the same as any user codes.

2. Your QolSysIQ2+ panel must be running system firmware 2.4.0 or later.  Instructions
on how to check your firmware version and install updates may be found here:
https://www.alarmgrid.com/downloads/qolsys-iqpanel2plus-firmware-updates
I have no affiliation with AlarmGrid, but they are a good source of information, are
very DIY friendly, and generally have very competitive pricing, so give them a look
if you need any alarm system devices.

3. You must have Wifi enabled on your alarm panel, and the IP address of your alarm
panel must be reachable from your HE hub.

Installation Instructions
=========================

1. On your alarm panel, touch the small gray bar at the top of the screen, then
choose Settings -> Advanced Settings -> Enter Dealer Code -> Installation
-> Devices -> WiFi Devices -> 3rd Party Connections.  Check the box for Control4.
Confirm Ok, and wait for the panel to reboot.

2. Obtain the IP address of your alarm panel.  Touch the small gray bar at the top
of the screen, then choose Settings -> System Status -> ...  

3. If you want to be able to arm and disarm the alarm system from HE, you will need
an access code.  If you just want to receive sensor and alarm status on HE, you may 
skip this step.  On your alarm panel, touch the small gray bar at the top of the screen,
then choose Settings -> Advanced Settings -> Enter Dealer Code -> Installation
-> Devices -> WiFi Devices -> 3rd Party Connections.  Touch "Reveal Secure Token".
Write down the 6 character token and make sure to keep it secure.  Touch the icon of a
house at the bottom of the screen when finished.

4. Install the HE drivers.  The easiest way to do this and keep up to date is to use
the Hubitat Package Manager app.

5. On the HE Devices page, click on the Add Device button in the upper right corner, then
click the Virtual button.  In the Device Name field enter something like "Alarm System".
In the Type field select "QolSys IQ Alarm Panel".  Click Save Device.

6. Under preferences, enter the IP address of the alarm panel, and optionally the access token
you obtained in step 3.  

7. There are three additional settings that will cause HE's mode to be set when the alarm
panel's arming status changes.  For example, when the alarm system is disarmed you may choose
to have HE's mode set to "Home", armed-stay to "Night" and armed-away to "Away".  If you have 
created or modified HE's default modes, they will appear in the list.  You may leave any or all
of these settings to "No selection", in which case no action will be taken when the alarm
system's arming mode chages to that particular state.

8. Click Save Preferences, then go back to the HE Devices page.  If everything is configured
correctly, you should see an entry for your alarm system, plus a child device for every sensor
connected to the system.  The child devices will be given the same name as the corresponding
physical devices have in the alarm system, but you can change the name if you want.  

Events
======

All alarm system events cause a corresponding event to occur in HE.  For example, opening a door
will cause the corresponding HE device attribute "contact" to change from "closed" to "open".

Arming or disarming the system will cause one of the "Alarm_Mode_Partition_x" attributes to 
change to one of the following enum values:

DISARM
ARM_STAY
ARM_AWAY
EXIT_DELAY
ENTRY_DELAY
ALARM_INTRUSION
ALARM_AUXILIARY
ALARM_FIRE
ALARM_POLICE

The QolSys IQ panel has a feature called "partitions".  When enabled, up to four partitions may
be configured.  Every alarm sensor belongs to a partition, and paritions may be armed and diarmed
individually (for example, your main house and a guest house, detached garage, etc.).  In essence, 
this allows one physical alarm system to act as up to four individual system.

Most residential systems will not have partitions enabled so all devices are in partition 0, and
the alarm system status is set in the "Alarm_Mode_Partition_0" attribute.

Commands
========

The parent "Alarm System" driver has commands to arm and disarm the system.  These commands will only
work if a valid alarm panel access token has been set.  If you have no need for HE to arm or disarm
your system, leave the access token setting blank.  This is the safest way to configure the driver,
as it will effecively be in "reporting only" mode and cannot change the alarm system's armed state.

If you do want to control your alarm system via HE, please insure that your hub is sufficiently 
protected.  You will also need a valid alarm system user code to disarm (and possibly arm) the system.
There is nothing stopping you from hard coding an alarm system user code into a HE rule (or in Webcore,
NodeRed, etc.), so make sure if you do this that those systems are as secure as possible.

The armAway and armStay commands have a 'bypass' parameter.  If set to Yes, any alarm zones (devices) in
an open, tampered or faulted state will automatically be bypassed.  

The armAway command has a 'exitDelay' parameter.  This is the length of time in seconds that you may open
any entry/exit doors before the alarm is armed.

All arm and disarm commands have a userCode parameter.  A valid user code must be supplied to disarm the system.
If your alarm system is not configured for "secure arming", a user code is not required to arm the system.  In
the Device Details table for the driver, in the Data section the "Secure_Arm_Partition_n" value indicates
whether secure arming is enabled for that partition.

If you add, remove or change any devices on your alarm system, simply click Refresh in the parent driver to update
the child drivers.

The child devices do not have any commands, they simply mirror the state of their respective physical devices
connected to the alarm system.  Generally there will be a single attribute such as "contact" or "motion" that
mirrors the state of the physical device.


Known issues
============

   1. Although there is a "tamper" value that is transmitted by the alarm panel, this value does not change even when
   the physical device is in a tampered state.  Therefore the "TamperAlert" capability has not been implemented in any
   of these drivers.  Perhaps a future alarm panel firmware update will enable tamper reporting.

   2. I do not have every possible type of device that could possibly be connected to this alarm panel, so there may
   be devices that are not properly detected.  If you have such a device, please turn on all the logging options for
   the parent driver, click Refresh, and send me the relevant log entries and I will add support for the device.




Release History
===============

v 1.0.0    09/19/21

   Initial public version