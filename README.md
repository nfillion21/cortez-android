# Cortez for Android

**Information:**  
Cortez is still active but not maintained by [Nomadic Labs](https://nomadic-labs.com/) anymore.

**Cortez for Android** is a mobile wallet for the Tezos blockchain.

This native Android app aims to bring maximum security and best user experience the Android operating system can bring to its users.

Feel free to download it on Google Play:  
<https://play.google.com/store/apps/details?id=com.tezcore.cortez>


## Features

* Create and restore a Tezos wallet
* Send and receive tez
* Delegate your tez
* Consult your operations history
* Keep track of public key hashes in a simple address book
* Deploy and handle your tez using different types of contracts: **default KT1 contract** and **daily spending limit contract**


## Security

Cortez for Android allows you to work with the Tezos blockchain and keeps your private keys protected using **Encryption**, **Fingerprint** and **Confirm Credentials** API’s. 

The wallet generates your **mnemonics 24 words** in the same way as Ledger Nano does. You need to keep these 24 words (in the same order) to recover your account.

To use the Cortez for Android app, the user needs to create a **master password** during the create/restore wallet process. 

This password will be used to protect your private keys: to send ꜩ to an address contact, the user needs to enter the **master password** (or use the **fingerprint** if the option is checked).

The private keys and master password are encrypted using the _Android Keystore system_.
<https://developer.android.com/training/articles/keystore>

### Daily Spending Limit contract

Overview of the advantages for using the DSL contract:  

* No fear of losing or having your phone hacked: your masterkey is not stored in the phone.
* Use of the daily limit, a specific feature of the traditional banking system  
* The contract is [formally verified](<https://blog.nomadic-labs.com/cortez-security-by-using-the-spending-limit-contract.html>)

More detailed information about the Daily Spending Limit contract on Nomadic Labs blog.  

Cortez security by using the Spending Limit contract.   
<https://blog.nomadic-labs.com/cortez-security-by-using-the-spending-limit-contract.html>

Formally Verifying a Critical Smart Contract.  
<https://blog.nomadic-labs.com/formally-verifying-a-critical-smart-contract.html>

## Resources
- [Issues][project-issues] — To report issues, submit pull requests and get involved (see [MIT License][project-license])
- [Change log][project-changelog] — To check the changes of the latest versions

## License

**Cortez for Android** is available under the **MIT License**. Check out the [license file][project-license] for more information.

[project-issues]: https://gitlab.com/nomadic-labs/cortez-android/issues

[project-license]: LICENSE.md
[project-changelog]: CHANGELOG.md
