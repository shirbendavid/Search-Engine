import com.medallia.word2vec.Searcher;
import com.medallia.word2vec.Word2VecModel;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Semantic {

    public String getSemanticTerm(String term){
        String matchStr="";
        try {
            Word2VecModel model = Word2VecModel.fromTextFile(new File("word2vec.c.output.model.txt"));
            com.medallia.word2vec.Searcher semanticSearcher = model.forSearch();
            int numOfResult = 10;
            List<Searcher.Match> matchList = semanticSearcher.getMatches(term, numOfResult);
            for(com.medallia.word2vec.Searcher.Match match : matchList){
                matchStr = match.match();
            }
        }
        catch (IOException ex){

        } catch (Searcher.UnknownWordException e) {

        }
        return matchStr;
    }


}
