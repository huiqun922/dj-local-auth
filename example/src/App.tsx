import * as React from 'react';

import { StyleSheet, View, Text, Button } from 'react-native';
import { authenticate } from 'dj-local-auth';

export default function App() {
  const [result, setResult] = React.useState<string>();

  return (
    <View style={styles.container}>
      <Text>Result: {result}</Text>

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
