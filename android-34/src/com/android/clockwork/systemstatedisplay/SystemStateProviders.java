package com.android.clockwork.systemstatedisplay;

import android.content.Context;
import com.android.internal.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A class that provides the available {@link SystemState}s from available {@link
 * SystemStateProvider}s.
 */
class SystemStateProviders {
  private final List<SystemStateProvider> mProviders;

  @VisibleForTesting
  SystemStateProviders(List<SystemStateProvider> providers) {
    mProviders = providers;
  }

  SystemStateProviders(Context context) {
    mProviders = new ArrayList<>();
    mProviders.add(new PssProvider(context));
    mProviders.add(new FreeMemoryProvider(context));
  }

  /**
   * Returns a {@link List} of{@link SystemState}s from all the available {@link
   * SystemStateProvider}s.
   */
  List<SystemState> getSystemStates() {
    return mProviders.stream()
        .map(provider -> provider.getSystemState())
        .filter(state -> !state.equals(SystemState.INVALID_STATE))
        .sorted(Comparator.comparing(SystemState::getTitle))
        .collect(Collectors.toList());
  }
}
