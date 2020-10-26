import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * this class that aggregates data on documents and word, write index to files
 */
public class Indexer {
    TreeMap<String, int[]> dictionary;
    File folderToSave ;
    String tempPostingPath;
    int index;
    int numOfDocs;
    int numberOfUniqueTerm;

    /**
     * constructor
     * @param path - path to save files
     */
    public Indexer(String path){
        dictionary = new TreeMap<>();
        folderToSave = new File(path);
        folderToSave.mkdir();
        tempPostingPath = folderToSave.getPath();
        index=1;
        numOfDocs=0;
        numberOfUniqueTerm =0;
    }

    /**
     * update path file
     * @param onS - if stemming on - true else false
     */
    public void updatePathFile(boolean onS){
        String fileName;
        if(onS)
            fileName="\\withStemming";
        else
            fileName="\\withoutStemming";
        File postingFinal = new File(tempPostingPath +fileName);
        postingFinal.mkdir();
        tempPostingPath+=fileName;
        File tempPosting = new File(folderToSave.getPath()+"\\tempPosting");
        if(tempPosting.exists())
            if(tempPosting.listFiles().length>0)
                for (File e:tempPosting.listFiles()) {
                    e.delete();
                }
    }

    /**
     * write temp posting
     * @param terms - terms without entities
     * @param entityTerms - entities terms
     * @param docs - docs after parse
     */
    public void writeToTempPost(TreeMap terms, TreeMap entityTerms, HashMap docs){
        numOfDocs += docs.size();
        postingDocs(docs);
        File fileTerms;
        File fileEntity;
        File tempPosting = new File(folderToSave.getPath()+"\\tempPosting");
        String tempPath= tempPosting.getPath();
        tempPosting.mkdir();
        if(terms.size()!=0) {
            fileTerms = new File(tempPath + "\\p" + index + ".txt");
            printTempPost(fileTerms, terms);
        }
        if(entityTerms.size()!=0) {
            fileEntity = new File(tempPath + "\\e" + index + ".txt");
            printTempPost(fileEntity, entityTerms);
        }
        if(entityTerms.size()!=0 || terms.size()!=0)
            index++;
    }

    /**
     * print temp posting
     * @param file - print to this file
     * @param map - data structure to print
     */
    public void printTempPost(File file, TreeMap map){
        Iterator it = map.entrySet().iterator();
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)))) {
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                out.println(pair.getValue().toString());
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * merge temp posting
     */
    public void mergeTempPost() {
        int i = 1;
        File tempFile = new File(folderToSave.getPath()+"\\tempPosting");
        String pathPosting =tempFile.getPath();
        while (tempFile.listFiles().length >= 4) {
            merge("p", new File(pathPosting +"\\p"+i+".txt"), new File(pathPosting +"\\p"+(i + 1)+".txt"));
            merge("e", new File(pathPosting +"\\e"+i+".txt"), new File(pathPosting +"\\e"+(i + 1)+".txt"));
            i = i + 2;
            index++;
        }
        System.gc();
        finalMerge(new File(pathPosting +"\\p"+(index-1)+".txt"), false);
        finalMerge(new File(pathPosting +"\\e"+(index-1)+".txt"), true);
        tempFile.delete();
    }

    /**
     * merge two temp posting
     * @param fileName - new merge file name
     * @param file1 - file1 to merge
     * @param file2- file2 to merge
     */
    private void merge(String fileName, File file1, File file2) {
        try {
            File mergeFile = new File(folderToSave.getPath()+"\\tempPosting"+"\\"+fileName+index+".txt");
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(mergeFile, true)));

            FileInputStream toMarge1 = new FileInputStream(file1);
            FileInputStream toMarge2 = new FileInputStream(file2);

            BufferedReader br1 = new BufferedReader(new InputStreamReader(toMarge1));
            BufferedReader br2 = new BufferedReader(new InputStreamReader(toMarge2));

            String lineOfFile1 = br1.readLine();
            String lineOfFile2 = br2.readLine();
            String line="";
            while (lineOfFile1 != null || lineOfFile2 != null) {
                if (lineOfFile1 != null && lineOfFile2 != null) {
                    if(fileName.equals("p")) {
                        int compareTerms = lineOfFile1.substring(0, lineOfFile1.indexOf(',')).compareToIgnoreCase(lineOfFile2.substring(0, lineOfFile2.indexOf(',')));
                        if (compareTerms == 0) {
                            if(lineOfFile1.substring(0, lineOfFile1.indexOf(',')).compareTo(lineOfFile2.substring(0, lineOfFile2.indexOf(',')))!=0)
                                line = changeLine(lineOfFile2, lineOfFile1);
                            else
                                line = changeLine(lineOfFile1, lineOfFile2);
                            lineOfFile1 = br1.readLine();
                            lineOfFile2 = br2.readLine();
                        } else if (compareTerms > 0) {
                            line = lineOfFile2;
                            lineOfFile2 = br2.readLine();
                        } else {
                            line = lineOfFile1;
                            lineOfFile1 = br1.readLine();
                        }
                    }
                    else if(fileName.equals("e")){
                        int compareTerms= lineOfFile1.substring(0, lineOfFile1.indexOf(',')).compareTo(lineOfFile2.substring(0, lineOfFile2.indexOf(',')));
                        if (compareTerms == 0) {
                            line = changeLine(lineOfFile1, lineOfFile2);
                            lineOfFile1 = br1.readLine();
                            lineOfFile2 = br2.readLine();
                        } else if (compareTerms > 0) {
                            line = lineOfFile2;
                            lineOfFile2 = br2.readLine();
                        } else {
                            line = lineOfFile1;
                            lineOfFile1 = br1.readLine();
                        }
                    }
                } else if (lineOfFile1 != null) {
                    line = lineOfFile1;
                    lineOfFile1 = br1.readLine();
                } else{
                    line = lineOfFile2;
                    lineOfFile2 = br2.readLine();
                }
                if(line.equals(""))
                    continue;
                out.println(line);
            }
            toMarge1.close();
            toMarge2.close();
            br1.close();
            br2.close();
            out.close();
            file1.delete();
            file2.delete();

        } catch (IOException e) {
        }
    }

    /**
     * func that change line according to compere function in merge if terms in lines are same
     * @param line1- line of file1
     * @param line2- line of file2
     * @return new line to print
     */
    private String changeLine(String line1, String line2){
        int freq1 = Integer.valueOf(line1.substring(line1.indexOf(',') + 1, line1.indexOf('#')));
        int freq2 = Integer.valueOf(line2.substring(line2.indexOf(',') + 1, line2.indexOf('#')));
        int freq = freq1 + freq2;
        String term = line1.substring(0, line1.indexOf(','));
        String change= term + "," + freq + line1.substring(line1.indexOf('#')) + line2.substring(line2.indexOf('#'));
        return change;
    }

    /**
     * create posting files a-z and numbers
     * @param filePost- final file after merge
     * @param entity- if file of entity is true else false
     */
    private void finalMerge(File filePost, boolean entity){
        try {
            FileInputStream f = new FileInputStream(filePost);
            BufferedReader br = new BufferedReader(new InputStreamReader(f));
            String line = br.readLine();
            int numberLine=1;

            char first = line.toLowerCase().charAt(0);
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(new File(tempPostingPath+"\\numbers.txt"), true)));
            while (line != null && !(first >= 'a' && first <= 'z')) {
                insertToDic(line, numberLine);
                numberLine++;
                out.println(line);
                line = br.readLine();
                if(line!=null)
                    first = line.toLowerCase().charAt(0);
                }
            out.close();
            line = br.readLine();
            while (line!=null && (first >= 'a' && first <= 'z')) {
                numberLine=1;
                first = line.toLowerCase().charAt(0);
                char next = line.toLowerCase().charAt(0);
                PrintWriter out1 = new PrintWriter(new BufferedWriter(new FileWriter(new File(tempPostingPath + "\\" + first + ".txt"), true)));
                while (line != null && first==next) {
                    if(!entity ||(entity && Integer.valueOf(line.substring(line.indexOf(',')+1,line.indexOf('#')))>=2)) {
                        if(entity)
                            line= line+"*";
                        insertToDic(line, numberLine);
                        numberLine++;
                        out1.println(line);
                    }
                    line = br.readLine();
                    if (line != null)
                        next = line.toLowerCase().charAt(0);
                }
                out1.close();
            }
            out.close();
            br.close();
            f.close();
            filePost.delete();
        }catch(IOException e){

        }
        }

    /**
     * insert term to dictionary with number of occurrences in all corpus
     * @param line - line to insert to dictionary
     */
    private void insertToDic(String line, int numberLine){
        if(line!=null) {
            String termName = line.substring(0, line.indexOf(','));
            int numOfCorpus = 0;
            String[] numFreqInDoc = line.split(":");
            for (int i = 1; i < numFreqInDoc.length; i++) {
                numOfCorpus += Integer.valueOf(numFreqInDoc[i].substring(0, numFreqInDoc[i].indexOf("|")));
            }
            if(!dictionary.containsKey(termName)) {
                dictionary.put(termName, new int[2]);
                dictionary.get(termName)[0]= numOfCorpus;
                dictionary.get(termName)[1]= numberLine;
                numberOfUniqueTerm++;
            }
            else{
                dictionary.get(termName)[0] = dictionary.get(termName)[0]+numOfCorpus;
            }
        }
        }

    /**
     * save dictionary in file of txt
     * @param onS- if stemming on - true else false
     */
        public void saveDic(boolean onS){
        String dicName;
        if(onS)
            dicName="\\dictionary.txt";
        else
            dicName="\\dictionary.txt";
        File file= new File(tempPostingPath +dicName);
        Iterator it = dictionary.entrySet().iterator();
        try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)))){
            while (it.hasNext()){
                Map.Entry pair = (Map.Entry)it.next();
                out.println(pair.getKey().toString()+","+((int[])pair.getValue())[0]);
            }
        }catch (FileNotFoundException e){
        }catch (IOException e){
        }
        }

    /**
     * create posting for a docs
     * @param docs- docs in corpus
     */
    public void postingDocs(HashMap docs){
        try {
            String name="\\docsPosting.txt";
            FileOutputStream os=null;
            PrintWriter out = null;
            File file = new File(tempPostingPath);
            if(!file.exists())
                file.mkdir();

            File docsPosting = new File(tempPostingPath+name);
            os = new FileOutputStream(docsPosting,true);
            out = new PrintWriter(os);
            Iterator it = docs.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                out.println(pair.getKey().toString() + "," + pair.getValue().toString());
            }
            out.close();
            os.close();
        } catch (FileNotFoundException e) {}
        catch (IOException e) {}
    }

    /**
     * getter
     */
    public int getNumOfDocs() {
        return numOfDocs;
    }
    public int getSizeOfDic(){
        return numberOfUniqueTerm;
    }
    public void setDictionary(TreeMap<String, int[]> dicLoad){
        dictionary = dicLoad;
    }
}
