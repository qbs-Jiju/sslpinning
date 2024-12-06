# sslpinning

react native ssl pinning

## Installation

```sh
npm install sslpinning
```

## Usage


```js
import { fetch } from 'sslpinning';


// ...

useEffect(() => {
    fetch("https://www.dynamicgyms.com/landingpage",{sslPinning:{certs:[""]}})
    .then(url=>console.log({url}))
    .catch(err=>{console.error({err})})
  }, []);
```


## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
