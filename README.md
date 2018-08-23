# Cortez for Android

**Cortez for Android** is a mobile wallet for the Tezos blockchain.

This native Android app aims to bring maximum security and best user experience the Android operating system can bring to its users.

Feel free to download it on Google Play:  
<https://play.google.com/store/apps/details?id=com.tezcore.cortez.zeronet>

## Features

- Create and restore a Tezos wallet
- Keep track of public key hashes in a simple address book
- Allows you to receive or send ꜩ to any of these addresses
- Consult your operations history or that of one of your contacts

## Security

Cortez for Android allows you to work with the Tezos blockchain and keeps your private keys protected using **Encryption**, **Fingerprint** and **Confirm Credentials** API’s. 

The wallet generates your **mnemonics 24 words** in the same way as Ledger Nano does. You need to keep these 24 words (in the same order) to recover your account.

To use the Cortez for Android app, the user needs to create a **master password** during the create/restore wallet process. 

This password will be used to protect your private keys: to send ꜩ to an address contact, the user needs to enter the **master password** (or use the **fingerprint** if the option is checked).

The private keys and master password are encrypted using the _Android Keystore system_.
<https://developer.android.com/training/articles/keystore>

## Resources
- [Issues][project-issues] — To report issues, submit pull requests and get involved (see [MIT License][project-license])
- [Change log][project-changelog] — To check the changes of the latest versions

## License

**Cortez for Android** is available under the **MIT License**. Check out the [license file][project-license] for more information.

[project-issues]: https://gitlab.com/tezos-private/cortez-android/issues

[project-license]: LICENSE.md
[project-changelog]: CHANGELOG.md
