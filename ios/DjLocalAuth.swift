import LocalAuthentication

@objc(DjLocalAuth)
class DjLocalAuth: NSObject {

    
    @objc(supportAuthenticationType:withRejecter:)
    func supportAuthenticationType(resolve:@escaping RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        var supportedAuthenticationTypes: [Int] = []
        
        if isTouchIdDevice() {
            supportedAuthenticationTypes.append(1)
        }
        
        if isFaceIdDevice() {
            supportedAuthenticationTypes.append(2)
        }
        resolve(supportedAuthenticationTypes);
    }
    
    @objc(authenticate:withResolver:withRejecter:)
    func authenticate(options: NSDictionary, resolve:@escaping RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        var warningMessage: String?
        let reason = options["promptMessage"] as? String;
        let cancelLabel = options["cancelLabel"] as? String
        let fallbackLabel = options["fallbackLabel"] as? String
        let disableDeviceFallback = (options["disableDeviceFallback"] as? Bool) ?? false
        
        if isFaceIdDevice() {
            let usageDescription = Bundle.main.object(forInfoDictionaryKey: "NSFaceIDUsageDescription")
            if usageDescription == nil {
                warningMessage = "FaceID is available but has not been configured. To enable FaceID, provide `NSFaceIDUsageDescription`."
            }
        }
        
        let context = LAContext()
        
        if fallbackLabel != nil {
            context.localizedFallbackTitle = fallbackLabel
        }
        
        if cancelLabel != nil {
            context.localizedCancelTitle = cancelLabel
        }
        
        context.interactionNotAllowed = false
        
        let policyForAuth = disableDeviceFallback ? LAPolicy.deviceOwnerAuthenticationWithBiometrics : LAPolicy.deviceOwnerAuthentication
        
        if disableDeviceFallback {
            if warningMessage != nil {
                // If the warning message is set (NSFaceIDUsageDescription is not configured) then we can't use
                // authentication with biometrics — it would crash, so let's just resolve with no success.
                // We could reject, but we already resolve even if there are any errors, so sadly we would need to introduce a breaking change.
                return resolve([
                    "success": false,
                    "error": "",
                    "warning": warningMessage ?? ""
                ]);
            }
        }
        
        context.evaluatePolicy(policyForAuth, localizedReason: reason ?? "111") { success, error in
            var err: String?
            
            if let error = error as? NSError {
                err = self.convertErrorCode(error: error)
            }
            
            return resolve([
                "success": success,
                "error": err ?? "",
                "warning": warningMessage ?? ""
            ])
        }
    }
    
    func isFaceIdDevice() -> Bool {
        let context = LAContext()
        context.canEvaluatePolicy(LAPolicy.deviceOwnerAuthenticationWithBiometrics, error: nil)
        
        return context.biometryType == LABiometryType.faceID
    }
    
    func isTouchIdDevice() -> Bool {
        let context = LAContext()
        context.canEvaluatePolicy(LAPolicy.deviceOwnerAuthenticationWithBiometrics, error: nil)
        
        return context.biometryType == LABiometryType.touchID
    }
    
    func convertErrorCode(error: NSError) -> String {
        switch error.code {
        case LAError.systemCancel.rawValue:
            return "system_cancel"
        case LAError.appCancel.rawValue:
            return "app_cancel"
        case LAError.biometryLockout.rawValue:
            return "lockout"
        case LAError.userFallback.rawValue:
            return "user_fallback"
        case LAError.userCancel.rawValue:
            return "user_cancel"
        case LAError.biometryNotAvailable.rawValue:
            return "not_available"
        case LAError.invalidContext.rawValue:
            return "invalid_context"
        case LAError.biometryNotEnrolled.rawValue:
            return "not_enrolled"
        case LAError.passcodeNotSet.rawValue:
            return "passcode_not_set"
        case LAError.authenticationFailed.rawValue:
            return "authentication_failed"
        default:
            return "unknown: \(error.code), \(error.localizedDescription)"
        }
    }
}
