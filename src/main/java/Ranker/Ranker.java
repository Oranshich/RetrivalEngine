package Ranker;

import IR.DocumentInfo;
import Indexer.*;
import javafx.util.Pair;

import java.util.*;

/**
 * This Class is a Singleton class that is capable of ranking Corpus Documents against a given Query
 */
public class Ranker {


    private static volatile Ranker mInstance;
    private HashMap<String,String> query;
    private boolean stemming = false;

    private Ranker() {
    }

    public static Ranker getInstance() {
        if (mInstance == null) {
            synchronized (Ranker.class) {
                if (mInstance == null) {
                    mInstance = new Ranker();
                }
            }
        }
        return mInstance;
    }


    public void setStemming(boolean stemming) {
        this.stemming = stemming;
    }

    /**
     * This Function is ranking the docs that are related to the term found in the query
     * @param termsAndLinesFromPost HashMap that contains all the terms and the posting file lines related to them
     * @param searchedQuery HashMap that contains all the term and the line with the tf in the query
     * @return ArrayList<docId> which contains the 50 Most Ranked docs by BM25 formula
     */
    public ArrayList<String> rankQueryDocs(HashMap<String,String> termsAndLinesFromPost,HashMap<String,String> searchedQuery) {
        try {
            ArrayList<String> docListRanked = new ArrayList<>();
            query.putAll(searchedQuery);
            HashMap<String, ArrayList<Pair<String, Integer>>> docToTermsInQry = getDocToTerm(termsAndLinesFromPost);
            DocumentIndexer docIndexer = DocumentIndexer.getInstance();
            docIndexer.loadDictionaryFromDisk();
            int M = docIndexer.getSizeOfDictionary();
            double docAvgLength = docIndexer.getAvgLengthOfDoc();
            PriorityQueue<RankedDocument> rankingQueue = new PriorityQueue<>(Comparator.naturalOrder());


            //Ranking All of the Docs using BM25
            for(String docId: docToTermsInQry.keySet())
            {
                int docLength = docIndexer.getLengthOfDoc(docId);
                double sumOfBM25 = 0;
                ArrayList<Pair<String,Integer>> allTermsInDocQuery = docToTermsInQry.get(docId);
                for (Pair<String,Integer> termAndTf: allTermsInDocQuery)
                {
                    int tfInDoc = termAndTf.getValue();
                    int termDf = termsAndLinesFromPost.get(termAndTf.getKey()).split(";").length;
                    double cwq = Double.parseDouble(query.get(termAndTf.getKey()).split("#")[1]);

                    sumOfBM25 += calcBM25(M,docAvgLength,docLength,tfInDoc,termDf,cwq);
                }
                rankingQueue.add(new RankedDocument(docId,sumOfBM25));
            }

            //Get only the 50 highest ranked docs
            docListRanked = get50RankedDocs(rankingQueue);
            return docListRanked;

        }
        catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Iterates over the PriorityQueue and returns only the 50 Highest Ranked Docs
     * or all of them if there are less than 50 in the Q
     * @param rankingQueue Priority Queue which contains all the Ranked Documents Prioritized
     * @return List of the 50 most Ranked Docs
     */
    private ArrayList<String> get50RankedDocs(PriorityQueue<RankedDocument> rankingQueue)
    {
        int count50Docs = 0;
        ArrayList<String> listOfRankedDocs = new ArrayList<>();
        while(!rankingQueue.isEmpty())
        {
            RankedDocument rd = rankingQueue.poll();

            listOfRankedDocs.add(rd.getDocId());
            count50Docs++;

            if(count50Docs > 50)
            {
                break;
            }
        }

        return listOfRankedDocs;

    }

    /**
     *
     * @param corpusSize - The size of the corpus
     * @param docAvgLength - The Average Length of a document in the corpus
     * @param docLength - The length of the Specific Document currently calculating BM25 for
     * @param tfInDoc - The Term Frequency of the specific term in the specific doc
     * @param termDf - The Doc Frequency of the specific term currently calculating
     * @param cwq - The Specific Term Frequency in the Query
     * @return value of the BM25 summation
     */
    private double calcBM25(int corpusSize, double docAvgLength, int docLength, int tfInDoc, int termDf,double cwq) {
        double k = 1.5;
        double b = 0.75;
        double sum = 0;
        sum = cwq*(((k+1)*tfInDoc)/(tfInDoc+k*(1-b+b*docLength/docAvgLength)))*(Math.log((double) (corpusSize+1)/(termDf)));

        return sum;
    }


    /**
     * Creates a HashMap of the docIds to an ArrayList<Pair<Term,TermTF in Doc>>
     * @param termsAndLinesFromPost a HashMap<Term,Line From Posting File*>
     * @return the HashMap that was created
     * line in posting file looks like this, ex':
     * FBIS3-67#1;FBIS4-55333#1;FBIS4-8952#6
     */
    private HashMap<String, ArrayList<Pair<String, Integer>>> getDocToTerm(HashMap<String, String> termsAndLinesFromPost)
    {
        HashMap<String, ArrayList<Pair<String, Integer>>> docToTerm = new HashMap<>();
        for(String term: termsAndLinesFromPost.keySet())
        {
            for(String docParams: termsAndLinesFromPost.get(term).split(";"))
            {
                String docId = docParams.split("#")[0];
                int termTfInDoc = Integer.parseInt(docParams.split("#")[1]);
                if(docToTerm.containsKey(docId))
                {
                    docToTerm.get(docId).add(new Pair<>(term,termTfInDoc));
                }
                else
                {
                    ArrayList<Pair<String,Integer>> newList = new ArrayList<>();
                    Pair<String,Integer> termToTf = new Pair<>(term,termTfInDoc);
                    newList.add(termToTf);
                    docToTerm.put(docId,newList);
                }
            }
        }
        return docToTerm;
    }

    public void resetQuery()
    {
        query = new HashMap<>();
    }


    /**
     * foreach entity in the doc, if it is in the query retrieve it
     * if found 5 entitys done
     * if there are no entity in the query, retrieve 5 entitys by the TF in the doc
     * if there a less than 5 entitys in the Query retrive all of them and add the remaining entity by the TF in the doc
     *
     * TF/NUM_OF_ENTITY
     * TF/DOC_LENGTH
     * TF/TOTAL_TF
     *
     * @param docID docID of the document to search for
     * @return
     */
    public HashMap<String,Double> rankEntitysOfDoc(String docID)
    {
        DocumentIndexer docIndexer = DocumentIndexer.getInstance();
        docIndexer.loadDictionaryFromDisk();
        Indexer myIndexer = Indexer.getInstance();
        myIndexer.loadDictionary(stemming);

        HashMap<String,Double> rankedEntitys = new HashMap<>();

        PriorityQueue<RankedEntity> entitiesPriorirtyQ = new PriorityQueue<RankedEntity>(Comparator.naturalOrder());

        DocumentInfo docInfo = docIndexer.getDocumentInfoOfDoc(docID);
        if(docInfo == null)
        {
            return null;
        }

        //All Entitys in the Doc
        Set<String> docEntitys = docInfo.getAllEntitysInDoc().keySet();
        Set<String> allEntitysInCorpus = myIndexer.getEntitiesInCorpus();

        //Get only the intersection of entities in the corpus
        docEntitys.retainAll(allEntitysInCorpus);

        double docLength = docInfo.getDocLength();

        for (String entity :
                docEntitys) {
            int tf = docInfo.getAllEntitysInDoc().get(entity);
            int totalTf =  myIndexer.getTotalTF(entity);

            double rankOfEntity = (double)(tf)/(totalTf) + (tf)/(docLength) + (double) (tf)/(docEntitys.size());
            RankedEntity entty = new RankedEntity(entity,rankOfEntity,totalTf);

            entitiesPriorirtyQ.add(entty);
        }


        int countEntities = 0;
        while(countEntities < 5 && !entitiesPriorirtyQ.isEmpty())
        {
            RankedEntity re = entitiesPriorirtyQ.poll();
            rankedEntitys.put(re.getEntityTerm(),re.getEntityRank());
            countEntities++;
        }

        return rankedEntitys;
    }



}
