/**
 * save information about doc in corpus
 */
public class Doc {

    private int maxTf;
    private String maxTerm;
    private int numberOfUniqueT;
    private int textLength;
    private String docID;

    /**
     * constructor
     * @param mTf - number of occurrences of the most term in doc
     * @param mTerm- most common term
     * @param uTerm- number og unique term
     * @param textL- length of text in doc
     */
    public Doc(int mTf, String mTerm, int uTerm, int textL){
        this.maxTerm=mTerm;
        this.maxTf = mTf;
        this.numberOfUniqueT = uTerm;
        this.textLength = textL;
    }
    /**
     * constructor- with docId
     */
    public Doc(String id, int mTf, String mTerm, int uTerm, int textL){
        this.docID=id;
        this.maxTerm=mTerm;
        this.maxTf = mTf;
        this.numberOfUniqueT = uTerm;
        this.textLength = textL;
    }

    @Override
    public String toString() {
        String info =maxTf+","+maxTerm+","+numberOfUniqueT+","+textLength;
        return info;
    }

    public int getMaxTf() {
        return maxTf;
    }

    public String getMaxTerm() {
        return maxTerm;
    }

    public int getNumberOfUniqueT() {
        return numberOfUniqueT;
    }

    public int getTextLength() {
        return textLength;
    }

    public String getDocID() {
        return docID;
    }
}
