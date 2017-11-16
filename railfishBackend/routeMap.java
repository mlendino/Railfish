package edu.cooper.hack.railfish.backend;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

import static edu.cooper.hack.railfish.backend.Station.st;

//backend for Facebook Global Hackathon Finals 2016: railfish (essentially a 'waze' for the midtown portion of the NYC subway system.
//operates at a higher precision than traditional block signaling based reporting

//defining the region of interest for mapping
public class RouteMap {

    static Line[] lines;
    //takes in parameters in which the type of keys maintained by this map are strings, and the type of mapped value are integers
    static HashMap<String, Integer> transferTimes = new HashMap<>();
    //similar to the above declaration, except the type of mapped value is a station (defined in another file)
    static HashMap<String, Station> stations = new HashMap<>();
    //defining each line in our ROI (lexington or "the 6", canarsie or "the L" etc...) as being constituted of various stops (such as astor, union sq etc..)
    //each of which have been renamed (i.e. Spring St has been renamed LEXSPRING)
    static Line lexington = new Line("LEX", st("LEXSPRING", "Spring St", "6"), st("LEXBLEECKER", "Bleecker St",  "6"),
            st("LEXASTOR", "Astor Pl",  "6"), st("LEX14", "14th St. Union Sq", "4", "5", "6"), st("LEX23", "23rd St", "6"),
            st("LEX28", "28th St", "6"), st("LEX33", "33rd St", "6"), st("LEX42", "Grand Central 42nd St", "4", "5", "6"));
    static Line canarsie = new Line("CAN", st("CAN8", "8th Av", "L"), st("CAN6", "6th Av", "L"), st("CANUNION", "Union Sq", "L"));
    static Line eighth = new Line("8TH", st("8THSPRING", "Spring St", "C", "E"), st("8THW4", "West 4th St. Washington Sq", "A", "C", "E"),
            st("8TH14", "14th St", "A", "C", "E"), st("8TH23", "23rd St", "C", "E"),
            st("8TH34", "34th St. Penn Stn", "A", "C", "E"), st("8TH42", "Times Sq. 42nd St", "A", "C", "E"), st("8TH50", "5th St", "C", "E"));
    static Line seventh = new Line("7TH", st("7THHOUSTON", "Houston St", "1"), st("7THCHRISTOPHER", "Christopher St. Sheridan Sq", "1"),
            st("7TH14", "14th St", "1", "2", "3"), st("7TH18", "18th St", "1"),
            st("7TH23", "23rd St", "1"), st("7TH28", "28th St", "1"), st("7TH34", "34th St. Penn Stn", "1", "2", "3"), st("7TH42", "Times Sq. 42nd St", "1", "2", "3"),
            st("7TH50", "50th St", "1"));
    static Line sixth = new Line("6TH", st("6THLAFAYETTE", "Broadway-Lafayette St", "B", "D", "F", "M"), st("6THW4", "West 4th St, Washington Sq", "B", "D", "F", "M"),
            st("6TH14", "14th St", "F", "M"), st("6TH23", "23rd St", "F", "M"), st("6TH34", "34th St. Herald Sq", "B", "D", "F", "M"),
            st("6TH42", "42nd St. Bryant Park", "B", "D", "F", "M"), st("6TH47", "47th-50th Sts. Rockefeller Ctr", "B", "D", "F", "M"));
    static Line broadway = new Line("BWY", st("BWYPRINCE", "Prince St", "R", "W"), st("BWY8", "8th St. NYU", "R", "W"),
            st("BWY14", "14th St. Union Sq", "N", "Q", "R", "W"), st("BWY23", "23rd St", "R", "W"),
            st("BWY28", "28th St", "R", "W"), st("BWY34", "34th St. Herald Sq", "N", "Q", "R", "W"), st("BWY42", "Times Sq. 42nd St", "N", "Q", "R", "W"),
            st("BWY49", "49th St", "R", "W"));
    static Line flushing = new Line("FLU", st("FLU34", "34th St. Hudson Yds", "7"), st("FLUTIMES", "Times Sq", "7"), st("FLU5", "5th Av", "7"),
            st("FLUGRAND", "Grand Central","7"));
    static Line shuttle = new Line("SHU", st("SHUTIMES", "Times Sq", "S"), st("SHUGRAND", "Grand Central", "S"));

    private static HashSet<String> sameTrackTransfers = new HashSet<>();

    static NodeSet nst = new NodeSet();
    static HashMap<String, String[]> multisets = new HashMap<>();
    //defining all of the possible locations of transfer (i.e. at LEX42, one could transfer to the shuttle (SHUGRAND) or the 7 (FLUGRAND))
    //transfers also include the time taken to walk the transfer from platform to platform
    static {
        lines = new Line[]{lexington, canarsie, eighth, seventh, sixth, broadway, flushing, shuttle};
        buildMaps();

        transfer("LEX14", "CANUNION");
        transfer("LEXBLEECKER", "6THLAFAYETTE", 7);
        transfer("LEX42", "SHUGRAND");
        transfer("LEX42", "FLUGRAND");
        transfer("6TH42", "FLU5", 10);
        transfer("6TH14", "CAN6");
        transfer("CAN6", "6TH14");
        transfer("7TH42", "BWY42");
        transfer("7TH42", "SHUTIMES");
        transfer("7TH42", "FLUTIMES");
        transfer("7TH14", "CAN6", 7);
        transfer("BWY42", "SHUTIMES");
        transfer("BWY42", "FLUTIMES");
        transfer("BWY34", "6TH34");
        transfer("BWY14", "LEX14");
        transfer("BWY14", "CANUNION");
        transfer("8THW4", "6THW4");
        transfer("8TH14", "CAN8");
        transfer("8TH42", "7TH42", 10);
        transfer("8TH42", "BWY42", 10);
        transfer("8TH42", "SHUTIMES", 10);
        transfer("8TH42", "FLUTIMES", 10);
        transfer("SHUTIMES", "FLUTIMES");
        transfer("SHUGRAND", "FLUGRAND");

        multiset("COOPER", "BWY80", "LEXASTOR0");
        multiset("PENN", "8TH340", "7TH340");
        multiset("UNIONSQ", "LEX140", "BWY140", "CANUNION0");
        multiset("14TH8TH", "8TH140", "CAN80");
        multiset("WASHSQ", "6THW40", "8THW40");
        multiset("GCT", "LEX420", "FLUGRAND0", "SHUGRAND0");
        multiset("HERALDSQ", "6TH340", "BWY340");
        multiset("TIMESSQ", "7TH420", "BWY420", "SHUTIMES0", "FLUTIMES0");
        multiset("14TH6TH", "6TH140", "CAN60");
        multiset("LAFLEX", "6THLAFAYETTE0", "LEXBLEECKER0");

        sameTrackTransfers.add("45");
        sameTrackTransfers.add("54");
        sameTrackTransfers.add("CE");
        sameTrackTransfers.add("EC");
        sameTrackTransfers.add("23");
        sameTrackTransfers.add("32");
        sameTrackTransfers.add("BD");
        sameTrackTransfers.add("DB");
        sameTrackTransfers.add("FM");
        sameTrackTransfers.add("MF");
        sameTrackTransfers.add("NQ");
        sameTrackTransfers.add("QN");
        sameTrackTransfers.add("RW");
        sameTrackTransfers.add("WR");
        buildNodeSet();
        //printSummary();
    }

    //assigns the value of string placename and stations
    private static void multiset(String placename, String... stations) {
        multisets.put(placename, stations);
    }

    
    static void printSummary() {
        System.out.println("===PRINTING ROUTE SUMMARY===");
        for (Line l : lines) {
            //prints out name of line
            System.out.println(String.format("%s line:", l.name));
            //prints out number of strops
            System.out.println(String.format("%d stop(s):", l.stations.length));
            for (Station s : l.stations) {
                //printed out transfers and route info
                System.out.println(String.format("* %s (%s)%s", s.name, StringUtils.join(s.routes, '•'),
                        s.transfers.isEmpty() ? "" : ", transfer to " + StringUtils.join(s.transfers.stream().map(t -> t.name).toArray(), ", ")));
            }
            //newlines
            System.out.println();
            System.out.println();
        }
    }
    //parsing through the array and putting the station name for each station on each line
    static void buildMaps() {
        for (Line l : lines) {
            for (Station s : l.stations) {
                stations.put(s.name, s);
            }
        }
    }
    
    public static void main(String[] args) {
        printSummary();
        List<NodeSet.Node> path = pathfind("COOPER", "PENN");
        System.out.println(evalCost(path) + ":" + path);
        //prints out getJSON (see line 170)
        System.out.println(getJSON("PENN", "COOPER"));
        System.out.println(getJSON("UNIONSQ", "FLUGRAND0"));
        NodeSet.costOverrides.put("BWY14R-BWY23R", 4000);
        NodeSet.costOverrides.put("BWY14W-BWY23W", 4000);
        NodeSet.costOverrides.put("BWY14Q-BWY34Q", 4000);
        NodeSet.costOverrides.put("BWY14N-BWY34N", 4000);
        System.out.println(getJSON("BWY140", "BWY280"));
    }
    //evaluating the cost in traversing from the source node to the destination node
    static List<NodeSet.Node> pathfind(String src, String dst) {
        String[] srcs = multisets.getOrDefault(src, new String[]{src});
        String[] dsts = multisets.getOrDefault(dst, new String[]{dst});
        double bestCost = Double.MAX_VALUE;
        List<NodeSet.Node> best = null;
        for (String src_ : srcs) {
            for (String dst_ : dsts) {
                List<NodeSet.Node> candidate = nst.pathfind(nst.allNodes.get(src_), nst.allNodes.get(dst_));
                double cost = evalCost(candidate);
                if (cost < bestCost) {
                    bestCost = cost;
                    best = candidate;
                }
            }
        }
        return best;
    }
   
    static String getJSON(String src, String dst) {
        try {
            dst = dst.trim();
            src = src.trim();
            //this List can only have src nodes and dst nodes inserted into it
            List<NodeSet.Node> path = RouteMap.pathfind(src, dst);
            StringBuilder json = new StringBuilder();
            json.append("{\"result\":\"ok\", \"error\":\"\", \"path\" : [");
            ArrayList<String> elements = new ArrayList<>();
            
            for (int i = 1; i < path.size(); i++) {
                //constructing the segment of traversal by incrementing through the previous and current node until 
                //one has traversed the length of the path
                NodeSet.Node prev = path.get(i - 1);
                NodeSet.Node cur = path.get(i);
                elements.add(toSegment(prev, cur));
            }
            json.append(StringUtils.join(elements, ","));
            json.append("], \"instructions\": [");
            //creating arraylist 'instructions'
            ArrayList<String> instructions = new ArrayList<>();
            String lastService = "";
            int stopCount = 0;
            int ptr = 0;
            boolean haveBoarded = false;
            //increment ptr until ptr is greater than the size of the path
            while (ptr < path.size() && path.get(ptr).service.equals("0")) {
                ptr++;
            }
            //adding to the array list the specific instructions needed for the individual to get from src to dst
            //
            instructions.add(String.format("\"Board the %s <span class=\\\"bullet %s\\\">%s</span> train at %s.\"",
                    resolveDirection(path.get(ptr), path.get(ptr + 1)),
                    "b"+path.get(ptr).stationRef.line.name.toLowerCase(),
                    path.get(ptr).service, path.get(ptr).stationRef.desc
            ));
            lastService = path.get(ptr).service;
            while(!Objects.equals(path.get(ptr).service, "0")) {

                int count = 0;
                while (path.get(++ptr).service.equals(lastService)) {
                    count++;
                }
                if(count>0) instructions.add(String.format("\"Ride %d %s to %s.\"", count, (count==1)?"station":"stations", path.get(ptr).stationRef.desc));
                lastService = path.get(ptr).service;
                if(ptr<(path.size()-1)){
                    if(!path.get(ptr+1).service.equals("0")){
                        //implementing resolveDirection with two elements ptr and ptr+1, the line name at ptr
                        //and adding a 'b' in the front, the service and desc parameter of the element at address ptr
                        instructions.add(String.format("\"Transfer to the %s <span class=\\\"bullet %s\\\">%s</span> train at %s.\"",
                                resolveDirection(path.get(ptr), path.get(ptr + 1)),
                                "b"+path.get(ptr).stationRef.line.name.toLowerCase(),
                                path.get(ptr).service, path.get(ptr-1).stationRef.desc
                        ));
                    }
                }
            }
            json.append(StringUtils.join(instructions, ","));

            json.append("]}");
            //converting json object to string
            return json.toString();
        } catch (Exception e) {
            //creating and formatting error messages
            return String.format("{\"result\":\"error\", \"error\":\"%s\"}",
                    StringEscapeUtils.escapeJavaScript(e.toString() + ":" + e.getMessage() + "//" +
                            StringUtils.join(Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).toArray(), "//")));
        }
    }
    //constructing convention given the relative positions of an src node and dst node and returns conventional names
    private static String resolveDirection(NodeSet.Node node, NodeSet.Node node2) {
        if (node.stationRef.line.name.equals("FLU")) {
            //returing the parameter of the node of the given station
            return (node.stationRef.ordinal < node2.stationRef.ordinal) ? "34 St Hudson Yards bound" : "Queens bound";
        } else if (node.stationRef.line.name.equals("SHU")) {
            return (node.stationRef.ordinal < node2.stationRef.ordinal) ? "Times Sq bound" : "Grand Central bound";
        } else if (node.stationRef.line.name.equals("CAN")) {
            return (node.stationRef.ordinal < node2.stationRef.ordinal) ? "Brooklyn bound" : "8th Av bound";
        } else {
            return (node.stationRef.ordinal < node2.stationRef.ordinal) ? "uptown" : "downtown";
        }
    }

    static String toSegment(NodeSet.Node prev, NodeSet.Node cur) {
        boolean rev = false;
        //if there exists a previous station and a current station and theyre on the same line
        if (prev.stationRef != null && cur.stationRef != null && prev.stationRef.line == cur.stationRef.line) {
            //if the origin of src station is greater than the origin of dst station, traverse each intermediate node
            //until you have reached the location of the dst station
            if (prev.stationRef.ordinal > cur.stationRef.ordinal) {
                NodeSet.Node temp = cur;
                cur = prev;
                prev = temp;
                rev = true;
            } 
        } else if (prev.stationRef != null && cur.stationRef != null) {
            if (prev.stationRef.line.name.compareTo(cur.stationRef.line.name) > 0) {
                NodeSet.Node temp = cur;
                cur = prev;
                prev = temp;
            }
        }
        //combining everything into one statement for ease of access later on
        return "\"" + prev.name + "-" + cur.name + "-" + (cur.service.equals(prev.service) ? cur.service : "X") + (rev?"_\"":"\"");
    }
    //evaluating/adding the cost of a given src or dst since those are the only instances that can be inserted into this list
    private static double evalCost(List<NodeSet.Node> candidate) {
        double sum = 0;
        for (int i = 1; i < candidate.size(); i++) {
            //constantly adding up the cost of each candidate and the previoous candidate to arrive at a cost
            sum += nst.calcCost(candidate.get(i - 1), candidate.get(i));
        }
        return sum;
    }
    //listing the times for transfer between two stations like at an airport
    static void transfer(String st1, String st2, int time) {
        transferTimes.put(st1 + "-" + st2, time);
        transferTimes.put(st2 + "-" + st1, time);
        stations.get(st1).transfers.add(stations.get(st2));
        stations.get(st2).transfers.add(stations.get(st1));
    }

    static void transfer(String st1, String st2) {
        transfer(st1, st2, 5);
    }


    static void buildNodeSet() {
        for (Line l : lines) {
            // front to back walk
            HashSet<String> services = new HashSet<>();
            //for every station in a line of stations 
            for (Station s : l.stations) {
                //and for every string svc in the station routes
                for (String svc : s.routes) {
                    //adding string svc to services
                    services.add(svc);
                }
            }
            //for every string in services
            for (String svc : services) {
                System.err.println("Building the linkages for " + svc);
                //emptying prev and cur nodes (we will assign them values below)
                NodeSet.Node prev = null;
                NodeSet.Node cur = null;
                //for every station in the line of stations
                for (Station s : l.stations) {
                    //refer to other file for object and method declarations of anything not present
                    //if theres a match for what the method stream returns, execute loop
                    if (Arrays.stream(s.routes).anyMatch(pSvc -> pSvc.equals(svc))) {

                        boolean isBMT = Character.isAlphabetic(svc.charAt(0));
                        //setting values of previous and current nodes
                        prev = cur;
                        cur = new NodeSet.Node();
                        //parameter nameAndService takes on the value of the station name and svc
                        //this is done for every svc
                        cur.nameAndService = s.name + svc;
                        System.err.println("Storing " + cur.nameAndService);
                        //copying the information from the s station, svc service, 
                        //and setting the address equal to the current station 
                        cur.name = s.name;
                        cur.service = svc;
                        cur.stationRef = s;
                        nst.allNodes.put(cur.nameAndService, cur);
                        if (prev != null) {
                            //prev.neighbours.add(cur);
                            //cur.neighbours.add(prev);
                            prev.nextOnLine = cur;
                            cur.prevOnLine = prev;
                        }
                    }
                }
            }
        } //crossLinkStations described below
        for (Line l : lines) {
            for (Station s : l.stations) {
                crossLinkStations(s, s);
            }
        }
        for (Line l : lines) {
            for (Station s : l.stations) {
                for (Station txd : s.transfers) {
                    crossLinkStations(s, txd);
                }
            }
        }

        for (Line l : lines) {
            for (Station s : l.stations) {
                //going through every line in line and every station in station lines and creating a new node
                //putting it into allNodes to organize so it can be called later
                NodeSet.Node stationNode = new NodeSet.Node();
                stationNode.name = s.name;
                stationNode.service = "0";
                stationNode.nameAndService = s.name + "0";
                stationNode.stationRef = s;
                nst.allNodes.put(s.name + "0", stationNode);
                //calling already created node and placing it into neighbours and parameter nd
                //adding new node to stationNode
                for (String service : s.routes) {
                    NodeSet.Node nd = nst.allNodes.get(s.name + service);
                    nd.neighbours.add(stationNode);
                    stationNode.neighbours.add(nd);
                }
            }
        }
    }
    //takes in two stations as its parameters and joins adjacent stations
    private static void crossLinkStations(Station s1, Station s2) {
        //goes through all the station 1 routes and all the station 2 routes and making them neighbours
        for (String sv1 : s1.routes) {
            for (String sv2 : s2.routes) {
                NodeSet.Node n1 = nst.allNodes.get(s1.name + sv1);
                NodeSet.Node n2 = nst.allNodes.get(s2.name + sv2);
                n1.neighbours.add(n2);
                n2.neighbours.add(n1);
            }
        }
    }


    static boolean isSameTrack(String sv, String sv2) {
        String c1 = sv + sv2;
        return (sameTrackTransfers.contains(c1));
    }


}
