import java.util.*;
import java.util.concurrent.*;

// Elimination array provides a list of exchangers which
// are picked at random for a given value.

class EliminationArray<T> {
  Exchanger<T>[] exchangers;
  final long TIMEOUT;
  final TimeUnit UNIT;
  Random random;
  // exchangers: array of exchangers
  // TIMEOUT: exchange timeout number
  // UNIT: exchange timeout unit
  // random: random number generator

  @SuppressWarnings("unchecked")
  public EliminationArray(int capacity, long timeout, TimeUnit unit) {
    exchangers = new Exchanger[capacity];
    for (int i=0; i<capacity; i++)
      exchangers[i] = new Exchanger<>();
    random = new Random();
    TIMEOUT = timeout;
    UNIT = unit;
  }

  // 1. Try exchanging value on a random exchanger.
  public T visit(T x, int range) throws TimeoutException {
    int i = random.nextInt(range);
    return exchangers[i].exchange(x, TIMEOUT, UNIT);
  }
}
