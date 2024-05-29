import * as React from 'react';

import { StyleSheet, View, Text, Button } from 'react-native';
import {
  authenticate,
  type AuthenticationType,
  supportAuthenticationType,
} from 'dj-local-auth';

export default function App() {
  const [result, setResult] = React.useState<string>();
  const [types, setTypes] = React.useState<string>();

  const typeDesc = React.useCallback((type: AuthenticationType) => {
    switch (type) {
      case 1:
        return 'Finger';
      case 2:
        return 'Face ID';
      case 3:
        return 'IRIS';
      default:
        return 'unknown';
    }
  }, []);

  React.useEffect(() => {
    supportAuthenticationType().then((r) => {
      let type = '';
      r?.forEach((t) => {
        type = type + typeDesc(t);
      });
      setTypes(type);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <View style={styles.container}>
      <Text>Result: {result}</Text>
      <Text>Supprot Type: {types}</Text>

      <Button
        title="Auth"
        onPress={() => {
          authenticate({}).then((r) => {
            setResult(r.success.toString());
          });
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
