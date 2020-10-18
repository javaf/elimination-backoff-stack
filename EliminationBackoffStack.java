import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;


class EliminationBackoffStack<T> {
  AtomicReference<Node<T>> top;
  EliminationArray<T> eliminationArray;
  static final int CAPACITY = 1;
  static ThreadLocal<RangePolicy> policy = new ThreadLocal<>() {
    protected RangePolicy initialValue() {
      return new RangePolicy(CAPACITY);
    }
  };

  public EliminationBackoffStack() {
    top = new AtomicReference<>(null);
    eliminationArray = new EliminationArray<>(CAPACITY);
  }

  public void push(T x) {
    RangePolicy p = policy.get();
    Node<T> n = new Node<>(x);
    while (true) {
      if (tryPush(n)) return;
      try {
      T y = eliminationArray.visit(x, p.range());
      if (y != null) continue;
      p.onSuccess(); return;
      }
      catch (TimeoutException e) { p.onTimeout(); }
    }
  }

  public T pop() throws EmptyStackException {
    RangePolicy p = policy.get();
    while (true) {
      Node<T> n = tryPop();
      if (n != null) return n.value;
      try {
      T y = eliminationArray.visit(null, p.range());
      if (y == null) continue;
      p.onSuccess(); return y;
      }
      catch (TimeoutException e) { p.onTimeout(); }
    }
  }

  // 1. Get stack top.
  // 2. Set node's next to top.
  // 3. Try push node at top (CAS).
  protected boolean tryPush(Node<T> n) {
    Node<T> m = top.get(); // 1
    n.next = m;                     // 2
    return top.compareAndSet(m, n); // 3
  }

  // 1. Get stack top, and ensure stack not empty.
  // 2. Try pop node at top, and set top to next (CAS).
  protected Node<T> tryPop() throws EmptyStackException {
    Node<T> m = top.get();                          // 1
    if (m == null) throw new EmptyStackException(); // 1
    Node<T> n = m.next;                       // 2
    return top.compareAndSet(m, n)? m : null; // 2
  }
}
