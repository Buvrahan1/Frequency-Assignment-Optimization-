import java.util.*;

public class TabuSearch {

    public static class Result {
        public final int[] bestAssign;
        public final Cost.Score bestScore;
        public final long timeMs;

        public Result(int[] bestAssign, Cost.Score bestScore, long timeMs) {
            this.bestAssign = bestAssign;
            this.bestScore = bestScore;
            this.timeMs = timeMs;
        }
    }

    private final Random rnd;

    public TabuSearch(long seed) {
        this.rnd = new Random(seed);
    }

    public Result run(InstanceLoader.Instance ins,
                      int iterations,
                      int neighborSamples,
                      int tabuTenure,
                      int stagnationLimit,
                      double lambda) {

        long start = System.currentTimeMillis();

        int[] cur = randomInit(ins);
        Cost.Score curScore = Cost.evaluate(ins, cur, lambda);

        int[] best = cur.clone();
        Cost.Score bestScore = curScore;

        // Tabu: store for each (node index) the iteration until which it's tabu
        int[] tabuUntil = new int[ins.n];

        int stagnation = 0;

        for (int it = 1; it <= iterations; it++) {

            Candidate bestCand = null;

            for (int s = 0; s < neighborSamples; s++) {
                // single-move
                int u = rnd.nextInt(ins.n);
                int[] dom = ins.domainByIndex[u];

                if (dom.length <= 1) continue;

                int newFreq = dom[rnd.nextInt(dom.length)];
                int tries = 0;
                while (newFreq == cur[u] && tries++ < 5) {
                    newFreq = dom[rnd.nextInt(dom.length)];
                }
                if (newFreq == cur[u]) continue;

                int[] nxt = cur.clone();
                nxt[u] = newFreq;

                Cost.Score nxtScore = Cost.evaluate(ins, nxt, lambda);

                boolean isTabu = tabuUntil[u] > it; // node tabu
                boolean aspiration = nxtScore.total < bestScore.total; // allow tabu if improves global best

                if (isTabu && !aspiration) continue;

                if (bestCand == null || nxtScore.total < bestCand.score.total) {
                    bestCand = new Candidate(u, newFreq, nxt, nxtScore);
                }
            }

            if (bestCand == null) {
                // couldn't find admissible move, just continue cooling (or break)
                stagnation++;
            } else {
                cur = bestCand.assign;
                curScore = bestCand.score;

                // mark node as tabu
                tabuUntil[bestCand.u] = it + tabuTenure;

                if (curScore.total < bestScore.total) {
                    best = cur.clone();
                    bestScore = curScore;
                    stagnation = 0;
                } else {
                    stagnation++;
                }
            }

            if (stagnationLimit > 0 && stagnation >= stagnationLimit) break;
        }

        long end = System.currentTimeMillis();
        return new Result(best, bestScore, end - start);
    }

    private static class Candidate {
        int u;
        int newFreq;
        int[] assign;
        Cost.Score score;

        Candidate(int u, int newFreq, int[] assign, Cost.Score score) {
            this.u = u;
            this.newFreq = newFreq;
            this.assign = assign;
            this.score = score;
        }
    }

    private int[] randomInit(InstanceLoader.Instance ins) {
        int[] a = new int[ins.n];
        for (int i = 0; i < ins.n; i++) {
            int[] dom = ins.domainByIndex[i];
            a[i] = dom[rnd.nextInt(dom.length)];
        }
        return a;
    }
}
