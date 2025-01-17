# Keystone-app

We added MPC-related features into Keystone, providing a 2/2 MPC wallet between MPCSnap and Keystone to manage EVM assets. 🎉🎉🎉

You can also follow [@Safeheron](https://twitter.com/Safeheron) on Twitter to learn more about MPC wallet.

To build this branch, you need to [compile the MPC algorithem](https://github.com/Safeheron/mpcsnap/blob/main/packages/mpc-adapter/README.md#bundle-for-keystone) and put the `MPC.bundle.js` into [assets/script](./app/src/main/assets/script) folder, and then follow [this](#build).

## Contents

- [Introduction](#introduction)
- [Clone](#clone)
- [Build](#build)
- [Test](#test)
- [Code Structure](#code-structure)
- [Core Dependencies](#core-dependencies)
- [License](#license)


## Introduction
Keystone runs as a standalone application on customized hardware and Android 8.1 Oreo (Go Edition).  This app performs:
1. Interaction with the user.
2. Interaction with the mobile application [Keystone companion app](https://keyst.one/companion-app) via QR code.
3. Interaction with the Secure Element (SE) via serial port, open source SE firmware can be found at [keystone-se-firmware](https://github.com/KeystoneHQ/keystone-se-firmware). Transaction data is signed by the Secure Element and the generated signature is sent back to the application. This signature and other necessary messages are displayed as a QR code. You can check the animation on our webpage to see the whole process. Users use their mobile or desktop application to acquire signed transaction data and broadcast it.

The hardware wallet application was programmed with Java language. The transaction related work is done by Typescript, for which open source code is available at [crypto-coin-kit](https://github.com/KeystoneHQ/crypto-coin-kit). The J2V8 framework is used as a bridge between Java and Typescript.


## Clone

    git clone git@github.com:KeystoneHQ/Keystone-cold-app.git --recursive

## Build
    cd Keystone-cold-app
    ./gradlew assembleVault_v2Release
You can also build with IDEs, such as `Android Studio`,`intelliJ`.

## Test
    ./gradlew test

<!-- ## Integration Guide
if you like to integrate with Keystone, checout this [intergration guide](https://github.com/KeystoneHQ/keystone-docs/blob/master/Integration_guide.md) -->

## Code Structure
Modules

`app`: Main application module

`coinlib`: Module for supported blockchains, currently included in 12 blockchains

`encryption-core`: Module for the Secure Element, includes commands, protocol, serialize/deserialize, serial port communication

## Core Dependencies
1. [crypto-coin-message-protocol](https://github.com/KeystoneHQ/keystone-crypto-coin-message-protocol) - protocol buffer of communication with the mobile application
2. [crypto-coin-kit](https://github.com/KeystoneHQ/crypto-coin-kit) - crypto-coin libraries
3. [keystone-se-firmware](https://github.com/KeystoneHQ/keystone-se-firmware) - Secure Element firmware

## License
[![GPLv3 License](https://img.shields.io/badge/License-GPL%20v3-green.svg)](https://opensource.org/licenses/)
This project is licensed under the GPL License. See the [LICENSE](LICENSE) file for details.