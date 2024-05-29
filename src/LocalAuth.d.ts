export type LocalAuthenticationResult =
  | { success: true }
  | { success: false; error: string; warning?: string };

// @needsAudit
export enum AuthenticationType {
  /**
   * Indicates fingerprint support.
   */
  FINGERPRINT = 1,
  /**
   * Indicates facial recognition support.
   */
  FACIAL_RECOGNITION = 2,
  /**
   * Indicates iris recognition support.
   * @platform android
   */
  IRIS = 3,
}

/**
 * Security level of the biometric authentication to allow.
 * @platform android
 */
export type BiometricsSecurityLevel = 'weak' | 'strong';

// @needsAudit
export type LocalAuthenticationOptions = {
  /**
   * A message that is shown alongside the TouchID or FaceID prompt.
   */
  promptMessage?: string;
  /**
   * Allows to customize the default `Cancel` label shown.
   */
  cancelLabel?: string;
  /**
   * After several failed attempts the system will fallback to the device passcode. This setting
   * allows you to disable this option and instead handle the fallback yourself. This can be
   * preferable in certain custom authentication workflows. This behaviour maps to using the iOS
   * [LAPolicyDeviceOwnerAuthenticationWithBiometrics](https://developer.apple.com/documentation/localauthentication/lapolicy/lapolicydeviceownerauthenticationwithbiometrics?language=objc)
   * policy rather than the [LAPolicyDeviceOwnerAuthentication](https://developer.apple.com/documentation/localauthentication/lapolicy/lapolicydeviceownerauthentication?language=objc)
   * policy. Defaults to `false`.
   */
  disableDeviceFallback?: boolean;
  /**
   * Sets a hint to the system for whether to require user confirmation after authentication.
   * This may be ignored by the system if the user has disabled implicit authentication in Settings
   * or if it does not apply to a particular biometric modality. Defaults to `true`.
   * @platform android
   */
  requireConfirmation?: boolean;
  /**
   * Sets the security class of biometric authentication to allow.
   * `strong` allows only Android Class 3 biometrics. For example, a fingerprint or a 3D face scan.
   * `weak` allows both Android Class 3 and Class 2 biometrics. Class 2 biometrics are less secure than Class 3. For example, a camera-based face unlock.
   * @platform android
   * @default 'weak'
   */
  biometricsSecurityLevel?: BiometricsSecurityLevel;
  /**
   * Allows to customize the default `Use Passcode` label shown after several failed
   * authentication attempts. Setting this option to an empty string disables this button from
   * showing in the prompt.
   * @platform ios
   */
  fallbackLabel?: string;
};
