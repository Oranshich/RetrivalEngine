package readFile;

import Indexer.Indexer;
import Parser.parseNumbers;
import Tokenizer.Tokenizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;

public class ReadFile {

    public static int numOfCorpusFiles = 0, numOfParsedDocs = 0;
    private Tokenizer theTokenizer = Tokenizer.getInstance();
    public parseNumbers prsNums = new parseNumbers();
    private Indexer myIndexer = Indexer.getInstance();
    private final int numberOfDocsToPost = 1000;

    public void readCorpus(File corpus){
        Document doc;

        for (File folder : corpus.listFiles()){
            if(folder.isDirectory()){
                readCorpus(folder);
            }
            else{
                try {
                    doc = Jsoup.parse(folder,"UTF8");
//                    String body = doc.body().text();
                    //for (Element sentence : doc.getElementsByTag("DOCNO"))
                    //   System.out.print(sentence);
                    // System.out.println(doc.getElementsByTag("DOCNO").text());
                    Elements docs = doc.getElementsByTag("doc");
                    for (Element fileDoc :
                            docs) {
                        numOfCorpusFiles++;
                        numOfParsedDocs++;
                        IR.Document document = new IR.Document(fileDoc);
                        prsNums.parse(document);
                        if(numOfParsedDocs > numberOfDocsToPost)
                        {
                            myIndexer.enqueue(prsNums.getCopyOfNumbersInText());
                            prsNums.clearDic();
                            numOfParsedDocs = 0;
                        }
//                        parseDates pDate = new parseDates();
//                        pDate.parse(document);
                        //parsePercentage pp = new parsePercentage();
                        //pp.parse(document);
                        //prsNums.parse(document);
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }




}