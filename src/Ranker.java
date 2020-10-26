import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;

public class Ranker {
    HashMap<Double, String> docsAfterRank;
    String postingPath;
    Indexer indexer;
    HashMap<String, Doc> docs;
    double avgDocs;
    double k;
    double b;

    /**
     * constructor
     * @param ind
     * @throws IOException
     */
    public Ranker(Indexer ind) throws IOException {
        avgDocs=0;
        k=1.2;
        b=0.75;
        docsAfterRank = new HashMap<>();
        postingPath = ind.tempPostingPath;
        indexer = ind;
        docs = new HashMap<>();
        readDocsPosting();
    }

    /**
     * Receives a termsQuery after parsing and returns a list of documents relevant to the termsQuery
     * after rating
     *
     * @param queryTerms
     * @return
     * @throws IOException
     */
    public HashMap<Double, String> rateDocs(HashMap<String, Integer> queryTerms) throws IOException {//<word in Q, number of occurrences in this Q>
        HashMap<Doc, LinkedList<TermQuery>> termsQueryInDoc= new HashMap<>();
        docsAfterRank.clear();
        for (String term : queryTerms.keySet()) {//For each word in the termsQuery we will bring its line from the posting file
            int semantic=0;
            int numInQuery = queryTerms.get(term);
            if(term.startsWith("*")) {
                 term = term.substring(1);
                semantic=1;
            }
            String line = lineFromPosting(term);
            if (line != null) {//The word exists in the dictionary
                String[] lineDetails = line.split("#");
                int numOfCorpus = Integer.parseInt(lineDetails[0].split(",")[1]);
                for (int i = 1; i < lineDetails.length; i++) {//per one doc in line
                    String[] docDetails = lineDetails[i].split(":");
                    Doc doc = docs.get(docDetails[0]);
                    TermQuery termQuery = new TermQuery(term, Integer.parseInt(docDetails[1].split("[|]")[0]), numOfCorpus, numInQuery);
                    termQuery.setSemantic(semantic);
                    String [] loc=docDetails[1].split("[|]")[1].split(" ");
                        if(Integer.parseInt(loc[0])<=100) //if include
                            termQuery.setIsLoc(1);
                        if(line.endsWith("*"))
                            termQuery.setIsEntity(1);
                        if(lengthTerm(term)>1)
                            termQuery.setIsEntity(1);
                        if(term.equals(doc.getMaxTerm()))
                            termQuery.setIsMaxTerm(1);
                    if (!termsQueryInDoc.containsKey(doc))
                        termsQueryInDoc.put(doc, new LinkedList<>());
                    termsQueryInDoc.get(doc).add(termQuery);
                }
            }
        }
        finalRank(termsQueryInDoc);
        return docsAfterRank;
    }




    /**
     * calculate the number or words in term
     * @param term
     * @return
     */
    public int lengthTerm(String term){
        String [] len=term.split(" ");
        return len.length;
    }

    /**
     * Rank all relevant documents for the termsQuery
     * @param termsQueryInDoc
     * @return
     */
    private void finalRank( HashMap<Doc, LinkedList<TermQuery>> termsQueryInDoc ){
        for (Doc doc :termsQueryInDoc.keySet()){
            double rateOfDoc=0;
            for (int i=0; i<termsQueryInDoc.get(doc).size(); i++){//run on the link list
                TermQuery term = termsQueryInDoc.get(doc).get(i);
                double rate=getRankTerm(term,doc);
                if(term.getSemantic()==1)
                    rateOfDoc+=0.02*rate;
                else
                    rateOfDoc+=rate;

            }
            docsAfterRank.put(rateOfDoc,doc.getDocID());
        }

    }

    /**
     * Rank each word in a specific document
     * @param term
     * @param doc
     * @return
     */
    private double getRankTerm(TermQuery term, Doc doc){
        int numInQuery=term.getNumInQuery();
        int numInDoc=term.getNumInDoc();
        int numOfDocs=term.getNumOfDoc();
        int docsInCorpus=docs.size();
        int lengthOfDoc=doc.getTextLength();
        double log =Math.log((docsInCorpus-numOfDocs+0.5)/(numOfDocs+0.5));
        return  0.7*( numInQuery*(k+1)*numInDoc/(numInDoc*numInQuery+k*(1-b+b*lengthOfDoc/avgDocs))*log)
                +0.05*term.getIsLoc()+0.2*term.getIsEntity()+0.05*term.getIsMaxTerm();
    }
    /**
     inserts to data structure the details of all documents in the corpus
     */
    private void readDocsPosting() throws IOException {
        FileReader posting = new FileReader(postingPath + "\\docsPosting.txt");
        BufferedReader reader = new BufferedReader(posting);
        String line;
        int lengthOfAllCorpus=0;
        Doc doc = null;
        while ((line = reader.readLine()) != null) {
            String[] details = line.split(",");
            doc = new Doc(details[0], Integer.parseInt(details[1]), details[2], Integer.parseInt(details[3]), Integer.parseInt(details[4]));
            docs.put(details[0], doc);
            lengthOfAllCorpus+=Integer.parseInt(details[4]);
        }
        avgDocs=((double) lengthOfAllCorpus/docs.size());
        posting.close();
        reader.close();
    }


    /**
     * Receives a word and returns its line from the posting file
     * @param term
     * @return
     * @throws IOException
     */
    public String lineFromPosting(String term) throws IOException {
        if(indexer.dictionary.containsKey(term.toLowerCase()))
            term=term.toLowerCase();
       else if(indexer.dictionary.containsKey(term.toUpperCase()))
            term=term.toUpperCase();
       else if (!indexer.dictionary.containsKey(term))
            return null;

        String postingFileName="\\";
        if(term.matches(".*\\d.*")) {//If the word contains a number
           if(term.matches("^[0-9]*$"))
               return null;
            else
                postingFileName += "numbers";
        }
        else
            postingFileName+=term.toLowerCase().substring(0,1);//The first letter of the word
        FileReader posting=new FileReader(postingPath+postingFileName+".txt");
        BufferedReader reader = new BufferedReader(posting);
        String line;//The exact line in the posting file
        while ((line = reader.readLine()) != null)
        {
            if(line.split(",")[0].equals(term))
                break;
        }
        posting.close();
        reader.close();
        return line;
    }
}

