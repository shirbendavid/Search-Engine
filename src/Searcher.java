import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Searcher {
    Parse parse;
    Indexer indexer;
    Ranker ranker;
    Semantic semantic;
    boolean isSemanticOn;
    boolean isStemmerOn;
    TreeMap<String,TreeMap<Double, String>> docsForQueries;
    String pathToSave;
    HashMap<String, String[]> relevantDocs;
    static int queryID=1;

    /**
     * constructor
     */
    public Searcher(boolean stemming, boolean semanticOn, String save,Indexer index, HashSet<String> stopWords) throws IOException {
        isStemmerOn= stemming;
        isSemanticOn = semanticOn;
        if(semanticOn)
            semantic= new Semantic();
        indexer = index;
        parse = new Parse(stopWords, stemming, indexer, indexer.tempPostingPath);
        ranker = new Ranker(indexer);
        docsForQueries = new TreeMap<>();
        relevantDocs= new HashMap<>();
        pathToSave = save;
        if(!pathToSave.equals("")){
            File file = new File(pathToSave+"\\results.txt");
            if(file.exists())
                file.delete();
        }
    }
    /**
     *read one query or file of queries and send to parse
     *after parse send the terms to ranker
     * @param singleQuery- if one query is true else false
     * @param query- one query or path of file termsQuery
     */
    public void readQuery(boolean singleQuery, String query )throws IOException {
        if (singleQuery) {
            String queryNum;
            if(queryID<10)
                queryNum="00"+queryID;
            else if(queryID<100)
                queryNum="0"+queryID;
            else
                queryNum=String.valueOf(queryID);
            parse.readTerms(queryNum, query, false);
            relevantDocsForQuery(queryNum, parse.getTermsQuery());
            queryID++;
        } else {
            File queryFile = new File(query);
            if (queryFile.isFile()) {
                Document htmlQuery = Jsoup.parse(queryFile, "UTF-8");
                Elements queries = htmlQuery.getElementsByTag("top");
                for (Element q : queries) {
                    String queryNumber = q.getElementsByTag("num").text().split(" ")[1];
                    String text = q.getElementsByTag("title").text();
                    String textDesc = q.getElementsByTag("desc").text();
                    if(textDesc.startsWith("Description:"))
                        textDesc = textDesc.split("Description:")[1];
                    //parse the termsQuery
                    parse.readTerms(queryNumber, text+" "+textDesc, false);
                    HashMap <String, Integer> afterParse=  parse.getTermsQuery();
                    if(isSemanticOn) {
                        afterParse = semanticFunc(afterParse);
                    }
                    relevantDocsForQuery(queryNumber,afterParse);
                }
                insertDominantEntities();
            }
        }
    }

    /**
     * send terms after parse to semantic class if semantic on
     * @param afterParse- terms after parse
     * @return terms after parse with semantic terms
     */
    private HashMap<String, Integer> semanticFunc(HashMap<String, Integer> afterParse){
        String semanticStr="";
        Set<String> semanticString = new HashSet<>();
        for(String str : afterParse.keySet()){
            if(str.contains(" ")){
                String [] splitStr = str.split(" ");
                for (int i=0; i< splitStr.length; i++)
                    semanticStr = semantic.getSemanticTerm(splitStr[i].toLowerCase());
            }
            else
                semanticStr = semantic.getSemanticTerm(str.toLowerCase());
            if(!semanticStr.equals(""))
                semanticString.add("*"+semanticStr);
        }
        for (String str : semanticString){
            if(afterParse.containsKey(str))
                afterParse.replace(str, afterParse.get(str)+1);
            else
                afterParse.put(str, 1);
        }
        return afterParse;
    }

    /**
     * send to ranker the terms after parse and sort from big to small and take the first fifty docs
     * @param queryNNumber- query number
     * @param afterParse - terms after parse for query
     * @throws IOException
     */
    public void relevantDocsForQuery(String queryNNumber, HashMap<String, Integer> afterParse) throws IOException{
        HashMap<Double, String> docsAfterRank = ranker.rateDocs(afterParse);
        TreeMap<Double, String> sortRelevantDocs = new TreeMap<>(new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return o2.compareTo(o1);
            }
        });
        docsForQueries.put(queryNNumber, new TreeMap<>(new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return o2.compareTo(o1);
            }
        }));
        sortRelevantDocs.putAll(docsAfterRank);
        int count=0;
        int max = 50;
        for (Map.Entry<Double,String> entry:sortRelevantDocs.entrySet()) {
            if (count >= max) break;
            docsForQueries.get(queryNNumber).put(entry.getKey(), entry.getValue());
            if(!pathToSave.equals(""))
                writeToDisk(queryNNumber, entry.getKey(), entry.getValue());
            if(!relevantDocs.containsKey(entry.getValue()))
                relevantDocs.put(entry.getValue(), new String[5]);
            count++;
        }

    }

    /**
     * write the query results to the disk to the chosen folder
     * @param queryNumber- query number
     * @param rank
     * @param docID -doc id
     */
    private void writeToDisk(String queryNumber, double rank, String docID) {
        File file = new File(pathToSave+"\\results.txt");
        StringBuilder data = new StringBuilder();
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(file, true);
            PrintWriter pw = new PrintWriter(os);

            data.append(queryNumber + " 0 " + docID + " " + rank + " 42.38 mt\n");
            pw.print(data);

            pw.close();
            os.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Moves to the document entity file and applies a formula for calculating the dominance
     * for the existing entities in the relevant documents
     * saves 5 entities or less
     */
    private void insertDominantEntities() {
        try {
            File docEntities= new File(indexer.tempPostingPath+"\\docs_Entities.txt");
            FileInputStream input = new FileInputStream(docEntities);
            BufferedReader br = new BufferedReader(new InputStreamReader(input));
            TreeMap<Double, String> entityForDoc= new TreeMap<>(new Comparator<Double>() {
                @Override
                public int compare(Double o1, Double o2) {
                    return o2.compareTo(o1);
                }
            });
            String line = br.readLine();
            while (line!=null){
                String[] splitLine= line.split(",");
                int counter = 0;
                int index = 1;
                String[] insertToRelevantDoc=new String[5];
                if (relevantDocs.containsKey(splitLine[0])) {
                    String entity;
                    String num;
                    while (index<splitLine.length) {
                        entity = splitLine[index].split(":")[0];
                        num = splitLine[index].split(":")[1];
                        if (indexer.dictionary.containsKey(entity)) {
                            int numInCorpus = indexer.dictionary.get(entity)[0];
                            entityForDoc.put(Double.parseDouble(num)/numInCorpus, entity);
                        }
                        index++;
                    }
                    while (counter<5 && counter<entityForDoc.size()){
                        for(String entityStr : entityForDoc.values()){
                            insertToRelevantDoc[counter]=entityStr;
                            counter++;
                            if(counter==5 || counter>entityForDoc.size())
                                break;
                        }
                    }
                    entityForDoc.clear();
                }
                relevantDocs.replace(splitLine[0], insertToRelevantDoc);
                line= br.readLine();
            }
            br.close();
            input.close();

        } catch (IOException ex){}
    }

    public String[] dominantEntities(String docId){
        return relevantDocs.get(docId);
    }
}

