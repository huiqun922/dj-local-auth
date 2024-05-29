import { NativeModules, Platform } from 'react-native';
import type {
  AuthenticationType,
  LocalAuthenticationOptions,
  LocalAuthenticationResult,
} from './LocalAuth';

const LINKING_ERROR =
  `The package 'dj-local-auth' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const DjLocalAuth = NativeModules.DjLocalAuth
  ? NativeModules.DjLocalAuth
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export function authenticate(
  options: LocalAuthenticationOptions
): Promise<LocalAuthenticationResult> {
  return DjLocalAuth.authenticate(options);
}

export function supportAuthenticationType(): Promise<AuthenticationType[]> {
  return DjLocalAuth.supportAuthenticationType();
}

export type {
  AuthenticationType,
  LocalAuthenticationOptions,
  LocalAuthenticationResult,
};
