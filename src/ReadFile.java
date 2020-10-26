import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;

/**
 * read the corpus
 */
public class ReadFile {
    public Parse parse ;
    private String dataPath;
    private boolean stemmerOn;
    HashSet <String> stopWords;


    /**
     * constructor- for corpus
     */
    public ReadFile(String path, boolean onS, Indexer indexer, String pathDes){
        dataPath = path;
        stemmerOn = onS;
        stopWords = new HashSet<>();
        addStopWord();
        parse = new Parse(stopWords, stemmerOn, indexer, pathDes);
    }

    /**
     * read all docs in corpus
     */
    public void Read () {
        File corpus = new File(dataPath + "//corpus");
        File[] listOfFile = corpus.listFiles();
        for (File dir : listOfFile) {
            if (dir.isDirectory()) {
                File[] files = dir.listFiles();
                for (File f : files) {
                    if (f.isFile()) {
                        try {
                            Document htmlDoc = Jsoup.parse(f, "UTF-8");
                            Elements docs = htmlDoc.getElementsByTag("doc");
                            for (Element doc : docs) {
                                if(doc.select("TEXT").hasText()) {
                                    String docID = doc.getElementsByTag("DOCNO").text();
                                    String text = doc.getElementsByTag("TEXT").text();
                                    parse.readTerms(docID, text, true);
                                }
                            }
                        } catch (IOException io) {
                        }
                    }
                }
            }
        }
        parse.sendToIndexer();
    }

    /**
     * add the list of stop word to HashSet
     */
    private void addStopWord(){
        File stop_word = new File(dataPath+"//stop_words.txt");
        try {
            BufferedReader br = new BufferedReader(new FileReader(stop_word));
            String line;
            while ((line=br.readLine()) !=null)
                stopWords.add(line.replaceAll("\\p{P}",""));
        }
        catch (IOException e){
            e.printStackTrace();}

    }

}
