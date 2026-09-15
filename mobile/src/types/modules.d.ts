declare module '@react-native-community/netinfo' {
  export interface NetInfoState {
    type: string;
    isConnected: boolean | null;
    isInternetReachable?: boolean | null;
    details: any;
  }

  export type NetInfoChangeHandler = (state: NetInfoState) => void;

  const NetInfo: {
    fetch(): Promise<NetInfoState>;
    addEventListener(listener: NetInfoChangeHandler): () => void;
  };

  export default NetInfo;
}

declare module '@react-native-async-storage/async-storage' {
  const AsyncStorage: {
    getItem(key: string): Promise<string | null>;
    setItem(key: string, value: string): Promise<void>;
    removeItem(key: string): Promise<void>;
    clear(): Promise<void>;
    getAllKeys(): Promise<string[]>;
    multiGet(keys: string[]): Promise<[string, string | null][]>;
    multiSet(keyValuePairs: [string, string][]): Promise<void>;
    multiRemove(keys: string[]): Promise<void>;
  };

  export default AsyncStorage;
}
