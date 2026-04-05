import java.util.*;

public class Cost {

    public static class Score {
        public final int distinct;
        public final long penalty;
        public final double total;

        public Score(int distinct, long penalty, double total) {
            this.distinct = distinct;
            this.penalty = penalty;
            this.total = total;
        }
    }

    public static Score evaluate(InstanceLoader.Instance ins, int[] assign, double lambda) {
        int distinct = distinctCount(assign);
        long penalty = penalty(ins, assign);
        double total = distinct + lambda * (double) penalty;
        return new Score(distinct, penalty, total);
    }

    public static int distinctCount(int[] assign) {
        // faster than HashSet for big n: use IntOpenHashSet would be nice,
        // but keep it standard:
        HashSet<Integer> set = new HashSet<>();
        for (int v : assign) set.add(v);
        return set.size();
    }

    public static long penalty(InstanceLoader.Instance ins, int[] assign) {
        long sum = 0;
        for (InstanceLoader.Constraint c : ins.constraints) {
            int diff = Math.abs(assign[c.i] - assign[c.j]);
            int v = c.d - diff;
            if (v > 0) sum += v;
        }
        return sum;
    }
}
