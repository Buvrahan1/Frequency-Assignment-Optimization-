import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.*;

public class Runner {

    public static void main(String[] args) throws Exception {
    	
    	/*----------TP2'de kendi yazmış olduğumuz küçük örneği de çözebiliyor mu diye bakmak için oluşturuldu.-------------*/
    	
    	/*
    	System.out.println("====================================");
    	System.out.println("Phase 2 Example Scenario Test");

    	// ---- Build instance directly (no file parsing) ----
    	int n = 5;

    	// domains for each node: {1,2,3,4}
    	int[][] domainByIndex = new int[n][];
    	for (int i = 0; i < n; i++) domainByIndex[i] = new int[]{1,2,3,4};

    	// constraints (0-based indices): (u, v, separation)
    	List<InstanceLoader.Constraint> cons = new ArrayList<>();
    	cons.add(new InstanceLoader.Constraint(0, 1, 2));
    	cons.add(new InstanceLoader.Constraint(0, 2, 1));
    	cons.add(new InstanceLoader.Constraint(1, 3, 1));
    	cons.add(new InstanceLoader.Constraint(2, 3, 2));
    	cons.add(new InstanceLoader.Constraint(2, 4, 1));
    	cons.add(new InstanceLoader.Constraint(3, 4, 1));

    	// the first two constructor args depend on your Instance definition.
    	// In your Eclipse hint, it wants: (int[], Map<Integer,Integer>, int[][], List<Constraint>)
    	int[] dummyArr = new int[n]; // fill with zeros
    	Map<Integer,Integer> dummyMap = new HashMap<>();

    	InstanceLoader.Instance ex = new InstanceLoader.Instance(dummyArr, dummyMap, domainByIndex, cons);

    	// ---- Run Tabu Search ----
    	TabuSearch ts = new TabuSearch(42L);
    	var res = ts.run(ex, 10_000, 50, 5, 3_000, 1000.0);

    	System.out.println("Phase 2 result:");
    	System.out.println("n = " + ex.n);
    	System.out.println("bestDistinct = " + res.bestScore.distinct);
    	System.out.println("penalty      = " + res.bestScore.penalty);
    	System.out.println("timeMs       = " + res.timeMs);
    	System.out.println("====================================\n");

    	*/
    	
    	/*-----------------------------------------------------------------------------------------------------------------*/
    	
        // --- Instances (10 total) ---
        String[] instances = new String[] {
                "data/celar/scen01",
                "data/celar/scen03",
                "data/celar/scen05",
                "data/celar/scen08",
                "data/celar/scen10",
                "data/surprise/graph01",
                "data/surprise/graph03",
                "data/surprise/graph05",
                "data/surprise/graph08",
                "data/surprise/graph12"
        };

        // --- Parameters (same for all; later you can tune) ---
        
        int runsPerInstance = 5;
        
        double lambda = 100000.0;
        int iterations = 80_000;
        int neighborSamples = 120;
        int tabuTenure = 15;
        int stagnationLimit = 20_000;


        // CSV writers
        PrintWriter runCsv = new PrintWriter(new FileWriter("results_runs.csv"));
        PrintWriter sumCsv = new PrintWriter(new FileWriter("results_summary.csv"));

        // headers
        runCsv.println("dataset,instance,run,n,constraints,bestDistinct,penalty,total,timeMs,seed");
        sumCsv.println("dataset,instance,n,constraints,meanDistinct,bestDistinct,stdDistinct,meanTimeMs,bestTimeMs");

        for (String path : instances) {

            String dataset = path.contains("/celar/") ? "CELAR" : "SURPRISE";
            String instanceName = path.substring(path.lastIndexOf('/') + 1);

            System.out.println("==================================================");
            System.out.println("Loading " + dataset + " / " + instanceName + " ...");

            InstanceLoader.Instance ins = InstanceLoader.load(path);
            System.out.println("Loaded: n=" + ins.n + ", constraints=" + ins.constraints.size());

            ArrayList<Integer> distincts = new ArrayList<>();
            ArrayList<Long> times = new ArrayList<>();

            int bestDistinct = Integer.MAX_VALUE;
            long bestTime = Long.MAX_VALUE;

            for (int run = 1; run <= runsPerInstance; run++) {
            	long seed = 1000L * (long)(Math.abs(instanceName.hashCode())) + run;
                TabuSearch ts1 = new TabuSearch(seed);

                var res1 = ts1.run(ins, iterations, neighborSamples, tabuTenure, stagnationLimit, lambda);

                distincts.add(res1.bestScore.distinct);
                times.add(res1.timeMs);

                if (res1.bestScore.distinct < bestDistinct) bestDistinct = res1.bestScore.distinct;
                if (res1.timeMs < bestTime) bestTime = res1.timeMs;

                System.out.println("Run " + run
                        + " | bestDistinct=" + res1.bestScore.distinct
                        + " | penalty=" + res1.bestScore.penalty
                        + " | timeMs=" + res1.timeMs);

                runCsv.println(dataset + "," + instanceName + "," + run + ","
                        + ins.n + "," + ins.constraints.size() + ","
                        + res1.bestScore.distinct + ","
                        + res1.bestScore.penalty + ","
                        + res1.bestScore.total + ","
                        + res1.timeMs + ","
                        + seed);
                runCsv.flush();
            }

            Stats sd = statsInt(distincts);
            StatsL st = statsLong(times);

            // Print summary line
            System.out.println("SUMMARY " + dataset + "/" + instanceName
                    + " | meanDistinct=" + fmt(sd.mean)
                    + " | bestDistinct=" + sd.best
                    + " | stdDistinct=" + fmt(sd.std)
                    + " || meanTimeMs=" + fmt(st.mean)
                    + " | bestTimeMs=" + st.best);

            // Summary CSV
            sumCsv.println(dataset + "," + instanceName + ","
                    + ins.n + "," + ins.constraints.size() + ","
                    + fmt(sd.mean) + "," + sd.best + "," + fmt(sd.std) + ","
                    + fmt(st.mean) + "," + st.best);
            sumCsv.flush();
        }

        runCsv.close();
        sumCsv.close();

        System.out.println("\nDONE. Files created: results_runs.csv , results_summary.csv");
        System.out.println("Working dir: " + System.getProperty("user.dir"));
    }

    // ---------- stats helpers ----------
    static class Stats {
        double mean;
        int best;
        double std;
        Stats(double mean, int best, double std) { this.mean = mean; this.best = best; this.std = std; }
    }

    static class StatsL {
        double mean;
        long best;
        double std;
        StatsL(double mean, long best, double std) { this.mean = mean; this.best = best; this.std = std; }
    }

    static Stats statsInt(ArrayList<Integer> vals) {
        int n = vals.size();
        double sum = 0;
        int best = Integer.MAX_VALUE;
        for (int v : vals) { sum += v; if (v < best) best = v; }
        double mean = sum / n;

        double var = 0;
        for (int v : vals) var += (v - mean) * (v - mean);
        var /= n; // population std (note in report)
        double std = Math.sqrt(var);

        return new Stats(mean, best, std);
    }

    static StatsL statsLong(ArrayList<Long> vals) {
        int n = vals.size();
        double sum = 0;
        long best = Long.MAX_VALUE;
        for (long v : vals) { sum += v; if (v < best) best = v; }
        double mean = sum / n;

        double var = 0;
        for (long v : vals) var += (v - mean) * (v - mean);
        var /= n;
        double std = Math.sqrt(var);

        return new StatsL(mean, best, std);
    }

    static String fmt(double x) {
        return String.format(java.util.Locale.US, "%.3f", x);
    }
}
