import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class Controller {
    public String dataPath;
    public String path;
    public String pathDest;
    public ReadFile readFile;
    public Indexer index;
    public Searcher searcher;
    public HashSet stopWords;

    public javafx.scene.control.Button start;
    public javafx.scene.control.Button reset;
    public javafx.scene.control.Button showDic;
    public javafx.scene.control.Button loadDic;
    public javafx.scene.control.Button runQuery;
    public javafx.scene.control.CheckBox stemmer;
    public javafx.scene.control.CheckBox semanticToQuery;
    public javafx.scene.control.TextField f1;
    public javafx.scene.control.TextField f2;
    public javafx.scene.control.TextField f3;
    public javafx.scene.control.TextField f4;
    public javafx.scene.control.TextField f5;
    public javafx.scene.layout.BorderPane borderPane;

    public void startCorpus(ActionEvent actionEvent) throws IOException {
        start.setDisable(true);
        index = new Indexer(pathDest);
        readFile = new ReadFile(dataPath, stemmer.isSelected(), index, pathDest);
        long startTime = System.currentTimeMillis();
        index.updatePathFile(stemmer.isSelected());
        readFile.Read();
        index.mergeTempPost();
        saveStopWords();
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        long finalTime = TimeUnit.MILLISECONDS.toSeconds(totalTime);
        StringBuilder sb = new StringBuilder();
        sb.append("Number of Documents: " + index.getNumOfDocs());
        sb.append("\n");
        System.gc();
        sb.append("RunTime: " + finalTime);
        sb.append("\n");
        System.gc();
        sb.append("Number of uniques terms: " + index.getSizeOfDic());
        index.saveDic(stemmer.isSelected());
        start.setDisable(false);
        loadDic.setDisable(false);
        showDic.setDisable(false);
        reset.setDisable(false);
        System.out.print(sb.toString());
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(sb.toString());
        alert.show();
        System.gc();
    }

    public String getPathToFolder(DirectoryChooser chooser) {
        try {
            chooser.setInitialDirectory(new File("C:\\"));
            File f = chooser.showDialog(null);
            return f.getPath();
        }
        catch (NullPointerException ex){
            Controller.sendToShow("no found path");
        }
        return null;
    }

    public void openPath(ActionEvent actionEvent) throws IOException {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("choose Corpus path");
        dataPath = getPathToFolder(chooser);
        f1.setText(dataPath);
        if (dataPath != null && pathDest != null)
            start.setDisable(false);
    }

    public void PathToSavePosting(ActionEvent actionEvent) throws IOException {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("choose Posting saving path");
        chooser.setInitialDirectory(new File("C:\\"));
        pathDest = getPathToFolder(chooser);
        pathDest += "\\tempFile";
        path = pathDest;
        f2.setText(pathDest);
        if (dataPath != null && pathDest != null)
            start.setDisable(false);
        loadDic.setDisable(false);

    }

    public void showDic(ActionEvent actionEvent) {
        if(index!=null){
            ListView<String> list = new ListView<>();

            ObservableList<String> items = FXCollections.observableArrayList();
            TreeMap<String, int[]> dic = index.dictionary;

            for (String s : dic.keySet()) {
                items.add("word: "+s+" num of occurrences in corpus: "+dic.get(s)[0]);
            }

            list.setItems(items);

            Stage stage = new Stage();
            stage.setTitle("Dictionary");
            BorderPane pane = new BorderPane();
            Scene s = new Scene(pane);
            pane.setMinWidth(400);
            stage.setScene(s);
            pane.setCenter(list);
            stage.setAlwaysOnTop(true);
            stage.setOnCloseRequest(e -> {
                e.consume();
                stage.close();
            });
            stage.showAndWait();
        }
        else
        {
            Controller.sendToShow("Error! please make sure the file exist or change the Stemming button");
        }

    }

    public void LoadDirectory(ActionEvent actionEvent) throws IOException {
        String dicLoad="\\dictionary.txt";
        if (stemmer.isSelected()) {
            pathDest= "\\withStemming";
        } else {
            pathDest= "\\withoutStemming";
        }
        path = f2.getText();
        pathDest = f2.getText() +pathDest;
        index = new Indexer(pathDest);
        TreeMap<String, int[]> hashDic = new TreeMap<>();
        String getPath = pathDest+ dicLoad;
        try {
            File toLoad = new File(getPath);
            if (toLoad.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(toLoad));
                String line = br.readLine();
                while (line != null) {
                    String[] s = line.split(",");
                    hashDic.put(s[0], new int[2]);
                    hashDic.get(s[0])[0] = Integer.valueOf(s[1]);
                    hashDic.get(s[0])[1] = Integer.valueOf(s[1]);
                    line = br.readLine();
                }
                LoadStopWords();
                runQuery.setDisable(false);
                index.setDictionary(hashDic);
                showDic.setDisable(false);
                reset.setDisable(false);
                Controller.sendToShow("The upload completed successfully");
            } else
                Controller.sendToShow("No dictionary to load");
        } catch (IOException e) {
        }
    }
    private void LoadStopWords() {
        stopWords= new HashSet<>();
        String loadStopWord;
        if (stemmer.isSelected()) {
            loadStopWord = "\\withStemming\\stop_words.txt";
        } else {
            loadStopWord = "\\withoutStemming\\stop_words.txt";
        }
        String getPath = f2.getText() + loadStopWord;
        try {
            File toLoad = new File(getPath);
            if (toLoad.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(toLoad));
                String line = br.readLine();
                while (line != null) {
                    stopWords.add(line);
                    line = br.readLine();
                }
            }
        } catch (IOException ex) {

        }
    }

    public static void sendToShow(String S){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(S);
        alert.show();
    }

    public void reset(){
        readFile=null;
        index=null;
        if(path!=null) {
            File withStem = new File(path + "\\withStemming");
            File withoutStem = new File(path + "\\withoutStemming");
            if (withStem.exists())
                resetFile(withStem);
            else if (withoutStem.exists())
                resetFile(withoutStem);
            else if (!withStem.exists() && !withoutStem.exists())
                Controller.sendToShow("no have files to reset");
            File save = new File(path);
            if (save.exists())
                resetFile(save);
            withoutStem.delete();
            withStem.delete();
        }
        else
            Controller.sendToShow("There is no folder path to reset");
        System.gc();
    }
    public void resetFile(File file){
        File[] f = file.listFiles();
        for (File s : f) {
            s.delete();
        }
        file.delete();
    }

    public void runQuery(ActionEvent actionEvent) throws IOException{
        if(f3.getText().isEmpty() && f4.getText().isEmpty())
            Controller.sendToShow("please insert termsQuery");
        if(!f3.getText().isEmpty() && !f4.getText().isEmpty())
            Controller.sendToShow("please insert only one termsQuery or only file termsQuery");
        boolean stemming = stemmer.isSelected();
        boolean semantic = semanticToQuery.isSelected();
        if(!f3.getText().isEmpty()){ // one termsQuery
            searcher = new Searcher(stemming, semantic,f5.getText(),index, stopWords);
            searcher.readQuery(true, f3.getText());
        }
        if(!f4.getText().isEmpty()){
            searcher = new Searcher(stemming, semantic,f5.getText(), index, stopWords);
            searcher.readQuery(false, f4.getText());
        }
        if(!f3.getText().isEmpty() || !f4.getText().isEmpty()) {
            printRelevantDocs();
        }
    }

    private void printRelevantDocs() {
        ListView<String> listView = new ListView<>();
        ObservableList<String> items = FXCollections.observableArrayList();
        TreeMap<String,TreeMap<Double, String>> queries = searcher.docsForQueries;

        for (String q : queries.keySet()) {
            Iterator treeMap = queries.get(q).values().iterator();
            while (treeMap.hasNext()) {
                items.add(q + ":" + treeMap.next());
            }
        }
        listView.setItems(items);

        listView.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(item);
                }
            };
            cell.setOnMouseClicked(e -> {
                if (!cell.isEmpty()) {
                    printEntitiesForDoc(cell.getItem());
                    e.consume();
                }
            });
            return cell;
        });
        listView.setOnMouseClicked(e -> {
            System.out.println("You clicked on an empty cell");
        });
            BorderPane pane = new BorderPane();
            pane.setCenter(listView);
            borderPane.setCenter(pane);
    }

    private void printEntitiesForDoc(String line) {
        String docId = line.split(":")[1];
        String[] entities= searcher.dominantEntities(docId);
        if(entities[0]!=null) {
            ListView<String> list = new ListView<>();
            ObservableList<String> items = FXCollections.observableArrayList();
            items.add("For doc- "+docId+":");
            for(int i=0; i<entities.length; i++){
                if(entities[i]!=null)
                    items.add((i+1)+" : "+entities[i]);
            }
            list.setItems(items);
            Stage stage = new Stage();
            stage.setTitle("Entities");
            BorderPane pane = new BorderPane();
            Scene s = new Scene(pane);
            stage.setScene(s);
            pane.setCenter(list);
            stage.setMaxHeight(200);
            stage.setMaxWidth(650);
            stage.setAlwaysOnTop(true);
            stage.setOnCloseRequest(e -> {
                e.consume();
                stage.close();
            });
            stage.showAndWait();
        }
        else
            Controller.sendToShow("no have entities for doc "+docId);

    }

    public void LoadFileQuery(ActionEvent actionEvent){
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("choose query path");
            chooser.setInitialDirectory(new File("C:\\"));
            File f = chooser.showOpenDialog(null);
            String getPath = f.getPath();
            f4.setText(getPath);
        }
        catch (NullPointerException ex){
            Controller.sendToShow("no found path");
        }
    }
    public void pathToSaveResult(ActionEvent actionEvent) {
        DirectoryChooser chooser= new DirectoryChooser();
        chooser.setTitle("choose path to save result");
        f5.setText(getPathToFolder(chooser));
    }
    /**
     * save the stop words with all posting
     */
    public void saveStopWords(){
            String name;
            if (stemmer.isSelected())
                name = "\\withStemming\\stop_words.txt";
            else
                name = "\\withoutStemming\\stop_words.txt";
            File toSaveStopWord= new File(pathDest+name);
            File stopWords = new File(dataPath+"\\stop_words.txt");
            if(stopWords.exists()) {
                try {
                    Files.copy(stopWords.toPath(), toSaveStopWord.toPath());
                }
                catch (IOException ex){
                }
            }
    }
}
