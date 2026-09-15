type NetworkListener = (isOnline: boolean) => void;

let NetInfoModule: any = null;
try {
  NetInfoModule = require('@react-native-community/netinfo').default;
} catch {
  NetInfoModule = null;
}

class NetworkService {
  private listeners: Set<NetworkListener> = new Set();
  private onlineStatus: boolean = true;
  private isMocked: boolean = false;
  private unsubscribeNetInfo: (() => void) | null = null;

  constructor() {
    this.init();
  }

  private init() {
    try {
      if (NetInfoModule && typeof NetInfoModule.addEventListener === 'function') {
        this.unsubscribeNetInfo = NetInfoModule.addEventListener((state: any) => {
          if (!this.isMocked) {
            const isConnected = Boolean(state.isConnected && state.isInternetReachable !== false);
            if (this.onlineStatus !== isConnected) {
              this.onlineStatus = isConnected;
              this.notifyListeners(isConnected);
            }
          }
        });
      }
    } catch {
      // NetInfo native module unavailable (e.g. in unit tests)
      this.onlineStatus = true;
    }
  }

  public async isOnline(): Promise<boolean> {
    if (this.isMocked) return this.onlineStatus;
    try {
      if (NetInfoModule && typeof NetInfoModule.fetch === 'function') {
        const state = await NetInfoModule.fetch();
        const isConnected = Boolean(state.isConnected && state.isInternetReachable !== false);
        this.onlineStatus = isConnected;
        return isConnected;
      }
      return this.onlineStatus;
    } catch {
      return this.onlineStatus;
    }
  }

  public getStatusSync(): boolean {
    return this.onlineStatus;
  }

  public setMockStatus(isOnline: boolean): void {
    this.isMocked = true;
    if (this.onlineStatus !== isOnline) {
      this.onlineStatus = isOnline;
      this.notifyListeners(isOnline);
    }
  }

  public resetMockStatus(): void {
    this.isMocked = false;
    this.isOnline().then((status) => {
      this.onlineStatus = status;
      this.notifyListeners(status);
    });
  }

  public subscribe(listener: NetworkListener): () => void {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  private notifyListeners(isOnline: boolean): void {
    this.listeners.forEach((listener) => {
      try {
        listener(isOnline);
      } catch {
        // Ignore listener error
      }
    });
  }

  public destroy(): void {
    if (this.unsubscribeNetInfo) {
      this.unsubscribeNetInfo();
      this.unsubscribeNetInfo = null;
    }
    this.listeners.clear();
  }
}

export const networkService = new NetworkService();
