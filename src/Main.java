

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import sa_retiming.SAretimer;
import scheduler.ALAP;
import scheduler.ASAP;
import scheduler.Dot_reader;
import scheduler.Graph;
import scheduler.RC;
import scheduler.Schedule;
import scheduler.Scheduler;

public class Main {

  public static void main(String[] args) {
    // read dot file
    Graph g = null;
    if (args.length > 0) {
      Dot_reader dr = new Dot_reader(true);
      g = dr.parse(args[0]);
    } else throw new IllegalArgumentException("No input graph given!");
    
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
    
    SAretimer retimer = new SAretimer(g);
    
    retimer.retime();
    Scheduler scheduler = new ASAP();
    Schedule sched = scheduler.schedule(g);
    
    sched.draw("out.dot");
    
    

	/*	SASDC sasdc = new SASDC(rc, quality);
		sched = sasdc.schedule(g);
		System.out.printf("Cost (SA/SDC) = %s%n", sasdcCost = sched.cost());
		sched.draw("schedules/SASDC_" + fn);*/

	/*	File file = new File("benchmark.csv");
		try {
			boolean heading = !file.exists();
			FileWriter wtr = new FileWriter("benchmark.csv", true);
			if (heading)
				wtr.write(String.format("%s;%s;%s;%s;%s;%s;%s;%s%n", "File", "# Nodes", "ASAP", "ALAP", "SA/SDC", "Quality", "# iterations", "Runtime"));
			//wtr.write(String.format("%s;%s;%.2f;%.2f;%.2f;%s;%.0f;%.2f%n", fn, g.size(), asapCost, alapCost, sasdcCost, quality, sasdc.iterations, sasdc.elapsedTime));
			wtr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}
}
