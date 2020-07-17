import edu.princeton.cs.algs4.FlowEdge;
import edu.princeton.cs.algs4.FlowNetwork;
import edu.princeton.cs.algs4.FordFulkerson;
import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.ST;
import edu.princeton.cs.algs4.StdOut;

import java.util.ArrayList;
import java.util.List;

public class BaseballElimination {
    private final int numberOfTeams;
    private final ST<String, Integer> teamToID = new ST<>(); // ST with team name as key and intID as value
    private final ST<Integer, String> IDToTeam = new ST<>(); // ST with intID as key and team name as value
    private final int[] wins;
    private final int[] losses;
    private final int[] remaining;
    private final int[][] against;
    private int totalCapacity;

    private List<String> certificate; // the list of teams that eliminate another given team

    public BaseballElimination(String filename) {
        In instream = new In(filename);
        numberOfTeams = Integer.parseInt(instream.readLine());
        wins = new int[numberOfTeams];
        losses = new int[numberOfTeams];
        remaining = new int[numberOfTeams];
        against = new int[numberOfTeams][numberOfTeams];

        for (int i = 0; i < numberOfTeams; i++) {
            String team = instream.readString();
            teamToID.put(team, i);
            IDToTeam.put(i, team);
            wins[i] = instream.readInt();
            losses[i] = instream.readInt();
            remaining[i] = instream.readInt();
            for (int j = 0; j < numberOfTeams; j++) {
                against[i][j] = instream.readInt();
            }
        }
    }

    public int numberOfTeams() {
        return numberOfTeams; // number of teams
    }

    public Iterable<String> teams() {
        return teamToID.keys(); // all teams
    }

    public int wins(String team) {
        checkExceptions(team);
        int teamIndex = teamToID.get(team); // number of wins for given team
        return wins[teamIndex];
    }

    public int losses(String team) {
        checkExceptions(team);
        int teamIndex = teamToID.get(team); // number of losses for given team
        return losses[teamIndex];
    }

    public int remaining(String team) {
        checkExceptions(team);
        int teamIndex = teamToID.get(team); // number of remaining games for given team
        return remaining[teamIndex];
    }

    public int against(String team1, String team2) {
        checkExceptions(team1);
        checkExceptions(team2);
        int team1Index = teamToID.get(team1);
        int team2Index = teamToID.get(team2);
        return against[team1Index][team2Index]; // number of remaining games between team1 and team2

    }

    // is given team eliminated?
    public boolean isEliminated(String team) {
        checkExceptions(team);
        // trivial elimination
        for (int i = 0; i < numberOfTeams; i++) {
            if (wins[i] > wins[teamToID.get(team)] + remaining[teamToID.get(team)]) {
                return true;
            }
        }
        // nontrivial elimination
        int totalMatchups = numberOfTeams * (numberOfTeams - 1) / 2;
        FlowNetwork network = createNetwork(team);
        FordFulkerson fordFulkerson = new FordFulkerson(network, 0, totalMatchups + numberOfTeams + 1);
        return (totalCapacity != fordFulkerson.value());
    }

    private FlowNetwork createNetwork(String team) {
        checkExceptions(team);
        int teamIndex = teamToID.get(team);
        int totalMatchups = numberOfTeams * (numberOfTeams - 1) / 2; // # of matchups that don't include input team
        int matchupVertex = 1;
        int winnerVertex = 1 + totalMatchups;
        int sink = numberOfTeams + totalMatchups + 1;
        totalCapacity = 0;
        FlowNetwork flow = new FlowNetwork(numberOfTeams + totalMatchups + 2); // +2 for the source and sink matchupVertex
        for (int row = 0; row < against.length; row++) {
            for (int col = row + 1; col < against[0].length; col++) {
                // add edge between source and each matchup
                flow.addEdge(new FlowEdge(0, matchupVertex, against[row][col]));

                // add edge between each matchup and the first possible winner
                flow.addEdge(new FlowEdge(matchupVertex, row + totalMatchups + 1, Double.POSITIVE_INFINITY));

                // add edge between each matchup and the other possible winner
                flow.addEdge(new FlowEdge(matchupVertex, col + totalMatchups + 1, Double.POSITIVE_INFINITY));
                matchupVertex++;
                totalCapacity += against[row][col];
            }
            // add edge between the winner and the sink matchupVertex
            int capacity = 0;
            if (wins[teamIndex] + remaining[teamIndex] - wins[winnerVertex - totalMatchups - 1] > 0) {
                capacity = wins[teamIndex] + remaining[teamIndex] - wins[winnerVertex - totalMatchups - 1];
            }
            flow.addEdge(new FlowEdge(winnerVertex, sink, capacity));
            winnerVertex++;
        }
        return flow;
    }


    public Iterable<String> certificateOfElimination(String team) {
        checkExceptions(team);
        if (!isEliminated(team)) {
            return null;
        }
        certificate = new ArrayList<>();
        // trivial elimination
        for (int i = 0; i < numberOfTeams; i++) {
            if (wins[i] > wins[teamToID.get(team)] + remaining[teamToID.get(team)]) {
                certificate.add(IDToTeam.get(i));
                return certificate;
            }
        }

        // nontrivial elimination
        int totalMatchups = numberOfTeams * (numberOfTeams - 1) / 2;
        FlowNetwork network = createNetwork(team);
        FordFulkerson fordFulkerson = new FordFulkerson(network, 0, totalMatchups + numberOfTeams + 1);
        for (int i = 0; i < numberOfTeams; i++) {
            if (fordFulkerson.inCut(i + totalMatchups + 1)) {
                certificate.add(IDToTeam.get(i));
            }
        }
        return certificate;
    }

    private void checkExceptions(String team) {
        if (team == null) {
            throw new IllegalArgumentException();
        }

        if (!teamToID.contains(team)) {
            throw new IllegalArgumentException();
        }
    }

    public static void main(String[] args) {
        BaseballElimination division = new BaseballElimination(args[0]);
        for (String team : division.teams()) {
            if (division.isEliminated(team)) {
                StdOut.print(team + " is eliminated by the subset R = { ");
                for (String t : division.certificateOfElimination(team)) {
                    StdOut.print(t + " ");
                }
                StdOut.println("}");
            } else {
                StdOut.println(team + " is not eliminated");
            }
        }
    }
}

