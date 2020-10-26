import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

public class Parse {

    List<String> termsList;
    Indexer indexer;
    int index;
    int indexTerm;
    boolean stemmerOn;
    HashMap<String, String> numbersCase;
    HashMap<String, String> dateCase;
    HashSet<String> listOfStopWords;
    //data structure
    HashMap <String,Term> upperTerms;
    HashMap <String,Term> lowerTerms;
    HashMap <String,Term> numberTerms;
    HashMap <String, Term> entityTerms;
    HashMap<String, Integer> dominantEntities;
    //details about doc
    HashMap <String, Doc> docs;
    //details about termsQuery
    HashMap<String, Integer> termsQuery;
    int maxTf;
    String maxTerm;
    int textLength;
    int numberOfUniqueT;
    int maxDocsParse;
    String pathForSave;
    private String parserID;
    boolean isDoc;

    /**
     * constructor
     */
    public Parse(HashSet stopWords, boolean sOn, Indexer index, String pathDes) {
        indexer = index;
        numbersCase = new HashMap<>();
        initNumbersCase();
        dateCase = new HashMap<>();
        initDateCase();
        upperTerms = new HashMap<>();
        lowerTerms = new HashMap<>();
        numberTerms = new HashMap<>();
        listOfStopWords = stopWords;
        entityTerms= new HashMap<>();
        dominantEntities = new HashMap<>();
        docs= new HashMap<>();
        termsQuery = new HashMap<>();
        maxDocsParse=1000;
        stemmerOn = sOn;
        pathForSave= pathDes;
        parserID ="";
    }

    /**
     * data structure for date cases
     */
    private void initDateCase(){
        dateCase.put("january" , "01");
        dateCase.put("jan" , "01");
        dateCase.put("february" , "02");
        dateCase.put("feb" , "02");
        dateCase.put("march" , "03");
        dateCase.put("mar" , "03");
        dateCase.put("april" , "04");
        dateCase.put("apr" , "04");
        dateCase.put("may" , "05");
        dateCase.put("june" , "06");
        dateCase.put("jun" , "06");
        dateCase.put("july" , "07");
        dateCase.put("jul" , "07");
        dateCase.put("aug" , "08");
        dateCase.put("september" , "09");
        dateCase.put("sep" , "09");
        dateCase.put("october" , "10");
        dateCase.put("oct" , "10");
        dateCase.put("november" , "11");
        dateCase.put("nov" , "11");
        dateCase.put("december" , "12");
        dateCase.put("dec" , "12");
    }

    /**
     * data structure for number cases
     */
    private void initNumbersCase() {
        numbersCase.put("thousand", "K");
        numbersCase.put("million", "M");
        numbersCase.put("billion", "B");
        numbersCase.put("percent", "%");
        numbersCase.put("percentage", "%");
        numbersCase.put("km", "km");
        numbersCase.put("kg", "kg");
        numbersCase.put("m", "m");
        numbersCase.put("cm", "cm");
        numbersCase.put("centimeter", "cm");
        numbersCase.put("km/h", "kmh");
        numbersCase.put("m/s", "m/s");
        numbersCase.put("g", "g");
        numbersCase.put("kw", "kw");
        numbersCase.put("mh", "mh");
        numbersCase.put("ton", "t");
        numbersCase.put("tons", "t");
        numbersCase.put("mg", "mg");
        numbersCase.put("lb", "lb");
        numbersCase.put("kilometers", "km");
        numbersCase.put("libras", "lb");
        numbersCase.put("kilograms", "km");
        numbersCase.put("grams", "g");
        numbersCase.put("kilowatts", "kw");
        numbersCase.put("megahertz", "mh");
        numbersCase.put("meters", "m");
        numbersCase.put("seconds", "s");
        numbersCase.put("hours", "h");
        numbersCase.put("minutes", "min");
    }

    /**
     * read every terms in each doc
     * @param ID the name of the document
     * @param text the text of the document
     */
    public void readTerms(String ID, String text, boolean is_doc) {
        termsQuery.clear();
        parserID = ID;
        numberOfUniqueT=0;
        maxTf=0;
        maxTerm="";
        textLength=0;
        isDoc = is_doc;
        text = text.replaceAll("[()]?,?:?;?&?", "");
        text = text.replaceAll("\n", " ");
        text = text.trim().replaceAll(" +", " ");
        termsList = new ArrayList<>();
        String[] terms =text.split(" ");
        termsList = Arrays.asList(terms);
        for (indexTerm = 0; indexTerm < termsList.size(); indexTerm=indexTerm+index) {
            index = 1;
            String nextTerm = "";
            if (indexTerm != (termsList.size() - 1))
                nextTerm = termsList.get(indexTerm+1);
            checkTerms(termsList.get(indexTerm), nextTerm);
        }
        if(isDoc){
            saveDominantEntities();
            dominantEntities.clear();
            docs.put(ID, new Doc(maxTf, maxTerm, numberOfUniqueT, textLength));
            if(docs.size()==maxDocsParse) {
                sendToIndexer();
            }
        }
    }

    /**
     * this func send to indexer after each iteration
     */
    public void sendToIndexer() {
        TreeMap<String, Term> margeTerms = margeUpperLower();
        margeTerms.putAll(numberTerms);
        TreeMap<String, Term> entity = new TreeMap<>(entityTerms);
        indexer.writeToTempPost(margeTerms, entity, docs);
        numberTerms.clear();
        entityTerms.clear();
        lowerTerms.clear();
        upperTerms.clear();
        docs.clear();
    }

    /**
     * check every term in docs what the relevant law for him
     * @param term - string of term
     * @param nextTerm - string of next term
     * @return
     */
    private String checkTerms(String term, String nextTerm){
        term = endWithPunctuation(term);
        nextTerm = endWithPunctuation(nextTerm);
        String change="";
        if(term.matches(".*\\d.*")) {
            if (nextTerm.toLowerCase().contains("dollars") || term.matches("^\\$\\d+\\.?\\d+$")) {
                change = checkPrice(term, nextTerm);
                insertToRelevantHash(change, numberTerms);
            }
            if ((indexTerm + 2 < (termsList.size())) && (termsList.get(indexTerm + 2).toLowerCase().contains("dollars")) ||
                    ((indexTerm + 3 < (termsList.size())) && termsList.get(indexTerm + 3).toLowerCase().contains("dollars"))) {
                change = checkPrice(term, nextTerm);
                insertToRelevantHash(change, numberTerms);
            }
            if ((dateCase.containsKey(nextTerm.toLowerCase()) && term.matches("^\\d+$")) ||
                    (dateCase.containsKey(term.toLowerCase()) && nextTerm.matches("^\\d+$"))) {
                change = checkDate(term, nextTerm);
                insertToRelevantHash(change, numberTerms);
            }
            if (nextTerm.equals("GMT") && term.matches("^\\d+\\-?\\d+$")) { //new rule - times
                index++;
                change = term + " " + nextTerm;
                insertToRelevantHash(change, numberTerms);
            }
            if (term.matches("^\\-?\\d*\\.?\\d+$")) {
                change = checkNumber(term, nextTerm);
                insertToRelevantHash(change, numberTerms);
            }
            if (term.matches("^\\d*\\.?\\d+\\%$")) {
                change = term;
                insertToRelevantHash(change, numberTerms);
            }
        }
        else {
            if (term.toLowerCase().equals("between") && nextTerm.matches("^\\-?\\d*\\.?\\d+$")) {
                change = expBetween(nextTerm);
                insertToRelevantHash(change, numberTerms);
                change="";
            }
            if(!removeStopWord(term)) {
                if (Character.isUpperCase(term.charAt(0)) && !nextTerm.equals("")) {
                    if (Character.isUpperCase(nextTerm.charAt(0)) && (indexTerm + 2 < (termsList.size())) && Character.isUpperCase(termsList.get(indexTerm + 2).charAt(0)))
                        if(!removeStopWord(nextTerm) && !removeStopWord(termsList.get(indexTerm + 2)))
                        change = term + " " + nextTerm + " " + termsList.get(indexTerm + 2);
                    else {
                        if (Character.isUpperCase(nextTerm.charAt(0)) && !removeStopWord(nextTerm))
                            change = term + " " + nextTerm;
                    }
                }
                if (!change.equals("")) {
                    insertToRelevantHash(change.replaceAll("\\p{P}", " ").toUpperCase(), entityTerms);
                }
                else {
                    if (Character.isUpperCase(term.charAt(0))) {
                        insertToRelevantHash(term.replaceAll("\\p{P}", " ").toUpperCase(), upperTerms);
                    }
                    if (Character.isLowerCase(term.charAt(0))) {
                        insertToRelevantHash(term.replaceAll("\\p{P}", " "), lowerTerms);
                    }
                }
            }
        }
        return change;
    }

    /**
     * insert term to relevant data structure
     * @param termToInsert- term to insert
     * @param hashMap - relevant data structure for term
     */
    private void insertToRelevantHash(String termToInsert,  HashMap<String, Term> hashMap){
        if(!termToInsert.equals("")){
            termToInsert = termToInsert.trim().replaceAll(" +", " ");
            if(termToInsert.endsWith(" "))
                termToInsert=termToInsert.substring(0, termToInsert.length()-1);
            if(stemmerOn)
                    termToInsert = stemmerFunc(termToInsert);
            if(isDoc) {
                if (!hashMap.containsKey(termToInsert)) {
                    hashMap.put(termToInsert, new Term(termToInsert, parserID, textLength));
                    numberOfUniqueT++;
                } else {
                    hashMap.get(termToInsert).setNumberOfTerm(parserID, textLength);
                }
                textLength++;
                if (maxTf < hashMap.get(termToInsert).getDocFreqNumber(parserID)) {
                    maxTf = hashMap.get(termToInsert).getDocFreqNumber(parserID);
                    maxTerm = termToInsert;
                }
                if (hashMap == entityTerms) {
                    if (!dominantEntities.containsKey(termToInsert))
                        dominantEntities.put(termToInsert, 1);
                    else
                        dominantEntities.replace(termToInsert, dominantEntities.get(termToInsert) + 1);
                }
            }
            else {
                if(termsQuery.containsKey(termToInsert))
                    termsQuery.replace(termToInsert, termsQuery.get(termToInsert)+1);
                else
                    termsQuery.put(termToInsert, 1);
            }
        }
    }

    /**
     * marge terms in lowercase and uppercase
     * @return tree map for all terms in lower and upper case
     */
    private TreeMap<String, Term> margeUpperLower(){
        TreeMap <String , Term> marge;
        ArrayList <String> toRemove= new ArrayList<>();
        for(String s : upperTerms.keySet()){
            if(lowerTerms.containsKey(s.toLowerCase())){
                lowerTerms.get(s.toLowerCase()).margeInHash(upperTerms.get(s));
                toRemove.add(s);
            }
        }
        for(String remove : toRemove)
            upperTerms.remove(remove);
        marge = new TreeMap<>(String::compareToIgnoreCase);
        marge.putAll(lowerTerms);
        marge.putAll(upperTerms);
        return marge;
    }

    /**
     * check if it expression
     * @param nextTerm - after term- between
     * @return
     */
    private String expBetween(String nextTerm){
        if(indexTerm+3<termsList.size() && termsList.get(indexTerm+2).equals("and")){
            if(termsList.get(indexTerm+3).matches("^\\-?\\d*\\.?\\d+$"))
                return nextTerm+"-"+termsList.get(indexTerm+3);
        }
        return "";
    }

    /**
     * check if two terms are date case
     * @param term - string of term
     * @param nextTerm - string of next term
     * @return
     */
    private String checkDate(String term, String nextTerm){
        index++;
        if(dateCase.containsKey(nextTerm.toLowerCase())) {
            if(term.length()>2)
                return formatDate("", nextTerm, term);
            else
                return formatDate(term,nextTerm,"");
        }
        else{
            if(nextTerm.length()>2)
                return formatDate("", term, nextTerm);
            else
                return formatDate(nextTerm, term, "");
        }
    }

    /**
     * return format for dare case
     * @param day- day
     * @param month - month
     * @param year - year
     * @return new term
     */
    private String formatDate(String day, String month, String year){
        if(year.length()>2)
            return (year+"-"+dateCase.get(month.toLowerCase()));
        if(day.length()==1)
            day="0"+day;
        return (dateCase.get(month.toLowerCase())+"-"+day);
    }
    private String checkPrice(String term, String nextTerm){
        String newTerm="";
        if(term.matches("^\\$\\d+\\.?\\d*\\.?$")){
            term = term.substring(1);
            newTerm = sizeOfPrice(term, nextTerm);
        }
        else if(term.matches("^\\d+\\.?\\d*$")){
            if(nextTerm.toLowerCase().contains("dollars")) {
                index++;
                newTerm = sizeOfPrice(term, nextTerm);
            }
            else if(termsList.get(indexTerm+2).toLowerCase().contains("dollars")) {
                index++;
                newTerm = sizeOfPrice(term, nextTerm);
            }
            else if(termsList.get(indexTerm+3).toLowerCase().contains("dollars")){
                index++;
                if(termsList.get(indexTerm+2).equals("U.S.")){
                    index++;
                    newTerm = sizeOfPrice(term, nextTerm);
                }
            }
        }
        else if(term.matches("^\\d*\\.?\\d*?(bn)?$")){
            term= term.substring(0, term.length()-2);
            newTerm = sizeOfPrice(term, nextTerm);
        }
        else if(term.matches("^\\d*\\.?\\d*?(m)?$")){
            term = term.substring(0, term.length()-1);
            newTerm = sizeOfPrice(term, nextTerm);

        }
        return newTerm;
    }
    /**
     * check the size of price
     * @param term - string of term
     * @param nextTerm - string of next term
     * @return new string
     */
    private String sizeOfPrice(String term, String nextTerm){
        double number = Double.parseDouble(term);
        String price;
        if(nextTerm.toLowerCase().equals("million") || nextTerm.equals("m")) {
            index++;
            price=" M Dollars";
        }
        else if(nextTerm.toLowerCase().equals("billion") || nextTerm.equals("bn")){
            index++;
            number=number*1000;
            price=" M Dollars";
        }
        else if(nextTerm.toLowerCase().equals("trillion")){
            index++;
            number=number*1000000;
            price=" M Dollars";
        }
        else if(number<1000000)
            price= " Dollars";
        else {
            number = number * 0.000001;
            price = " M Dollars";
        }
        if(nextTerm.matches("[0-9]+/[0-9]")){
            index++;
            price=" "+nextTerm+price;
        }

        return pointCase(number)+price;
    }

    /**
     * check if term is number case
     * @param term - string of term
     * @param nextTerm - string of next term
     * @return new string
     */
    private String checkNumber (String term, String nextTerm) {
        String newTerm="";
        nextTerm = checkNextTerm(nextTerm);
        double number = Double.parseDouble(term);

        if(Math.abs(number)>=1000 && Math.abs(number)<1000000) {
            number = number * 0.001;
            nextTerm = "K";
        }
        else if(Math.abs(number)>=1000000 && Math.abs(number)<1000000000) {
            number = number * 0.000001;
            nextTerm = "M";
        }
        else if(Math.abs(number)>=1000000000) {
            number = number * 0.000000001;
            nextTerm = "B";
        }

        newTerm = pointCase(number) + nextTerm;
        return newTerm;
    }

    /**
     * check the type of next term
     * @param nextTerm- string of next term
     * @return new term
     */
        private String checkNextTerm(String nextTerm){
            String nextNext="";
            nextTerm= endWithPunctuation(nextTerm);
            if(numbersCase.containsKey(nextTerm.toLowerCase())) { //K, M, B, %
                index++;
                return numbersCase.get(nextTerm.toLowerCase());
            }
            else if(nextTerm.matches("[0-9]+/[0-9]")){ //fracture 1/2
                index++;
                if(indexTerm+index > termsList.size())
                    nextNext = termsList.get(indexTerm+index);
                return " " + nextTerm + " " + checkNextTerm(nextNext);
            }
            return "";
        }

    /**
     * updates the numbers after point
     * @param number- term is number
     * @return update number
     */
        private String pointCase(double number){
            DecimalFormat df = new DecimalFormat("#.###");
            return df.format(number);
        }

    /**
     * check if term end if any punctuation
     * @param term - string of term
     * @return new term
     */
        private String endWithPunctuation(String term){
        term = term.replaceAll("-(\\s*-)+", "-");
        if(term.length()>1 && (term.endsWith(".") || term.endsWith(",")))
                term= term.substring(0, term.length()-1);
        if(term.length()>2 && term.endsWith("'s"))
            term= term.substring(0, term.length()-2);

                return term;
        }

    /**
     * remove term if exists in hash of stop words
     * @param term- string of term
     * @return- if term stop word
     */
    private boolean removeStopWord(String term){
        term= term.replaceAll("\\p{P}", "");
        if(listOfStopWords.contains(term.toLowerCase()))
            return true;
        return false;
        }
        private String stemmerFunc(String sTerm){
        char[] term= sTerm.toCharArray();
        Stemmer stemmer = new Stemmer();
        for (int c=0; c<term.length; c++)
            stemmer.add(term[c]);
        stemmer.stem();
        return stemmer.toString();
        }

    /**
     * create file for all entities in each docs
     * after indexer we create file for dominant entities in each docs
     */
    private void saveDominantEntities() {
        try {
            String name="\\docs_Entities.txt";
            String pathFile=checkPathFile();
            FileOutputStream os;
            PrintWriter out;

            File file = new File(pathForSave+pathFile);
            if(file.exists()) {
                os = new FileOutputStream(new File(pathForSave+pathFile+name), true);
                out = new PrintWriter(os);
                Object[] sortEntities = sortByValue();

                out.print(parserID);
                for (Object e : sortEntities) {
                    out.print(","+((Map.Entry<String, Integer>) e).getKey() + ":"
                            + ((Map.Entry<String, Integer>) e).getValue());
                }
                out.println();
                out.close();
                os.close();
            }
        } catch (FileNotFoundException e) {}
        catch (IOException e) {}
    }

    /**
     * helper func to sort hash dominantEntities by value
     * @return object that sort by value
     */
    private Object[] sortByValue() {
        Object[] a = dominantEntities.entrySet().toArray();
        Arrays.sort(a, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Map.Entry<String, Integer>) o2).getValue()
                        .compareTo(((Map.Entry<String, Integer>) o1).getValue());
            }
        });
        return a;
    }

    /**
     *getter
     * @return- termsQuery data structure
     */
    public HashMap<String, Integer> getTermsQuery() {
        return termsQuery;
    }

    /**
     * check if file with stemming or without
     * @return the path of file
     */
    private String checkPathFile(){
        if(stemmerOn)
            return "\\withStemming";
        else
            return "\\withoutStemming";
    }

    }

