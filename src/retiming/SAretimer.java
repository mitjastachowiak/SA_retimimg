package retiming;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import scheduler.Graph;
import scheduler.Node;
import scheduler.Schedule;
import scheduler.Scheduler;


/**
 * This class extends the basic retimer to enable a simulated annealing approach for finding good retimings.
 * @author Mitja Stachowiak, Ludwig Meysel
 */
public class SAretimer extends Retimer {
  private final ArrayList<Node> randomSort;
  private int randomRepeats = 0; // increased whenever one randomSort is completely processed
  private int randomPos = 0;
  public float dirChangeInterval = 5f;
  public Scheduler scheduler = null; // if this value is set, the real length of the schedule is used as the cost-function
  
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
    nodeIterator: for (int j = 0; j < randomSort.size(); j++) {
      randomPos++;
      if (randomPos >= randomSort.size()) {
        randomPos = 0;
        randomRepeats++;
        mix();
      }
      Node n = randomSort.get(randomPos);
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
        n.prepend(e.getKey(), e.getValue() + 1);
      }
      // decrement weight of all outgoing edges
      HashMap<Node, Integer> out = n.allSuccessors();
      eit = out.entrySet().iterator();
      while (eit.hasNext()) {
        Entry<Node, Integer> e = eit.next();
        n.append(e.getKey(), e.getValue() - 1);
      }
    } else {
      // increment weight of all outgoing edges
      HashMap<Node, Integer> out = n.allSuccessors();
      Iterator<Entry<Node, Integer>> eit = out.entrySet().iterator();
      while (eit.hasNext()) {
        Entry<Node, Integer> e = eit.next();
        n.append(e.getKey(), e.getValue() + 1);
      }
      // decrement weight of all incoming edges
      HashMap<Node, Integer> in = n.allPredecessors();
      eit = in.entrySet().iterator();
      while (eit.hasNext()) {
        Entry<Node, Integer> e = eit.next();
        n.prepend(e.getKey(), e.getValue() - 1);
      }
    }
    /*
     * There is no need to add an exception for edges pointing to their own source node.
     * The weight of such edges simply gets increased and then decreased again.
     */
  }
  
  private int cost () {
    if (scheduler == null) return longestPath();
    else {
      Schedule schedule = scheduler.schedule(graph);
      if (schedule == null) return 0;
      return schedule.max();
    }
  }

  /**
   * Applies simulated annealing on the graph to minimize the longest path.
   */
  @Override
  public int[] retime(int quality) {
    int C = cost();
    int nCycles = 0;
    int startC = C;
    double T = C / 0.693147; // Init Temp: Accept a double of cost with 50% probability
    if (quality > 0) while (T > 0.1) {
      int nAccepted = 0;
      randomPos = randomSort.size(); // start with value > size, so that the randomRepeats is increased and mix() is called in first call of findRotatableNode
      randomRepeats = -1;
      float nextDirChange = 0;
      boolean dir = false;
      int nChanges = 0;
      while (randomRepeats < quality) {
        nChanges++;
        nCycles++;
        // check for dir change
        if (randomRepeats + randomPos/randomSort.size() >= nextDirChange) {
          if (Math.random() > 0.5) dir = true;
          else dir = false;
          nextDirChange += dirChangeInterval * Math.random();
        }
        // apply one rotation
        Node n = findRotatableNode(dir);
        if (n == null) {
          dir = !dir;
          n = findRotatableNode(dir);
          if (n == null) return new int[]{startC, C, nCycles}; // no rotations were possible!
        }
        rotateNode(n, dir);
        // calc new cost
        int newC = cost();
        double dC = newC - C;
        if (Math.random() < Math.exp(-dC/T)) {
          C = newC; // accept change
          nAccepted++;
        } else {
          rotateNode(n, !dir); // revert change
        }
      }
      // reduce T according to acceptance ratio
      double a = nAccepted / nChanges;
      if (a > 0.96) T *= 0.5;
      else if (a > 0.8) T *= 0.9;
      else if (a > 0.15) T *= 0.95;
      else T *= 0.8;
    }
    return new int[]{startC, C, nCycles};
  }

}
