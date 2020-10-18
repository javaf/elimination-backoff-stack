import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

// Elimination-backoff stack is an unbounded lock-free LIFO linked list, that eliminates concurrent pairs of pushes and pops with exchanges.

class EliminationBackoffStack<T> {
  AtomicReference<Node<T>> top;
  EliminationArray<T> eliminationArray;
  static final int CAPACITY = 100;
  static final long TIMEOUT = 10;
  static final TimeUnit UNIT = TimeUnit.MILLISECONDS;
  static ThreadLocal<RangePolicy> policy = new ThreadLocal<>() {
    protected RangePolicy initialValue() {
      return new RangePolicy(CAPACITY);
    }
  };
  // top: top of stack (null if empty)
  // eliminationArray: for exchanging values between push, pop
  // CAPACITY: capacity of elimination array
  // TIMEOUT: exchange timeout for elimination array
  // UNIT: exchange timeout unit for elimination array
  // policy: strategy for in use range of elimination array

  public EliminationBackoffStack() {
    top = new AtomicReference<>(null);
    eliminationArray = new EliminationArray<>(CAPACITY, TIMEOUT, UNIT);
  }

  // 1. Create a new node with given value.
  // 2. Try pushing it to stack.
  // 3a. If successful, return.
  // 3b. Otherwise, try exchanging on elimination array.
  // 4a. If exchange failed to find a pop, retry 2.
  // 4b. Otherwise, record success and return.
  // 4c. If timeout ocurred, record it.
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

  // 1. Try popping a node from stack.
  // 2a. If successful, return node's value
  // 2b. Otherwise, try exchanging on elimination array.
  // 3a. If exchange failed to find a push, retry 1.
  // 3b. Otherwise, return paired push value.
  // 3c. If timeout occurred, record it.
  public T pop() throws EmptyStackException {
    RangePolicy p = policy.get();
    while (true) {
      Node<T> n = tryPop(); // 1
      if (n != null) return n.value; // 2a
      try {
      T y = eliminationArray.visit(null, p.range()); // 2b
      if (y == null) continue; // 3a
      p.onSuccess(); return y; // 3b
      }
      catch (TimeoutException e) { p.onTimeout(); } // 3c
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
