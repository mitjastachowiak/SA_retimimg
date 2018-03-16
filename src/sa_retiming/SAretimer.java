package sa_retiming;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import scheduler.Graph;
import scheduler.Node;

public class SAretimer extends Retimer {
  private final ArrayList<Node> randomSort;
  
  public SAretimer (Graph graph) {
    super(graph);
    randomSort = new ArrayList<Node>(graph.size());
    Iterator<Node> it = graph.iterator();
    while (it.hasNext()) randomSort.add(it.next());
  }
  
  /**
   * randomly mixes the array randomSort
   */
  private void mix () {
    for (int i = randomSort.size(); i >= 2; i--) {
      int r = (int)(Math.random() * i);
      Node n = randomSort.get(i-1);
      randomSort.set(i-1, randomSort.get(r));
      randomSort.set(r, n);
    }
  }
  
  /**
   * Returns a randomly chosen node, which can be rotated in desired direction
   * @param rotDir
   * true, if node should be forward-rotated (into future), false otherwise
   * @return
   * the node, which can be rotated, null if no node can be rotated in given direction
   */
  private Node findRotatableNode (boolean rotDir) {
    mix();
    nodeIterator: for (int i = 0; i < randomSort.size(); i++) {
      Node n = randomSort.get(i);
      if (rotDir) {
        // check if node can be rotated into future
        Iterator<Integer> it = n.allSuccessors().values().iterator();
        while (it.hasNext()) if (it.next() < 1) continue nodeIterator; // node cannot be rotated into future
        return n;
      } else {
        // check if node can be rotated into past
        Iterator<Integer> it = n.allPredecessors().values().iterator();
        while (it.hasNext()) if (it.next() < 1) continue nodeIterator; // node cannot be rotated into past
        return n;
      }
    }
    return null;
  }
  
  /**
   * Applies a rotation on a given node.
   * @param n
   * the node to be rotated
   * @param rotDir
   * the direction of rotation (if true, node is rotated into future, if false into past)
   */
  private void rotateNode (Node n, boolean rotDir) {
    if (rotDir) {
      // increment weight of all incoming edges
      HashMap<Node, Integer> in = n.allPredecessors();
      Iterator<Entry<Node, Integer>> eit = in.entrySet().iterator();
      while (eit.hasNext()) {
        Entry<Node, Integer> e = eit.next();
        if (e.getKey() == n) continue; // edges to itself can become zero weight, if the out-map is not re-copied after in-map has changed
        n.prepend(e.getKey(), e.getValue() + 1);
      }
      // decrement weight of all outgoing edges
      HashMap<Node, Integer> out = n.allSuccessors();
      eit = out.entrySet().iterator();
      while (eit.hasNext()) {
        Entry<Node, Integer> e = eit.next();
        if (e.getKey() == n) continue; // edges to itself can become zero weight, if the out-map is not re-copied after in-map has changed
        n.append(e.getKey(), e.getValue() - 1);
      }
    } else {
      // increment weight of all outgoing edges
      HashMap<Node, Integer> out = n.allSuccessors();
      Iterator<Entry<Node, Integer>> eit = out.entrySet().iterator();
      while (eit.hasNext()) {
        Entry<Node, Integer> e = eit.next();
        if (e.getKey() == n) continue; // edges to itself can become zero weight, if the out-map is not re-copied after in-map has changed
        n.append(e.getKey(), e.getValue() + 1);
      }
      // decrement weight of all incoming edges
      HashMap<Node, Integer> in = n.allPredecessors();
      eit = in.entrySet().iterator();
      while (eit.hasNext()) {
        Entry<Node, Integer> e = eit.next();
        if (e.getKey() == n) continue; // edges to itself can become zero weight, if the out-map is not re-copied after in-map has changed
        n.prepend(e.getKey(), e.getValue() - 1);
      }
    }
  }

  @Override
  public void retime() {
    double C = longestPath();
    double T = C / 0.693147; // Init Temp: Accept a double of cost with 50% probability
    while (T > 0.1) {
      for (int i = 0; i < graph.size(); i++) {
        // apply one rotation
        boolean dir = false;
        if (Math.random() > 0.5) dir = true;
        Node n = findRotatableNode(dir);
        if (n == null) {
          dir = !dir;
          n = findRotatableNode(dir);
          if (n == null) return; // no rotations were possible!
        }
        rotateNode(n, dir);
        if (dir) System.out.println(n+" rotated into future."); else System.out.println(n+" rotated into past.");
        // calc new cost
        double newC = longestPath();
        double dC = newC - C;
        if (Math.random() < Math.exp(-dC/T)) {
          C = newC; // accept change
          System.out.println(" ... accepted!  Cost = "+C);
        } else {
          rotateNode(n, !dir); // revert change
          System.out.println(" ... reverted!");
        }
      }
      T = T * 0.9;
    }
  }

}
