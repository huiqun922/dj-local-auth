# dj-local-auth

react native local authentication

## Installation

```sh
npm install dj-local-auth
```

## Usage

```js
import { authenticate, supportAuthenticationType } from 'dj-local-auth';

  React.useEffect(() => {
    supportAuthenticationType().then((r) => {
      r?.forEach((t) => {
        console.log(t.toString());
      });
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

// const options = {}; options can empty
const options = {
    biometricsSecurityLevel: "weak", //Only Android, weak or strong
    promptMessage: "Title", //A message that is shown alongside the TouchID or FaceID prompt.
    cancelLabel: "Cancel Label", //Allows to customize the default Cancel label shown.
    disableDeviceFallback: true; //Allows to customize the default Cancel label shown.
    requireConfirmation: true; //Only Android,Sets a hint to the system for whether to require user confirmation after authentication.
};
// Result Type
// {
//     success: bool,
//     error: string.
// }
 authenticate(options).then((r) => {
            setResult(r.success.toString());
          });
```

## Permission
#### iOS
```
NSFaceIDUsageDescription
```
#### Android
```
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.USE_FINGERPRINT" />
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
