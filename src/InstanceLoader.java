import java.io.*;
import java.nio.file.*;
import java.util.*;

public class InstanceLoader {

    public static class Instance {
        public final int n;
        public final int[] indexToNodeId;
        public final Map<Integer, Integer> nodeIdToIndex;
        public final int[][] domainByIndex; // allowed freqs
        public final List<Constraint> constraints;

        public Instance(int[] indexToNodeId,
                        Map<Integer, Integer> nodeIdToIndex,
                        int[][] domainByIndex,
                        List<Constraint> constraints) {
            this.indexToNodeId = indexToNodeId;
            this.nodeIdToIndex = nodeIdToIndex;
            this.domainByIndex = domainByIndex;
            this.constraints = constraints;
            this.n = indexToNodeId.length;
        }
    }

    public static class Constraint {
        public final int i, j;
        public final int d;

        public Constraint(int i, int j, int d) {
            this.i = i;
            this.j = j;
            this.d = d;
        }
    }

    public static Instance load(String folderPath) throws IOException {
        Path folder = Paths.get(folderPath);
        Path domPath = folder.resolve("DOM.TXT");
        Path varPath = folder.resolve("VAR.TXT");
        Path ctrPath = folder.resolve("CTR.TXT");

        Map<Integer, int[]> domainIdToFreqs = parseDOM(domPath);
        Map<Integer, Integer> nodeToDomainId = parseVAR(varPath);

        // index mapping (node ids can be like 185.. etc)
        List<Integer> nodeIds = new ArrayList<>(nodeToDomainId.keySet());
        Collections.sort(nodeIds);

        int n = nodeIds.size();
        int[] indexToNodeId = new int[n];
        Map<Integer, Integer> nodeIdToIndex = new HashMap<>(n * 2);

        for (int idx = 0; idx < n; idx++) {
            int nodeId = nodeIds.get(idx);
            indexToNodeId[idx] = nodeId;
            nodeIdToIndex.put(nodeId, idx);
        }

        int[][] domainByIndex = new int[n][];
        for (int idx = 0; idx < n; idx++) {
            int nodeId = indexToNodeId[idx];
            int domainId = nodeToDomainId.get(nodeId);
            int[] freqs = domainIdToFreqs.get(domainId);
            if (freqs == null) {
                throw new IllegalStateException("DOM.TXT missing domainId=" + domainId + " for nodeId=" + nodeId);
            }
            domainByIndex[idx] = freqs;
        }

        List<Constraint> constraints = parseCTR(ctrPath, nodeIdToIndex);

        return new Instance(indexToNodeId, nodeIdToIndex, domainByIndex, constraints);
    }

    private static Map<Integer, int[]> parseDOM(Path domPath) throws IOException {
        Map<Integer, int[]> map = new HashMap<>();
        for (String raw : Files.readAllLines(domPath)) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            String[] tok = line.split("\\s+");
            if (tok.length < 3) continue;

            int domainId = Integer.parseInt(tok[0]);
            int k = Integer.parseInt(tok[1]);

            if (tok.length < 2 + k) {
                throw new IllegalStateException("DOM bad line: " + line);
            }

            int[] freqs = new int[k];
            for (int i = 0; i < k; i++) {
                freqs[i] = Integer.parseInt(tok[2 + i]);
            }
            map.put(domainId, freqs);
        }
        return map;
    }

    private static Map<Integer, Integer> parseVAR(Path varPath) throws IOException {
        Map<Integer, Integer> map = new HashMap<>();
        for (String raw : Files.readAllLines(varPath)) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            String[] tok = line.split("\\s+");

            // header lines like "1" -> skip
            if (tok.length == 1) continue;

            int nodeId = Integer.parseInt(tok[0]);
            int domainId = Integer.parseInt(tok[1]);
            map.put(nodeId, domainId);
        }
        return map;
    }

    private static List<Constraint> parseCTR(Path ctrPath, Map<Integer, Integer> nodeIdToIndex) throws IOException {
        List<Constraint> list = new ArrayList<>();
        for (String raw : Files.readAllLines(ctrPath)) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            String[] tok = line.split("\\s+");

            // header like "84" -> skip
            if (tok.length == 1) continue;

            int nodeI = Integer.parseInt(tok[0]);
            int nodeJ = Integer.parseInt(tok[1]);

            Integer i = nodeIdToIndex.get(nodeI);
            Integer j = nodeIdToIndex.get(nodeJ);
            if (i == null || j == null) continue; // safety

            int d = Integer.parseInt(tok[tok.length - 1]); // last token is number
            list.add(new Constraint(i, j, d));
        }
        return list;
    }
}
