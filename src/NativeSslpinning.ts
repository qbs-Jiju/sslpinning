import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

import type { Int32 } from 'react-native/Libraries/Types/CodegenTypes';

enum ResponseType {
  Text = 'text',
  Base64 = 'base64'
}

enum Method {
  DELETE = 'DELETE',
  GET = 'GET',
  POST = 'POST',
  PUT = 'PUT'
}

export interface Cookies {
  [cookieName: string]: string;
}

export interface Options {
  body?: string,
  responseType?: ResponseType,
  credentials?: string,
  headers?: {
      [key: string]: string;
  };
  method?: Method,
  pkPinning?: boolean,
  sslPinning: {
      certs: string[]
  },
  timeoutInterval?: Int32,
  disableAllSecurity?: boolean,
  caseSensitiveHeaders?: boolean
}

export interface FetchResponse {
  bodyString?: string;
  data?: string;
  headers: {
      [key: string]: string;
  };
  status: number;
  url: string;
}
export interface Spec extends TurboModule {
  fetch(
    url: string,
    options: Options,
): Promise<FetchResponse>;

getCookies(
    domain: string
): Promise<Cookies>;

removeCookieByName(
    cookieName: string
): Promise<void>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('SslPinning');
