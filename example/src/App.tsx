import { useState, useEffect } from 'react';
import { StyleSheet, View, Text } from 'react-native';
import { fetch } from 'sslpinning';

export default function App() {
  const [result, setResult] = useState<number | undefined>();

  useEffect(() => {
  
    fetch("https://www.dynamicgyms.com/landingpage",{sslPinning:{certs:[""]}})
    .then(url=>console.log({url}))
    .catch(err=>{console.error({err})})
   
  }, []);

  return (
    <View style={styles.container}>
      <Text>Result: {result}</Text>
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
