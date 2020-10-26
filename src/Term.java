import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * information about terms
 */
public class Term {
    private String term;
    private HashMap<String , Integer> docFreq;// number of times a term appears in the doc
    private HashMap<String, LinkedList<Integer>> docLoc; // locations of each term in the doc

    /**
     * constructor
     * @param t- term
     * @param docID- number of doc
     * @param location - location of term in doc
     */
    public Term(String t, String docID , int location){
        term=t;
        docFreq = new HashMap<>();
        docLoc = new HashMap<>();
        docLoc.put(docID, (new LinkedList<>()));
        docLoc.get(docID).add(location);
        docFreq.put(docID, 1);
    }

    /**
     * summarize the number of occurrences of the term
     * @param docID- number of doc
     * @param location- location of term in doc
     */
    public void setNumberOfTerm(String docID, int location){
        if(docFreq.containsKey(docID)){
            docFreq.replace(docID, docFreq.get(docID)+1);
            docLoc.get(docID).add(location);
        }
        else
            addDoc(docID, location);
    }

    /**
     * @param docID - number of doc
     * @param location - location of term in doc
     */
    public void addDoc (String docID, int location){
        docFreq.put(docID, 1);
        docLoc.put(docID, new LinkedList<>());
        docLoc.get(docID).add(location);
    }

    /**
     * **getter**
     * @param docId- number of doc
     * @return number occurrences of doc
     */
    public int getDocFreqNumber(String docId){
        return this.docFreq.get(docId);
    }

    /**
     * marge same term- lower term with upper term
     * @param valueUpper - upper term
     */
    public void margeInHash(Term valueUpper){
        for(String sDoc : valueUpper.docFreq.keySet()){
            if(this.docFreq.containsKey(sDoc)){
                this.docFreq.replace(sDoc, this.docFreq.get(sDoc)+valueUpper.docFreq.get(sDoc));
                this.docLoc.get(sDoc).addAll(valueUpper.docLoc.get(sDoc));
            }
            else{
                this.docFreq.put(sDoc, valueUpper.docFreq.get(sDoc));
                this.docLoc.put(sDoc, valueUpper.docLoc.get(sDoc));
            }
        }
    }

    @Override
    public String toString() {
        String info =term+","+docFreq.size();
        for(Map.Entry entry: docFreq.entrySet()){
            info+="#"+entry.getKey()+":"+entry.getValue()+"|";
            for (int i : docLoc.get(entry.getKey())){
                info+=i+" ";
            }
        }
        return info;
    }
}
