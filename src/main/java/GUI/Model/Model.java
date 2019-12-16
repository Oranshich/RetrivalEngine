package GUI.Model;

import Indexer.Indexer;
import org.apache.commons.io.FileUtils;
import readFile.ReadFile;

import java.io.File;
import java.util.HashMap;
import java.util.Observable;

public class Model extends Observable implements IModel {
    String corpusPath;
    String postingPath;
    private static final int MAX_NUMBER_OF_THREADS = 1;


    @Override
    public void loadDictionary(boolean withStemm) {
        Indexer myIndexer = Indexer.getInstance();
        myIndexer.loadDictionary(withStemm);
    }

    public void startParse(String corpusPath, String postingPath, boolean withStemm){
        Thread[] IndexerThreads = new Thread[MAX_NUMBER_OF_THREADS];
        int indexerIndex = 0;


        for (int i = 0; i < IndexerThreads.length; i++) {
            IndexerThreads[i] = new Thread(Indexer.getInstance());
            IndexerThreads[i].setName("Indexer " + indexerIndex++);
        }

        ReadFile f = new ReadFile(withStemm);
        File corpus = new File(corpusPath);
        long startTime,endTime;
        startTime = System.nanoTime();
        this.corpusPath = corpusPath;
        this.postingPath = postingPath;
        Indexer.getInstance().setPathToPostFiles(postingPath);

        for (int i = 0; i < IndexerThreads.length; i++) {
            System.out.println(IndexerThreads[i].getName() + " has started...");
            IndexerThreads[i].start();
        }

        f.readCorpus(corpus);
        f.stopThreads();



        try{
            for (int i = 0; i < IndexerThreads.length; i++) {

                IndexerThreads[i].join();
                System.out.println(IndexerThreads[i].getName() + " has stopped...");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        Indexer myIndexer = Indexer.getInstance();
        myIndexer.createCorpusDictionary();

        myIndexer.saveCorpusDictionary(withStemm);
        System.out.println("Corpus Size = " + myIndexer.corpusSize());


        endTime = System.nanoTime();
        System.out.println("There are "+ f.numOfCorpusFiles + " files in the corpus and it took: " + (endTime - startTime)/1000000000 + " Seconds to iterate over them all");
        setChanged();
        notifyObservers(1);
    }

    @Override
    public void setReset(String corpusPath,String postingPath) {
        try
        {
            if(corpusPath!=null && postingPath!=null){
                //FileUtils.cleanDirectory(new File(corpusPath));
                FileUtils.cleanDirectory(new File(postingPath));
            }
        }
        catch (Exception e)
        {
            //System.out.println("Could not clean Dirs");
        }
    }

    public HashMap<String,String> getDictionary(){

        Indexer myIndexer = Indexer.getInstance();

        return myIndexer.getCorpusDictionary();
    }


}