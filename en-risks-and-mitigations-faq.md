# Exposure Notification: Risks and Mitigations FAQ

This document explains the key design decisions that affect the privacy and
security of the Google/Apple Exposure Notifications system (ENS), the risks that
were evaluated, and the mitigations that are in place for these risks. Details
of the ENS design can be found
[here](https://www.google.com/covid19/exposurenotifications/).

# Key design decisions and constraints relevant to security and privacy

## Decentralized architecture

The design of an effective exposure notification system requires trade-offs
between efficacy, privacy, and security. A common way of categorizing contact
tracing and Exposure Notification designs is between:

1.  **Centralized.** A central service makes decisions about whether to
    notify someone if they have been exposed or not. Centralized designs
    require devices to share a list of observed Bluetooth identifiers with a
    central notification service.
2.  **Decentralized.** The user's device decides locally whether to notify
    someone if they have been exposed or not. Decentralized designs require
    devices to receive a list of all COVID-positive users' broadcast Bluetooth
    identifiers.

The Google/Apple design uses a decentralized architecture because:

1.  Devices of COVID-negative users do not share any information about
    social interactions, location, or infection status. Decentralized designs
    provide better privacy protection for COVID-negative participants compared
    to centralized designs, since they do not require sharing any information
    with a central service unless a user declares themselves COVID-positive.
    This is especially important since we assume that the vast majority of
    participants will be COVID-negative.
    In contrast, centralized designs have the property that any party with
    access to the central service has access to observed Bluetooth Low Energy
    (BLE) identifiers—and therefore information about social interactions—for
    all participants, including those who have not declared themselves as
    COVID-positive.
2.  For COVID-positive users, the information shared with the server in a
    decentralized model is their COVID-positive status rather than any
    information about social interactions: decentralized designs require
    sharing broadcast Rolling Proximity Identifiers (RPIs) of COVID-positive
    users with all participating devices (usually via a central service).
    Centralized designs expose users' social interactions to a central service
    in order to receive notifications. Even if care is taken not to store
    information on relationships, the centralized designs inherently risk that
    this information may be obtained by an adversary.
    COVID-positive status data presents less incremental risk to the user and
    is less permanent than a person's social interactions: see
    [here](#covid-positive) for more details.
3.  Centralized designs also expose COVID-positive status. While centralized
    designs can minimize information revealed to the public at large, and make
    such attacks more difficult, they cannot prevent attacks that identify
    COVID-positive users entirely.
    In a centralized model an adversary can emit Bluetooth IDs in the vicinity
    of a device whose owner has been identified via other means, for example,
    by using vehicle number plate recognition. Once this device reports its
    owner as COVID-positive, the adversary's device will receive an exposure
    notification, which can be tied to the identified user. If they want to do
    this multiple times, however, they would need to have a single Healthcare
    Authority account, that is notification channel, per identified user.
    Healthcare Authority diagnosis servers could implement defenses against
    Sybil attacks using multiple accounts, but this could put legitimate users
    at higher risk of being falsely excluded from receiving exposure notifications.

## Location

Location data from the device is not used by the Google/Apple EN API in any
way.

Healthcare Authority applications using the Exposure Notification API are also
subject to the condition that applications do not make use of location APIs, and
do not record or store location information from the device.

EN respects the Android requirement for all Bluetooth scanning services to
enable the device location setting. This requirement has been present in the
Android platform since 2015, and is designed to protect the user's privacy by
preventing apps from using nearby Bluetooth beacons to locate the user when the
user has explicitly turned off all location for the device.

Enabling this setting by itself does _not_ mean that an application is using the
device's location: the location runtime permission is _also_ required for any
application to access the device's location. Android users can still decide
whether any app they've installed has permission to access the location of their
device, even if the device location setting is on.

## Bandwidth usage

The decentralized model requires a larger amount of data to be downloaded by
each device. In the EN, devices download a highly compressed version of 14 days
worth of Rolling Proximity Identifiers of every COVID-positive user. Using daily
exposure keys achieves approximately 100x compression by allowing the 15 minute
RPIs to be regenerated from daily seeds. We estimate that 14 days' worth of
daily keys for 100,000 COVID-positive people is 22.4 MB. We recommend that EN
apps download keys when the device is connected to wifi and power if possible.

## Use of connectionless protocol

The ENS uses a connectionless protocol. In our experience, protocols that
require BLE connections or other forms of two-way interactions between
participants' phones result in error-prone implementations, and can have
significant negative effects on battery life. This led to a decision to use
connectionless BLE advertisements as part of the protocol. One consequence of
this is that the size of proximity identifiers is constrained to 20 bytes.

# Risks and Mitigations

The following is an analysis of security and privacy risks to the system and its
users that were considered and mitigated to the extent possible within the above
design constraints.

## Disruption

**Concern**

Adversaries may attempt to disrupt the EN to create fraudulent exposure
notifications. Such fraudulent exposure notifications may result in
individuals or targeted groups unnecessarily self-quarantining. Disruptions
could also decrease the effectiveness of the system and increase its cost
of operation to both users and networks by, for example, increasing
bandwidth costs for users.

### RPI Spoofing using reported diagnosis keys

**Concern**

An adversary may try to use diagnosis keys downloaded from the server to
generate and broadcast proximity identifiers in an attempt to create false
ENs to users receiving them.

**Mitigations**

The EN implementations in both Android and iOS manage temporary exposure
keys on behalf of client apps. When a user tests positive, the user can
instruct their client app to upload their keys.

Both Android and iOS only return keys that are no longer in use. This means
that only expired keys are uploaded and revealed publicly. The matching
algorithm prevents a proximity identifier generated by expired keys from
leading to Exposure Notifications, mitigating this replay attack scenario.

### Relaying RPIs

**Concern**

An adversary could record proximity identifiers being broadcast by
legitimate users and rebroadcast them elsewhere. This could be done for
users likely to be diagnosed soon thereafter—for example, near a hospital
or testing center—and then rebroadcast to target a large number of users.
Alternatively, a large number of collected RPIs could be rebroadcast over a
large group, increasing the notification rate for that group in line with
the increased broadcast rate, effectively simulating being in a crowd.

A second variant, where the adversary is able to compromise the test
verification process, is to broadcast RPIs from Temporary Exposure Keys
(TEKs) that are subsequently uploaded to a diagnosis key server.

**Mitigations and considerations**

We acknowledge that a sophisticated adversary could mount relay attacks
against our system but we note that:

*   Any potential attack would require relayed broadcasts to persist for
    as long as the Healthcare Authority app threshold for an exposure, for
    example, 5-10 minute duration of close proximity to the target devices.
    This would require high powered antennas for most targets, which both
    increases the complexity of an attack and makes it more detectable.
*   Potential device-based malware that rebroadcasts BLE RPIs
    would have to be deployed at large scale to be successful. Any such malware
    detected during Google Play's rigorous review process would be removed.
*   Deploying fixed BLE listeners and broadcasters requires non-trivial
    physical setup and a source of power, which puts adversaries and their
    equipment at risk of discovery.
*   A hypothetical attack of this type could readily be detected by
    monitoring the rate of exposure notifications for anomalous changes that
    are inconsistent with an epidemiologically realistic model and/or data
    from other sources, such as manual contract tracing.

    For example, if mutually independent sources—e.g. test results and
    manual contact tracing data—show the infection rate is stable at 0.1% in
    a specific area, but exposure notifications increase tenfold, this would
    be a clear abuse signal.
*   A mechanism to verify that keys uploaded are tied to a positive test
    result is strongly recommended to all Health Authorities to limit the
    ability to upload fake diagnosis keys.
    Google has published a reference verification service design
    [here](https://developers.google.com/android/exposure-notifications/verification-system)
    and open source code
    [here](https://github.com/google/exposure-notifications-verification-server).
    This design is built with two foundational privacy goals in mind:

    *   The diagnosis key server should not learn the identity of a
        COVID-positive individual
    *   The verification server and the Healthcare Authority should not
        learn the TEKs of the COVID-positive individual

#### Additional considerations

Relay attacks are well understood and have been studied for many years:
all mitigations require either location information, which creates a new
privacy risk for ENS, or precise timing information. Timing-based solutions
are not feasible in this system since BLE packets can be relayed almost
instantaneously over the internet. Matching in ENS must be tolerant to
legitimate variations in timing such as processor speed. Distance-bounding
protocols that also rely on precise timing were discounted because they
require a 2-way protocol (see [connectionless
protocols](#use-of-connectionless-protocol)) and the high degree of precision
required is not supported by most BLE device firmware. That said, we
continue to evaluate potential mitigations within the parameters of our
design goals.

**Concern**

Associated Encrypted Metadata (AEM) TX (transmit) power could be altered as
part of a relay attack. We do not believe that TX power authentication
would be a useful defense against relay attacks for the following reasons:

*   TX power authentication doesn't protect against the use of a
    high-power antenna positioned to cover a large area with a signal strength
    indicative of proximity. This is because the system is designed to be
    highly tolerant of differences between TX-power and received signal
    strength indicator (RSSI) caused, for example, by both devices being
    in-pocket. The Android ecosystem also has a diverse range of hardware: some
    Android devices transmit signals as much as 30dB weaker than others. The
    required system tolerance to differences in transmit power means that
    relaying a packet from such a weak device on a higher transmit power device
    would already allow the higher power device to appear nearby without
    altering the TX field.
*   TX power authentication would not prevent the deployment of malware on
    phones to perform relay packets; app-store and OS policy are more effective
    to prevent collection of EN packets by third party apps.
*   In this context, encrypting the AEM prevents an adversary from joining
    between RPIs coming from the same device (using device-specific TX power
    as a source of entropy).


### Server data pollution

**Concern**

An adversary broadcasts RPIs based on adversary-chosen diagnosis keys to
many targets, then fraudulently uploads the keys as if they belonged to a
legitimately diagnosed individual. Even without broadcasting RPIs based on
these keys, this could be used to disrupt the service since an excessive
number of keys would result in unacceptable bandwidth and CPU usage to
download and match keys on devices.

**Mitigations**

*   We strongly recommend that all Health Authorities implement a
    mechanism to verify that keys uploaded are tied to a positive test result.
    This will limit the ability to upload fake diagnosis keys. See [Relaying
    RPIs](#relaying-rpis) for more details
*   Servers are also strongly encouraged to implement standard denial of
    service/abuse prevention mechanisms.

## Learning COVID-positive status

### Re-identification

**Concern**

During the 14 day exposure window, an adversary could collect a target's
Rolling Proximity Identifiers and then correlate them with published
diagnosis keys. The adversary (who could be a legitimate participant in EN)
could then identify the source of the exposure based on information
external to the system, for example, using knowledge of who was present at
the same time and place.

To succeed, the adversary must simultaneously identify the specific target
(visually, for example via number plate recognition, at a border control
point, physical access controls to buildings) and record the RPI
transmitted by an EN app on the target's mobile device. If both can be done
at the same time, the adversary can discover that the target was
COVID-positive after they upload their Temporary Exposure Keys (TEKs).
If multiple users of the EN app are within range, the adversary will have
to distinguish between several users' RPIs, though signal strength could be
used to narrow identification. Conclusive identification of the target's
diagnosis status might require several periods of RPI capture during the
same day. TEKs are randomly and independently generated using a
cryptographic random number generator (see
[EN Cryptography Specification](https://blog.google/documents/69/Exposure_Notification_-_Cryptography_Specification_v1.2.1.pdf)).
Therefore, an adversary would have to carry out re-identification attacks
separately for each day.

**Mitigations**

The core purpose of an EN system is to learn whether any COVID-positive
people have been close enough for long enough to put the device owner at
risk of infection. Without this ability, the system doesn't do what it was
designed to. The alternative is not to operate an EN system at all. While
ENS apps do not have access to scanned RPIs via the ENS APIs, it's not
possible to prevent a sophisticated adversary from capturing BLE RPIs using
a dedicated eavesdropping device.

<a id="covid-positive">With</a> this in mind, an important motivation in deciding between centralized
and decentralized has been that COVID-positive status data presents less
incremental risk to a person, and is less permanent than a person's social
interactions, which are exposed by a centralized approach. For example:

*   COVID-positive status may only be accurate for a few days after
    someone reports their diagnosis keys
*   Many celebrities have publicly shared their COVID-positive status on
    social media
*   Governments keep records of COVID-positive individuals—separate from
    diagnosis key servers—whether or not an ENS is in operation
*   COVID-positive status is easily observed by other means; for example,
    neighbors or the press can often observe if someone has been hospitalized
*   Some already operational government contact tracing discloses a person's
    COVID-positive status to their contacts

#### Additional considerations

**Split key** approaches are one proposed mitigation: multiple pieces
(cryptographic secret shares) of an RPI, broadcast at different times, must
be observed before a match with a reported diagnosis key is possible. To
succeed, a re-identifying adversary would have to be near the target for
long enough to successfully match. We have not implemented such a scheme
for several reasons:

*   The requirements for an adversary seem no different from the
    Healthcare Authority-defined criteria for an exposure
*   The risk of missing relevant contacts increases as the split key window
    increases. Our design avoids situations where valid contacts are otherwise lost.
*   The encounter duration threshold for a valid contact can be set by an EN
    app's Healthcare Authority at matching time. A split key solution requires
    this threshold to be decided at broadcast time, which reduces the
    flexibility to modify duration thresholds, for example, in roaming scenarios.

We note that centralized solutions are also vulnerable to a targeted
re-identification attack. In the centralized case, the adversary broadcasts
RPIs in proximity to their target, shares the RPIS with the central
service, and then observes whether or not they receive an Exposure
Notification.

### Network traffic analysis

**Concern**

An adversary could learn about infection status by observing network
traffic between devices and servers, including diagnosis servers.

**Mitigations**

*   Use Transport Layer Security (TLS) to protect the integrity and
    confidentiality of network data.
*   We recommend that Healthcare Authority apps make randomized requests to
    servers to prevent an adversary from concluding a user was diagnosed based
    on observing network traffic to the diagnosis server that only happens upon
    diagnosis. This is implemented in the Google open-source sample app
[here](https://github.com/google/exposure-notifications-android/blob/master/app/src/main/java/com/google/android/apps/exposurenotification/network/DiagnosisKeyUploader.java#L115).  

### Diagnosis server compromise

**Concern**

An adversary with access to the data on the diagnosis server could use this
information to potentially identify users.

**Mitigations**

Diagnosis keys do not identify users. They are randomly generated and not
inherently linked to individuals' identities. Each person's diagnosis keys
are generated independently each day and are unrelated.
Google's API Terms of Service forbid associating diagnosis keys with
personally identifiable information, or with other diagnosis keys from the
same device, except temporarily for verification purposes to support
certain already deployed verification flows. We also recommend that
diagnosis server operators limit retention of potentially identifiable
information including, but not limited to, server logs that contain IP
addresses.

### Compromised mobile device

**Concern**

EN apps store information on-device that would allow an adversary to
determine the device user's diagnosis status. For example, whether or not
the user is participating in EN and hasn't yet uploaded diagnosis keys.

**Mitigations**

Android includes multiple strong layers of defense against device
compromise, including [Google Play
Protect](https://www.android.com/play-protect/). If an adversary is able to
compromise the device, much more sensitive information is available,
including the user's contacts, messaging history, emails, and photos.

### Forensics and physical access to devices

**Concern**

An adversary with physical access to the device could extract historical
information such as observed RPIs or the TEKs used to generate broadcast
RPIs

**Mitigations**

*   Whenever a device supports full-disk encryption (FDE) or file-based
	encryption (FBE), all data stored on disk is encrypted using this
	standard mechanism by default. For devices that do not support either of
	these, encryption-at-rest of data has no benefit because the key would
	need to be stored in plaintext alongside the data. Such encryption is
	mandatory on all devices running Android 10 and above and on all devices
	running Android 6.0 or later, excluding low-RAM devices and devices on
	which AES bulk encryption performance is below 50MB/s. 

*   Encryption of data sent to the server and stored by the HA app is
	the responsibility of these apps. Google’s sample app uses HTTPS and the
	default FBE implementation available. Disabling HTTPS requires the HA to
	set an explicit network security configuration setting in the APK
	manifest.

*   TEKs generated by the device are stored for 14 days before being
	deleted by a daily maintenance process. The same is true for RPIs
	scanned by the device and matches detected. Note that Healthcare
	Authorities are also responsible for handling TEKs when reporting a
	positive diagnosis. In this case, storage and deletion are under the
	control of the HA app.

*   Even when an attacker has physical access to local storage, platform
	encryption protects the data. If an attacker were to try and compromise
	system integrity (rooting) this would require unlocking the bootloader,
	which on all modern Android devices automatically wipes user data as a
	security precaution.

*   Android devices do not have swap partitions. Where the related
	feature ``zram writeback`` is used, compressed RAM is written to the
	``/data/per_boot`` directory, which is encrypted with a newly generated key
	on each boot and stored only in RAM.

*   Devices using a physical secure element (SE) and implementing
	File-Based Encryption (FBE, introduced in Android 7.0 and mandatory on
	new devices since Android 10) store the Key Derivation Function (KDF)
	secret in the SE and destroy it upon Factory Data Reset (FDR), thus
	rendering data irrecoverable;

*   Devices using the SE in general provide non-recoverability guarantees
	that are at least as strong as the physical and logical integrity of the
	SE.

*   Even if they do not have a secure element (SE), devices running
	either FBE or full-disk encryption (FDE) generally require at least the
	physical compromise of the flash memory to recover any data after FDR.

## Learning social interactions

**Concern**

An adversary could potentially identify a participant of EN as the origin
of an Exposure Notification. This would also reveal social interactions
since it implies the two were in proximity.

An adversary with access to a device's EN APIs could feed a custom match
query to the provideDiagnosisKeys API with a known, identified diagnosis
key. If the API returns a match, the adversary knows that the individual
with the diagnosis key exposed the device owner, that they met, and for how
long.

An adversary could also feed a group of diagnosis keys to the API to learn
if a user associated with the group of keys exposed the device owner. TEK
metadata could be used to increase the effectiveness of this potential
attack by observing matching results for a subset of diagnosis keys, tagged
using TEK metadata. This could involve chaining between multiple
observations if metadata state changes are possible.

Location variant: If the RPIs associated with that group of diagnosis keys
were broadcast from a fixed location known to the adversary, the same
attack could be used to learn about a target's location.

**Mitigations**

This type of potential attack would require connecting a user's identity
with any diagnosis keys used in the attack. Mitigations against this attack
include:

1. Logging hashes of queries and making them exportable in the UI with the
user's consent. These keys can be pooled for analysis to detect targeted
attacks.

2. The diagnosis server must sign all key bundles used in matching. Invalid
signatures will result in an error upon matching. This prevents an attack
via a compromised app that submits malicious queries. This mechanism also
lets servers control the queries made and rate-limit them.

3. Starting with Exposure Notification API v1.5, Healthcare Authority apps
can only make six queries for `ExposureWindow` per day, reducing the
risk of brute force attacks of this type.

4. Metadata state changes are limited to only one per key. Such state
changes are used, for example, to allow self-reported keys to transition to
a confirmed diagnosis.

5. The API can only be called from a limited set of known, approved apps.
This means that an adversary must either have root-exploited the device or
has compromised an Exposure Notification app's code signing keys.

6. Connecting diagnosis keys with a user's identity for any purpose other
than test verification is against
[Google's additional terms of service for ENS apps](https://blog.google/documents/72/Exposure_Notifications_Service_Additional_Terms.pdf).

#### Additional considerations

Requiring a minimum query size would not help mitigate this attack
because the API has no way to differentiate between a valid and invalid
diagnosis key. An adversary could fill the rest of the query with fake or
COVID-negative keys.

## Tracking

### Tracking COVID-positive users for the rolling window period using the Temporary Exposure Key

**Concern**

An adversary with access to the downloaded TEKs could link a
COVID-positive user's RPIs for the duration of a rolling window period.
Using BLE sniffers in multiple places, an adversary could link together
different observations of the user over the rolling window period but not
longer.

See also [Learning Social Interactions: location
variant](#learning-social-interactions) above.

**Mitigations and considerations**

Linking Rolling Proximity Identifiers with a Temporary Exposure Key for the
duration of the Rolling Window is a network compression mechanism that
reduces bandwidth costs for all users by approximately 100X. (See
[Bandwidth Usage](#bandwidth-usage) above). The impact of this
effect is greater in high-population countries where bandwidth is
relatively costly. Our design must balance cost and network load with
limiting trackability. We note that:

1.  This attack would allow trackability **only** **for declared
    COVID-positive users** and only over the rolling period window, which is 24
    hours in EN version 1.5. Users are clearly informed and can opt-out when
    uploading TEKs following a positive diagnosis.
2.  For users who have not consented to upload their diagnosis keys
    following a positive COVID test result, an adversary cannot learn any more
    about the user than they already can by sniffing BLE packets.

    Rolling Proximity Identifiers are designed to be no more linkable than
    standard Bluetooth broadcast packets, which include a frequently rotating
    MAC address. Bluetooth Low-Energy (BLE) MAC addresses rotate at least every
    15 minutes, at the same time as the EN RPI. Therefore, a COVID-negative
    person's device can at most be recognized for 15 minutes by another device
    within physical BLE range. RPI intervals are synchronized with these
    rotating Bluetooth MAC addresses to prevent linking alternately based on
    MAC address and RPI.

    By restarting the BLE interface on RPI reset, EN creates a random rotation
    interval for all devices that is synchronized with BLE MAC rotation. This
    prevents the BLE MAC being used to bridge the RPI rotation.
3.  This attack would require a large network of BLE sniffers.
4.  Unless the adversary has an independent means (for example, a camera
    network) of identifying users whose devices are transmitting BLE packets,
    the adversary will not learn the identity of COVID-positive users. If an
    adversary has a mechanism for identifying users in proximity, then they
    could already use this to track the user's movements. Using EN does not
    significantly increase tracking risks.
5.  To reduce the impact on the user's network bandwidth, the rolling period
    window is set at 24 hours but can be reduced in lower infection rate
    scenarios to lessen risks.
6.  The movements and interactions of COVID-positive users are revealed to a
    much greater extent by some manual contact tracing methods.
7.  Google Play’s policies forbid the malicious use of BLE scanning, and
    Play’s rigorous review processes are designed to detect it. Any app
    found to be explicitly capturing BLE RPIs will be removed.
	
#### Additional considerations

**Cuckoo filter approach as a mitigation**

One mitigation that has been proposed
[[see DP3T White Paper](https://github.com/DP-3T/documents/blob/master/DP3T%20White%20Paper.pdf)]
is to distribute RPIs using a cuckoo filter rather than using temporary
exposure keys and a pseudo-random function as in the current EN design.

A cuckoo filter approach trades a reduction in bandwidth usage for a
higher number of false-positives and reduced trackability. We decided
against this approach because achieving an acceptable number of
false-positives would require excessive bandwidth and device storage
consumption.

An in-depth analysis of the tolerable false-positive rate as part of our
initial design led us to use 128-bit RPIs. Reaching a similarly low
false-positive rate would make the use of cuckoo filters unfeasible from a
bandwidth and storage perspective, given that the cuckoo filter is used for
matching with RPIs, 144x more of them than Temporary Exposure Keys. For
example, even allowing for a significantly higher false-positive rate for
large deployments, for a daily infection rate of 60,000 with 50 million
users, the download sizes required by each user would be over 1 GB per day.

Unlike our current design, another concern with the use of cuckoo filters
is that a match can be caused without the knowledge of the TEK that
generated that RPI value. This means that clients performing RPI matching
cannot verify that a corresponding TEK from which they were derived was
released, or when they were supposed to be broadcast. This makes the time
window for messages to be replayed longer.

Using a cuckoo filter as a first stage match with a full check following
would leak information about the user's social interactions to the server,
which goes against a core privacy property of the system.

### Bluetooth-based tracking

**Concern**

Switching on Bluetooth for users who had it switched off enables any
tracking risk present with the standard Bluetooth stack.

**Mitigations and considerations**

The incremental risk of turning Bluetooth on to enable EN should be
considered small because:

*   All BLE packet MAC addresses rotate at least every 15 minutes in sync
    with the EN RPI. At most, a person's device can be recognized for 15
    minutes by another device within physical BLE range.
*   7-15 minute rotating BLE advertising IDs are used by Android devices
    starting with Android Marshmallow, that is approximately 80% of devices in
    use. A large percentage of pre-Marshmallow devices do not have BLE, and
    would therefore not be able to run EN. Bluetooth Classic uses a fixed MAC
    address but this is only advertised if the device is in discoverable mode,
    that is when Bluetooth settings are open or when an app has triggered this
    mode, with a short timeout. Some manufacturers' Android devices enable
    Bluetooth Classic in discoverable mode whenever Bluetooth is turned on. We
    have notified, and are working with, affected manufacturers to fix this
    vulnerability. In the meantime, on Android, we turn off discoverability the
    first time EN is run, and every 24 hours thereafter.
*   Unless the adversary has an independent means of identifying users whose
    devices are transmitting BLE packets ( for example, a network of cameras)
    an adversary could not learn the identity of users or anything about who
    associates with whom. If an adversary has a mechanism for identifying users
    in proximity, then they can already use this to track the users' movements,
    and BLE does not introduce additional risk.

**Bridging rotations at MAC/RPI boundaries**

MAC and RPI rotations are designed to protect against adversaries
observing the device’s BLE emissions across different locations and
times, rather than continuously. For example, this mitigation protects
against an adversary putting BLE sniffers in multiple train stations
across a city, or against a store owner who links a user’s prior
purchases to their path around the store.

Conversely, protecting against an adversary who can continuously collect
every BLE frame emitted by a device is not a design goal for either MAC
or RPI rotation.

*   This attack requires continuous proximity; an adversary would need
    to have an independent means of tracking the user’s location, or a
    spatially continuous network of sensors. For example, in order to track
    a device leaving from home at 0900, going to a coffee shop at 0930, and
    then to the library at 1100, the attacker must observe all 0915, 0930,
    0945, 1000, … 1100 rotation events. If they could only observe the 0900
    and 1100 rotations they would not be able to deduce that those were the
    same device. 

*   While this means that such attacks aren’t useful for an adversary,
    it’s worth noting that the ability to bridge between temporally adjacent
    broadcast IDs is inherent in the use of any RF protocol since: 

    *   Adjacent timestamps can be used to link sources across frames

    *   Signal strength can be assumed to be invariant between adjacent frames
        and can therefore also be used to link sources across frames.

For the sake of transparency, we note that this issue was confirmed on a
subset of Android devices globally.  These issues likely resulted from
how certain OEMs have implemented Bluetooth since, for the reasons noted
above, the [Android Compatibility Definition Document](https://source.android.com/compatibility/10/android-10-cdd#7_4_3_bluetooth) (CDD) does not
require rotation in sync.  After extensive testing, a change to EN has
nevertheless been rolled out that removes this opportunity for
device-specific misbehavior with respect to EN for all devices. The RPI
is now set to a globally fixed value for a small number of BLE frames
surrounding the RPI rollover.

As noted above, Google Play’s policies forbid the malicious use of BLE
scanning, and Play’s rigorous review processes are designed to detect
it. Any app found to be explicitly capturing BLE RPIs will be removed.

### Linking diagnosis keys through export file analysis

**Concern**

An adversary could attempt to link Diagnosis Keys from different days—which
are independently randomly generated and _a priori_ not linkable—based on
analysis of Diagnosis Key batches served by the Diagnosis server.

**Mitigations**

*   This may be feasible specifically in situations where very few
    individuals are diagnosed positive in a given timeframe. If a diagnosis key
    batch only contains one diagnosis key for each day, the keys in the batch
    must correspond to the same individual. To mitigate this potential for
    correlation, we recommend that diagnosis key servers pad out diagnosis key
    batches with random keys, with some jitter so exports don't leak the fact
    that they were padded. This is implemented in our reference server
    ([code](https://github.com/google/exposure-notifications-server/blob/821531b167d794ec4a57075cd2009adbcd137505/internal/export/worker.go#L358)).
*   The natural order in which diagnosis keys are stored in the diagnosis
    server's database may confer some information about their association at
    time of upload. To ensure this information is not leaked in diagnosis key
    export batches, we recommend that batches are re-ordered before
    publication, as we've done in our
    [reference implementation](https://github.com/google/exposure-notifications-server/blob/1aef0fbe22aef93ec86f5432dd9d0317418eba15/internal/export/exportfile.go#L139).

### Feedback

If you have questions or feedback about this document, please let us know by
emailing [ens-privsec@google.com](mailto:ens-privsec@google.com).
