package scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;


/**
 * Creates a list schedule of a graph.
 * @author Mitja Stachowiak, Ludwig Meysel
 */
public class ListScheduler extends Scheduler {
  public RC constraints = null;
  
  private static class TopologicalComparator implements Comparator<Node> {
    @Override
    public int compare(Node o1, Node o2) { return o1.getDepth() - o2.getDepth(); }
  }
  private static class PriorityComparator implements Comparator<Node> {
    @Override
    public int compare(Node o1, Node o2) { return o1.tmp1 - o2.tmp1; }
  }
  
  /**
   * Computes priorities for all nodes and stores them in Node.tmp1.
   * The priority value is the length of the longest path beyond a node.
   * @param g
   * the graph to be priorized
   * @return
   * returns an array of all nodes in the graph, sorted by their priority
   */
  private ArrayList<Node> setPriorities (Graph g) {
    if (!g.tmp1Used) throw new IllegalArgumentException("tmp1Used not set!");
    // create topologicalSort
    ArrayList<Node> topologicalSort = new ArrayList<Node>(g.size());
    Iterator<Node> it = g.iterator();
    while (it.hasNext()) topologicalSort.add(it.next());
    topologicalSort.sort(new TopologicalComparator());
    // set all priorities to zero
    for (int i = 0; i < topologicalSort.size(); i++) topologicalSort.get(i).tmp1 = 0;
    // inc priorities to their sub tree height
    for (int i = topologicalSort.size()-1; i >= 0; i--) {
      topologicalSort.get(i).tmp1 += topologicalSort.get(i).getDelay();
      Iterator<Entry<Node, Integer>> inIt = topologicalSort.get(i).allPredecessors().entrySet().iterator();
      while (inIt.hasNext()) {
        Entry<Node, Integer> e = inIt.next();
        if (e.getValue() != 0) continue; // ignore edges to other iterations
        if (e.getKey().tmp1 < topologicalSort.get(i).tmp1) e.getKey().tmp1 = topologicalSort.get(i).tmp1;
      }
    }
    topologicalSort.sort(new PriorityComparator());
    return topologicalSort;
  }

  @Override
  public Schedule schedule(Graph g) {
    if (constraints == null) throw new IllegalArgumentException("No resource constraints given!");
    if (g.tmp1Used) throw new IllegalArgumentException("tmp1 is already used by an other algorithm!");
    g.tmp1Used = true;
    // get list of unscheduled resources, sorted by priority
    ArrayList<Node> unscheduled = setPriorities(g);
    // get resource list
    String[] resNames = new String[constraints.getAllRes().size()];
    RT[][] resOps = new RT[constraints.getAllRes().size()][];
    int[] resBusy = new int[constraints.getAllRes().size()]; // init with 0 - list of cycles, when the resources will be availlable again
    Iterator<Entry<String, Set<RT>>> itRes = constraints.getAllRes().entrySet().iterator();
    int t = 0;
    while (itRes.hasNext()) {
      Entry<String, Set<RT>> e = itRes.next();
      resNames[t] = e.getKey();
      resOps[t] = new RT[e.getValue().size()];
      int i = 0;
      Iterator<RT> it = e.getValue().iterator();
      while (it.hasNext()) {
        resOps[t][i] = it.next();
        i++;
      }
      t++;
    }
    // prepare schedule
    Schedule schedule = new Schedule();
    t = 0; // current time slot
    while (unscheduled.size() > 0) {
      int nResFree = 0;
      int nOpPlaned = 0;
      for (int r = 0; r < resNames.length; r++) {
        if (resBusy[r] > t) continue; // resource is still busy in current time slot
        nResFree++;
        nodeSearch: for (int i = unscheduled.size()-1; i >= 0; i--) {
          Node n = unscheduled.get(i);
          // check weather all predecessors of n are processed
          Iterator<Entry<Node, Integer>> it = n.allPredecessors().entrySet().iterator();
          while (it.hasNext()) {
            Entry<Node, Integer> e = it.next();
            if (e.getValue() != 0) continue; // don't regard predecessors in different iterations
            if (e.getKey().tmp1 >= 0) continue nodeSearch; // this predecessor was not planned, n cannot be processed
            if (-1 - e.getKey().tmp1 > t) continue nodeSearch; // this predecessor is still executing, n cannot be processed
          }
          // check weather n can be processed by res[r]
          boolean found = false;
          for (int j = 0; j < resOps[r].length; j++) if (resOps[r][j].equals(n.getRT())) {
            found = true;
            break;
          }
          if (!found) continue nodeSearch; // unscheduled[i] cannot be processed by res[r]
          // plan node n
          schedule.add(n, new Interval(t, t+n.getDelay()), resNames[r]);
          resBusy[r] = t+n.getDelay();
          unscheduled.remove(i);
          n.tmp1 = -1 - t-n.getDelay(); // -1 - interval when n will be processed
          nOpPlaned++;
          break;
        }
      }
      if (nResFree == resNames.length && nOpPlaned == 0) return null; // cannot plan any operation. Maybe there is one resource type missing...
      t++;
    }
    g.tmp1Used = false;
    return schedule;
  }

}
