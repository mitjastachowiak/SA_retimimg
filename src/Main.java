import java.io.File;
import java.io.FilenameFilter;

import retiming.SAretimer;
import scheduler.Dot_reader;
import scheduler.Graph;
import scheduler.ListScheduler;
import scheduler.RC;
import scheduler.Schedule;

public class Main {
  public static void main(String[] args) {
    // prepare input file list
    File[] inFiles = null;
    if (args.length == 0) throw new IllegalArgumentException("No input file or folder given!");
    else {
      File f = new File(args[0]);
      if (!f.exists()) throw new IllegalArgumentException("Input file does not exist!");
      if (f.isDirectory()) inFiles = f.listFiles(new FilenameFilter() { public boolean accept(File dir, String name) { return name.toLowerCase().endsWith(".dot"); }});
      else inFiles = new File[] {f};
    }
    
    // read resource constraints
    RC constraints = null;
    if (args.length > 1) {
      constraints = new RC();
      constraints.parse(args[1]);
    } else throw new IllegalArgumentException("No resource constraints given!");
    
    // read SA-quality
    int quality = 0;
    if (args.length > 2) {
      quality = Integer.parseInt(args[2]);
    } else throw new IllegalArgumentException("No quality value given!");
    
    // read output directory
    File outputDir;
    if (args.length > 3) {
      outputDir = new File(args[3]);
      if (!outputDir.exists()) throw new IllegalArgumentException("Output path doesn't exist!");
      if (!outputDir.isDirectory()) throw new IllegalArgumentException("Output path is not a directory!");
    } else throw new IllegalArgumentException("No output path given!");
    
    // read scheduleAsCost
    boolean scheduleAsCost = false;
    if (args.length > 4 && args[4].equalsIgnoreCase("scheduleascost")) scheduleAsCost = true;
    
    // process files
    System.out.println("nodes\tbefore\tafter\tsched\tcycles\tfile name");
    System.out.println("-------------------------------------------");
    for (int i = 0; i < inFiles.length; i++) processFile(inFiles[i], outputDir, constraints, quality, scheduleAsCost);
  }
  
  
  private static void processFile (File inFile, File outDir, RC constraints, int quality, boolean scheduleAsCost) {
    // read dot file
    Dot_reader dr = new Dot_reader(true);
    Graph g = dr.parse(inFile.getAbsolutePath());
    
    // prepare scheduler
    ListScheduler scheduler = new ListScheduler();
    scheduler.constraints = constraints;

    // do retiming
    SAretimer retimer = new SAretimer(g);
    if (scheduleAsCost) retimer.scheduler = scheduler;
    int[] cost = retimer.retime(quality);
        
    // schedule
    Schedule sched = scheduler.schedule(g);
    if (sched == null) System.out.println("Cannot schedule "+inFile.getName()+"!");
    else sched.draw(outDir.getAbsolutePath() + "/" + inFile.getName());
    
    // print information
    if (sched != null) System.out.println(g.size() + "\t" + cost[0] + "\t" + cost[1] + "\t" + sched.max() + "\t" + cost[2] + "\t" + inFile.getName());
  }
}
