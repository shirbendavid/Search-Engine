/**
 *This class save information about term in Query and specific Document
 */
public class TermQuery {

    String name;
    int numInDoc;//Number of occurrences of a word in a document
    int numOfDoc;//Number of documents containing the word
    int numInQuery;
    int isLoc;
    int isEntity;
    int isMaxTerm;
    int semantic;



    /**
     * constructor
     * @param name
     * @param numInDoc
     * @param numOfDoc
     * @param numInQuery
     */
    public TermQuery(String name, int numInDoc, int numOfDoc, int numInQuery) {
        this.name = name;
        this.numInDoc = numInDoc;
        this.numOfDoc = numOfDoc;
        this.numInQuery=numInQuery;
        isLoc=0;
        isEntity =0;
        isMaxTerm =0;
        semantic=0;

    }

    public int getSemantic() {
        return semantic;
    }

    public void setSemantic(int semantic) {
        this.semantic = semantic;
    }

    public int getIsMaxTerm() {
        return isMaxTerm;
    }

    public void setIsMaxTerm(int isMaxTerm) {
        this.isMaxTerm = isMaxTerm;
    }

    public int getIsEntity() {
        return isEntity;
    }

    public void setIsEntity(int isEntity) {
        this.isEntity = isEntity;
    }

    public int getIsLoc() {
        return isLoc;
    }

    public void setIsLoc(int isLoc) {
        this.isLoc = isLoc;
    }

    public int getNumInDoc() {
        return numInDoc;
    }

    public int getNumOfDoc() {
        return numOfDoc;
    }

    public int getNumInQuery() {
        return numInQuery;
    }
}

