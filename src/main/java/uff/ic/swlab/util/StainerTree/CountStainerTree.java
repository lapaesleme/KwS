/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uff.ic.swlab.util.StainerTree;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import mitre.Mitre;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.RDFDataMgr;
import util.Graph;

/**
 *
 * @author angelo
 */
public class CountStainerTree {

    public static Dataset ReadDataset(String filename) {
        Dataset dataset = RDFDataMgr.loadDataset(filename);
        return dataset;
    }

    public static ArrayList<String> getSG(Dataset dataset) {
        ArrayList<String> listSG = new ArrayList<>();
        String queryString = "SELECT DISTINCT ?sg\n"
                + "  			WHERE{\n"
                + "    			graph ?sg{\n"
                + "    				?s ?p ?o.\n"
                + "    			}\n"
                + "   			 	FILTER(regex(str(?sg),\"urn:graph:kws:[0-9]{3}:sol\"))\n"
                + "  			}";

        QueryExecution q = QueryExecutionFactory.create(queryString, dataset);
        ResultSet result = q.execSelect();
        while (result.hasNext()) {
            QuerySolution soln = result.nextSolution();
            String sgURI = String.valueOf(soln.get("sg"));
            listSG.add(sgURI);

        }
        q.close();
        return listSG;

    }

    public static List<String> getTerminalsSG(Dataset dataset, String sg) {
        List<String> terminals = new ArrayList<>();
        String queryString = String.format("SELECT ?seeds\n"
                + "  			WHERE{\n"
                + "      			graph <urn:graph:kws:groups>{\n"
                + "        			<%1$s> <http://www.w3.org/2000/01/rdf-schema#member> ?seeds.\n"
                + "      			}\n"
                + "			}\n"
                + "    		group by ?seeds", sg);

        QueryExecution q = QueryExecutionFactory.create(queryString, dataset);
        ResultSet result = q.execSelect();
        while (result.hasNext()) {
            QuerySolution soln = result.nextSolution();
            String sgURI = String.valueOf(soln.get("seeds"));
            terminals.add(sgURI);

        }
        q.close();
        return terminals;
    }

    public static Graph CreateGraphbySG(Dataset dataset, String sg) {
        Graph graph = new Graph();
        String queryString = String.format("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX urn: <http://fliqz.com/>\n"
                + "\n"
                + "SELECT DISTINCT ?s ?p ?o\n"
                + "WHERE{\n"
                + "  graph <%1$s>{\n"
                + "  		?s ?p ?o.\n"
                + "  }\n"
                + "  FILTER(!isLiteral(?o))\n"
                + "  		\n"
                + "}\n"
                + "\n"
                + "\n"
                + "\n"
                + "", sg);
        QueryExecution q = QueryExecutionFactory.create(queryString, dataset);
        ResultSet result = q.execSelect();
        while (result.hasNext()) {
            QuerySolution soln = result.nextSolution();
            String s = String.valueOf(soln.get("s"));
            String o = String.valueOf(soln.get("o"));
            graph.addEdge(s, o);

        }
        q.close();

        return graph;
    }
    
       public static void ExportCSV(HashMap<Integer, Integer> mapSTs, String nameDataset) throws FileNotFoundException {
        File folder = new File(String.format("./src/main/resources/draft/STs/Results/%1$s_result.csv", nameDataset));
        try ( PrintWriter writer = new PrintWriter(folder)) {
            StringBuilder sb = new StringBuilder();
            sb.append("query");
            sb.append(',');
            sb.append("Recall");
            sb.append('\n');

            for (Integer key : mapSTs.keySet()) {
                Integer recall = mapSTs.get(key);

                sb.append(key);
                sb.append(',');
                sb.append(recall);
                sb.append('\n');
            }

            writer.write(sb.toString());

        }

    }

    public static void main(String[] args) throws FileNotFoundException {
        String nameDataset = "Mondial";
        File folder = new File(String.format("./src/main/resources/benchmarks/ESWC2021/%1$s", nameDataset));
        File[] listOfFiles = folder.listFiles();
        Arrays.sort(listOfFiles);
        HashMap<Integer, Integer> mapST = new HashMap<>();
        
        Integer count = 1;
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].toString().endsWith(".nq.gz")) {
                System.out.println(count);
                Dataset dataset = ReadDataset(listOfFiles[i].toString());
                ArrayList<String> listSG = getSG(dataset);
                Integer total = 0;
                for (String sg : listSG) {
                    List<String> terminals = getTerminalsSG(dataset, sg);
                    Graph graph = CreateGraphbySG(dataset, sg);
                    List<Graph> steinerTrees = Mitre.execute(graph, terminals);
                    total = total + steinerTrees.size();
                }
                mapST.put(count, total);
                dataset.close();

            }
        }
        ExportCSV(mapST, nameDataset);

    }

}