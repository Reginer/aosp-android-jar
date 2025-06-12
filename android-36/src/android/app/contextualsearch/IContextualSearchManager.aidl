package android.app.contextualsearch;

import android.app.contextualsearch.IContextualSearchCallback;
/**
 * @hide
 */
interface IContextualSearchManager {
  void startContextualSearchForForegroundApp();
  oneway void startContextualSearch(int entrypoint);
  oneway void getContextualSearchState(in IBinder token, in IContextualSearchCallback callback);
}
