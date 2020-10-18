class RangePolicy {
  int requests;
  int successes;
  int timeouts;
  int range;
  final int LIMIT;

  public RangePolicy(int limit) {
    LIMIT = limit;
  }

  public int range() {
    requests++;
    if (successes+5 >= requests/2) return range;
    if (timeouts-5 >= requests/2) range = Math.max(range-1, 0);
    else range = Math.min(range+1, LIMIT);
    return range;
  }

  public void onSuccess() {
    successes++;
    refresh();
  }

  public void onTimeout() {
    timeouts++;
    refresh();
  }

  private void refresh() {
    if (requests < 100) return;
    requests = 0;
    successes = 0;
    timeouts = 0;
  }
}
